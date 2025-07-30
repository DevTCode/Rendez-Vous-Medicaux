module com.example.rendezvousmedicaux {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.rendezvousmedicaux to javafx.fxml;
    exports com.example.rendezvousmedicaux;
}