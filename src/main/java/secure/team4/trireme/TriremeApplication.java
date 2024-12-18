package secure.team4.trireme;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import secure.team4.triremelib.Client;
import secure.team4.triremelib.Server;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Optional;

public class TriremeApplication extends Application {
    private Server server;
    private Client client;
    private Thread clientThread;
    private boolean serverRunning = false;
    private GridPane grid;

    // Paths to keystore and truststore
    private final String keystorePath = "keystore.p12";
    private final String truststorePath = "truststore.p12";

    @Override
    public void start(Stage stage) {
        // Set up the application theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Create the main grid pane
        grid = createMainGridPane();
        Menu certMenu = new Menu("Certificates");
        certMenu.getItems().addAll(createViewCertsButton(), createExportCertButton(), createImportCertButton(), createResetCertButton());
        MenuBar menuBar = new MenuBar(certMenu);
        VBox vBox = new VBox(menuBar);
        // Add components to the grid
        addComponentsToGrid();

        vBox.getChildren().add(grid);

        // Set up the scene and stage
        Scene scene = new Scene(vBox, 600, 600);
        stage.setTitle("Trireme");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane createMainGridPane() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        return grid;
    }

    private ImageView createLogoImageView() {
        ImageView imageView = new ImageView(getClass().getResource("/trireme-logo.png").toString());
        imageView.setFitHeight(120);
        imageView.setFitWidth(283);
        return imageView;
    }

    private TextField createSendHostField() {
        TextField sendHost = new TextField();
        sendHost.setPromptText("Hostname");
        return sendHost;
    }

