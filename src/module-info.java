module SharedStorage {
	requires javafx.controls;
    requires javafx.fxml;
    
    opens application to javafx.fxml;
    exports application;
}