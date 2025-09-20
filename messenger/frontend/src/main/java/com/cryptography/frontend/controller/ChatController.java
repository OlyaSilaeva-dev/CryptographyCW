package com.cryptography.frontend.controller;

import com.cryptography.frontend.apiclient.MessageSender;
import com.cryptography.frontend.apiclient.UsersClient;
import com.cryptography.frontend.dto.UserDTO;
import com.cryptography.frontend.stompclient.StompClient;
import com.cryptography.frontend.algorithms.DiffieHellman;
import com.cryptography.frontend.algorithms.MacGuffin.MacGuffin;
import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.algorithms.symmetricCipherContext.SymmetricCipherContext;
import com.cryptography.frontend.dto.KeyParams;
import com.cryptography.frontend.entity.ChatMessage;
import com.cryptography.frontend.entity.ReceivedFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cryptography.frontend.controller.ControllerUtils.ERR;
import static com.cryptography.frontend.controller.ControllerUtils.showAlert;

@Slf4j
public class ChatController {
    @FXML
    private ListView<UserDTO> contactsView;
    @FXML
    private ListView<String> listView;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button attachButton;

    private File attachFile;
    private String attachFileName;
    private byte[] encryptedFileData;

    private String senderId;
    private String recipientId = null;

    private Map<String, SymmetricCipherContext> cipherContexts = new HashMap<>();
    DiffieHellman diffieHellman;

    private final Map<String, List<String>> messageHistory = new HashMap<>();
    private final Map<String, List<ReceivedFile>> receivedFiles = new HashMap<>();

    /// Map<recipient, key>
    private final Map<String, BigInteger> sharedSecrets = new HashMap<>();

    private boolean isSharedSecretEstablished() {
        return  recipientId != null
                && sharedSecrets.containsKey(recipientId)
                && sharedSecrets.get(recipientId) != null;
    }

    private void onMessageReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            String fromId = msg.getSenderId();
            String contactId = fromId.equals(senderId) ? msg.getRecipientId() : fromId;

            if (!sharedSecrets.containsKey(fromId)) {
                log.warn("Нет общего секрета для сообщения от {}", fromId);
                showAlert("System", "Нет общего секрета для сообщения от {" + fromId + "}");
                return;
            }

            UserDTO contact = contactsView.getItems().stream()
                    .filter(u -> u.getId().equals(contactId))
                    .findFirst()
                    .orElseGet(() -> {
                        UserDTO user = UserDTO.builder()
                                .id(contactId)
                                .name("Unknown (" + contactId + ")")
                                .build();
                        contactsView.getItems().add(user);
                        return user;
                    });

