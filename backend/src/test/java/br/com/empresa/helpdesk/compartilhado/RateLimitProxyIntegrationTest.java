package br.com.empresa.helpdesk.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;

class RateLimitProxyIntegrationTest {
  @Test
  void valveDoTomcatNaoAceitaCabecalhoDeParNaoConfiavel() throws Exception {
    assertThat(remoteAddrAfterValve("", "192.0.2.55")).isEqualTo("10.0.0.10");
    assertThat(remoteAddrAfterValve("", "192.0.2.56")).isEqualTo("10.0.0.10");
  }

  @Test
  void valveAceitaIpEncaminhadoSomenteQuandoProxyConfigurado() throws Exception {
    assertThat(remoteAddrAfterValve("10\\.0\\.0\\.10", "192.0.2.55")).isEqualTo("192.0.2.55");
  }

  private String remoteAddrAfterValve(String trustedRegex, String forwardedIp) throws Exception {
    var request = mock(Request.class);
    var response = mock(Response.class);
    when(request.getConnector()).thenReturn(mock(org.apache.catalina.connector.Connector.class));
    var address = new AtomicReference<>("10.0.0.10");
    when(request.getRemoteAddr()).thenAnswer(invocation -> address.get());
    when(request.getRemoteHost()).thenReturn("proxy.example.invalid");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("portal.example.invalid");
    when(request.getHeader("x-forwarded-for")).thenReturn(forwardedIp);
    when(request.getHeaders(anyString()))
        .thenAnswer(
            invocation ->
                "x-forwarded-for".equalsIgnoreCase(invocation.getArgument(0))
                    ? java.util.Collections.enumeration(java.util.List.of(forwardedIp))
                    : java.util.Collections.emptyEnumeration());
    when(request.getCoyoteRequest()).thenReturn(new org.apache.coyote.Request());
    doAnswer(
            invocation -> {
              address.set(invocation.getArgument(0));
              return null;
            })
        .when(request)
        .setRemoteAddr(anyString());
    var forwardedAtNextValve = new AtomicReference<String>();
    var next = mock(Valve.class);
    doAnswer(
            invocation -> {
              forwardedAtNextValve.set(request.getRemoteAddr());
              return null;
            })
        .when(next)
        .invoke(request, response);
    var valve = new RemoteIpValve();
    valve.setInternalProxies(trustedRegex);
    valve.setNext(next);
    valve.invoke(request, response);
    return forwardedAtNextValve.get();
  }
}
