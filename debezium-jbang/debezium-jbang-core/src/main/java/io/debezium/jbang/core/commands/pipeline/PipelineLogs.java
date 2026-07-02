/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.pipeline;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.debezium.jbang.core.DebeziumJBangMain;
import io.debezium.jbang.core.commands.DebeziumCommand;
import io.debezium.jbang.core.commands.PlatformFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "logs", description = "Retrieve logs for a pipeline", sortOptions = false)
public class PipelineLogs extends DebeziumCommand {

    @CommandLine.Parameters(index = "0", description = "Pipeline id")
    Long id;

    @CommandLine.Option(names = { "--stream" }, description = "Stream logs in real-time via WebSocket")
    boolean stream;

    @CommandLine.Mixin
    PlatformFactory platformFactory;

    public PipelineLogs(DebeziumJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        if (stream) {
            streamLogs();
        }
        else {
            println(platformFactory.pipeline().getLogs(id));
        }
        return 0;
    }

    private void streamLogs() throws Exception {
        String baseUrl = platformFactory.resolvedApiUrl().toString();
        URI wsUri = URI.create(baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                + "/api/pipelines/" + id + "/logs/stream");

        CountDownLatch latch = new CountDownLatch(1);
        HttpClient client = HttpClient.newHttpClient();
        WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(wsUri, new LogStreamListener(this::print, latch)).join();
        ws.request(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
            latch.countDown();
        }));

        latch.await();
    }

    static final class LogStreamListener implements WebSocket.Listener {

        private final Consumer<String> output;
        private final CountDownLatch latch;

        LogStreamListener(Consumer<String> output, CountDownLatch latch) {
            this.output = output;
            this.latch = latch;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            output.accept(data.toString());
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            latch.countDown();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            latch.countDown();
        }
    }
}
