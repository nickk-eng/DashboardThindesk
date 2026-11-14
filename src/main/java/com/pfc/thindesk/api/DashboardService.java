package com.pfc.thindesk.api;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final MongoTemplate mongo;
    private static final List<String> RESOLVIDOS = List.of("concluído", "concluido", "concluídos", "concluidos",
            "fechado", "fechados", "concluída", "concluida", "fechada");

    public DashboardService(MongoTemplate mongo) { this.mongo = mongo; }

    public record Indicadores(long total, long abertos, long emAndamento, long resolvidos,
            long outros, double taxaResolucao, long semDataAbertura, long comConsideracoesTecnicas) { }
    public record Distribuicao(String nome, long quantidade, double percentual) { }
    public record Periodo(String periodo, long quantidade) { }
    public record Evolucao(String agrupamento, long semData, List<Periodo> periodos) { }
    public record Pagina(long total, int pagina, int tamanho, List<Document> itens) { }

    public Indicadores indicadores(DashboardFiltro filtro) {
        Document grupo = new Document("_id", null).append("total", new Document("$sum", 1))
                .append("abertos", somaStatus(List.of("aberto", "abertos", "aberta")))
                .append("emAndamento", somaStatus(List.of("em andamento")))
                .append("resolvidos", somaStatus(RESOLVIDOS))
                .append("semDataAbertura", somaCondicao(new Document("$eq", java.util.Arrays.asList("$dataAbertura", null))))
                .append("comConsideracoesTecnicas", somaCondicao(new Document("$ne", List.of("$consideracoesTecnicas", ""))));
        Document dados = first(execute(filtro, List.of(stage("$group", grupo))));
        long total = number(dados, "total"), abertos = number(dados, "abertos"), andamento = number(dados, "emAndamento"),
                resolvidos = number(dados, "resolvidos");
        return new Indicadores(total, abertos, andamento, resolvidos, total - abertos - andamento - resolvidos,
                percentual(resolvidos, total), number(dados, "semDataAbertura"), number(dados, "comConsideracoesTecnicas"));
    }

    public List<Distribuicao> distribuicao(DashboardFiltro filtro, String campo) {
        List<Document> grupos = execute(filtro, List.of(stage("$group", new Document("_id", "$" + campo)
                .append("quantidade", new Document("$sum", 1))),
                stage("$sort", new Document("quantidade", -1).append("_id", 1))));
        long total = grupos.stream().mapToLong(d -> number(d, "quantidade")).sum();
        return grupos.stream().map(d -> new Distribuicao(d.getString("_id"), number(d, "quantidade"),
                percentual(number(d, "quantidade"), total))).toList();
    }

    public Evolucao porPeriodo(DashboardFiltro filtro, String agrupamento) {
        if (!List.of("dia", "mes").contains(agrupamento)) throw new IllegalArgumentException("Agrupamento inválido");
        Document formato = new Document("date", "$dataAbertura")
                .append("format", agrupamento.equals("dia") ? "%Y-%m-%d" : "%Y-%m")
                .append("timezone", DashboardFiltro.ZONA.getId());
        Document facet = new Document("periodos", List.of(
                stage("$match", new Document("dataAbertura", new Document("$ne", null))),
                stage("$group", new Document("_id", new Document("$dateToString", formato))
                        .append("quantidade", new Document("$sum", 1))), stage("$sort", new Document("_id", 1))))
                .append("semData", List.of(stage("$match", new Document("dataAbertura", null)), stage("$count", "total")));
        Document resultado = first(execute(filtro, List.of(stage("$facet", facet))));
        return new Evolucao(agrupamento, number(first(documents(resultado, "semData")), "total"),
                documents(resultado, "periodos").stream()
                        .map(d -> new Periodo(d.getString("_id"), number(d, "quantidade"))).toList());
    }

    public Pagina chamados(DashboardFiltro filtro, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException("Paginação inválida");
        }
        Document facet = new Document("contagem", List.of(stage("$count", "total")))
                .append("itens", List.of(stage("$sort", new Document("dataAbertura", -1).append("_id", 1)),
                        stage("$skip", (long) pagina * tamanho), stage("$limit", tamanho), projection()));
        Document resultado = first(execute(filtro, List.of(stage("$facet", facet))));
        return new Pagina(number(first(documents(resultado, "contagem")), "total"), pagina, tamanho,
                documents(resultado, "itens"));
    }

    public Document chamado(String id) {
        if (!org.bson.types.ObjectId.isValid(id)) throw new IllegalArgumentException("Id inválido");
        List<Document> itens = execute(new DashboardFiltro(null, null, null, null, null, null),
                List.of(stage("$match", new Document("_id", new org.bson.types.ObjectId(id))), projection()));
        if (itens.isEmpty()) throw new RecursoNaoEncontradoException("Chamado não encontrado");
        return itens.getFirst();
    }

    private static Document projection() {
        return stage("$project", new Document("_id", 0).append("id", new Document("$toString", "$_id"))
                .append("descricao", 1).append("status", 1).append("tipo", 1).append("tecnico", 1)
                .append("usuario", 1).append("clienteId", 1).append("consideracoesTecnicas", 1)
                .append("dataAbertura", 1).append("dataConclusao", 1));
    }

    private List<Document> execute(DashboardFiltro filtro, List<Document> etapas) {
        List<Document> pipeline = new ArrayList<>();
        Document campos = new Document();
        campos.append("tipo", texto(new Document("$ifNull", List.of("$tipo", "$Tipo")), "Não informado"));
        campos.append("status", texto("$status", "Não informado"));
        for (String campo : List.of("descricao", "tecnico", "usuario", "clienteId", "consideracoesTecnicas")) {
            campos.append(campo, texto("$" + campo, ""));
        }
        for (String campo : List.of("dataAbertura", "dataConclusao")) {
            campos.append(campo, new Document("$convert", new Document("input", "$" + campo).append("to", "date")
                    .append("onError", null).append("onNull", null)));
        }
        pipeline.add(stage("$addFields", campos));
        pipeline.add(stage("$match", filtro.criteria().getCriteriaObject()));
        pipeline.addAll(etapas);
        List<AggregationOperation> operacoes = pipeline.stream().<AggregationOperation>map(d -> context -> d).toList();
        return mongo.aggregate(Aggregation.newAggregation(operacoes), "chamados", Document.class).getMappedResults();
    }

    private static Document texto(Object entrada, String padrao) {
        Document limpo = new Document("$trim", new Document("input", new Document("$convert",
                new Document("input", entrada).append("to", "string").append("onError", "").append("onNull", ""))));
        return new Document("$cond", List.of(new Document("$eq", List.of(limpo, "")), padrao, limpo));
    }
    private static Document somaStatus(List<String> status) {
        String alternativas = status.stream().map(java.util.regex.Pattern::quote)
                .collect(java.util.stream.Collectors.joining("|"));
        return somaCondicao(new Document("$regexMatch", new Document("input", "$status")
                .append("regex", "^(?:" + alternativas + ")$").append("options", "i")));
    }
    private static Document somaCondicao(Document condicao) {
        return new Document("$sum", new Document("$cond", List.of(condicao, 1, 0)));
    }
    private static Document stage(String operador, Object valor) { return new Document(operador, valor); }
    private static Document first(List<Document> documentos) { return documentos.isEmpty() ? new Document() : documentos.getFirst(); }
    private static List<Document> documents(Document documento, String campo) { return documento.getList(campo, Document.class, List.of()); }
    private static long number(Document documento, String campo) { return ((Number) documento.getOrDefault(campo, 0)).longValue(); }
    private static double percentual(long quantidade, long total) { return total == 0 ? 0 : Math.round(10000.0 * quantidade / total) / 100.0; }
}
