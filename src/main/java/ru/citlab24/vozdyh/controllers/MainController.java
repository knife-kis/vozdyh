package ru.citlab24.vozdyh.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;
import ru.citlab24.vozdyh.DocxGenerator;
import ru.citlab24.vozdyh.MainModel;
import ru.citlab24.vozdyh.RoomData;
import ru.citlab24.vozdyh.RoomInputPanel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    @FXML private VBox roomsContainer;
    @FXML private TextField protocolNumberField;
    @FXML private DatePicker testDatePicker;
    @FXML private TextField customerField;
    @FXML private TextField customerAddressField;
    @FXML private TextField objectNameField;
    @FXML private ChoiceBox<String> buildingTypeChoiceBox;
    @FXML private TextField wallTypeField;
    @FXML private TextField windowTypeField;
    @FXML private ChoiceBox<String> ventilationTypeChoiceBox;
    @FXML
    private TextField roomCountField;
    @FXML
    private Button applyRoomCountButton;

    private final MainModel model = new MainModel();
    private List<RoomInputPanel> roomPanels = new ArrayList<>();


    @FXML
    public void initialize() {
        protocolNumberField.textProperty().bindBidirectional(model.protocolNumberProperty());
        testDatePicker.valueProperty().bindBidirectional(model.testDateProperty());
        customerField.textProperty().bindBidirectional(model.customerProperty());
        objectNameField.textProperty().bindBidirectional(model.objectNameProperty());
        buildingTypeChoiceBox.valueProperty().bindBidirectional(model.buildingTypeProperty());
        customerAddressField.textProperty().bindBidirectional(model.customerAddressProperty());
        wallTypeField.textProperty().bindBidirectional(model.wallTypeProperty());
        windowTypeField.textProperty().bindBidirectional(model.windowTypeProperty());
        ventilationTypeChoiceBox.getItems().addAll("естественная", "искусственная");
        ventilationTypeChoiceBox.valueProperty().bindBidirectional(model.ventilationTypeProperty());

        buildingTypeChoiceBox.getItems().addAll("Жилое", "Общественное");
        applyRoomCountButton.setOnAction(event -> handleRoomCountApply());

        roomCountField.setText("3");
        handleRoomCountApply(); // инициализировать три панели
    }
    @FXML
    private void handleRoomCountApply() {
        try {
            int count = Integer.parseInt(roomCountField.getText());
            if (count > 0 && count <= 10) { // Ограничение до 10 комнат
                initializeRooms(count);
            } else {
                showError("Некорректное количество", "Введите число от 1 до 10");
            }
        } catch (NumberFormatException e) {
            showError("Ошибка ввода", "Пожалуйста, введите целое число");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void initializeRooms(int count) {
        roomsContainer.getChildren().clear();
        roomPanels.clear();
        model.getRooms().clear();

        for (int i = 0; i < count; i++) {
            RoomData room = new RoomData();
            model.getRooms().add(room);

            RoomInputPanel panel = new RoomInputPanel(room, i + 1);
            roomPanels.add(panel);
            roomsContainer.getChildren().add(panel);
        }

        // Обновляем поле с количеством комнат
        roomCountField.setText(String.valueOf(count));
    }

    @FXML
    private void handleCalculate() {
        model.calculateCoefficients();
        for (RoomInputPanel panel : roomPanels) {
            panel.updateResults();
        }
    }

    @FXML
    private void handleSave() {
        try {
            URL templateUrl = getClass().getResource("/ru/citlab24/vozdyh/Шаблон.docx");
            if (templateUrl == null) {
                throw new RuntimeException("Файл шаблона не найден в ресурсах!");
            }

            File savedFile = DocxGenerator.generateDocument(model, templateUrl.getFile());
            int actualRoomCount = model.getRooms().size();
            System.out.println("Saving document with " + actualRoomCount + " rooms");
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Файл успешно сохранён как:\n" + savedFile.getName(), ButtonType.OK);
            ok.setHeaderText("Готово");
            ok.showAndWait();

        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Не удалось сохранить документ:\n" + e.getMessage(),
                    ButtonType.OK);
            error.setHeaderText("Ошибка сохранения");
            error.showAndWait();
            e.printStackTrace();
        }
    }
}