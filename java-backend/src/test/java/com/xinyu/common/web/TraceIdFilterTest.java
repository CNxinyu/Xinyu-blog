package com.xinyu.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    @Test
    void propagatesSafeRequestIdAndCleansMdc() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(TraceContext.currentTraceId()).isEqualTo("request-123"));

        assertThat(request.getAttribute(TraceContext.REQUEST_ATTRIBUTE)).isEqualTo("request-123");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("request-123");
        assertThat(TraceContext.currentTraceId()).isNotEqualTo("request-123");
    }

    @Test
    void replacesUnsafeRequestIdWithGeneratedId() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "not safe/for logs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String traceId = (String) request.getAttribute(TraceContext.REQUEST_ATTRIBUTE);
        assertThat(traceId).isNotBlank().doesNotContain("/");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceId);
    }
}
