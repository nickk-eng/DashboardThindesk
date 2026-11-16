package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.ConfiguracaoItem;
import com.pfc.thindesk.service.ConfiguracaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfiguracaoController {
    private final ConfiguracaoService service;
    public ConfiguracaoController(ConfiguracaoService service) { this.service = service; }

    @GetMapping("/{categoria:tecnicos|tipo-chamado|recursos}")
    public String pagina(@PathVariable String categoria, @RequestParam(required = false) String editar, Model model) {
        if (!model.containsAttribute("item")) model.addAttribute("item", editar == null
                ? new ConfiguracaoItem() : service.buscar(categoria, editar));
        model.addAttribute("categoria", categoria);
        model.addAttribute("titulo", service.titulo(categoria));
        model.addAttribute("itens", service.listar(categoria));
        model.addAttribute("content", "configuracoes");
        return "layout";
    }
    @PostMapping("/{categoria:tecnicos|tipo-chamado|recursos}/salvar")
    public String salvar(@PathVariable String categoria, @ModelAttribute("item") ConfiguracaoItem item,
            Model model, RedirectAttributes flash) {
        try {
            service.salvar(categoria, item);
            flash.addFlashAttribute("mensagem", "Cadastro salvo com sucesso.");
            return "redirect:/" + categoria;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("erro", exception.getMessage());
            return pagina(categoria, null, model);
        }
    }
    @PostMapping("/{categoria:tecnicos|tipo-chamado|recursos}/excluir/{id}")
    public String excluir(@PathVariable String categoria, @PathVariable String id, RedirectAttributes flash) {
        service.excluir(categoria, id);
        flash.addFlashAttribute("mensagem", "Cadastro excluído.");
        return "redirect:/" + categoria;
    }
}
