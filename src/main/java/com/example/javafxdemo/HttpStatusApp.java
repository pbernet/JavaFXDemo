package com.example.javafxdemo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.http.HttpResponse;

public class HttpStatusApp extends Application {
    private TextArea logArea;
    private TextField statusField;
    private Button sendButton;
    private HttpServiceWithRetry<String> httpService;  // Add type parameter

    @Override
    public void start(Stage stage) {
        // UI Setup
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        statusField = new TextField("200");
        statusField.setText("Enter status code (200, 503, etc)");

        sendButton = new Button("Send Request");
        logArea = new TextArea();
        logArea.setEditable(false);

        root.getChildren().addAll(
                new Label("Status Code:"),
                statusField,
                sendButton,
                logArea
        );

        // Updated service initialization with BodyHandler
        httpService = new HttpServiceWithRetry<>(HttpResponse.BodyHandlers.ofString());
        setupServiceHandlers();
        sendButton.setOnAction(e -> sendRequest());

        Scene scene = new Scene(root, 400, 400);
        stage.setTitle("HTTP Status Tester");
        stage.setScene(scene);
        stage.show();
    }

    private void setupServiceHandlers() {
        httpService.setOnSucceeded(e -> {
            String response = httpService.getValue();
            logArea.appendText("Success: " + response + "\n");
            sendButton.setDisable(false);
        });

        httpService.setOnFailed(e -> {
            Throwable ex = httpService.getException();
            logArea.appendText("Failed: " + ex.getMessage() + "\n" + ex.getCause() + "\n");
            sendButton.setDisable(false);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Retry Connection");
            alert.setHeaderText("Maximum retries exceeded. Cause: " + ex.getCause());
            alert.setContentText("Would you like to try again?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    sendRequest();
                }
            });
        });

        httpService.messageProperty().addListener((obs, old, newMsg) -> {
            if (newMsg != null) {
                logArea.appendText(newMsg + "\n");
            }
        });
    }
    private void sendRequest() {
        sendButton.setDisable(true);
        logArea.clear();
        String statusCode = statusField.getText();
        httpService.setUrl("http://httpstat.us/" + statusCode);
        httpService.restart();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

