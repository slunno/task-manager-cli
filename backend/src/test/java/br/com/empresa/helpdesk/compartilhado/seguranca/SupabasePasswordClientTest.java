package br.com.empresa.helpdesk.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabasePasswordClientTest {
  @Test
  void validaSenhaNoProvedorSemUsarMetadadosParaAutorizar() {
    var builder = RestClient.builder().baseUrl("https://auth.example");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://auth.example/auth/v1/token?grant_type=password"))
        .andExpect(jsonPath("$.password").value("senha-teste"))
        .andRespond(
            withSuccess(
                """
            {"user":{"id":"uid","email":"pessoa@example.com","email_confirmed_at":"2026-09-28T00:00:00Z","user_metadata":{"perfil":"TI_ADMIN"}}}
            """,
                MediaType.APPLICATION_JSON));
    assertThat(
            new SupabasePasswordClient(builder.build())
                .autenticar("pessoa@example.com", "senha-teste"))
        .isEqualTo("pessoa@example.com");
    server.verify();
  }

  @Test
  void rejeitaIdentidadeSemEmailConfirmado() {
    var builder = RestClient.builder().baseUrl("https://auth.example");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://auth.example/auth/v1/token?grant_type=password"))
        .andRespond(
            withSuccess(
                "{\"user\":{\"id\":\"uid\",\"email\":\"pessoa@example.com\"}}",
                MediaType.APPLICATION_JSON));
    assertThatThrownBy(
            () ->
                new SupabasePasswordClient(builder.build())
                    .autenticar("pessoa@example.com", "senha"))
        .isInstanceOf(SupabasePasswordClient.LoginRecusadoException.class);
  }

  @Test
  void erroDoProvedorNaoExpoeRespostaOuCredenciais() {
    var builder = RestClient.builder().baseUrl("https://auth.example");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://auth.example/auth/v1/token?grant_type=password"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("segredo-que-nao-pode-vazar"));
    assertThatThrownBy(
            () ->
                new SupabasePasswordClient(builder.build())
                    .autenticar("pessoa@example.com", "senha"))
        .isInstanceOf(SupabasePasswordClient.LoginRecusadoException.class)
        .hasMessageNotContaining("segredo-que-nao-pode-vazar")
        .hasNoCause();
  }

  @Test
  void rejeitaChaveSecretaEConexaoSemTls() {
    assertThatThrownBy(
            () -> new SupabasePasswordClient("http://auth.example", "sb_publishable_test"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new SupabasePasswordClient("https://auth.example", "sb_secret_test"))
        .isInstanceOf(IllegalStateException.class);
  }
}
