package com.nexuscore.gui.dialogs;

import com.nexuscore.llm.LLMService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Dialog class for configuring Ollama settings
 */
public class OllamaSettingsDialog extends Dialog<Boolean> {

    private ComboBox<String> modelComboBox;
    private TextField ollamaPathField;
    private Button browseButton;
    private Slider temperatureSlider;
    private Button refreshButton;
    private Button testButton;
    private Label statusLabel;

    private LLMService llmService;

    /**
     * Constructor
     *
     * @param llmService LLM service to which settings are applied
     */
    public OllamaSettingsDialog(LLMService llmService) {
        this.llmService = llmService;

        // Basic dialog settings
        setTitle("Ollama Settings");
        setHeaderText("Configure Ollama Integration");

        // Create dialog content
        createContent();

        // Configure dialog buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Load the model list
        refreshModelList();

        // Action for the Save button
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                saveSettings();
                return true;
            }
            return false;
        });
    }

    /**
     * Create the content of the dialog
     */
    private void createContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Ollama path
        Label pathLabel = new Label("Ollama Path:");
        grid.add(pathLabel, 0, 0);

        ollamaPathField = new TextField("ollama");
        grid.add(ollamaPathField, 1, 0);

        browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> browsePath());
        grid.add(browseButton, 2, 0);

        // Model selection
        Label modelLabel = new Label("Ollama Model:");
        grid.add(modelLabel, 0, 1);

        modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true); // Allows manual input
        modelComboBox.setPrefWidth(200);
        grid.add(modelComboBox, 1, 1);

        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshModelList());
        grid.add(refreshButton, 2, 1);

        // Temperature setting
        Label tempLabel = new Label("Temperature:");
        grid.add(tempLabel, 0, 2);

        temperatureSlider = new Slider(0.0, 1.0, 0.7);
        temperatureSlider.setShowTickLabels(true);
        temperatureSlider.setShowTickMarks(true);
        temperatureSlider.setMajorTickUnit(0.25);
        temperatureSlider.setBlockIncrement(0.1);
        grid.add(temperatureSlider, 1, 2, 2, 1);

        // Test connection button
        testButton = new Button("Test Ollama");
        testButton.setOnAction(e -> testOllama());
        grid.add(testButton, 0, 3);

        // Status label
        statusLabel = new Label("");
        grid.add(statusLabel, 1, 3, 2, 1);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(450);
    }

    /**
     * Opens a file selection dialog to choose the Ollama path
     */
    private void browsePath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Ollama Executable");
        File initialDirectory = new File(System.getProperty("user.home"));
        fileChooser.setInitialDirectory(initialDirectory);

        // Filter for .exe files if the OS is Windows
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Executable Files", "*.exe"));
        }

        File selectedFile = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (selectedFile != null) {
            ollamaPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Retrieves and updates the model list from Ollama
     */
    private void refreshModelList() {
        refreshButton.setDisable(true);
        statusLabel.setText("Loading models...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        // Fetch model list in a new thread
        new Thread(() -> {
            try {
                // Temporarily set the Ollama path
                llmService.setOllamaPath(ollamaPathField.getText());

                // Retrieve the model list
                List<String> models = llmService.getInstalledModels();

                // Update the ComboBox on the UI thread
                Platform.runLater(() -> {
                    String currentSelection = modelComboBox.getValue();
                    modelComboBox.getItems().clear();

                    if (models.isEmpty()) {
                        statusLabel.setText("No models found. Please install models with 'ollama pull'.");
                        statusLabel.setStyle("-fx-text-fill: orange;");
                    } else {
                        modelComboBox.getItems().addAll(models);
                        if (models.contains(currentSelection)) {
                            modelComboBox.setValue(currentSelection);
                        } else if (!models.isEmpty()) {
                            modelComboBox.setValue(models.get(0));
                        }
                        statusLabel.setText("Found " + models.size() + " models.");
                        statusLabel.setStyle("-fx-text-fill: green;");
                    }

                    refreshButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading models: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    refreshButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Tests the connection to Ollama
     */
    private void testOllama() {
        testButton.setDisable(true);
        statusLabel.setText("Testing Ollama...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        // Test connection in a separate thread
        new Thread(() -> {
            boolean isAvailable = false;

            try {
                // Set current input values to the LLM service
                llmService.setOllamaPath(ollamaPathField.getText());

                // Perform connection test
                isAvailable = llmService.isAvailable();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Display the test result on the UI thread
            final boolean finalIsAvailable = isAvailable;
            Platform.runLater(() -> {
                if (finalIsAvailable) {
                    statusLabel.setText("Ollama is available!");
                    statusLabel.setStyle("-fx-text-fill: green;");

                    // If Ollama is available, update the model list as well
                    refreshModelList();
                } else {
                    statusLabel.setText("Ollama not found or not working!");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
                testButton.setDisable(false);
            });
        }).start();
    }

    /**
     * Saves the settings and applies them to the LLM service
     */
    private void saveSettings() {
        String ollamaPath = ollamaPathField.getText();
        String modelName = modelComboBox.getValue();
        float temperature = (float) temperatureSlider.getValue();

        // Apply settings to the LLM service
        llmService.setOllamaPath(ollamaPath);
        llmService.setModelName(modelName);
        llmService.setTemperature(temperature);

        System.out.println("Ollama settings saved. Path: " + ollamaPath +
                ", Model: " + modelName +
                ", Temperature: " + temperature);
    }
}