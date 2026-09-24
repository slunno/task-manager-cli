package br.com.empresa.helpdesk.chamados.application;

import br.com.empresa.helpdesk.admin.application.CategoriaService;
import br.com.empresa.helpdesk.chamados.api.AtualizarChamadoRequest;
import br.com.empresa.helpdesk.chamados.api.ChamadoMapper;
import br.com.empresa.helpdesk.chamados.api.ChamadoResponse;
import br.com.empresa.helpdesk.chamados.api.CriarChamadoRequest;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.domain.ChamadoAlteradoEvent;
import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.chamados.infra.FilaSpecifications;
import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.historico.api.HistoricoResponse;
import br.com.empresa.helpdesk.historico.application.HistoricoService;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChamadoService {
  private static final Set<String> ORDENACOES_MEUS = Set.of("criadoEm", "numero");
  private static final Set<String> ORDENACOES_FILA =
      Set.of("criadoEm", "numero", "atualizadoEm", "prioridade", "status", "prazoResolucao");
  private final ChamadoRepository repository;
  private final CategoriaService categorias;
  private final UsuarioService usuarios;
  private final NumeroChamadoService numeros;
  private final ChamadoMapper mapper;
  private final HistoricoService historico;
  private final ApplicationEventPublisher eventos;
  private final Clock clock;

  public ChamadoService(
      ChamadoRepository repository,
      CategoriaService categorias,
      UsuarioService usuarios,
      NumeroChamadoService numeros,
      ChamadoMapper mapper,
      HistoricoService historico,
      ApplicationEventPublisher eventos,
      Clock clock) {
    this.repository = repository;
    this.categorias = categorias;
    this.usuarios = usuarios;
    this.numeros = numeros;
    this.mapper = mapper;
    this.historico = historico;
    this.eventos = eventos;
    this.clock = clock;
  }

  @Transactional
  public ChamadoResponse criar(CriarChamadoRequest dados, Usuario ator) {
    categorias.exigirAtiva(dados.categoriaId());
    Long solicitanteId = ator.getId();
    Long abertoPorId = null;
    if (ator.getPerfil() == Perfil.FUNCIONARIO) {
      if (dados.solicitanteId() != null) {
        throw new RequisicaoInvalidaException("Solicitante é definido pela sessão");
      }
    } else if (dados.solicitanteId() != null && !dados.solicitanteId().equals(ator.getId())) {
      usuarios
          .buscarAtivoPorId(dados.solicitanteId())
          .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitante não encontrado"));
      solicitanteId = dados.solicitanteId();
      abertoPorId = ator.getId();
    }
    Chamado chamado =
        Chamado.abrir(
            numeros.proximo(),
            dados.titulo(),
            dados.descricao(),
            solicitanteId,
            abertoPorId,
            dados.categoriaId(),
            dados.prioridadeSugerida(),
            Instant.now(clock));
    return mapper.paraResponse(repository.saveAndFlush(chamado));
  }

  @Transactional(readOnly = true)
  public PaginaResponse<ChamadoResponse> meus(Usuario ator, int page, int size, String sort) {
    var pageable = paginar(page, size, sort, ORDENACOES_MEUS);
    return PaginaResponse.de(
        repository.findBySolicitanteId(ator.getId(), pageable).map(mapper::paraResponse));
  }

  @Transactional(readOnly = true)
  public PaginaResponse<ChamadoResponse> fila(
      Usuario ator, FiltroFila filtro, int page, int size, String sort) {
    if (filtro.semResponsavel() && (filtro.responsavelId() != null || filtro.meus())) {
      throw new RequisicaoInvalidaException("Filtros de responsável incompatíveis");
    }
    if (filtro.meus() && filtro.responsavelId() != null) {
      throw new RequisicaoInvalidaException("Escolha meus chamados ou um responsável");
    }
    if (filtro.desde() != null && filtro.ate() != null && !filtro.desde().isBefore(filtro.ate())) {
      throw new RequisicaoInvalidaException("Período inválido");
    }
    if (filtro.texto() != null && filtro.texto().length() > 200) {
      throw new RequisicaoInvalidaException("Texto de busca muito longo");
    }
    List<Long> solicitantesSetor =
        filtro.setorId() == null ? List.of() : usuarios.idsPorSetor(filtro.setorId());
    return PaginaResponse.de(
        repository
            .findAll(
                FilaSpecifications.filtrar(
                    filtro, solicitantesSetor, ator.getId(), Instant.now(clock)),
                paginar(page, size, sort, ORDENACOES_FILA))
            .map(mapper::paraResponse));
  }

  @Transactional(readOnly = true)
  public ChamadoResponse detalhe(Long id, Usuario ator) {
    var encontrado =
        ator.getPerfil() == Perfil.FUNCIONARIO
            ? repository.findByIdAndSolicitanteId(id, ator.getId())
            : repository.findById(id);
    return encontrado
        .map(mapper::paraResponse)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));
  }

  @Transactional
  public ChamadoResponse assumir(Long id, Long version, Usuario ator) {
    Chamado chamado = exigir(id);
    verificarVersion(chamado, version);
    Estado anterior = Estado.de(chamado);
    Instant agora = Instant.now(clock);
    chamado.assumir(ator.getId(), agora);
    repository.saveAndFlush(chamado);
    registrarAlteracoes(anterior, chamado, ator.getId(), agora);
    return mapper.paraResponse(chamado);
  }

  @Transactional
  public ChamadoResponse atualizar(Long id, AtualizarChamadoRequest dados, Usuario ator) {
    Chamado chamado = exigir(id);
    verificarVersion(chamado, dados.version());
    if (dados.responsavelId() != null && Boolean.TRUE.equals(dados.removerResponsavel())) {
      throw new RequisicaoInvalidaException("Informe um responsável ou remova a atribuição");
    }
    if (dados.solucao() != null && dados.status() != StatusChamado.RESOLVIDO) {
      throw new RequisicaoInvalidaException("Solução só é aceita ao resolver o chamado");
    }
    Estado anterior = Estado.de(chamado);
    Instant agora = Instant.now(clock);
    if (dados.responsavelId() != null) {
      Usuario responsavel =
          usuarios
              .buscarAtivoPorId(dados.responsavelId())
              .orElseThrow(() -> new RecursoNaoEncontradoException("Responsável não encontrado"));
      if (responsavel.getPerfil() == Perfil.FUNCIONARIO) {
        throw new RequisicaoInvalidaException("Responsável deve pertencer à TI");
      }
      chamado.atribuir(responsavel.getId(), agora);
    } else if (Boolean.TRUE.equals(dados.removerResponsavel())) {
      chamado.atribuir(null, agora);
    }
    if (dados.categoriaId() != null) {
      categorias.exigirAtiva(dados.categoriaId());
      chamado.alterarCategoria(dados.categoriaId(), agora);
    }
    if (dados.prioridade() != null) chamado.alterarPrioridade(dados.prioridade(), agora);
    if (dados.status() != null) chamado.alterarStatus(dados.status(), dados.solucao(), agora);
    if (anterior.equals(Estado.de(chamado))) {
      throw new RequisicaoInvalidaException("Nenhuma alteração informada");
    }
    repository.saveAndFlush(chamado);
    registrarAlteracoes(anterior, chamado, ator.getId(), agora);
    return mapper.paraResponse(chamado);
  }

  @Transactional(readOnly = true)
  public PaginaResponse<HistoricoResponse> historico(Long id, int page, int size) {
    exigir(id);
    paginar(page, size, "criadoEm,asc", Set.of("criadoEm"));
    return historico.listar(id, page, size);
  }

  private Chamado exigir(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));
  }

  private void verificarVersion(Chamado chamado, Long version) {
    if (!Objects.equals(chamado.getVersion(), version)) {
      throw new ConflitoChamadoException("O chamado foi alterado por outra pessoa");
    }
  }

  private PageRequest paginar(int page, int size, String sort, Set<String> ordenacoes) {
    if (page < 0 || size < 1 || size > 100) {
      throw new RequisicaoInvalidaException("Paginação inválida: page >= 0 e size entre 1 e 100");
    }
    String[] partes = sort.split(",", -1);
    if (partes.length != 2 || !ordenacoes.contains(partes[0])) {
      throw new RequisicaoInvalidaException("Campo de ordenação não permitido");
    }
    Sort.Direction direcao;
    if ("asc".equalsIgnoreCase(partes[1])) {
      direcao = Sort.Direction.ASC;
    } else if ("desc".equalsIgnoreCase(partes[1])) {
      direcao = Sort.Direction.DESC;
    } else {
      throw new RequisicaoInvalidaException("Direção permitida: asc ou desc");
    }
    return PageRequest.of(
        page, size, Sort.by(direcao, partes[0]).and(Sort.by(Sort.Direction.DESC, "id")));
  }

  private void registrarAlteracoes(
      Estado anterior, Chamado chamado, Long usuarioId, Instant agora) {
    registrar(
        chamado,
        usuarioId,
        "responsavel",
        anterior.responsavelId(),
        chamado.getResponsavelId(),
        agora);
    registrar(chamado, usuarioId, "status", anterior.status(), chamado.getStatus(), agora);
    registrar(
        chamado, usuarioId, "prioridade", anterior.prioridade(), chamado.getPrioridade(), agora);
    registrar(
        chamado, usuarioId, "categoria", anterior.categoriaId(), chamado.getCategoriaId(), agora);
  }

  private void registrar(
      Chamado chamado, Long usuarioId, String campo, Object anterior, Object novo, Instant agora) {
    if (Objects.equals(anterior, novo)) return;
    String antes = anterior == null ? null : anterior.toString();
    String depois = novo == null ? null : novo.toString();
    historico.registrar(chamado.getId(), usuarioId, campo, antes, depois, agora);
    eventos.publishEvent(
        new ChamadoAlteradoEvent(chamado.getId(), usuarioId, campo, antes, depois, agora));
  }

  private record Estado(
      Long responsavelId,
      StatusChamado status,
      Prioridade prioridade,
      Long categoriaId,
      String solucao) {
    static Estado de(Chamado chamado) {
      return new Estado(
          chamado.getResponsavelId(),
          chamado.getStatus(),
          chamado.getPrioridade(),
          chamado.getCategoriaId(),
          chamado.getSolucao());
    }
  }
}
