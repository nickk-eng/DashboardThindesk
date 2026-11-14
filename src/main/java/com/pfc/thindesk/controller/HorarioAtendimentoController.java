package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/ajustes-horarios", "/api/ajustes-horarios"})
public class HorarioAtendimentoController {
    private final HorarioAtendimentoService service;
    public HorarioAtendimentoController(HorarioAtendimentoService service) { this.service = service; }

    @GetMapping
    public String listarHorarios(Model model) {
        model.addAttribute("horarios", service.listarTodos());
        if (!model.containsAttribute("novoHorario")) model.addAttribute("novoHorario", new HorarioAtendimento());
        model.addAttribute("content", "ajustes-horarios");
        return "layout";
    }
    @PostMapping("/salvar")
    public String salvarHorario(@ModelAttribute("novoHorario") HorarioAtendimento horario,
            Model model, RedirectAttributes flash) {
        try {
            horario.setId(null);
            service.salvar(horario);
            flash.addFlashAttribute("mensagem", "Horário salvo com sucesso.");
            return "redirect:/ajustes-horarios";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("erro", exception.getMessage());
            return listarHorarios(model);
        }
    }
    @PostMapping("/deletar/{id}")
    public String deletarHorario(@PathVariable String id, RedirectAttributes flash) {
        service.deletar(id);
        flash.addFlashAttribute("mensagem", "Horário excluído.");
        return "redirect:/ajustes-horarios";
    }
}
