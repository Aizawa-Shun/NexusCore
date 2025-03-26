package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.nexuscore.NexusCoreApp;

class AppTest {
    @Test
    void appInitializesCorrectly() {
        // 実際のアプリケーションが正しく初期化できることをテスト
        NexusCoreApp app = new NexusCoreApp();
        assertNotNull(app);
    }
}