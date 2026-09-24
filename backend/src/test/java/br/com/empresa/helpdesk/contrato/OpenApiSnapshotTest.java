package br.com.empresa.helpdesk.contrato;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"dev", "test"})
class OpenApiSnapshotTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;

  @Test
  void geraContratoOpenApi() throws Exception {
    String contrato =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(contrato).contains("/api/v1/chamados", "/api/v1/categorias");
    ObjectNode documento = (ObjectNode) mapper.readTree(contrato);
    documento.remove("servers");
    Files.writeString(
        Path.of("target", "openapi.json"),
        mapper.writeValueAsString(documento) + System.lineSeparator());
  }
}
