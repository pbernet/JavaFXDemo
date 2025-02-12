package com.example.javafxdemo;

import com.example.javafxdemo.service.HttpServiceWithRetry;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.http.HttpResponse;

public class HttpStatusController {
    @FXML
    private TextField statusField;
    @FXML
    private Button sendButton;
    @FXML
    private TextArea logArea;

    private HttpServiceWithRetry<String> httpService;

    @FXML
    public void initialize() {
        statusField.setText("Enter status code (200, 503, etc)");
        httpService = new HttpServiceWithRetry<>(HttpResponse.BodyHandlers.ofString());
        setupServiceHandlers();
    }

    @FXML
    private void handleSendRequest() {
        sendButton.setDisable(true);
        logArea.clear();
        String statusCode = statusField.getText();
        httpService.setUrl("http://httpstat.us/" + statusCode);
        httpService.restart();
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
                    handleSendRequest();
                }
            });
        });

        httpService.messageProperty().addListener((obs, old, newMsg) -> {
            if (newMsg != null) {
                logArea.appendText(newMsg + "\n");
            }
        });
    }
}

