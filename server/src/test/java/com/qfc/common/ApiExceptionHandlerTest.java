package com.qfc.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    @Test
    void unexpectedExceptionsReturnGenericInternalError() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<ApiResponse<Object>> response = handler.handleUnexpectedException(new NullPointerException("secret"));

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("服务器内部错误", response.getBody().getMessage());
    }
}
