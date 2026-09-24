package br.com.empresa.helpdesk.compartilhado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SegurancaConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(Customizer.withDefaults())
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'")))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());
    return http.build();
  }
}
