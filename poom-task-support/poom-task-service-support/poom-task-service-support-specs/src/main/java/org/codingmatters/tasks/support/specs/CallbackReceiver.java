package org.codingmatters.tasks.support.specs;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Point de rappel des tâches (callback) : serveur HTTP local qui enregistre les notifications postées par les
 * handlers et répond 204, ou le code choisi par un scénario.
 */
public final class CallbackReceiver {

    /** Notification reçue : chemin appelé, en-têtes {@code status} et {@code result}, corps JSON. */
    public record Callback(String path, String status, String result, String body) {}

    private final List<Callback> received = new CopyOnWriteArrayList<>();
    private final Undertow server;
    private final int port;
    private volatile int responseCode = 204;

    CallbackReceiver() {
        this.server = Undertow.builder()
                .addHttpListener(0, "localhost")
                .setHandler(this::receive)
                .build();
        this.server.start();
        this.port = ((InetSocketAddress) this.server.getListenerInfo().get(0).getAddress()).getPort();
    }

    /** URL de rappel à donner à la création d'une tâche. */
    public String url(String name) {
        return "http://localhost:" + this.port + "/callbacks/" + name;
    }

    public List<Callback> received() {
        return List.copyOf(this.received);
    }

    public void respondWith(int code) {
        this.responseCode = code;
    }

    void reset() {
        this.received.clear();
        this.responseCode = 204;
    }

    private void receive(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullBytes((ex, body) -> {
            this.received.add(new Callback(
                    ex.getRequestPath(),
                    ex.getRequestHeaders().getFirst("status"),
                    ex.getRequestHeaders().getFirst("result"),
                    new String(body, StandardCharsets.UTF_8)));
            ex.setStatusCode(this.responseCode);
            ex.endExchange();
        });
    }
}
