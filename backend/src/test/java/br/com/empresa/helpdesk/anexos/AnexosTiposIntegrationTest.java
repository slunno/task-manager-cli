package br.com.empresa.helpdesk.anexos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class AnexosTiposIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired CategoriaRepository categorias;

  @ParameterizedTest
  @MethodSource("br.com.empresa.helpdesk.anexos.ArquivosAnexoTeste#amostras")
  void novosTiposSobemEBaixamComCabecalhosEInvalidosRetornam400(
      String ext, String mime, byte[] dados) throws Exception {
    var token = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
    var sessao = (MockHttpSession) token.getRequest().getSession(false);
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .session(sessao)
                .header(
                    "X-CSRF-TOKEN",
                    mapper
                        .readTree(token.getResponse().getContentAsString())
                        .path("token")
                        .asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        Map.of(
                            "email", "anexo-" + ext + "@example.com", "nome", "Pessoa fictícia"))))
        .andExpect(status().isOk());
    long categoria =
        categorias.findByAtivaTrueOrderByNomeAsc().stream()
            .findFirst()
            .orElseGet(() -> categorias.saveAndFlush(new Categoria("Anexos C4")))
            .getId();
    String criado =
        mvc.perform(
                post("/api/v1/chamados")
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "titulo",
                                "Arquivo de diagnóstico",
                                "descricao",
                                "Conteúdo fictício de diagnóstico para teste",
                                "categoriaId",
                                categoria))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long chamado = mapper.readTree(criado).path("id").asLong();
    String enviado =
        mvc.perform(
                multipart("/api/v1/chamados/" + chamado + "/anexos")
                    .file(new MockMultipartFile("arquivo", "diagnostico." + ext, mime, dados))
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long anexo = mapper.readTree(enviado).path("id").asLong();
    mvc.perform(get("/api/v1/anexos/" + anexo + "/download").session(sessao))
        .andExpect(status().isOk())
        .andExpect(content().bytes(dados))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(
            header()
                .string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    mvc.perform(
            multipart("/api/v1/chamados/" + chamado + "/anexos")
                .file(
                    new MockMultipartFile(
                        "arquivo", "falso." + ext, mime, new byte[] {77, 90, 0, 0}))
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao)))
        .andExpect(status().isBadRequest());
  }

  private String csrf(MockHttpSession sessao) throws Exception {
    return mapper
        .readTree(
            mvc.perform(get("/api/v1/auth/csrf").session(sessao))
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("token")
        .asText();
  }
}
