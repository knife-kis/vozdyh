package ru.citlab24.vozdyh;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.converter.NumberStringConverter;

public class RoomInputPanel extends GridPane {
    private final RoomData room;
    private final Label volumeLabel = new Label();
    private final Label coefficientLabel = new Label();
    private final Label n50Label = new Label();

    public RoomInputPanel(RoomData room, int index) {
        this.room = room;
        initUI(index);
    }

    private void initUI(int index) {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(15));
        setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5;");

        Label title = new Label("Квартира #" + index);
        title.setStyle("-fx-font-weight: bold;");
        add(title, 0, 0, 2, 1);

        // Поля ввода
        add(new Label("Название:"), 0, 1);
        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(room.nameProperty());
        add(nameField, 1, 1);

        add(new Label("Площадь (м²):"), 0, 2);
        TextField areaField = new TextField();
        areaField.textProperty().bindBidirectional(room.areaProperty(), new NumberStringConverter());
        add(areaField, 1, 2);

        add(new Label("Высота (м):"), 0, 3);
        TextField heightField = new TextField();
        heightField.textProperty().bindBidirectional(room.heightProperty(), new NumberStringConverter());
        add(heightField, 1, 3);

        add(new Label("Объем:"), 0, 4);
        add(volumeLabel, 1, 4);

        add(new Label("Коэффициент:"), 0, 5);
        add(coefficientLabel, 1, 5);

        add(new Label("n50:"), 0, 6);
        add(n50Label, 1, 6);
    }
    public void updateResults() {
        volumeLabel.setText(String.format("%.2f м³", room.getVolume()));
        coefficientLabel.setText(String.format("%.4f", room.getCoefficient()));
        n50Label.setText(String.format("%.2f ч⁻¹", room.getN50()));
    }
}