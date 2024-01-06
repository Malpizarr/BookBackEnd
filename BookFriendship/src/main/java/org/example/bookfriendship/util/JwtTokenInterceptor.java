package org.example.bookfriendship.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class JwtTokenInterceptor implements ClientHttpRequestInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtTokenInterceptor(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpServletRequest servletRequest =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String jwtToken = jwtTokenUtil.obtenerJwtDeLaSolicitud(servletRequest);
        if (jwtToken != null) {
            request.getHeaders().set("Authorization", "Bearer " + jwtToken);
        }
        return execution.execute(request, body);
    }
}

