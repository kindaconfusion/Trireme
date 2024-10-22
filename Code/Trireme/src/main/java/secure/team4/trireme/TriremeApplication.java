package secure.team4.trireme;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;
import secure.team4.triremelib.Client;
import secure.team4.triremelib.Server;

import java.io.File;
import java.io.IOException;
import java.util.function.UnaryOperator;

public class TriremeApplication extends Application {
    private boolean serverRunning = false;
    private Thread server;
    private Thread client;
    @Override
    public void start(Stage stage) throws IOException {
        final FileChooser fileChooser = new FileChooser();
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        UnaryOperator<TextFormatter.Change> portFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?([1-9][0-9]*)?") && newText.length() < 6) {
                return change;
            }
            return null;
        };
        TextField sendHost = new TextField();
        sendHost.setPromptText("Hostname");
        TextField sendPort = new TextField();
        sendPort.setPromptText("Host Port");
        sendPort.setTextFormatter(
                new TextFormatter<Integer>(new IntegerStringConverter(), null, portFilter));
        Button selectBtn = new Button("Select File");
        TextField fileText = new TextField();
        fileText.setPromptText("File");
        Button sendBtn = new Button("Send");
        TextField recvPort = new TextField();
        recvPort.setPromptText("Listening Port");
        recvPort.setTextFormatter(
                new TextFormatter<Integer>(new IntegerStringConverter(), null, portFilter));

        Button recvBtn = makeRecvBtn(recvPort);

        selectBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Node source = (Node) actionEvent.getSource();
                Window stage = source.getScene().getWindow();
                File file = fileChooser.showOpenDialog(stage);
                fileText.setText(file.getAbsolutePath());
            }
        });

        sendBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                client = new Thread(new Client(sendHost.getText(), Integer.parseInt(sendPort.getText()), fileText.getText()));
                client.start();
            }
        });

        grid.add(sendHost, 0, 0);
        grid.add(sendPort, 0, 1);
        grid.add(selectBtn, 0, 2);
        grid.add(fileText, 0, 3);
        grid.add(sendBtn, 0, 4);

        grid.add(recvPort, 1, 0);
        grid.add(recvBtn, 1, 1);

        Scene scene = new Scene(grid, 600, 400);
        stage.setTitle("Trireme");
        stage.setScene(scene);
        stage.show();
    }

    private Button makeRecvBtn(TextField recvPort) {
        Button recvBtn = new Button("Start Listening");

        recvBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!serverRunning) {
                    server = new Thread(new Server(Integer.parseInt(recvPort.getText())));
                    serverRunning = true;
                    recvBtn.setText("Stop Listening");
                    server.start();
                } else {
                    server.interrupt();
                    serverRunning = false;
                    recvBtn.setText("Start Listening");
                }
            }
        });
        return recvBtn;
    }

    public static void main(String[] args) {
        launch();
    }
}