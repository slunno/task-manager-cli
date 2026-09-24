package br.com.empresa.helpdesk.compartilhado.config;

import br.com.empresa.helpdesk.compartilhado.seguranca.ProblemaSeguranca;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.application.CorporateOidcUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SegurancaConfig {
  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  @Profile("!prod")
  SecurityFilterChain localSecurityFilterChain(
      HttpSecurity http,
      UsuarioSessaoFilter usuarioFilter,
      SecurityContextRepository contextRepository,
      ObjectMapper mapper)
      throws Exception {
    configurarBase(http, usuarioFilter, contextRepository, mapper);
    return http.build();
  }

  @Bean
  @Profile("prod")
  SecurityFilterChain oidcSecurityFilterChain(
      HttpSecurity http,
      UsuarioSessaoFilter usuarioFilter,
      SecurityContextRepository contextRepository,
      ObjectMapper mapper,
      CorporateOidcUserService oidcUserService)
      throws Exception {
    configurarBase(http, usuarioFilter, contextRepository, mapper);
    http.oauth2Login(
        oauth ->
            oauth
                .userInfoEndpoint(info -> info.oidcUserService(oidcUserService))
                .defaultSuccessUrl("/", true));
    return http.build();
  }

  private void configurarBase(
      HttpSecurity http,
      UsuarioSessaoFilter usuarioFilter,
      SecurityContextRepository contextRepository,
      ObjectMapper mapper)
      throws Exception {
    http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/**",
                        "/api/v1/auth/config",
                        "/api/v1/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/dev/login")
                    .permitAll()
                    .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .securityContext(context -> context.securityContextRepository(contextRepository))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            ProblemaSeguranca.escrever(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Autenticação necessária",
                                mapper))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            ProblemaSeguranca.escrever(
                                response, HttpStatus.FORBIDDEN, "Acesso negado", mapper)))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .logoutSuccessHandler(
                        (request, response, authentication) -> {
                          SecurityContextHolder.clearContext();
                          response.setStatus(HttpStatus.NO_CONTENT.value());
                        }))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'")))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .addFilterBefore(usuarioFilter, AuthorizationFilter.class);
  }
}
