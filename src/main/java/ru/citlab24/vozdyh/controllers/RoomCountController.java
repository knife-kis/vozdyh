package ru.citlab24.vozdyh.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

public class RoomCountController {
    @FXML
    private ChoiceBox<Integer> roomCountChoice;
    @FXML private Button proceedButton;

    private Stage stage;
    private int selectedCount;

    @FXML
    public void initialize() {
        roomCountChoice.getItems().addAll(1, 2, 3);
        roomCountChoice.setValue(1);

        proceedButton.setOnAction(e -> {
            selectedCount = roomCountChoice.getValue();
            stage.close();
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getSelectedCount() {
        return selectedCount;
    }
}
