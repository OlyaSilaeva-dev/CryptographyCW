package com.cryptography.frontend.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UILogger {
    private static final int MAX_LOGS = 1000;
    @Getter
    private static ObservableList<String> logs = FXCollections.observableArrayList();
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void add(String level, String message) {
        String log = String.format("[%s] %-5s %s",
                LocalDateTime.now().format(dateFormat),
                level,
                message);

        Platform.runLater(() -> {
            if (logs.size() >= MAX_LOGS) {
                logs.remove(0);
            }
            logs.add(log);
        });
    }

    public static void debug(String message) {
        add("DEBUG", message);
    }

    public static void info(String message) {
        add("INFO", message);
    }

    public static void warn(String message) {
        add("WARN", message);
    }

    public static void error(String message) {
        add("ERROR", message);
    }
}
