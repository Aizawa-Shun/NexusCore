package com.nexuscore.gui.controllers;

import com.nexuscore.database.DatabaseManager;
import com.nexuscore.gui.components.ChatBubble;
import com.nexuscore.gui.dialogs.OllamaSettingsDialog;
import com.nexuscore.llm.LLMService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * アプリケーションのメインウィンドウのコントローラー
 */
public class MainController {

    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatBox;
    @FXML
    private TextField userInputField;
    @FXML
    private Button sendButton;

    private DatabaseManager dbManager;
    private LLMService llmService;
    private int currentConversationId;

    /**
     * コントローラーの初期化
     */
    @FXML
    public void initialize() {
        // スクロールペインの設定
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setFitToHeight(true);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // チャットボックスの設定
        chatBox.setSpacing(15);
        chatBox.setPadding(new Insets(15));

        // データベースマネージャーの初期化
        dbManager = new DatabaseManager();

        // LLMサービスの初期化（デフォルトは「llama2」モデル）
        llmService = new LLMService("llama2");

        // 新しい会話を作成
        currentConversationId = dbManager.createConversation("New Conversation");

        // 歓迎メッセージを追加
        Platform.runLater(() -> {
            addSystemMessage("Nexus Core", "Welcome to Nexus Core. How can I help you today?\n\n" +
                    "Note: Please configure Ollama in Settings to use your preferred local LLM model.");
        });
    }

    /**
     * 送信ボタンクリックとテキストフィールドのEnterキー押下を処理
     */
    @FXML
    public void handleSendMessage() {
        String message = userInputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        // ユーザーメッセージを表示
        addUserMessage("You", message);

        // データベースに保存
        dbManager.saveMessage(currentConversationId, "User", message);

        // 入力フィールドをクリア
        userInputField.clear();

        // 送信ボタンを無効化
        sendButton.setDisable(true);

        // 「入力中...」表示
        Label typingLabel = new Label("Nexus is thinking...");
        typingLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        HBox typingBox = new HBox(typingLabel);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        chatBox.getChildren().add(typingBox);

        // 自動的に下にスクロール
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });

        // LLMの応答を取得 (ノンブロッキング)
        llmService.sendPromptAsync(message).thenAccept(response -> {
            // JavaFXスレッドでUIを更新
            Platform.runLater(() -> {
                // 「入力中...」表示を削除
                chatBox.getChildren().remove(typingBox);

                // LLM応答を表示
                addSystemMessage("Nexus", response);

                // データベースに保存
                dbManager.saveMessage(currentConversationId, "LLM", response);

                // 送信ボタンを再度有効化
                sendButton.setDisable(false);
            });
        }).exceptionally(e -> {
            // エラー処理
            Platform.runLater(() -> {
                // 「入力中...」表示を削除
                chatBox.getChildren().remove(typingBox);

                // エラーメッセージを表示
                addSystemMessage("Nexus", "Sorry, an error occurred: " + e.getMessage());

                // 送信ボタンを再度有効化
                sendButton.setDisable(false);
            });
            return null;
        });
    }

    /**
     * システムメッセージをチャット表示に追加（左寄せ）
     */
    private void addSystemMessage(String sender, String message) {
        HBox messageContainer = new HBox();
        messageContainer.setMaxWidth(Double.MAX_VALUE);
        messageContainer.setAlignment(Pos.CENTER_LEFT); // 左寄せ

        ChatBubble bubble = new ChatBubble(sender, message, false);
        messageContainer.getChildren().add(bubble);

        chatBox.getChildren().add(messageContainer);

        // 自動的に下にスクロール
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * ユーザーメッセージをチャット表示に追加（右寄せ）
     */
    private void addUserMessage(String sender, String message) {
        HBox messageContainer = new HBox();
        messageContainer.setMaxWidth(Double.MAX_VALUE);
        messageContainer.setAlignment(Pos.CENTER_RIGHT); // 右寄せ

        ChatBubble bubble = new ChatBubble(sender, message, true);
        messageContainer.getChildren().add(bubble);

        chatBox.getChildren().add(messageContainer);

        // 自動的に下にスクロール
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * [新しい会話]メニュー項目を処理
     */
    @FXML
    public void handleNewConversation() {
        currentConversationId = dbManager.createConversation("New Conversation");
        chatBox.getChildren().clear();
        addSystemMessage("Nexus", "Started a new conversation. How can I help you?");
    }

    /**
     * [設定]メニュー項目を処理
     */
    @FXML
    public void handlePreferences() {
        // Ollama設定ダイアログを表示
        OllamaSettingsDialog dialog = new OllamaSettingsDialog(llmService);
        dialog.showAndWait().ifPresent(result -> {
            if (result) {
                // 設定が保存された場合、メッセージを表示
                addSystemMessage("System", "Ollama settings updated. Using model: " + llmService.getModelName());
            }
        });
    }

    /**
     * [About]メニュー項目を処理
     */
    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Nexus Core");
        alert.setContentText("Version: 0.1.0\n\n" +
                "A simple interface for Ollama-powered local LLMs.\n\n" +
                "Configure your Ollama models in Settings.");

        alert.showAndWait();
    }

    /**
     * [終了]メニュー項目を処理
     */
    @FXML
    public void handleExit() {
        cleanupResources();
        Platform.exit();
    }

    /**
     * 終了前にリソースをクリーンアップ
     */
    public void cleanupResources() {
        System.out.println("Shutting down application...");

        if (llmService != null) {
            llmService.shutdown();
        }

        if (dbManager != null) {
            dbManager.closeConnection();
        }
    }
}