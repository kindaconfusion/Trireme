module secure.team4.trireme {
    requires javafx.fxml;
    requires atlantafx.base;


    opens secure.team4.trireme to javafx.fxml;
    exports secure.team4.trireme;
}