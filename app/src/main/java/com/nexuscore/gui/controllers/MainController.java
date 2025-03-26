package com.nexuscore.gui.controllers;

import com.nexuscore.database.DatabaseManager;
import com.nexuscore.gui.components.ChatBubble;
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
 * Controller for the main application window
 */
public class MainController {

    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatBox;
    @FXML
    private TextField userInputField;

    private DatabaseManager dbManager;
    private LLMService llmService;
    private int currentConversationId;

    /**
     * Initializes the controller
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

        // LLMサービスの初期化
        llmService = new LLMService("local-llm-model");

        // 新しい会話を作成
        currentConversationId = dbManager.createConversation("New Conversation");

        // 歓迎メッセージを追加
        Platform.runLater(() -> {
            addSystemMessage("Nexus Core", "Welcome to Nexus Core. How can I help you today?");
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

        // LLMの応答を取得 (ノンブロッキング)
        new Thread(() -> {
            final String response = llmService.sendPrompt(message);

            // JavaFXスレッドでUIを更新
            Platform.runLater(() -> {
                addSystemMessage("Nexus", response);
                dbManager.saveMessage(currentConversationId, "LLM", response);
            });
        }).start();
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
        // 簡単な設定ダイアログ
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Preferences");
        dialog.setHeaderText("Application Preferences");

        // 設定UI用のプレースホルダー
        DialogPane dialogPane = dialog.getDialogPane();

        // ダミーの設定コントロールを追加
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().add(okButton);

        dialog.showAndWait();
    }

    /**
     * [About]メニュー項目を処理
     */
    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Nexus Core");
        alert.setContentText("Version: 0.1.0\n\nA simple interface for interacting with LLMs.");

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