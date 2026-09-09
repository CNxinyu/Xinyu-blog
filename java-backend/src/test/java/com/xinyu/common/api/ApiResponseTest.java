package com.xinyu.common.api;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void serializesSuccessEnvelopeWithTraceId() throws Exception {
        ApiResponse<String> response = new ApiResponse<>(0, "success", "ok", "trace-123");

        String json = jsonMapper.writeValueAsString(response);

        assertThat(json).contains("\"code\":0", "\"message\":\"success\"",
                "\"data\":\"ok\"", "\"traceId\":\"trace-123\"");
    }

    @Test
    void createsFailureWithBusinessErrorCode() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.NOT_FOUND, null);

        assertThat(response.code()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
        assertThat(response.message()).isEqualTo(ErrorCode.NOT_FOUND.getMessage());
    }
}
