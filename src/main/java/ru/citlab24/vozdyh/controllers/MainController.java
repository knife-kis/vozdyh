package ru.citlab24.vozdyh.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
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
import java.util.Optional;
import javafx.stage.FileChooser;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainController {
    @FXML private VBox roomsContainer;
    @FXML private TextField protocolNumberField;
    @FXML private DatePicker testDatePicker;
    @FXML private TextField customerField;
    @FXML private TextField customerAddressField;
    @FXML private TextField objectNameField;
    @FXML private ChoiceBox<String> buildingTypeChoiceBox;
    @FXML private TextArea wallTypeField;
    @FXML private TextArea windowTypeField;
    @FXML private ChoiceBox<String> ventilationTypeChoiceBox;
    @FXML
    private TextField roomCountField;
    @FXML
    private Button applyRoomCountButton;
    @FXML
    private ChoiceBox<String> timeOfDayChoiceBox;

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

        // Инициализация выбора
        ventilationTypeChoiceBox.getItems().addAll("естественная", "искусственная");
        ventilationTypeChoiceBox.valueProperty()
                .bindBidirectional(model.ventilationTypeProperty());
        buildingTypeChoiceBox.getItems().addAll("Жилое", "Общественное");

        // Установка значений по умолчанию
        buildingTypeChoiceBox.setValue("Жилое");
        ventilationTypeChoiceBox.setValue("естественная");
        model.updateBuildingClass();

        setupAutoExpandingTextArea(wallTypeField);
        setupAutoExpandingTextArea(windowTypeField);

        // Настройка обработчика для кнопки
        applyRoomCountButton.setOnAction(event -> handleRoomCountApply());

        // Установка значения по умолчанию для количества комнат
        roomCountField.setText("3");

        // Слушатель для автоматического изменения типа вентиляции
        buildingTypeChoiceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            // Проверка, нужно ли подтверждение
            boolean needConfirmation = false;
            String currentVentilation = ventilationTypeChoiceBox.getValue();

            if (currentVentilation != null && !currentVentilation.isEmpty()) {
                // Проверяем, соответствует ли текущая вентиляция автоматической для старого типа здания
                boolean wasAutoMatch = isAutoVentilationMatch(oldVal, currentVentilation);

                // Проверяем, будет ли новое значение отличаться от автоматического
                String autoVentilation = "Жилое".equals(newVal) ? "естественная" : "искусственная";
                boolean willBeDifferent = !autoVentilation.equals(currentVentilation);

                needConfirmation = !wasAutoMatch && willBeDifferent;
            }

            if (needConfirmation) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Подтверждение изменения");
                confirm.setHeaderText("Изменение типа вентиляции");
                confirm.setContentText("При смене типа здания тип вентиляции будет изменен автоматически. Продолжить?");

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() != ButtonType.OK) {
                    // Отмена изменения - восстанавливаем предыдущее значение
                    buildingTypeChoiceBox.setValue(oldVal);
                    return;
                }
            }

            // Установка соответствующего типа вентиляции
            if ("Жилое".equals(newVal)) {
                ventilationTypeChoiceBox.setValue("естественная");
            } else if ("Общественное".equals(newVal)) {
                ventilationTypeChoiceBox.setValue("искусственная");
            }
            timeOfDayChoiceBox.getItems().addAll("утро", "вечер");
            timeOfDayChoiceBox.setValue("утро");
            timeOfDayChoiceBox.valueProperty().bindBidirectional(model.timeOfDayProperty());
        });
        // Отложенная инициализация комнат
        Platform.runLater(() -> {
            initializeRooms(3);
        });
    }
    private boolean isAutoVentilationMatch(String buildingType, String ventilationType) {
        return ("Жилое".equals(buildingType) && "естественная".equals(ventilationType)) ||
                ("Общественное".equals(buildingType) && "искусственная".equals(ventilationType));
    }
    private void setupAutoExpandingTextArea(TextArea textArea) {
        // Высота одной строки (эмпирически подобранное значение)
        final double lineHeight = 25;

        // Начальная высота для 2 строк
        final double initialHeight = 2 * lineHeight;

        textArea.setPrefHeight(initialHeight);
        textArea.setMinHeight(initialHeight);
        textArea.setMaxHeight(Region.USE_PREF_SIZE);

        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            // Считаем количество видимых строк
            int visibleLines = (int) Math.ceil(textArea.getHeight() / lineHeight);
            int actualLines = newVal.split("\n").length + 1;

            // Определяем необходимое количество строк
            int neededLines = Math.max(2, Math.min(Math.max(visibleLines, actualLines), 5));

            // Устанавливаем новую высоту
            double newHeight = neededLines * lineHeight;
            textArea.setPrefHeight(newHeight);
        });
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
            model.setTimeOfDay(timeOfDayChoiceBox.getValue());

            // 1) Попросим пользователя выбрать файл назначения
            FileChooser fc = new FileChooser();
            fc.setTitle("Сохранить протокол");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Документ Word (*.docx)", "*.docx")
            );
            // Пример дефолтного имени файла: ПРОТОКОЛ-<номер>.docx
            String proto = (model.getProtocolNumber() == null || model.getProtocolNumber().isBlank())
                    ? "Протокол" : model.getProtocolNumber().trim();
            fc.setInitialFileName("ПРОТОКОЛ-" + proto + ".docx");

            File targetFile = fc.showSaveDialog(roomsContainer.getScene().getWindow());
            if (targetFile == null) {
                return; // пользователь нажал Cancel
            }

            // 2) Достаём шаблон из ресурсов корректно (без URL.getFile())
            String templateResPath = "/ru/citlab24/vozdyh/Шаблон.docx";
            try (InputStream in = getClass().getResourceAsStream(templateResPath)) {
                if (in == null) {
                    throw new IOException("Шаблон не найден в ресурсах: " + templateResPath);
                }
                // Скопируем ресурс во временный файл (Windows не любит кириллицу в URL)
                Path tmp = Files.createTempFile("vozdyh-template-", ".docx");
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 3) Генерируем документ
                // Вариант A: если DocxGenerator принимает путь к шаблону и сам сохраняет файл,
                //            и возвращает готовый файл — как у тебя:
                File savedFile = DocxGenerator.generateDocument(model, tmp.toString());

                // Вариант B (если DocxGenerator сохраняет в фиксированное место):
                //    после генерации просто перенеси/скопируй в targetFile
                //    Files.move(savedFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);

                // Если твой DocxGenerator умеет сразу принять выходной файл,
                // лучше сделать перегрузку: generateDocument(model, templatePath, targetFile)

                // На случай, если генератор сохранит не по выбранному пути —
                // подстрахуемся: если файлы отличаются, копируем:
                if (!savedFile.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                    Files.createDirectories(targetFile.toPath().getParent());
                    Files.copy(savedFile.toPath(), targetFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                int actualRoomCount = model.getRooms().size();
                System.out.println("Saving document with " + actualRoomCount + " rooms");

                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Файл успешно сохранён:\n" + targetFile.getAbsolutePath(), ButtonType.OK);
                ok.setHeaderText("Готово");
                ok.showAndWait();
            }

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