package secure.team4.trireme;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import secure.team4.triremelib.Client;
import secure.team4.triremelib.Server;

import java.io.File;
import java.util.function.UnaryOperator;

public class TriremeApplication extends Application {

    private boolean serverRunning = false;
    private Server server;
    private Thread client;
    private UnaryOperator<TextFormatter.Change> portFilter;

    @Override
    public void start(Stage stage) {
        // Set up the application theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Initialize the port filter
        portFilter = createPortFilter();

        // Create the main layout grid
        GridPane grid = createMainGridPane();

        // Create UI components
        ImageView logoImageView = createLogoImageView();
        TextField sendHostField = createSendHostField();
        TextField sendPortField = createSendPortField();
        TextField fileTextField = createFileTextField();
        Button selectFileButton = createSelectFileButton(fileTextField);
        Button sendButton = createSendButton(sendHostField, sendPortField, fileTextField);
        TextField recvPortField = createRecvPortField();
        Button recvButton = createRecvButton(recvPortField);

        // Add components to the grid
        addComponentsToGrid(grid, logoImageView, sendHostField, sendPortField, selectFileButton,
                fileTextField, sendButton, recvPortField, recvButton);

        // Set up the scene and stage
        Scene scene = new Scene(grid, 600, 500);
        stage.setTitle("Trireme");
        stage.setScene(scene);
        stage.show();
    }

    private UnaryOperator<TextFormatter.Change> createPortFilter() {
        return change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?([1-9][0-9]*)?") && newText.length() < 6) {
                return change;
            }
            return null;
        };
    }

    private GridPane createMainGridPane() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
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
        sendPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, portFilter));
        return sendPort;
    }

    private TextField createFileTextField() {
        TextField fileText = new TextField();
        fileText.setPromptText("File");
        fileText.setEditable(false);
        return fileText;
    }

    private Button createSelectFileButton(TextField fileTextField) {
        Button selectBtn = new Button("Select File");
        FileChooser fileChooser = new FileChooser();
        selectBtn.setOnAction(actionEvent -> {
            Stage stage = (Stage) selectBtn.getScene().getWindow();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                fileTextField.setText(file.getAbsolutePath());
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

            client = new Thread(new Client(host, port, filePath));
            client.start();
        });
        return sendBtn;
    }

    private TextField createRecvPortField() {
        TextField recvPort = new TextField();
        recvPort.setPromptText("Listening Port");
        recvPort.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, portFilter));
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
            } else {
                server.interrupt();
                serverRunning = false;
                recvBtn.setText("Start Listening");
            }
        });
        return recvBtn;
    }

    private void addComponentsToGrid(GridPane grid, ImageView logoImageView, TextField sendHostField,
                                     TextField sendPortField, Button selectFileButton, TextField fileTextField,
                                     Button sendButton, TextField recvPortField, Button recvButton) {
        grid.add(logoImageView, 0, 0, 2, 1);
        GridPane.setHalignment(logoImageView, HPos.CENTER);

        // Sending section
        grid.add(sendHostField, 0, 1);
        grid.add(sendPortField, 0, 2);
        grid.add(selectFileButton, 0, 3);
        grid.add(fileTextField, 0, 4);
        grid.add(sendButton, 0, 5);

        // Receiving section
        grid.add(recvPortField, 1, 1);
        grid.add(recvButton, 1, 2);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}