package com.pfc.thindesk.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(basePackages = "com.pfc.thindesk.api")
public class DashboardApiAdvice {
    private static final Logger log = LoggerFactory.getLogger(DashboardApiAdvice.class);

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "Parâmetros inválidos. Use datas AAAA-MM-DD, agrupamento dia/mês (mes) e limites válidos."));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> internalError(Exception exception) {
        log.error("Falha ao consultar dashboard", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.error(500,
                "Não foi possível consultar os dados. Tente novamente mais tarde."));
    }
}
