package com.nexuscore;

import com.nexuscore.database.DatabaseManager;
import com.nexuscore.llm.LLMService;
import java.util.Scanner;

/**
 * Main class for the Nexus Core application
 */
public class NexusCoreApp {

    private DatabaseManager dbManager;
    private LLMService llmService;
    private int currentConversationId;

    /**
     * Constructor to initialize the application
     */
    public NexusCoreApp() {
        // Initialize database manager
        dbManager = new DatabaseManager();

        // Initialize LLM service (model name is temporary)
        llmService = new LLMService("local-llm-model");

        // Create a new conversation
        currentConversationId = dbManager.createConversation("New Conversation");
    }

    /**
     * Method to handle user input from command line
     */
    public void startCommandLineInterface() {
        Scanner scanner = new Scanner(System.in, "UTF-8");
        String userInput;

        System.out.println("Nexus Core v0.1 - Command Line Interface");
        System.out.println("Type 'exit' to quit");
        System.out.println("------------------------------------------");

        while (scanner.hasNextLine()) {
            System.out.print("You > ");
            System.out.flush(); // Ensure prompt is displayed

            userInput = scanner.nextLine();

            // Check for exit command
            if ("exit".equalsIgnoreCase(userInput)) {
                break;
            }

            // Save user input to database
            dbManager.saveMessage(currentConversationId, "User", userInput);

            // Send prompt to LLM and get response
            String llmResponse = llmService.sendPrompt(userInput);

            // Display LLM response
            System.out.println("Nexus > " + llmResponse);

            // Save LLM response to database
            dbManager.saveMessage(currentConversationId, "LLM", llmResponse);
        }

        scanner.close();
        cleanupResources();
    }

    /**
     * Method to clean up resources
     */
    private void cleanupResources() {
        System.out.println("Shutting down application...");

        // Shutdown LLM service
        if (llmService != null) {
            llmService.shutdown();
        }

        // Close database connection
        if (dbManager != null) {
            dbManager.closeConnection();
        }
    }

    /**
     * Main method - application entry point
     */
    public static void main(String[] args) {
        try {
            // Set console character encoding
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");

            // Log encoding information for debugging
            System.out.println("Current encoding: " + System.getProperty("file.encoding"));
            System.out.println("Default Charset: " + java.nio.charset.Charset.defaultCharset());

            NexusCoreApp app = new NexusCoreApp();
            app.startCommandLineInterface();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}