            try {
                SymmetricCipherContext senderCipherContext = cipherContexts.get(contactId);
                String displayText;

                if (msg.isFile()) {
                    byte[] decryptedFileData = senderCipherContext.decrypt(msg.getMessage());

                    ReceivedFile receivedFile = new ReceivedFile(msg.getFileName(), decryptedFileData, contact.getId());

                    receivedFiles.putIfAbsent(contactId, new ArrayList<>());
                    receivedFiles.get(contactId).add(receivedFile);

                    displayText = contact.getName() + ": отправил(а) файл " + msg.getFileName();
                } else {
                    byte[] decryptedBytes = senderCipherContext.decrypt(msg.getMessage());
                    displayText = contact.getName() + ": " + new String(decryptedBytes, StandardCharsets.UTF_8);
                }

                appendMessage(contactId, displayText);

                if (contactId.equals(recipientId)) {
                    updateMessageList();
                }

                log.debug("Получено сообщение от {}: {}", contact.getName(), msg);

            } catch (Exception e) {
                log.error("Ошибка дешифрования сообщения от {}: {}", contact.getName(), e.getMessage());
                appendMessage(contactId, contact.getName() + ": не удалось расшифровать сообщение");
            }
        });
    }

    private void onPublicKeyReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            String from = msg.getSenderId();
            byte[] publicKey = msg.getMessage();

            if (diffieHellman != null && from.equals(recipientId)) {
                try {
                    BigInteger receivedPublicKey = new BigInteger(1, publicKey);
                    BigInteger sharedSecret = diffieHellman.computeSharedSecret(receivedPublicKey);

                    log.debug("Shared secret with {}, shared secret {}", from, sharedSecret);
                    sharedSecrets.put(from, sharedSecret);

                    SymmetricCipherContext context = new SymmetricCipherContext(
                            new MacGuffin(),
                            sharedSecrets.get(recipientId).toByteArray(),
                            EncryptionMode.ECB,
                            PaddingMode.PKCS7,
                            new byte[0]
                    );

                    cipherContexts.put(from, context);
                    updateMessageList();

                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private void appendMessage(String contactId, String message) {
        messageHistory.putIfAbsent(contactId, new ArrayList<>());
        messageHistory.get(contactId).add(message);

        if (Objects.equals(recipientId, contactId)) {
            updateMessageList();
        }
    }

    private void updateMessageList() {
        List<String> messages = messageHistory.getOrDefault(recipientId, new ArrayList<>());
        listView.getItems().setAll(messages);
    }

    public void init(String myId) throws RuntimeException {
        this.senderId = myId;

        StompClient.connect(myId, this::onMessageReceived, this::onPublicKeyReceived);

        contactsView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        try {
            List<UserDTO> allUsers = UsersClient.getUsers(myId);
            List<UserDTO> users = allUsers.stream()
                    .filter(user -> !Objects.equals(user.getId(), myId))
                    .collect(Collectors.toList());
            log.debug("список пользователей: {}", users);

            contactsView.getItems().setAll(users);
        } catch (Exception e) {
            log.error("Ошибка загрузки списка пользователей {}",e.getMessage());
            throw new RuntimeException(e);
        }

        contactsView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                recipientId = newVal.getId();
                String recipientName = newVal.getName();

                log.debug("Выбран контакт: {} (id={})", recipientName, recipientId);

                if (sharedSecrets.containsKey(newVal.getId())) {
                    updateMessageList();
                    return;
                }

                KeyParams keyParams = new KeyParams();
                try {
                    keyParams = MessageSender.getKeyParams(myId, recipientId);
                    log.debug("keyParams: {}", keyParams);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

                if (keyParams != null) {
                    BigInteger p = new BigInteger(keyParams.getP(), 16);
                    BigInteger g = new BigInteger(keyParams.getG(), 16);
                    diffieHellman = new DiffieHellman(p, g);

                    try {
                        MessageSender.sendPublicKeyMessage(
                                ChatMessage.builder()
                                        .senderId(myId)
                                        .recipientId(recipientId)
                                        .message(bigIntToUnsignedBytes(diffieHellman.getPublicKey()))
                                        .timestamp(LocalDateTime.now().toString())
                                        .build());

                        log.debug("User {} send public key to {}", myId, recipientId);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }

                    byte[] receivedPublicKey = null;
                    try {
                        receivedPublicKey = MessageSender.getPublicKey(myId, recipientId);
                        log.debug("receivedPublicKey: {}", receivedPublicKey);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }

                    if (receivedPublicKey != null) {
                        BigInteger sharedSecret = diffieHellman.computeSharedSecret(new BigInteger(1, receivedPublicKey));
                        sharedSecrets.put(recipientId, sharedSecret);
                        log.debug("Shared secret with (by redis) {}, shared secret {}", recipientId, sharedSecret);

                        SymmetricCipherContext symmetricCipherContext = new SymmetricCipherContext(
                                new MacGuffin(),
                                sharedSecret.toByteArray(),
                                EncryptionMode.ECB,
                                PaddingMode.PKCS7,
                                new byte[0]
                        );
                        cipherContexts.put(recipientId, symmetricCipherContext);

                        updateMessageList();
                    }
                }
            }
        });

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

                        String contact = recipientId;
                        if (contact != null && receivedFiles.containsKey(contact)) {
                            List<ReceivedFile> files = receivedFiles.get(contact);
                            fileRef = files.get(files.size() - 1); // последний файл
                        }

                        setGraphic(hbox);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
        });


        sendButton.setOnAction(event -> send(myId));
        attachButton.setOnAction(event -> attachFile());
    }

    private void send(String myId) {
        String text = messageField.getText().trim();
        if ((text.isEmpty() && attachFile == null) || recipientId == null) {
            if (recipientId == null) {
                showAlert(ERR, "Выберите получателя");
            }
            return;
        }

        if (!isSharedSecretEstablished()) {
            showAlert(ERR, "Общий секрет с {" + recipientId + "} не установлен, дождитесь обмена ключами.");
            return;
        }

        try {
            if (attachFile != null) {
                if (encryptedFileData == null) {
                    showAlert(ERR, "Файл не подготовлен к отправке");
                    return;
                }
                ChatMessage fileMsg = ChatMessage.builder()
                        .senderId(myId)
                        .recipientId(recipientId)
                        .message(encryptedFileData)
                        .fileName(attachFileName)
                        .isFile(true)
                        .timestamp(LocalDateTime.now().toString())
                        .build();

                MessageSender.sendChatMessage(fileMsg);
                appendMessage(recipientId, "Вы: отправили файл " + attachFileName);
                log.info("Файл '{}' отправлен пользователю {}", attachFileName, recipientId);

                resetAttachedFile();
            } else {
                SymmetricCipherContext symmetricCipherContext = cipherContexts.get(recipientId);
                ChatMessage msg = ChatMessage.builder()
                        .senderId(myId)
                        .recipientId(recipientId)
                        .message(symmetricCipherContext.encrypt(text.getBytes()))
                        .timestamp(LocalDateTime.now().toString())
                        .build();

                MessageSender.sendChatMessage(msg);
                appendMessage(recipientId, "Вы: " + text);
                messageField.clear();
            }
        } catch (Exception e) {
            log.error("Ошибка отправки: {}", e.getMessage());
            showAlert(ERR, "Ошибка отправки: " + e.getMessage());

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
                appendMessage(recipientId, "Файл сохранен: " + saveFile.getName());
                log.info("Файл '{}' сохранен в {}", file.getFileName(), saveFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Ошибка сохранения файла: {}", e.getMessage());
            appendMessage(recipientId, "Ошибка сохранения файла: " + e.getMessage());
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
        if (recipientId == null) {
            appendMessage("system", "Ошибка: сначала выберите получателя");
            return;
        }

        if (!isSharedSecretEstablished()) {
            appendMessage(recipientId, "Ошибка: общий секрет не установлен");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для отправки");

        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("Все файлы", "*.*");
        FileChooser.ExtensionFilter images = new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        FileChooser.ExtensionFilter documents = new FileChooser.ExtensionFilter("Документы", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf");
        FileChooser.ExtensionFilter archives = new FileChooser.ExtensionFilter("Архивы", "*.zip", "*.rar", "*.7z");

        fileChooser.getExtensionFilters().addAll(images, documents, archives, allFiles);

        Stage stage = (Stage) attachButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.attachFile = file;
            this.attachFileName = file.getName();

            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                SymmetricCipherContext symmetricCipherContext = cipherContexts.get(recipientId);
                this.encryptedFileData = symmetricCipherContext.encrypt(fileData);
                messageField.setPromptText("Файл: " + file.getName() + " (" + formatFileSize(file.length()) + ")");

                log.debug("Файл {} подготовлен к отправке", file.getName());
            } catch (IOException e) {
                appendMessage(recipientId, "Ошибка чтения файла: " + e.getMessage());
                resetAttachedFile();
            } catch (Exception e) {
                appendMessage(recipientId, "Ошибка шифрования: " + e.getMessage());
                resetAttachedFile();
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"KB", "MB", "GB", "TB"};
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), units[exp-1]);
    }

    public static byte[] bigIntToUnsignedBytes(BigInteger value) {
        byte[] signed = value.toByteArray();
        if (signed[0] == 0) {
            return Arrays.copyOfRange(signed, 1, signed.length);
        }
        return signed;
    }
}
