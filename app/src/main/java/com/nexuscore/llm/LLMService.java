package com.nexuscore.llm;

import com.nexuscore.database.DatabaseManager;
import com.nexuscore.database.DatabaseManager.ConversationMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local LLM service using Ollama
 */
public class LLMService {

    private String modelName;
    private String ollamaPath = "ollama"; // Assumes path is set by default
    private int maxTokens = 50;
    private float temperature = 0.7f;
    private int timeoutSeconds = 30; // Timeout duration in seconds

    /**
     * Constructor
     * 
     * @param modelName Name of the Ollama model to be used
     */
    public LLMService(String modelName) {
        this.modelName = modelName;
        System.out.println("LLM service initialized with Ollama model: " + modelName);
    }

    /**
     * Set the execution path for Ollama
     * 
     * @param path Path to the Ollama executable
     */
    public void setOllamaPath(String path) {
        this.ollamaPath = path;
    }

    /**
     * Get the current Ollama path
     * 
     * @return Path to the Ollama executable
     */
    public String getOllamaPath() {
        return ollamaPath;
    }

    /**
     * Set the model name
     * 
     * @param modelName Name of the model to be used
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Get the currently used model name
     * 
     * @return Model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Set the maximum number of tokens to be generated
     * 
     * @param maxTokens Maximum token count
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * Set the temperature (randomness) parameter
     * 
     * @param temperature Temperature (0.0 to 1.0)
     */
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    /**
     * Get the current temperature setting
     * 
     * @return Temperature value
     */
    public float getTemperature() {
        return temperature;
    }

    /**
     * Set the timeout duration in seconds
     * 
     * @param seconds Timeout duration in seconds
     */
    public void setTimeout(int seconds) {
        this.timeoutSeconds = seconds;
    }

    /**
     * Send a prompt to the LLM and get the response
     * 
     * @param prompt User's prompt
     * @return Response from the LLM
     */
    public String sendPrompt(String prompt) {
        try {
            return runOllamaWithTimeout(prompt);
        } catch (Exception e) {
            System.err.println("Error running Ollama: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse(prompt, e.getMessage());
        }
    }

    /**
     * 過去の会話履歴を考慮して、プロンプトを送信
     * 
     * @param prompt              ユーザーのプロンプト
     * @param conversationHistory 会話履歴
     * @return LLMからの応答
     */
    public String sendPromptWithHistory(String prompt, List<ConversationMessage> conversationHistory) {
        try {
            // 会話履歴とユーザープロンプトを結合
            StringBuilder enhancedPrompt = new StringBuilder();

            // ヘッダー
            enhancedPrompt.append(
                    "Below is a history of past conversations.Please take this into account when answering the last question.\n\n");

            // 会話履歴を追加
            for (ConversationMessage message : conversationHistory) {
                String role = "User".equals(message.getSender()) ? "User" : "Assistant";
                enhancedPrompt.append(role).append(": ").append(message.getContent()).append("\n\n");
            }

            // 新しいプロンプト
            enhancedPrompt.append("User: ").append(prompt).append("\n\n");
            enhancedPrompt.append("Assistant: ");

            System.out.println("Sending enhanced prompt with conversation history");
            return runOllamaWithTimeout(enhancedPrompt.toString());
        } catch (Exception e) {
            System.err.println("Error running Ollama with history: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse(prompt, e.getMessage());
        }
    }

    /**
     * 標準プロンプトを非同期で送信（UI非ブロッキング）
     * 
     * @param prompt User's prompt
     * @return 応答を含むCompletableFuture
     */
    public CompletableFuture<String> sendPromptAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> sendPrompt(prompt));
    }

    /**
     * 会話履歴付きのプロンプトを非同期で送信（UI非ブロッキング）
     * 
     * @param prompt              ユーザープロンプト
     * @param conversationHistory 会話履歴
     * @return 応答を含むCompletableFuture
     */
    public CompletableFuture<String> sendPromptWithHistoryAsync(String prompt,
            List<ConversationMessage> conversationHistory) {
        return CompletableFuture.supplyAsync(() -> sendPromptWithHistory(prompt, conversationHistory));
    }

