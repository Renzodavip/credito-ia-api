package com.renzochirinos.creditoia.ia;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

final class HttpClientes {

    private static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(5);

    private HttpClientes() {
    }

    /** java.net.http.HttpClient con timeout de conexión y de lectura explícitos. */
    static ClientHttpRequestFactory conTimeout(Duration timeoutLectura) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_CONEXION)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeoutLectura);
        return factory;
    }
}
