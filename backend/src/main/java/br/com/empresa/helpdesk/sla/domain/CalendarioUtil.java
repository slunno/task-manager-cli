package br.com.empresa.helpdesk.sla.domain;

import java.time.*;
import java.util.List;
import java.util.Set;

public class CalendarioUtil {
  public record Janela(DayOfWeek dia, LocalTime inicio, LocalTime fim) {}

  private final ZoneId zona;
  private final List<Janela> janelas;
  private final Set<LocalDate> feriados;

  public CalendarioUtil(ZoneId zona, List<Janela> janelas, Set<LocalDate> feriados) {
    this.zona = zona;
    this.janelas = List.copyOf(janelas);
    this.feriados = Set.copyOf(feriados);
  }

  public Instant adicionarMinutosUteis(Instant inicio, long minutos) {
    if (minutos < 0) throw new IllegalArgumentException("Minutos úteis negativos");
    ZonedDateTime cursor = inicio.atZone(zona);
    for (int dia = 0; dia < 3660; dia++) {
      LocalDate data = cursor.toLocalDate();
      if (!feriados.contains(data)) {
        for (Janela janela : janelas) {
          if (janela.dia() != data.getDayOfWeek()) continue;
          ZonedDateTime abre = data.atTime(janela.inicio()).atZone(zona);
          ZonedDateTime fecha = data.atTime(janela.fim()).atZone(zona);
          ZonedDateTime comeco = cursor.isAfter(abre) ? cursor : abre;
          if (!comeco.isBefore(fecha)) continue;
          long disponivel = Duration.between(comeco, fecha).toMinutes();
          if (minutos <= disponivel) return comeco.plusMinutes(minutos).toInstant();
          minutos -= disponivel;
          cursor = fecha;
        }
      }
      cursor = data.plusDays(1).atStartOfDay(zona);
    }
    throw new IllegalStateException("Calendário sem expediente suficiente");
  }

  public long minutosUteisEntre(Instant inicio, Instant fim) {
    if (!fim.isAfter(inicio)) return 0;
    ZonedDateTime cursor = inicio.atZone(zona);
    long minutos = 0;
    for (int dia = 0; dia < 3660 && cursor.toInstant().isBefore(fim); dia++) {
      LocalDate data = cursor.toLocalDate();
      if (!feriados.contains(data)) {
        for (Janela janela : janelas) {
          if (janela.dia() != data.getDayOfWeek()) continue;
          Instant abre = data.atTime(janela.inicio()).atZone(zona).toInstant();
          Instant fecha = data.atTime(janela.fim()).atZone(zona).toInstant();
          Instant comeco = cursor.toInstant().isAfter(abre) ? cursor.toInstant() : abre;
          Instant termino = fim.isBefore(fecha) ? fim : fecha;
          if (termino.isAfter(comeco)) minutos += Duration.between(comeco, termino).toMinutes();
        }
      }
      cursor = data.plusDays(1).atStartOfDay(zona);
    }
    return minutos;
  }
}
