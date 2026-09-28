package br.com.empresa.helpdesk.anexos.application;

import br.com.empresa.helpdesk.anexos.api.AnexoResponse;
import br.com.empresa.helpdesk.anexos.domain.Anexo;
import br.com.empresa.helpdesk.anexos.infra.AnexoRepository;
import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.comentarios.application.ComentarioService;
import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnexoService {
  private final AnexoRepository repository;
  private final ComentarioService comentarios;
  private final ChamadoService chamados;
  private final StorageService storage;
  private final Clock clock;
  private final ConteudoAnexo conteudo;
  private final ScannerAnexo scanner;

  public AnexoService(
      AnexoRepository repository,
      ComentarioService comentarios,
      ChamadoService chamados,
      StorageService storage,
      Clock clock,
      ConteudoAnexo conteudo,
      ScannerAnexo scanner) {
    this.repository = repository;
    this.comentarios = comentarios;
    this.chamados = chamados;
    this.storage = storage;
    this.clock = clock;
    this.conteudo = conteudo;
    this.scanner = scanner;
  }

  @Transactional
  public AnexoResponse enviar(
      Long chamadoId, Long comentarioId, boolean interno, MultipartFile arquivo, Usuario ator) {
    var chamado = chamados.exigirAcesso(chamadoId, ator);
    if (chamado.getStatus() == StatusChamado.RESOLVIDO
        || chamado.getStatus() == StatusChamado.FECHADO) {
      throw new ConflitoChamadoException("Chamado concluído não aceita anexos");
    }
    if (interno && ator.getPerfil() == Perfil.FUNCIONARIO) {
      throw new AccessDeniedException("Anexo interno exclusivo da TI");
    }
    if (comentarioId != null) {
      if (comentarios.visibilidadeDoComentario(comentarioId, chamadoId) != interno) {
        throw new RequisicaoInvalidaException("Visibilidade do anexo deve seguir o comentário");
      }
    }
    if (arquivo == null || arquivo.isEmpty() || arquivo.getSize() > ConteudoAnexo.LIMITE) {
      throw new RequisicaoInvalidaException("Anexo deve ter entre 1 byte e 10 MB");
    }
    String nome = limparNome(arquivo.getOriginalFilename());
    byte[] dados;
    try {
      dados = arquivo.getBytes();
    } catch (IOException ex) {
      throw new RequisicaoInvalidaException("Não foi possível ler o anexo");
    }
    String mime = conteudo.validar(dados, arquivo.getContentType(), nome);
    scanner.verificar(dados, mime);
    String chave = UUID.randomUUID().toString();
    storage.gravar(chave, dados, mime);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) storage.remover(chave);
          }
        });
    return AnexoResponse.de(
        repository.saveAndFlush(
            new Anexo(
                chamadoId,
                comentarioId,
                nome,
                chave,
                mime,
                dados.length,
                ator.getId(),
                interno,
                Instant.now(clock))));
  }

  @Transactional(readOnly = true)
  public PaginaResponse<AnexoResponse> listar(Long chamadoId, Usuario ator, int page, int size) {
    chamados.exigirAcesso(chamadoId, ator);
    if (page < 0 || size < 1 || size > 100) {
      throw new RequisicaoInvalidaException("Paginação inválida: page >= 0 e size entre 1 e 100");
    }
    var ordem = PageRequest.of(page, size, Sort.by("criadoEm").ascending().and(Sort.by("id")));
    var resultado =
        ator.getPerfil() == Perfil.FUNCIONARIO
            ? repository.findByChamadoIdAndInternoFalse(chamadoId, ordem)
            : repository.findByChamadoId(chamadoId, ordem);
    return PaginaResponse.de(resultado.map(AnexoResponse::de));
  }

  @Transactional(readOnly = true)
  public DownloadAnexo baixar(Long id, Usuario ator) {
    Anexo anexo =
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado"));
    chamados.exigirAcesso(anexo.getChamadoId(), ator);
    if (anexo.isInterno() && ator.getPerfil() == Perfil.FUNCIONARIO) {
      throw new RecursoNaoEncontradoException("Anexo não encontrado");
    }
    return new DownloadAnexo(
        anexo.getNomeOriginal(), anexo.getTipoMime(), storage.ler(anexo.getChaveStorage()));
  }

  private String limparNome(String original) {
    if (original == null) throw new RequisicaoInvalidaException("Informe o nome do arquivo");
    String nome = original.replace('\\', '/');
    nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
    if (nome.isBlank() || nome.length() > 255 || nome.contains("\r") || nome.contains("\n")) {
      throw new RequisicaoInvalidaException("Nome do anexo inválido");
    }
    return nome;
  }

  public record DownloadAnexo(String nome, String mime, byte[] dados) {}
}
