module ru.citlab24.vozdyh {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.apache.poi.ooxml;

    opens ru.citlab24.vozdyh to javafx.fxml;
    exports ru.citlab24.vozdyh;
    exports ru.citlab24.vozdyh.controllers;
    opens ru.citlab24.vozdyh.controllers to javafx.fxml;
}