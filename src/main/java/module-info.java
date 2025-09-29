module ru.citlab24.vozdyh {
    requires java.desktop;            // часто нужен (AWT/изображения и т.п.)
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    // Apache POI для DOCX/XLSX:
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires org.apache.xmlbeans;

    // Оставь, если твой json-jar даёт имя модуля org.json. Иначе закомментируй.
    requires org.json;

    // Открываем пакеты для FXML:
    opens ru.citlab24.vozdyh to javafx.fxml;
    opens ru.citlab24.vozdyh.controllers to javafx.fxml;
    opens ru.citlab24.vozdyh.service to javafx.fxml;

    // Экспортируем (если к ним обращаются извне):
    exports ru.citlab24.vozdyh;
    exports ru.citlab24.vozdyh.controllers;
    exports ru.citlab24.vozdyh.service;
}
