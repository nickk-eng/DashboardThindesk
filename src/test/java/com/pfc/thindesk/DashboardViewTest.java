package com.pfc.thindesk;

import com.pfc.SecurityConfig;
import com.pfc.thindesk.controller.HomeController;
import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.ClienteService;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class DashboardViewTest {
    @Autowired MockMvc mvc;
    @MockBean ChamadoService chamados;
    @MockBean ClienteService clientes;
    @MockBean HorarioAtendimentoService horarios;

    @Test void dashboardRenderizaNoLayoutComSidebar() throws Exception {
        mvc.perform(get("/dashboard").with(user("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dashboard\"")))
                .andExpect(content().string(containsString("id=\"sidebar\"")))
                .andExpect(content().string(containsString("/js/dashboard.js")))
                .andExpect(content().string(containsString("Considerações técnicas")));
    }

    @Test void homeContinuaExibindoBoasVindas() throws Exception {
        mvc.perform(get("/").with(user("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bem-vindo ao")))
                .andExpect(content().string(not(containsString("/js/dashboard.js"))));
    }
}
