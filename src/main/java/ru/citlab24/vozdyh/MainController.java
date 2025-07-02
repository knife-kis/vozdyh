package ru.citlab24.vozdyh;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;

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