module secure.team4.trireme {
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires atlantafx.base;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires java.prefs;
    requires com.sshtools.twoslices;


    opens secure.team4.trireme to javafx.fxml;
    exports secure.team4.trireme;
}