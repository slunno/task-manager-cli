package br.com.empresa.helpdesk.anexos.infra;

import br.com.empresa.helpdesk.anexos.application.ScannerAnexo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ScannerAnexoConfig {
  @Bean
  @ConditionalOnMissingBean(ScannerAnexo.class)
  ScannerAnexo scannerAnexo() {
    return (dados, mime) -> {};
  }
}
