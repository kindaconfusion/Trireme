module secure.team4.trireme {
    requires javafx.fxml;
    requires atlantafx.base;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires java.prefs;


    opens secure.team4.trireme to javafx.fxml;
    exports secure.team4.trireme;
}