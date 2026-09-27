package br.com.empresa.helpdesk.usuarios.application;

import br.com.empresa.helpdesk.admin.application.SetorService;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdminService {
  private static final String EMAIL_ANONIMO = "anonimo@helpdesk.invalid";
  private final UsuarioRepository repository;
  private final SetorService setores;

  public UsuarioAdminService(UsuarioRepository repository, SetorService setores) {
    this.repository = repository;
    this.setores = setores;
  }

  @Transactional(readOnly = true)
  public Page<Usuario> listar(int pagina, int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100)
      throw new RequisicaoInvalidaException("Paginação inválida");
    return repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("nome", "id")));
  }

  @Transactional
  public Usuario atualizar(
      Long id, String nome, String email, Perfil perfil, boolean ativo, Long setorId) {
    Usuario usuario =
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    if (EMAIL_ANONIMO.equalsIgnoreCase(usuario.getEmail()))
      throw new RequisicaoInvalidaException("Conta técnica de retenção não pode ser alterada");
    validar(nome, email, perfil);
    String normalizado = email.trim().toLowerCase(Locale.ROOT);
    repository
        .findByEmailIgnoreCase(normalizado)
        .filter(outro -> !outro.getId().equals(id))
        .ifPresent(
            outro -> {
              throw new RequisicaoInvalidaException("E-mail já cadastrado");
            });
    protegerUltimoAdmin(usuario, perfil, ativo);
    if (setorId != null && !setorId.equals(usuario.getSetorId())) setores.exigirAtivo(setorId);
    usuario.atualizarCadastro(nome.trim(), normalizado, perfil, ativo);
    usuario.atribuirSetor(setorId);
    return repository.save(usuario);
  }

  @Transactional
  public RelatorioImportacao importar(byte[] arquivo) {
    if (arquivo.length == 0 || arquivo.length > 1_000_000)
      throw new RequisicaoInvalidaException("O CSV deve ter até 1 MB");
    String conteudo = new String(arquivo, StandardCharsets.UTF_8).replace("\uFEFF", "");
    String[] linhas = conteudo.split("\\R", -1);
    if (linhas.length < 2
        || linhas.length > 1002
        || !linhas[0].trim().equalsIgnoreCase("nome;email;perfil;ativo"))
      throw new RequisicaoInvalidaException(
          "Cabeçalho esperado: nome;email;perfil;ativo (até 1000 registros)");
    List<ErroLinha> erros = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    int importados = 0;
    for (int indice = 1; indice < linhas.length; indice++) {
      String linha = linhas[indice].trim();
      if (linha.isEmpty()) continue;
      String[] campos = linha.split(";", -1);
      int numero = indice + 1;
      if (campos.length != 4 || linha.contains("\"")) {
        erros.add(
            new ErroLinha(numero, "Use quatro colunas separadas por ponto e vírgula, sem aspas"));
        continue;
      }
      try {
        String nome = campos[0].trim();
        String email = campos[1].trim().toLowerCase(Locale.ROOT);
        if (EMAIL_ANONIMO.equals(email))
          throw new RequisicaoInvalidaException("Conta técnica de retenção não pode ser importada");
        Perfil perfil = Perfil.valueOf(campos[2].trim().toUpperCase(Locale.ROOT));
        String ativoTexto = campos[3].trim().toLowerCase(Locale.ROOT);
        if (!ativoTexto.equals("true") && !ativoTexto.equals("false"))
          throw new RequisicaoInvalidaException("Ativo deve ser true ou false");
        boolean ativo = Boolean.parseBoolean(ativoTexto);
        validar(nome, email, perfil);
        if (!vistos.add(email)) throw new RequisicaoInvalidaException("E-mail repetido no arquivo");
        Usuario usuario =
            repository
                .findByEmailIgnoreCase(email)
                .orElseGet(() -> Usuario.novoFuncionario(nome, email));
        if (usuario.getId() != null) protegerUltimoAdmin(usuario, perfil, ativo);
        usuario.atualizarCadastro(nome, email, perfil, ativo);
        repository.save(usuario);
        importados++;
      } catch (IllegalArgumentException ex) {
        erros.add(new ErroLinha(numero, "Perfil inválido"));
      } catch (RequisicaoInvalidaException ex) {
        erros.add(new ErroLinha(numero, ex.getMessage()));
      }
    }
    return new RelatorioImportacao(importados, erros.size(), erros);
  }

  private void protegerUltimoAdmin(Usuario atual, Perfil novoPerfil, boolean ativo) {
    if (atual.getPerfil() == Perfil.TI_ADMIN
        && atual.isAtivo()
        && (novoPerfil != Perfil.TI_ADMIN || !ativo)
        && repository.countByPerfilAndAtivoTrue(Perfil.TI_ADMIN) <= 1)
      throw new RequisicaoInvalidaException("É necessário manter um administrador de TI ativo");
  }

  private void validar(String nome, String email, Perfil perfil) {
    if (nome == null
        || nome.isBlank()
        || nome.trim().length() > 180
        || email == null
        || email.isBlank()
        || email.length() > 254
        || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        || perfil == null) throw new RequisicaoInvalidaException("Nome, e-mail ou perfil inválido");
  }

  public record ErroLinha(int linha, String motivo) {}

  public record RelatorioImportacao(int importados, int rejeitados, List<ErroLinha> erros) {}
}
