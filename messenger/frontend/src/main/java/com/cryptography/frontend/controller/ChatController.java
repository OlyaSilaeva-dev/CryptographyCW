package com.cryptography.frontend.controller;

import com.cryptography.frontend.algorithms.RC5.RC5;
import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.algorithms.interfaces.SymmetricCipher;
import com.cryptography.frontend.apiclient.ChatClient;
import com.cryptography.frontend.apiclient.MessageSender;
import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.ChatDTO;
import com.cryptography.frontend.dto.UserDTO;
import com.cryptography.frontend.algorithms.DiffieHellman;
import com.cryptography.frontend.algorithms.MacGuffin.MacGuffin;
import com.cryptography.frontend.algorithms.symmetricCipherContext.SymmetricCipherContext;
import com.cryptography.frontend.dto.KeyParams;
import com.cryptography.frontend.entity.ChatMessage;
import com.cryptography.frontend.entity.ReceivedFile;
import com.cryptography.frontend.stompclient.StompClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

import static com.cryptography.frontend.controller.ControllerUtils.*;

@Slf4j
public class ChatController {
    @FXML
    public Button exitButton;
    @FXML
    public Button addChatButton;
    @FXML
    private ListView<ChatDTO> chatsView;
    @FXML
    private ListView<String> listView;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button attachButton;
    @FXML
    private ListView<String> logView;

    private File attachFile;
    private String attachFileName;
    private byte[] encryptedFileData;

    private ChatDTO selectedChat;
    private UserDTO currentUser;
    private UserDTO recipient;

    // Map<chatId, smth>
    private final Map<String, DiffieHellman> diffieHellmanMap = new HashMap<>();
    private final Map<String, SymmetricCipherContext> cipherContexts = new HashMap<>();
    private final Map<String, BigInteger> sharedSecrets = new HashMap<>();

    private final Map<String, List<String>> messageHistory = new HashMap<>();
    private final Map<String, List<ReceivedFile>> receivedFiles = new HashMap<>();

    private boolean isSharedSecretEstablished() {
        return selectedChat != null
                && sharedSecrets.containsKey(selectedChat.getChatId())
                && sharedSecrets.get(selectedChat.getChatId()) != null;
    }

    private void onMessageReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            if (msg.getSenderId().equals(currentUser.getId())) return;

            String chatId = msg.getChatId();

            ChatDTO chat = chatsView.getItems().stream()
                    .filter(c -> c.getChatId().equals(chatId))
                    .findFirst()
                    .orElse(null);

            if (chat == null) {
                UILogger.warn("Сообщение для неизвестного чата " + chatId);
                return;
            }

            UserDTO opponent = chat.getFirstUser().getId().equals(currentUser.getId())
                    ? chat.getSecondUser()
                    : chat.getFirstUser();

            if (!sharedSecrets.containsKey(chat.getChatId())) {
                UILogger.warn("Нет общего секрета для сообщения с " + opponent.getName());
                showAlert(ERR, "Нет общего секрета для сообщения с " + opponent.getName());
                return;
            }

