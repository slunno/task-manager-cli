package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DestinatariosTi {
  private final UsuarioService usuarios;
  private final String modo;
  private final List<String> lista;

  public DestinatariosTi(
      UsuarioService usuarios,
      @Value("${helpdesk.mail.ti.mode:usuarios}") String modo,
      @Value("${helpdesk.mail.ti.addresses:}") String enderecos) {
    this.usuarios = usuarios;
    this.modo = modo;
    if (!List.of("usuarios", "lista").contains(modo))
      throw new IllegalArgumentException("HELPDESK_MAIL_TI_MODE deve ser usuarios ou lista");
    this.lista =
        Arrays.stream(enderecos.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.toLowerCase(Locale.ROOT))
            .distinct()
            .toList();
    if ("lista".equals(modo)
        && (lista.isEmpty()
            || lista.stream()
                .anyMatch(s -> !s.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || s.length() > 254)))
      throw new IllegalArgumentException(
          "HELPDESK_MAIL_TI_ADDRESSES deve conter e-mails válidos separados por vírgula");
  }

  public List<String> emails() {
    return "lista".equals(modo) ? lista : usuarios.emailsTiAtivos();
  }
}
