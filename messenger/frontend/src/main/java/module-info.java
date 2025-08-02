module com.cryptography.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires spring.websocket;
    requires spring.messaging;
    requires static lombok;
    requires com.fasterxml.jackson.databind;

    opens com.cryptography.frontend to javafx.fxml;
    exports com.cryptography.frontend;
    exports com.cryptography.frontend.Мусор;
    opens com.cryptography.frontend.Мусор to javafx.fxml;
    exports com.cryptography.frontend.entity;
    opens com.cryptography.frontend.entity to javafx.fxml;
}