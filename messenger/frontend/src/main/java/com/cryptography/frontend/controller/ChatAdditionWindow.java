package com.cryptography.frontend.controller;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public Button exitButton;

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
        exitButton.setOnAction(event -> exit());
    }

    private void createChat() {
        String myId = SessionManager.getInstance().getUserId();
        String myName = SessionManager.getInstance().getUserName();
        NewChatDTO newChatDTO = NewChatDTO.builder()
                .firstUserId(myId)
                .secondUserId(userComboBox.getSelectionModel().getSelectedItem().getId())
                .symmetricCipher(cipherComboBox.getSelectionModel().getSelectedItem().toString())
                .encryptionMode(encryptionModeComboBox.getSelectionModel().getSelectedItem().toString())
                .paddingMode(paddingModeComboBox.getSelectionModel().getSelectedItem().toString())
                .build();

        try {
            ChatClient.addChat(newChatDTO);
            log.info("Создан новый чат {} - {}", myName, userComboBox.getSelectionModel().getSelectedItem().getName());
            UILogger.info("Создан новый чат " + myName + " - " + userComboBox.getSelectionModel().getSelectedItem().getName());

            exit();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void exit() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    private void onContactAdded(UserDTO user) {
        userComboBox.getItems().add(user);
    }
}
