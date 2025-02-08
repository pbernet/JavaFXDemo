package com.example.javafxdemo;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpServiceWithRetry<T> extends Service<T> {
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private String url;
    private int retryCount = 0;
    private final HttpResponse.BodyHandler<T> bodyHandler;

    public HttpServiceWithRetry(HttpResponse.BodyHandler<T> bodyHandler) {
        this.bodyHandler = bodyHandler;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected Task<T> createTask() {
        return new Task<T>() {
            @Override
            protected T call() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                while (true) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .build();

                        HttpResponse<T> response =
                                client.send(request, bodyHandler);

                        if (response.statusCode() >= 500) {
                            throw new IOException("Server error: " + response.statusCode());
                        }

                        return response.body();

                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount >= MAX_RETRIES) {
                            throw new RuntimeException("Failed after " + MAX_RETRIES + " attempts", e);
                        }
                        updateMessage("Retry attempt " + retryCount + " of " + MAX_RETRIES);
                        Thread.sleep(RETRY_DELAY.toMillis());
                    }
                }
            }
        };
    }
}

