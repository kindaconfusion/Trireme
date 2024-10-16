package secure.team4.trireme;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class HomeController {
    public TextField sendPort;
    public TextField recvPort;
    public Button selectFileBtn;
    public TextField sendHost;
    public Button sendFileBtn;
    public TextField fileField;
    @FXML
    private Label welcomeText;
    final FileChooser fileChooser = new FileChooser();

    @FXML
    protected void onSelectButtonClick(ActionEvent ae) {
        Node source = (Node) ae.getSource();
        Window stage = source.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        fileField.setText(file.getAbsolutePath());
    }

    public void onSendButtonClick(ActionEvent actionEvent) {
    }
}