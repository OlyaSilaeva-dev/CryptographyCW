package com.cryptography.frontend.controller;

import com.cryptography.frontend.algorithms.RC5.RC5;
import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.apiclient.ChatClient;
import com.cryptography.frontend.apiclient.UsersClient;
import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.NewChatDTO;
import com.cryptography.frontend.dto.UserDTO;
import com.cryptography.frontend.entity.enums.SymmetricCipherEnum;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static javafx.application.Platform.exit;

@Slf4j
public class ChatAdditionWindow {
    @FXML
    public ComboBox<UserDTO> userComboBox;
    @FXML
    public ComboBox<SymmetricCipherEnum> cipherComboBox;
    @FXML
    public ComboBox<EncryptionMode> encryptionModeComboBox;
    @FXML
    public ComboBox<PaddingMode> paddingModeComboBox;
    @FXML
    public Button chatCreationButton;

    @FXML
    private void initialize() {
        cipherComboBox.getItems().addAll(SymmetricCipherEnum.values());
        encryptionModeComboBox.getItems().addAll(EncryptionMode.values());
        paddingModeComboBox.getItems().addAll(PaddingMode.values());

        try {
            String myId = SessionManager.getInstance().getUserId();
            List<UserDTO> allUsers = UsersClient.getUsers();

            List<UserDTO> users = new ArrayList<>();
            for (UserDTO user : allUsers) {
                if (!Objects.equals(user.getId(), myId)) {
                    users.add(user);
                }
            }

            log.debug("список пользователей: {}", users);
            userComboBox.getItems().setAll(users);
        } catch (Exception e) {
            UILogger.error("Ошибка при загрузке списка пользователей!");
        }

        userComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserDTO user) {
                return user == null ? "" : user.getName();
            }

            @Override
            public UserDTO fromString(String string) {
                return userComboBox.getItems().stream()
                        .filter(u -> u.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        userComboBox.setCellFactory(lv -> new ListCell<>() {
           @Override
           protected void updateItem(UserDTO item, boolean empty) {
               super.updateItem(item, empty);
               setText(empty || item == null ? null : item.getName());
           }
        });

        userComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UserDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        chatCreationButton.setOnAction(event -> createChat());
    }

    private void createChat() {
        String myId = SessionManager.getInstance().getUserId();
        String myName = SessionManager.getInstance().getUserName();
        byte[] iv = generateIV(cipherComboBox.getSelectionModel().getSelectedItem(), encryptionModeComboBox.getSelectionModel().getSelectedItem());
        NewChatDTO newChatDTO = NewChatDTO.builder()
                .firstUserId(myId)
                .secondUserId(userComboBox.getSelectionModel().getSelectedItem().getId())
                .symmetricCipher(cipherComboBox.getSelectionModel().getSelectedItem().toString())
                .encryptionMode(encryptionModeComboBox.getSelectionModel().getSelectedItem().toString())
                .paddingMode(paddingModeComboBox.getSelectionModel().getSelectedItem().toString())
                .iv(iv)
                .build();

        try {
            ChatClient.addChat(newChatDTO);
            log.info("Создан новый чат {} - {}", myName, userComboBox.getSelectionModel().getSelectedItem().getName());
            UILogger.info("Создан новый чат " + myName + " - " + userComboBox.getSelectionModel().getSelectedItem().getName());

            this.exit();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void exit() {
        Stage stage = (Stage) chatCreationButton.getScene().getWindow();
        stage.close();
    }

    private void onContactAdded(UserDTO user) {
        userComboBox.getItems().add(user);
    }

    private static byte[] generateIV(SymmetricCipherEnum cipher, EncryptionMode mode) {
        if (!needsIV(mode)) {
            return new byte[0];
        }

        int blockSize = 8;
        byte[] iv = new byte[blockSize];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static boolean needsIV(EncryptionMode mode) {
        return switch (mode) {
            case ECB -> false;
            default -> true;
        };
    }

}
