module com.dinurobongtubo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    requires com.github.librepdf.openpdf;

    opens com.dinurobongtubo to javafx.fxml;
    exports com.dinurobongtubo;
}