    private TextField createSendPortField() {
        TextField sendPort = new TextField();
        sendPort.setPromptText("Host Port");
        sendPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));
        return sendPort;
    }

    private TextField createFileText() {
        TextField fileText = new TextField();
        fileText.setPromptText("File");
        fileText.setEditable(false);
        return fileText;
    }

    private Button createSelectFileButton(TextField fileText) {
        Button selectBtn = new Button("Select File");
        FileChooser fileChooser = new FileChooser();
        selectBtn.setOnAction(actionEvent -> {
            Stage stage = (Stage) selectBtn.getScene().getWindow();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                fileText.setText(file.getAbsolutePath());
            }
        });
        return selectBtn;
    }

    private Button createSendButton(TextField sendHostField, TextField sendPortField, TextField fileTextField) {
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(actionEvent -> {
            String host = sendHostField.getText();
            String portText = sendPortField.getText();
            String filePath = fileTextField.getText();

            if (host.isEmpty() || portText.isEmpty() || filePath.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Missing Information", "Please fill in all the fields.");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Port", "Please enter a valid port number.");
                return;
            }

            // Start the client thread
            client = new Client(host, port, filePath);
            clientThread = new Thread(client);
            clientThread.start();
            ProgressBar bar = new ProgressBar(0);
            grid.add(bar, 0, 7);
            server.progressProperty().addListener((ov, old_val, new_val) -> bar.setProgress(new_val.doubleValue()));
        });
        return sendBtn;
    }

    private TextField createRecvPortField() {
        TextField recvPort = new TextField();
        recvPort.setPromptText("Listening Port");
        recvPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));
        return recvPort;
    }

    private Button createRecvButton(TextField recvPortField) {
        Button recvBtn = new Button("Start Listening");

        recvBtn.setOnAction(actionEvent -> {
            if (!serverRunning) {
                String portText = recvPortField.getText();

                if (portText.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Missing Port", "Please enter a listening port.");
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Port", "Please enter a valid port number.");
                    return;
                }

                server = new Server(port);
                serverRunning = true;
                recvBtn.setText("Stop Listening");
                server.start();
                ProgressBar bar = new ProgressBar(0);
                grid.add(bar, 1, 4);
                server.progressProperty().addListener((ov, old_val, new_val) -> bar.setProgress(new_val.doubleValue()));
            } else {
                if (server != null) {
                    server.closeServer();
                }
                serverRunning = false;
                recvBtn.setText("Start Listening");
            }
        });
        return recvBtn;
    }

    public void createProgressBar() {

    }

    private MenuItem createExportCertButton() {
        MenuItem exportBtn = new MenuItem("Export Certificate");
        exportBtn.setOnAction(actionEvent -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Certificate");
                fileChooser.setInitialFileName("certificate.cer");
                File file = fileChooser.showSaveDialog(null);
                if (file != null) {
                    // Create a temporary client instance if none exists
                    if (client == null) {
                        client = new Client("", 0, "");
                    }
                    client.exportCertificate(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to export certificate: " + e.getMessage());
            }
        });
        return exportBtn;
    }

    private MenuItem createImportCertButton() {
        MenuItem importBtn = new MenuItem("Import Certificate");
        importBtn.setOnAction(actionEvent -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Import Certificate");
                File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    TextInputDialog aliasDialog = new TextInputDialog();
                    aliasDialog.setTitle("Certificate Alias");
                    aliasDialog.setHeaderText("Enter an alias for the certificate (e.g., IP address):");
                    Optional<String> aliasResult = aliasDialog.showAndWait();
                    if (aliasResult.isPresent() && !aliasResult.get().isEmpty()) {
                        String alias = aliasResult.get();
                        // Create a temporary client instance if none exists
                        if (client == null) {
                            client = new Client("", 0, "");
                        }
                        client.importCertificate(file, alias);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Import Error", "Alias cannot be empty.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Import Error", "Failed to import certificate: " + e.getMessage());
            }
        });
        return importBtn;
    }

    private MenuItem createResetCertButton() {
        MenuItem resetBtn = new MenuItem("Reset Certificates");
        resetBtn.setOnAction(actionEvent -> {
            File ksFile = new File(keystorePath);
            File tsFile = new File(truststorePath);

            boolean ksDeleted = ksFile.delete();
            boolean tsDeleted = tsFile.delete();

            String message = "Certificates reset:\n";
            message += "Keystore deleted: " + ksDeleted + "\n";
            message += "Truststore deleted: " + tsDeleted;

            showAlert(Alert.AlertType.INFORMATION, "Reset Certificates", message);
        });
        return resetBtn;
    }

    private MenuItem createViewCertsButton() {
        MenuItem viewBtn = new MenuItem("View Certificates");
        viewBtn.setOnAction(actionEvent -> {
            try {
                // Load truststore
                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                File tsFile = new File(truststorePath);
                if (tsFile.exists()) {
                    trustStore.load(new FileInputStream(truststorePath), null);
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "No Certificates", "No certificates have been imported.");
                    return;
                }

                // Build list of certificates
                StringBuilder certList = new StringBuilder();
                Enumeration<String> aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = trustStore.getCertificate(alias);
                    certList.append("Alias: ").append(alias).append("\n");
                    certList.append(cert.toString()).append("\n\n");
                }

                // Display in a dialog
                Alert certAlert = new Alert(Alert.AlertType.INFORMATION);
                certAlert.setTitle("Trusted Certificates");
                certAlert.setHeaderText("List of Trusted Certificates");
                TextArea textArea = new TextArea(certList.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                certAlert.getDialogPane().setContent(textArea);
                certAlert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "View Certificates Failed", e.getMessage());
            }
        });
        return viewBtn;
    }

    private void addComponentsToGrid() {
        ImageView logo = createLogoImageView();
        grid.add(logo, 0, 0, 2, 1);
        GridPane.setHalignment(logo, javafx.geometry.HPos.CENTER);

        // Sending section
        grid.add(new Label("Send to:"), 0, 1);
        TextField sendHost = createSendHostField();
        grid.add(sendHost, 0, 2);
        TextField sendPort = createSendPortField();
        grid.add(sendPort, 0, 3);
        TextField fileText = createFileText();
        grid.add(fileText, 0, 5);
        grid.add(createSelectFileButton(fileText), 0, 4);
        grid.add(createSendButton(sendHost, sendPort, fileText), 0, 6);

        // Receiving section
        grid.add(new Label("Receive from:"), 1, 1);
        TextField field = createRecvPortField();
        grid.add(field, 1, 2);
        grid.add(createRecvButton(field), 1, 3);
    }

    /**
     * Displays an alert dialog to the user.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        // Uncomment the following line to enable SSL debugging
        // System.setProperty("javax.net.debug", "all");
        launch();
    }
}