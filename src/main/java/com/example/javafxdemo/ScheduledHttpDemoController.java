package com.example.javafxdemo;

import com.example.javafxdemo.service.ScheduledHttpService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScheduledHttpDemoController {

    @FXML
    private TextField urlField;
    @FXML
    private TextField intervalField;
    @FXML
    private Button startStopButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label lastExecutionTimeLabel;
    @FXML
    private TextArea resultTextArea;
    @FXML
    private Circle statusIndicator;
    @FXML
    private ListView<String> historyListView;

    private final ScheduledHttpService<String> httpService;
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean isRunning = false;

    public ScheduledHttpDemoController() {
        httpService = new ScheduledHttpService<>(HttpResponse.BodyHandlers.ofString());
    }

    @FXML
    private void initialize() {
        urlField.setText("http://httpstat.us/random/200,201,500-504");
        intervalField.setText("10");
        historyListView.setItems(historyItems);

        // Bind service properties to UI elements
        httpService.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
            statusLabel.setText(newVal);
            addToHistory("Status: " + newVal);
        });

        httpService.lastRequestSuccessfulProperty().addListener((obs, oldVal, newVal) -> {
            statusIndicator.setFill(newVal ? Color.GREEN : Color.RED);
            String currentTime = LocalDateTime.now().format(timeFormatter);
            lastExecutionTimeLabel.setText(currentTime);

            String result = newVal ? "Success" : "Failed";
            addToHistory("[" + currentTime + "] " + result);
        });

        httpService.lastResponseDataProperty().addListener((obs, oldVal, newVal) -> {
            resultTextArea.setText(newVal);
            if (!newVal.isEmpty()) {
                // If the response is too long, truncate it for the history
                String historyEntry = newVal.length() > 50 ?
                        newVal.substring(0, 47) + "..." :
                        newVal;
                addToHistory("Response: " + historyEntry);
            }
        });
    }

    @FXML
    private void onStartStopClicked() {
        if (isRunning) {
            httpService.stop();
            startStopButton.setText("Start");
            statusIndicator.setFill(Color.GRAY);
            isRunning = false;
        } else {
            httpService.setUrl(urlField.getText());
            try {
                httpService.setPeriodSeconds(Long.parseLong(intervalField.getText()));
                httpService.start();
                startStopButton.setText("Stop");
                isRunning = true;
            } catch (NumberFormatException e) {
                statusLabel.setText("Invalid interval value");
                addToHistory("Error: Invalid interval value");
            }
        }
    }

    private void addToHistory(String entry) {
        // Limit history size to prevent memory issues
        if (historyItems.size() >= 100) {
            historyItems.removeFirst();
        }
        historyItems.add(entry);
        // Auto-scroll to bottom
        historyListView.scrollTo(historyItems.size() - 1);
    }
}