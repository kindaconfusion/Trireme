package secure.team4.trireme;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import secure.team4.triremelib.Client;
import secure.team4.triremelib.Server;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class HomeController {
    public TextField sendPort;
    public TextField recvPort;
    public Button selectFileBtn;
    public TextField sendHost;
    public Button sendFileBtn;
    public TextField fileField;
    @FXML
    final FileChooser fileChooser = new FileChooser();

    @FXML
    protected void onSelectButtonClick(ActionEvent ae) {
        Node source = (Node) ae.getSource();
        Window stage = source.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        fileField.setText(file.getAbsolutePath());
    }

    @FXML
    protected void onSendButtonClick(ActionEvent actionEvent) throws IOException, NoSuchAlgorithmException {
        Thread t = new Thread(new Client(sendHost.getText(), Integer.parseInt(sendPort.getText()), fileField.getText()));
        t.start();
    }

    @FXML
    protected void onListenButtonClick(ActionEvent actionEvent) throws IOException {
        Thread t = new Thread(new Server(Integer.parseInt(recvPort.getText())));
        t.start();
    }
}