            try {
                SymmetricCipherContext cipherContext = cipherContexts.get(chat.getChatId());
                if (cipherContext == null) {
                    UILogger.error("Нет SymmetricCipherContext для чата " + chat.getChatId());
                    return;
                }

                String displayText;

                if (msg.isFile()) {
                    byte[] decryptedFileData = cipherContext.decrypt(msg.getMessage());
                    ReceivedFile receivedFile = new ReceivedFile(msg.getFileName(), decryptedFileData, opponent.getId());
                    receivedFiles.putIfAbsent(chatId, new ArrayList<>());
                    receivedFiles.get(chatId).add(receivedFile);
                    displayText = opponent.getName() + ": отправил(а) файл " + msg.getFileName();
                    UILogger.debug("Получен файл от " + opponent.getName() + " " + msg.getFileName());
                } else {
                    byte[] decryptedBytes = cipherContext.decrypt(msg.getMessage());
                    displayText = opponent.getName() + ": " + new String(decryptedBytes, StandardCharsets.UTF_8);
                    UILogger.debug("Получено сообщение от " + displayText);
                }

                appendMessage(chatId, displayText);

                ChatDTO selectedChat = chatsView.getSelectionModel().getSelectedItem();
                if (selectedChat != null && selectedChat.getChatId().equals(chatId)) {
                    updateMessageList();
                }

            } catch (Exception e) {
                UILogger.error("Ошибка дешифрования сообщения от" + opponent.getName() + ":" + e.getMessage());
                appendMessage(chatId, opponent.getName() + ": не удалось расшифровать сообщение");
            }
        });
    }

    private void onPublicKeyReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            if (msg.getSenderId().equals(currentUser.getId())) return;

            String chatId = msg.getChatId();
            ChatDTO chat = chatsView.getItems().stream()
                    .filter(chatDTO -> chatDTO.getChatId()
                            .equals(chatId))
                    .findFirst()
                    .orElse(null);

            if (chat == null) {
                UILogger.warn("Чат не найден для получения публичного ключа");
                return;
            }

            DiffieHellman dh = diffieHellmanMap.get(chatId);
            if (dh == null) {
                return;
            }

            UserDTO firstUser = chat.getFirstUser();
            UserDTO from = firstUser.getId().equals(msg.getSenderId()) ? firstUser : chat.getSecondUser();
            byte[] publicKey = msg.getMessage();

            try {
                BigInteger sharedSecret = dh.computeSharedSecret(new BigInteger(1, publicKey));
                UILogger.debug("Установлен общий секрет с " + from.getName() + ": " + sharedSecret);

                sharedSecrets.put(chatId, sharedSecret);

                SymmetricCipherContext context = getSymmetricCipherContextForChat(chat, sharedSecret);
                cipherContexts.put(chatId, context);
                updateMessageList();
            } catch (Exception e) {
                UILogger.error("Ошибка при установлении общего секрета: " + e.getMessage());
            }
        });
    }

    private void onChatAdded(ChatDTO chatDTO) {
        Platform.runLater(() -> {
            chatsView.getItems().add(chatDTO);
            UILogger.debug("Добавлен новый чат с "
                    + chatDTO.getFirstUser().getName() + " и " + chatDTO.getSecondUser().getName());

        });
    }

    private void onChatRemoved(String chatId) {
        Platform.runLater(() -> {
            chatsView.getItems().removeIf(chat -> chat.getChatId().equals(chatId));
            UILogger.debug("Удален чат " + chatId);
        });
    }

    private void appendMessage(String chatId, String message) {
        messageHistory.putIfAbsent(chatId, new ArrayList<>());
        messageHistory.get(chatId).add(message);

        ChatDTO selectedChat = chatsView.getSelectionModel().getSelectedItem();
        if (selectedChat != null && selectedChat.getChatId().equals(chatId)) {
            updateMessageList();
        }
    }

    private void updateMessageList() {
        ChatDTO selectedChat = chatsView.getSelectionModel().getSelectedItem();
        if (selectedChat == null) return;

        String chatId = selectedChat.getChatId();
        List<String> messages = messageHistory.getOrDefault(chatId, new ArrayList<>());
        listView.getItems().setAll(messages);
    }

    public void init(String myId, String myName) throws RuntimeException {
        this.currentUser = UserDTO.builder()
                .id(myId)
                .name(myName)
                .build();

        StompClient.connect(myId, this::onMessageReceived, this::onPublicKeyReceived, userDTO -> {
        }, this::onChatAdded, this::onChatRemoved);

        logView.setItems(UILogger.getLogs());

        try {
            List<ChatDTO> chats = ChatClient.getChats(myId);
            chatsView.getItems().setAll(chats);
            UILogger.info("Список чатов: " + chatsView.getItems());
        } catch (Exception e) {
            UILogger.error(e.getMessage());
        }
        chatsView.setCellFactory(lv -> new ListCell<>() {
            private final HBox hBox = new HBox(10);
            private final Label nameLabel = new Label();
            private final Button deleteButton = new Button("Удалить");

            {
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE);

                hBox.setStyle("-fx-alignment: center-left;");
                hBox.getChildren().addAll(nameLabel, deleteButton);

                deleteButton.setOnAction(e -> {
                    ChatDTO chatDTO = getItem();
                    if (chatDTO != null) {
                        try {
                            ChatClient.removeChat(chatDTO.getChatId());
                        } catch (Exception ex) {
                            UILogger.error(ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(ChatDTO chat, boolean empty) {
                super.updateItem(chat, empty);
                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String myId = SessionManager.getInstance().getUserId();
                    UserDTO opponent = chat.getFirstUser().getId().equals(myId)
                            ? chat.getSecondUser()
                            : chat.getFirstUser();
                    nameLabel.setText(opponent.getName());

                    hBox.setSpacing(10);
                    hBox.setStyle("-fx-alignment: center-left;");
                    setText(null);
                    setGraphic(hBox);
                }
            }
        });

        chatsView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                selectingChatFromChatsView(myId, myName, newVal));

        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox hbox = new HBox(5);
            private final Label label = new Label();
            private final Button downloadBtn = new Button("Скачать");
            private ReceivedFile fileRef;

            {
                hbox.getChildren().addAll(label, downloadBtn);
                downloadBtn.setOnAction(e -> {
                    if (fileRef != null) {
                        downloadFile(fileRef);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.contains("отправил(а) файл")) {
                        label.setText(item);

                        String chatId = (selectedChat != null) ? selectedChat.getChatId() : null;
                        if (chatId != null && receivedFiles.containsKey(chatId)) {
                            List<ReceivedFile> files = receivedFiles.get(chatId);
                            fileRef = files.get(files.size() - 1);
                        }

                        setGraphic(hbox);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
        });

        addChatButton.setOnAction(e -> openChatAdditionWindow());
        sendButton.setOnAction(event -> send(myId));
        attachButton.setOnAction(event -> attachFile());
        exitButton.setOnAction(event -> handleExit());
    }

    private void deleteChat(ChatDTO chatDTO) {
        try {
            ChatClient.removeChat(chatDTO.getChatId());
            chatsView.getItems().remove(chatDTO);
            UILogger.info("Чат " + chatDTO + " успешно удален");
        } catch (Exception e) {
            UILogger.error(e.getMessage());
        }
    }

    private void openChatAdditionWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cryptography/frontend/chat_addition_window.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Добавление чата");
            stage.initOwner(addChatButton.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void selectingChatFromChatsView(String myId, String myName, ChatDTO newVal) {
        if (newVal != null) {
            selectedChat = newVal;
            recipient = myId.equals(newVal.getFirstUser().getId())
                    ? newVal.getSecondUser()
                    : newVal.getFirstUser();

            UILogger.debug("Выбран контакт: " + recipient.getName());
            updateMessageList();

            String recipientId = recipient.getId();
            if (sharedSecrets.containsKey(selectedChat.getChatId())) return;

            try {
                KeyParams keyParams = MessageSender.getKeyParams(myId, recipientId);
                UILogger.debug("Параметры ключа: " + keyParams);

                BigInteger p = new BigInteger(keyParams.getP(), 16);
                BigInteger g = new BigInteger(keyParams.getG(), 16);
                DiffieHellman diffieHellman = new DiffieHellman(p, g);
                diffieHellmanMap.put(newVal.getChatId(), diffieHellman);


                MessageSender.sendPublicKeyMessage(
                        ChatMessage.builder()
                                .chatId(newVal.getChatId())
                                .senderId(myId)
                                .recipientId(recipientId)
                                .message(bigIntToUnsignedBytes(diffieHellman.getPublicKey()))
                                .timestamp(LocalDateTime.now().toString())
                                .build());

                UILogger.debug("Пользователь " + myName + " отправил публичный ключ " + recipient.getName());

                byte[] receivedPublicKey = MessageSender.getPublicKey(newVal.getChatId(), myId);
                UILogger.debug("Получен публичный ключ(redis): " + Arrays.toString(receivedPublicKey) + " от пользователя " + recipient.getName());

                if (receivedPublicKey != null) {
                    BigInteger sharedSecret = diffieHellman.computeSharedSecret(new BigInteger(1, receivedPublicKey));
                    sharedSecrets.put(newVal.getChatId(), sharedSecret);
                    UILogger.debug("Установлен общий секрет (redis) с " + recipient.getName() + " : " + sharedSecret);
                    SymmetricCipherContext symmetricCipherContext = getSymmetricCipherContextForChat(newVal, sharedSecret);
                    cipherContexts.put(newVal.getChatId(), symmetricCipherContext);

                    updateMessageList();
                }
            } catch (Exception e) {
                UILogger.error(e.getMessage());
            }
        }
    }

    private SymmetricCipherContext getSymmetricCipherContextForChat(ChatDTO chat, BigInteger sharedSecret) {
        if (chat == null || sharedSecret == null) throw new IllegalArgumentException("chat or secret is null");
        SymmetricCipher symmetricCipher = switch (chat.getSymmetricCipher()) {
            case "RC5" -> new RC5(32, 16, 16);
            case "MacGuffin" -> new MacGuffin();
            default -> throw new IllegalStateException("Unexpected value: " + chat.getSymmetricCipher());
        };

        return new SymmetricCipherContext(
                symmetricCipher,
                bigIntToUnsignedBytes(sharedSecret),
                EncryptionMode.valueOf(chat.getEncryptionMode()),
                PaddingMode.valueOf(chat.getPaddingMode()),
                chat.getIv()
        );
    }


    private void send(String myId) {
        this.selectedChat = chatsView.getSelectionModel().getSelectedItem();
        if (selectedChat == null) {
            showAlert(ERR, "Выберите получателя");
            return;
        }

        String chatId = selectedChat.getChatId();
        String text = messageField.getText().trim();

        if (!isSharedSecretEstablished()) {
            String recipName = recipient != null ? recipient.getName() : "получатель";
            showAlert(ERR, "Общий секрет с {" + recipName + "} не установлен, обмен ключами не завершён.");
            return;
        }

        try {
            if (attachFile != null) {
                if (encryptedFileData == null) {
                    showAlert(ERR, "Файл не подготовлен к отправке");
                    UILogger.error("Файл не подготовлен к отправке");
                    return;
                }

                ChatMessage fileMsg = ChatMessage.builder()
                        .chatId(chatId)
                        .senderId(myId)
                        .recipientId(recipient.getId())
                        .message(encryptedFileData)
                        .fileName(attachFileName)
                        .isFile(true)
                        .timestamp(LocalDateTime.now().toString())
                        .build();

                MessageSender.sendChatMessage(fileMsg);
                appendMessage(selectedChat.getChatId(), "Вы: отправили файл " + attachFileName);
                UILogger.debug("Файл " + attachFileName + " отправлен пользователю " + recipient.getName());

                resetAttachedFile();
            } else {
                if (text.isEmpty()) return;
                SymmetricCipherContext symmetricCipherContext = cipherContexts.get(chatId);
                ChatMessage msg = ChatMessage.builder()
                        .chatId(chatId)
                        .senderId(myId)
                        .recipientId(recipient.getId())
                        .message(symmetricCipherContext.encrypt(text.getBytes()))
                        .timestamp(LocalDateTime.now().toString())
                        .build();

                MessageSender.sendChatMessage(msg);
                appendMessage(chatId, "Вы: " + text);
                UILogger.debug("Отправлено сообщение: " + text + " пользователю " + recipient.getName());
                messageField.clear();
            }
        } catch (Exception e) {
            UILogger.error("Ошибка отправки: " + e.getMessage());
            showAlert(ERR, "Ошибка отправки сообщения: " + e.getMessage());

            if (attachFile != null) {
                resetAttachedFile();
            }
        }
    }

    private void downloadFile(ReceivedFile file) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить файл");
            fileChooser.setInitialFileName(file.getFileName());
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Downloads"));

            String extension = getFileExtension(file.getFileName());
            if (extension != null) {
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Files (." + extension + ")", "*." + extension));
            }

            Stage stage = (Stage) attachButton.getScene().getWindow();
            File saveFile = fileChooser.showSaveDialog(stage);

            if (saveFile != null) {
                Files.write(saveFile.toPath(), file.getFileData());
                showAlert(SUCCESS, "Файл сохранен: " + saveFile.getName());
                UILogger.debug("Файл " + file.getFileName() + "сохранен в " + saveFile.getAbsolutePath());
            }
        } catch (Exception e) {
            UILogger.error("Ошибка сохранения файла: " + e.getMessage());
            showAlert(ERR, "Ошибка сохранения файла!");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }

    private void resetAttachedFile() {
        this.attachFile = null;
        this.encryptedFileData = null;
        this.attachFileName = null;
        messageField.setPromptText("Введите сообщение...");
    }

    private void attachFile() {
        if (recipient == null) {
            UILogger.error("Получатель не выбран");
            showAlert(ERR, "Сначала выберите получателя!");
            return;
        }

        if (!isSharedSecretEstablished()) {
            UILogger.error("Общий секрет не установлен");
            showAlert(ERR, "Общий секрет не установлен!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для отправки");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        Stage stage = (Stage) attachButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.attachFile = file;
            this.attachFileName = file.getName();

            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                SymmetricCipherContext symmetricCipherContext = cipherContexts.get(selectedChat.getChatId());
                this.encryptedFileData = symmetricCipherContext.encrypt(fileData);
                messageField.setPromptText("Файл: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                UILogger.debug("Файл " + file.getName() + " зашифрован");
            } catch (IOException e) {
                showAlert(ERR, "Ошибка чтения файла!");
                UILogger.error("Ошибка чтения файла: " + e.getMessage());
                resetAttachedFile();
            } catch (Exception e) {
                showAlert(ERR, "Ошибка шифрования!");
                UILogger.error("Ошибка шифрования: " + e.getMessage());
                resetAttachedFile();
            }
        }
    }

    private void handleExit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cryptography/frontend/login.fxml"));
            Parent root = loader.load();

            SessionManager.getInstance().clear();

            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login Page");
        } catch (IOException e) {
            e.printStackTrace();
            UILogger.error("Не удалось перейти на окно авторизации: " + e.getMessage());
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"KB", "MB", "GB", "TB"};
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }

    public static byte[] bigIntToUnsignedBytes(BigInteger value) {
        byte[] signed = value.toByteArray();
        if (signed.length > 1 && signed[0] == 0) {
            return Arrays.copyOfRange(signed, 1, signed.length);
        }
        return signed;
    }
}
