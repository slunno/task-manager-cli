package br.com.empresa.helpdesk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HelpdeskApplicationTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void saudePublicaResponde() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void apiPermaneceFechadaAntesDaIdentidade() throws Exception {
    mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }
}
