package ru.citlab24.vozdyh;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.converter.NumberStringConverter;

public class RoomInputPanel extends GridPane {
    private final RoomData room;
    private final TextField nameField = new TextField();
    private final TextField floorField = new TextField();
    private final TextField areaField = new TextField();
    private final TextField windowAreaField = new TextField();
    private final TextField heightField = new TextField();
    private final Label volumeLabel = new Label();
    private final Label coefficientLabel = new Label();
    private final Label n50Label = new Label();


    public RoomInputPanel(RoomData room, int index) {

        this.room = room;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(15));
        setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5;");

        // Заголовок
        add(new Label("Помещение №" + index), 0, 0, 2, 1);

        // Наименование
        add(new Label("Наименование:"), 0, 1);
        add(nameField, 1, 1);
        nameField.textProperty().bindBidirectional(room.nameProperty());

        // Этаж
        add(new Label("Этаж:"), 0, 2);
        add(floorField, 1, 2);
        floorField.textProperty().bindBidirectional(room.floorProperty());

        // Площадь
        add(new Label("Площадь (м²):"), 0, 3);
        add(areaField, 1, 3);
        areaField.textProperty().bindBidirectional(
                room.areaProperty(), new NumberStringConverter()
        );

        // Площадь окон
        add(new Label("Площадь окон (м²):"), 0, 4);
        add(windowAreaField, 1, 4);
        windowAreaField.textProperty().bindBidirectional(
                room.windowAreaProperty(), new NumberStringConverter()
        );

        // Высота
        add(new Label("Высота (м):"), 0, 5);
        add(heightField, 1, 5);
        heightField.textProperty().bindBidirectional(
                room.heightProperty(), new NumberStringConverter()
        );

        // Результаты
        add(new Label("Объем:"), 0, 6);
        add(volumeLabel, 1, 6);
        add(new Label("Коэффициент:"), 0, 7);
        add(coefficientLabel, 1, 7);
        add(new Label("n50:"), 0, 8);
        add(n50Label, 1, 8);

        // Пересчет при изменении
        room.heightProperty().addListener((o,ov,nv)-> updateResults());
        room.areaProperty().addListener((o,ov,nv)-> updateResults());

        updateResults();
    }

    public void updateResults() {
        volumeLabel.setText(String.format("%.2f м³", room.getVolume()));
        coefficientLabel.setText(String.format("%.4f", room.getCoefficient()));
        n50Label.setText(String.format("%.2f ч⁻¹", room.getN50()));
    }
}
