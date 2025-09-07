module com.cryptography.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires spring.web;
    requires spring.core;
    requires spring.websocket;
    requires spring.messaging;

    requires com.fasterxml.jackson.databind;
    requires spring.webflux;
    requires java.net.http;
    requires spring.context;


    opens com.cryptography.frontend to javafx.fxml;
    exports com.cryptography.frontend;
    exports com.cryptography.frontend.entity;
    opens com.cryptography.frontend.entity to javafx.fxml;
    exports com.cryptography.frontend.controller;
    opens com.cryptography.frontend.controller to javafx.fxml;
    exports com.cryptography.frontend.dto;
    opens com.cryptography.frontend.dto to javafx.fxml;
    exports com.cryptography.frontend.apiclient;
    opens com.cryptography.frontend.apiclient to javafx.fxml;
    exports com.cryptography.frontend.stompclient;
    opens com.cryptography.frontend.stompclient to javafx.fxml;
}