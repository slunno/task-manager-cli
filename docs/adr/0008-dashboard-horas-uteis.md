# ADR 0008 — Dashboard em horas úteis

## Decisão

O tempo útil de resolução é calculado com `CalendarioUtil` quando a TI resolve o chamado e persistido como minutos. Dashboard agrega esse valor no banco, sem carregar chamados individualmente. Reabertura limpa a métrica; a próxima resolução mede desde a criação até a última resolução com o calendário vigente nesse momento. Trata-se de duração em expediente, incluindo espera pelo solicitante dentro das janelas, e não tempo de trabalho de um agente. Não se recria um calendário histórico inexistente para chamados antigos: seus valores nulos ficam fora da média e a resposta/tela mostram a quantidade sem medição.

Todos os indicadores usam o conjunto de chamados criados no período (dias inclusivos em São Paulo), padrão últimos 30 dias e limite de 366 dias. Percentual de SLA = resolvidos dentro do prazo / resolvidos; resolução sem prazo não conta como cumprimento. Sem resolvidos, percentual é nulo.

## Versionamento

C5 exige alteração de banco antes de C6, embora o prompt reserve V9 ao histórico. Para preservar ordem e não editar migrations aplicadas, V9 adiciona os minutos úteis e V10 criará o trigger. O ADR 0007 permanece reservado ao histórico. V1–V8 ficam intactas.
