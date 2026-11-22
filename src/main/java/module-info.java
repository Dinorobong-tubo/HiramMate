module com.dinurobongtubo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.dinurobongtubo to javafx.fxml;
    exports com.dinurobongtubo;
}
