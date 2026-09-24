package br.com.empresa.helpdesk.chamados.application;

import br.com.empresa.helpdesk.admin.application.CategoriaService;
import br.com.empresa.helpdesk.chamados.api.ChamadoMapper;
import br.com.empresa.helpdesk.chamados.api.ChamadoResponse;
import br.com.empresa.helpdesk.chamados.api.CriarChamadoRequest;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChamadoService {
  private static final Set<String> ORDENACOES = Set.of("criadoEm", "numero");
  private final ChamadoRepository repository;
  private final CategoriaService categorias;
  private final UsuarioService usuarios;
  private final NumeroChamadoService numeros;
  private final ChamadoMapper mapper;
  private final Clock clock;

  public ChamadoService(
      ChamadoRepository repository,
      CategoriaService categorias,
      UsuarioService usuarios,
      NumeroChamadoService numeros,
      ChamadoMapper mapper,
      Clock clock) {
    this.repository = repository;
    this.categorias = categorias;
    this.usuarios = usuarios;
    this.numeros = numeros;
    this.mapper = mapper;
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
    if (page < 0 || size < 1 || size > 100) {
      throw new RequisicaoInvalidaException("Paginação inválida: page >= 0 e size entre 1 e 100");
    }
    String[] partes = sort.split(",", -1);
    if (partes.length != 2 || !ORDENACOES.contains(partes[0])) {
      throw new RequisicaoInvalidaException("Ordenação permitida: criadoEm ou numero");
    }
    Sort.Direction direcao;
    if ("asc".equalsIgnoreCase(partes[1])) {
      direcao = Sort.Direction.ASC;
    } else if ("desc".equalsIgnoreCase(partes[1])) {
      direcao = Sort.Direction.DESC;
    } else {
      throw new RequisicaoInvalidaException("Direção permitida: asc ou desc");
    }
    var pageable =
        PageRequest.of(
            page, size, Sort.by(direcao, partes[0]).and(Sort.by(Sort.Direction.DESC, "id")));
    return PaginaResponse.de(
        repository.findBySolicitanteId(ator.getId(), pageable).map(mapper::paraResponse));
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
}
