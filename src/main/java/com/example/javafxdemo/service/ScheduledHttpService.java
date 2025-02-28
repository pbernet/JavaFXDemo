package com.example.javafxdemo.service;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledHttpService<T> {
    private final HttpResponse.BodyHandler<T> bodyHandler;
    private ScheduledExecutorService scheduler;
    private final StringProperty statusMessage = new SimpleStringProperty("Idle");
    private final BooleanProperty lastRequestSuccessful = new SimpleBooleanProperty(false);
    private final StringProperty lastResponseData = new SimpleStringProperty("");

    private String url;
    private long periodSeconds = 30;

    public ScheduledHttpService(HttpResponse.BodyHandler<T> bodyHandler) {
        this.bodyHandler = bodyHandler;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPeriodSeconds(long periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    public void start() {
        if (scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        scheduler.scheduleAtFixedRate(this::executeRequest, 0, periodSeconds, TimeUnit.SECONDS);
        updateStatus("ScheduledHttpService started. Polling every " + periodSeconds + " seconds");
    }

    public void stop() {
        scheduler.shutdown();
        updateStatus("ScheduledHttpService stopped");
    }

    private void executeRequest() {
        HttpServiceWithRetry<T> httpService = new HttpServiceWithRetry<>(bodyHandler);
        httpService.setUrl(url);

        httpService.setOnSucceeded(event -> {
            T result = httpService.getValue();
            Platform.runLater(() -> {
                lastRequestSuccessful.set(true);
                lastResponseData.set(result != null ? result.toString() : "No data");
                statusMessage.set("Request successful");
            });
        });

        httpService.setOnFailed(event -> {
            Throwable exception = httpService.getException();
            Platform.runLater(() -> {
                lastRequestSuccessful.set(false);
                lastResponseData.set("");
                statusMessage.set("Request failed: " + exception.getMessage());
            });
        });

        // Propagate messages from the retry service
        httpService.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateStatus(newVal);
            }
        });

        httpService.start();
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusMessage.set(message));
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public BooleanProperty lastRequestSuccessfulProperty() {
        return lastRequestSuccessful;
    }

    public StringProperty lastResponseDataProperty() {
        return lastResponseData;
    }
}