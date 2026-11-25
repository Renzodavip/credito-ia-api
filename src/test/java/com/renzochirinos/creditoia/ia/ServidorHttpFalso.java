package com.renzochirinos.creditoia.ia;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servidor HTTP mínimo del JDK para probar los clientes de LLM sin red ni librerías extra.
 * Responde siempre lo mismo y guarda el último request recibido.
 */
final class ServidorHttpFalso implements AutoCloseable {

    record RequestRecibido(String ruta, String cuerpo, String apiKey) {
    }

    private final HttpServer servidor;
    private final AtomicReference<RequestRecibido> ultimo = new AtomicReference<>();

    ServidorHttpFalso(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ultimo.set(new RequestRecibido(
                    intercambio.getRequestURI().getPath(),
                    cuerpo,
                    intercambio.getRequestHeaders().getFirst("x-api-key")));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    RequestRecibido ultimoRequest() {
        return ultimo.get();
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