    /**
     * Run the Ollama command with a timeout
     * 
     * @param prompt User's prompt
     * @return Response from Ollama
     * @throws IOException          If process execution fails
     * @throws InterruptedException If the thread is interrupted
     */
    private String runOllamaWithTimeout(String prompt) throws IOException, InterruptedException {
        // Separate thread pool to run the process
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // Run the Ollama process asynchronously
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return runOllama(prompt);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            // Set the timeout and wait for the result
            try {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                System.err.println("Ollama process timed out after " + timeoutSeconds + " seconds");
                return "Sorry, the response took too long to generate. Please try again with a shorter prompt or a different model.";
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                }
                throw new RuntimeException("Error executing Ollama process", e);
            }
        } finally {
            // Always shut down the executor
            executor.shutdownNow();
        }
    }

    /**
     * Run the Ollama command and send the prompt
     * 
     * @param prompt User's prompt
     * @return Response from Ollama
     * @throws IOException          If process execution fails
     * @throws InterruptedException If the thread is interrupted
     */
    private String runOllama(String prompt) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(ollamaPath);
        command.add("run");
        command.add(modelName);

        System.out.println("Executing command: " + String.join(" ", command));

        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Write the prompt to standard input
        process.getOutputStream().write((prompt + "\n").getBytes());
        process.getOutputStream().flush();
        process.getOutputStream().close();

        // Read the response from standard output
        StringBuilder output = new StringBuilder();

        // Read output asynchronously
        AtomicBoolean outputComplete = new AtomicBoolean(false);
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                System.err.println("Error reading process output: " + e.getMessage());
            } finally {
                outputComplete.set(true);
            }
        });
        outputThread.start();

        // Also read error output asynchronously
        StringBuilder errorOutput = new StringBuilder();
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            } catch (IOException e) {
                System.err.println("Error reading process error output: " + e.getMessage());
            }
        });
        errorThread.start();

        // Wait for the process to finish
        int exitCode = process.waitFor();

        // Wait for the output reading threads to finish (up to 5 seconds)
        outputThread.join(5000);
        errorThread.join(1000);

        if (exitCode != 0) {
            throw new IOException("Ollama process exited with code " + exitCode + ": " + errorOutput.toString());
        }

        return output.toString().trim();
    }

    /**
     * Fallback response if the LLM API is unavailable
     * 
     * @param prompt      User's prompt
     * @param errorDetail Error details (if available)
     * @return Fallback response
     */
    private String getFallbackResponse(String prompt, String errorDetail) {
        // Fallback response if Ollama cannot be executed
        if (errorDetail != null) {
            return "I'm sorry, I couldn't connect to Ollama. Error: " + errorDetail +
                    "\n\nYour message was: \"" + prompt + "\"";
        } else {
            return "Hello, this is Nexus Core using fallback mode. I received your message: \"" + prompt +
                    "\". Currently, I'm operating without LLM capabilities. Please make sure Ollama is installed and configured correctly.";
        }
    }

    /**
     * Check the availability of Ollama
     * 
     * @return true if Ollama is available
     */
    public boolean isAvailable() {
        try {
            // Test by running Ollama's version command
            ProcessBuilder processBuilder = new ProcessBuilder(ollamaPath, "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Ollama availability check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get a list of installed models
     * 
     * @return List of model names
     */
    public List<String> getInstalledModels() {
        List<String> models = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ollamaPath, "list");
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Extract model name (first word in the line)
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 0) {
                            models.add(parts[0]);
                        }
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.err.println("Failed to get installed models: " + e.getMessage());
        }
        return models;
    }

    /**
     * Shut down the service (release resources, if needed)
     */
    public void shutdown() {
        System.out.println("Shutting down LLM service...");
        // No special cleanup is required
    }
}