package com.pfc.thindesk.api;

public record ApiResponse<T>(boolean sucesso, T dados, ApiError erro) {
    public record ApiError(int status, String mensagem) { }
    public static <T> ApiResponse<T> ok(T dados) {
        return new ApiResponse<>(true, dados, null);
    }
    public static ApiResponse<Void> error(int status, String mensagem) {
        return new ApiResponse<>(false, null, new ApiError(status, mensagem));
    }
}
