package com.pfc.thindesk;

import com.pfc.SecurityConfig;
import com.pfc.thindesk.controller.ConfiguracaoController;
import com.pfc.thindesk.controller.HorarioAtendimentoController;
import com.pfc.thindesk.entity.ConfiguracaoItem;
import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.repository.ConfiguracaoItemRepository;
import com.pfc.thindesk.repository.HorarioAtendimentoRepository;
import com.pfc.thindesk.service.ConfiguracaoService;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ConfiguracaoController.class, HorarioAtendimentoController.class})
@Import({SecurityConfig.class, ConfiguracaoService.class, HorarioAtendimentoService.class})
class ConfiguracaoWebTest {
    @Autowired MockMvc mvc;
    @MockBean ConfiguracaoItemRepository cadastros;
    @MockBean HorarioAtendimentoRepository horarios;

    private ConfiguracaoItem item(String categoria) {
        ConfiguracaoItem item = new ConfiguracaoItem();
        item.setId("cadastro-1");
        item.setCategoria(categoria);
        item.setNome("Cadastro de teste");
        item.setDescricao("Descrição de teste");
        return item;
    }

    @BeforeEach void defaults() {
        when(cadastros.findByCategoriaOrderByNomeAsc(anyString())).thenReturn(List.of());
        when(horarios.findAll()).thenReturn(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tecnicos", "tipo-chamado", "recursos"})
    void paginasDoMenuRenderizamCadastros(String categoria) throws Exception {
        when(cadastros.findByCategoriaOrderByNomeAsc(categoria)).thenReturn(List.of(item(categoria)));
        mvc.perform(get("/" + categoria).with(user("admin"))).andExpect(status().isOk())
                .andExpect(view().name("layout"))
                .andExpect(content().string(containsString("id=\"sidebar\"")))
                .andExpect(content().string(containsString("Cadastro de teste")))
                .andExpect(content().string(containsString("/" + categoria + "/salvar")))
                .andExpect(content().string(not(containsString("canva.com"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tecnicos", "tipo-chamado", "recursos"})
    void salvarPersisteNaCategoriaDaRota(String categoria) throws Exception {
        mvc.perform(post("/" + categoria + "/salvar").with(user("admin"))
                .param("nome", "  Novo cadastro  ").param("descricao", "Descrição")
                .param("categoria", "outra-categoria"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/" + categoria));
        verify(cadastros).save(argThat(item -> item.getCategoria().equals(categoria)
                && item.getNome().equals("Novo cadastro") && item.getId() == null));
    }

    @Test void nomeVazioReexibeErroSemSalvar() throws Exception {
        mvc.perform(post("/tecnicos/salvar").with(user("admin")).param("nome", " "))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Informe um nome")));
        verify(cadastros, never()).save(any());
    }

    @Test void editarCarregaEAtualizaRegistro() throws Exception {
        when(cadastros.findByIdAndCategoria("cadastro-1", "tecnicos"))
                .thenReturn(Optional.of(item("tecnicos")));
        mvc.perform(get("/tecnicos").param("editar", "cadastro-1").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Editar cadastro")));
        mvc.perform(post("/tecnicos/salvar").with(user("admin")).param("id", "cadastro-1")
                .param("nome", "Nome atualizado"))
                .andExpect(redirectedUrl("/tecnicos"));
        verify(cadastros).save(argThat(item -> item.getId().equals("cadastro-1")
                && item.getNome().equals("Nome atualizado")));
    }

    @Test void excluirRemoveSomenteRegistroDaCategoria() throws Exception {
        ConfiguracaoItem item = item("recursos");
        when(cadastros.findByIdAndCategoria("cadastro-1", "recursos")).thenReturn(Optional.of(item));
        mvc.perform(post("/recursos/excluir/cadastro-1").with(user("admin")))
                .andExpect(redirectedUrl("/recursos"));
        verify(cadastros).delete(item);
    }

    @Test void registroDeOutraCategoriaRetorna404() throws Exception {
        when(cadastros.findByIdAndCategoria("cadastro-1", "tecnicos")).thenReturn(Optional.empty());
        mvc.perform(post("/tecnicos/excluir/cadastro-1").with(user("admin"))).andExpect(status().isNotFound());
        verify(cadastros, never()).delete(any());
    }

    @Test void horariosRenderizamFormularioEValores() throws Exception {
        when(horarios.findAll()).thenReturn(List.of(new HorarioAtendimento("horario-1", "TI", "Segunda-feira", "08:00", "18:00")));
        mvc.perform(get("/ajustes-horarios").with(user("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Segunda-feira")))
                .andExpect(content().string(containsString("08:00")))
                .andExpect(content().string(containsString("/ajustes-horarios/salvar")))
                .andExpect(content().string(containsString("id=\"sidebar\"")));
    }

    @Test void salvarHorarioValidoPersisteCamposDoFormulario() throws Exception {
        mvc.perform(post("/ajustes-horarios/salvar").with(user("admin"))
                .param("setor", "TI").param("diaSemana", "Segunda-feira")
                .param("horarioInicio", "08:00").param("horarioFim", "18:00"))
                .andExpect(redirectedUrl("/ajustes-horarios"));
        verify(horarios).save(argThat(h -> "TI".equals(h.getSetor()) && "08:00".equals(h.getHorarioInicio())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"07:00", "08:00", "invalido"})
    void horariosInvalidosReexibemMensagem(String fim) throws Exception {
        mvc.perform(post("/ajustes-horarios/salvar").with(user("admin"))
                .param("setor", "TI").param("diaSemana", "Segunda-feira")
                .param("horarioInicio", "08:00").param("horarioFim", fim))
                .andExpect(status().isOk()).andExpect(model().attributeExists("erro"))
                .andExpect(content().string(containsString("alert-danger")));
        verify(horarios, never()).save(any());
    }

    @Test void excluirHorarioRedirecionaParaTela() throws Exception {
        mvc.perform(post("/ajustes-horarios/deletar/horario-1").with(user("admin")))
                .andExpect(redirectedUrl("/ajustes-horarios"));
        verify(horarios).deleteById("horario-1");
    }

    @Test void paginasExigemLogin() throws Exception {
        mvc.perform(get("/tecnicos")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void erroRenderizaMensagemLocalSemCanva() throws Exception {
        mvc.perform(get("/error").accept("text/html").with(user("admin"))
                .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Página não encontrada")))
                .andExpect(content().string(not(containsString("iframe"))));
    }
}
