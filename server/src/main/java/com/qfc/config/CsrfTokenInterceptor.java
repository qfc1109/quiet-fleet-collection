package com.qfc.config;

import com.qfc.common.ApiException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CsrfTokenInterceptor implements HandlerInterceptor {

    public static final String CSRF_HEADER = "X-CSRF-Token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isSafeMethod(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        String expectedToken = session == null ? "" : (String) session.getAttribute(SessionKeys.CSRF_TOKEN);
        String actualToken = request.getHeader(CSRF_HEADER);
        if (!StringUtils.hasText(expectedToken) || !expectedToken.equals(actualToken)) {
            throw new ApiException("CSRF_TOKEN_INVALID", "请求安全校验失败", HttpServletResponse.SC_FORBIDDEN);
        }
        return true;
    }

    private boolean isSafeMethod(String method) {
        return HttpMethod.GET.matches(method)
            || HttpMethod.HEAD.matches(method)
            || HttpMethod.OPTIONS.matches(method)
            || HttpMethod.TRACE.matches(method);
    }
}
