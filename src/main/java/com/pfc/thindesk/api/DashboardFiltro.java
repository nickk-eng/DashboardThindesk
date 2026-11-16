package com.pfc.thindesk.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.query.Criteria;

public record DashboardFiltro(LocalDate dataInicio, LocalDate dataFim, String status,
        String tipo, String cliente, String busca) {
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    public DashboardFiltro {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Intervalo invertido");
        }
        if (dataFim != null && dataFim.getYear() >= 9999) {
            throw new IllegalArgumentException("Data final fora do intervalo");
        }
        for (String valor : new String[]{status, tipo, cliente, busca}) {
            if (valor != null && valor.length() > 200) throw new IllegalArgumentException("Filtro muito longo");
        }
    }

    public Criteria criteria() {
        List<Criteria> filtros = new ArrayList<>();
        if (dataInicio != null || dataFim != null) {
            Criteria data = Criteria.where("dataAbertura");
            if (dataInicio != null) data.gte(Date.from(dataInicio.atStartOfDay(ZONA).toInstant()));
            if (dataFim != null) data.lt(Date.from(dataFim.plusDays(1).atStartOfDay(ZONA).toInstant()));
            filtros.add(data);
        }
        exact(filtros, "status", status);
        exact(filtros, "tipo", tipo);
        exact(filtros, "clienteId", cliente);
        if (busca != null && !busca.isBlank()) {
            Pattern termo = Pattern.compile(Pattern.quote(busca.trim()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            filtros.add(new Criteria().orOperator(
                    Criteria.where("descricao").regex(termo), Criteria.where("usuario").regex(termo),
                    Criteria.where("tecnico").regex(termo), Criteria.where("consideracoesTecnicas").regex(termo)));
        }
        return filtros.isEmpty() ? new Criteria() : new Criteria().andOperator(filtros);
    }

    private static void exact(List<Criteria> filtros, String campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            filtros.add(Criteria.where(campo).regex(Pattern.compile("^" + Pattern.quote(valor.trim()) + "$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        }
    }
}
