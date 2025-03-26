package com.nexuscore.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * モダンなデザインのチャットバブルコンポーネント
 */
public class ChatBubble extends HBox {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * チャットバブルを作成
     * 
     * @param sender  送信者名
     * @param message メッセージ内容
     * @param isUser  ユーザーからのメッセージかどうか（スタイリングと配置に影響）
     */
    public ChatBubble(String sender, String message, boolean isUser) {
        super(10); // 子要素間の水平方向の間隔

        // 基本設定
        if (isUser) {
            this.setAlignment(Pos.CENTER_RIGHT);
        } else {
            this.setAlignment(Pos.CENTER_LEFT);
        }

        // メッセージコンテナ
        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(500); // 最大幅

        // 送信者名とタイムスタンプのヘッダー
        HBox header = new HBox(5);

        Label senderLabel = new Label(sender);
        senderLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(LocalDateTime.now().format(TIME_FORMATTER));
        timeLabel.setFont(Font.font("Segoe UI", 10));
        timeLabel.setTextFill(Color.GRAY);

        header.getChildren().addAll(senderLabel, spacer, timeLabel);

        // メッセージの本文
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setFont(Font.font("Segoe UI", 14));
        // 文字色を黒に設定
        messageLabel.setTextFill(Color.BLACK);
        messageLabel.setMaxWidth(480);

        // コンポーネントをメッセージボックスに追加
        messageBox.getChildren().addAll(header, messageLabel);
        messageBox.setPadding(new Insets(10));

        // 送信者に基づいてスタイリングとレイアウト
        if (isUser) {
            // ユーザーメッセージ（右寄せ）
            messageBox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 15px 2px 15px 15px;");
            senderLabel.setTextFill(Color.web("#0277bd"));
        } else {
            // システムメッセージ（左寄せ）
            messageBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 2px 15px 15px 15px;");
            senderLabel.setTextFill(Color.web("#2c3e50"));
        }

        this.getChildren().add(messageBox);
        this.setPadding(new Insets(5, 20, 5, 20)); // 左右の余白を増やす
    }
}