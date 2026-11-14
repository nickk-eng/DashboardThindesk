package com.pfc.thindesk.api;

import java.time.LocalDate;
import java.util.List;
import org.bson.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {
    private final DashboardService service;
    public DashboardApiController(DashboardService service) { this.service = service; }

    @ModelAttribute
    public DashboardFiltro filtro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String status, @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String cliente, @RequestParam(required = false) String busca) {
        return new DashboardFiltro(dataInicio, dataFim, status, tipo, cliente, busca);
    }

    @GetMapping("/indicadores")
    public ApiResponse<DashboardService.Indicadores> indicadores(@ModelAttribute DashboardFiltro filtro) {
        return ApiResponse.ok(service.indicadores(filtro));
    }
    @GetMapping("/por-periodo")
    public ApiResponse<DashboardService.Evolucao> periodo(@ModelAttribute DashboardFiltro filtro,
            @RequestParam(defaultValue = "dia") String agrupamento) {
        return ApiResponse.ok(service.porPeriodo(filtro, agrupamento));
    }
    @GetMapping("/situacao")
    public ApiResponse<List<DashboardService.Distribuicao>> situacao(@ModelAttribute DashboardFiltro filtro) {
        return ApiResponse.ok(service.distribuicao(filtro, "status"));
    }
    @GetMapping("/tipos")
    public ApiResponse<List<DashboardService.Distribuicao>> tipos(@ModelAttribute DashboardFiltro filtro) {
        return ApiResponse.ok(service.distribuicao(filtro, "tipo"));
    }
    @GetMapping("/chamados")
    public ApiResponse<DashboardService.Pagina> chamados(@ModelAttribute DashboardFiltro filtro,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return ApiResponse.ok(service.chamados(filtro, pagina, tamanho));
    }
    @GetMapping("/chamados/{id}")
    public ApiResponse<Document> chamado(@PathVariable String id) { return ApiResponse.ok(service.chamado(id)); }
}
