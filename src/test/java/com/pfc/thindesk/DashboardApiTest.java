package com.pfc.thindesk;

import com.pfc.SecurityConfig;
import com.pfc.thindesk.api.*;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DashboardApiController.class, properties = {
        "spring.security.user.name=admin", "spring.security.user.password=teste-dashboard"})
@Import({DashboardService.class, DashboardApiAdvice.class, SecurityConfig.class})
class DashboardApiTest {
    @Autowired MockMvc mvc;
    @MockBean MongoTemplate mongo;

    @BeforeEach void emptyMongo() { results(List.of()); }

    private void results(List<Document> documentos) {
        when(mongo.aggregate(any(Aggregation.class), eq("chamados"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(documentos, new Document()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"indicadores", "por-periodo", "situacao", "tipos", "chamados"})
    void todosEndpointsAceitamFiltros(String endpoint) throws Exception {
        mvc.perform(get("/api/dashboard/" + endpoint).with(user("admin"))
                .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-14")
                .param("status", "Aberto").param("tipo", "Manutenção").param("cliente", "cliente-1")
                .param("busca", ".*"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sucesso").value(true));
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(captor.capture(), eq("chamados"), eq(Document.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document match = pipeline.get(1).get("$match", Document.class);
        List<Document> criterios = match.getList("$and", Document.class);
        assertThat(criterios).hasSize(5);
        Document datas = criterios.getFirst().get("dataAbertura", Document.class);
        assertThat(datas.getDate("$gte").toInstant().toString()).isEqualTo("2026-11-01T03:00:00Z");
        assertThat(datas.getDate("$lt").toInstant().toString()).isEqualTo("2026-11-15T03:00:00Z");
        assertThat(match.toJson()).contains("clienteId", "\\\\Q.*\\\\E", "Manutenção", "Aberto");
        assertThat(pipeline.getFirst().toJson()).contains("$Tipo", "onError", "Não informado");
    }

    @Test void indicadoresVaziosNaoDividemPorZero() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(user("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.total").value(0))
                .andExpect(jsonPath("$.dados.taxaResolucao").value(0));
    }

    @Test void indicadoresCalculamPercentualEOutros() throws Exception {
        results(List.of(new Document("total", 4).append("abertos", 1).append("emAndamento", 1)
                .append("resolvidos", 1).append("semDataAbertura", 2).append("comConsideracoesTecnicas", 1)));
        mvc.perform(get("/api/dashboard/indicadores").with(user("admin")))
                .andExpect(jsonPath("$.dados.taxaResolucao").value(25.0))
                .andExpect(jsonPath("$.dados.outros").value(1)).andExpect(jsonPath("$.dados.semDataAbertura").value(2));
    }

    @Test void situacaoETiposIncluemPercentuais() throws Exception {
        results(List.of(new Document("_id", "Aberto").append("quantidade", 3),
                new Document("_id", "Não informado").append("quantidade", 1)));
        for (String endpoint : List.of("situacao", "tipos")) {
            mvc.perform(get("/api/dashboard/" + endpoint).with(user("admin")))
                    .andExpect(jsonPath("$.dados[0].quantidade").value(3))
                    .andExpect(jsonPath("$.dados[0].percentual").value(75.0))
                    .andExpect(jsonPath("$.dados[1].nome").value("Não informado"));
        }
    }

    @Test void evolucaoMensalSeparaSemData() throws Exception {
        results(List.of(new Document("periodos", List.of(new Document("_id", "2026-11").append("quantidade", 2)))
                .append("semData", List.of(new Document("total", 1)))));
        mvc.perform(get("/api/dashboard/por-periodo").param("agrupamento", "mes").with(user("admin")))
                .andExpect(jsonPath("$.dados.agrupamento").value("mes"))
                .andExpect(jsonPath("$.dados.semData").value(1))
                .andExpect(jsonPath("$.dados.periodos[0].periodo").value("2026-11"));
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(captor.capture(), eq("chamados"), eq(Document.class));
        assertThat(captor.getValue().toString()).contains("%Y-%m", "America/Sao_Paulo");
    }

    @Test void listagemPaginadaIncluiConsideracoes() throws Exception {
        results(List.of(new Document("contagem", List.of(new Document("total", 21)))
                .append("itens", List.of(new Document("id", "675b1fa1d64af05ea59ee7d3")
                        .append("consideracoesTecnicas", "Equipamento revisado")))));
        mvc.perform(get("/api/dashboard/chamados").param("pagina", "1").with(user("admin")))
                .andExpect(jsonPath("$.dados.total").value(21)).andExpect(jsonPath("$.dados.pagina").value(1))
                .andExpect(jsonPath("$.dados.itens[0].consideracoesTecnicas").value("Equipamento revisado"));
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(captor.capture(), eq("chamados"), eq(Document.class));
        assertThat(captor.getValue().toString()).contains("$skip", "20", "$limit");
    }

    @ParameterizedTest
    @ValueSource(strings = {"indicadores?dataInicio=erro", "tipos?dataInicio=2026-11-15&dataFim=2026-11-01",
            "por-periodo?agrupamento=ano", "chamados?pagina=-1", "chamados?tamanho=101", "chamados?pagina=abc",
            "chamados/id-invalido"})
    void parametrosInvalidosRetornam400(String path) throws Exception {
        mvc.perform(get("/api/dashboard/" + path).with(user("admin")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro.status").value(400));
        verifyNoInteractions(mongo);
    }

    @Test void detalheInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/dashboard/chamados/675b1fa1d64af05ea59ee7d3").with(user("admin")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.erro.status").value(404));
    }

    @Test void falhaMongoRetorna500SemExporDetalhes() throws Exception {
        when(mongo.aggregate(any(Aggregation.class), eq("chamados"), eq(Document.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("segredo-interno"));
        mvc.perform(get("/api/dashboard/indicadores").with(user("admin")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.erro.status").value(500))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo-interno"))));
    }

    @Test void apiSemSessaoRetornaJsonSemRedirecionar() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.erro.status").value(401));
    }

    @Test void sessaoDoFormularioAutenticaFetchDaApi() throws Exception {
        var login = mvc.perform(formLogin().user("admin").password("teste-dashboard"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/dashboard/indicadores").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test void basicAutenticaCurl() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(httpBasic("admin", "teste-dashboard")))
                .andExpect(status().isOk());
    }

    @Test void basicInvalidoRetornaJson() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(httpBasic("admin", "senha-incorreta")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro.status").value(401))
                .andExpect(header().doesNotExist("Location"));
        verifyNoInteractions(mongo);
    }

    @Test void mvcContinuaRedirecionandoParaLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
