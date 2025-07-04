package ru.citlab24.vozdyh;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MainModel {
    private static final double[][] INITIAL_Q_VALUES = {
            {512.4, 512.6, 512.5, 512.5, 512.6},
            {375.3, 375.4, 375.2, 375.3, 375.3},
            {288.3, 288.4, 288.2, 288.2, 288.3},
            {195.8, 195.8, 195.7, 195.6, 195.8},
            {94.5, 94.6, 94.6, 94.5, 94.6}
    };

    private final ObservableList<RoomData> rooms = FXCollections.observableArrayList();
    private final ObjectProperty<LocalDate> testDate = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty protocolNumber = new SimpleStringProperty();
    private final StringProperty customer = new SimpleStringProperty();
    private final StringProperty objectName = new SimpleStringProperty();
    private final StringProperty buildingType = new SimpleStringProperty("Жилое");
    private final StringProperty customerAddress = new SimpleStringProperty();
    private final StringProperty wallTypeProperty = new SimpleStringProperty("");
    private final StringProperty windowTypeProperty = new SimpleStringProperty("ПВХ переплетов с двухкамерным стеклопакетом");
    private final StringProperty ventilationTypeProperty = new SimpleStringProperty("");
    private final IntegerProperty airExchangeNormProperty = new SimpleIntegerProperty(4);

    public MainModel() {
        // Инициализация 3 комнат по умолчанию
        for (int i = 0; i < 3; i++) {
            rooms.add(new RoomData());
        }
    }

    public void calculateCoefficients() {
        for (RoomData room : rooms) {
            double volume = room.getArea() * room.getHeight();
            room.setVolume(volume);

            // Определяем целевой диапазон n50
            double minN50 = buildingType.get().equals("Жилое") ? 2.3 : 1.3;
            double maxN50 = buildingType.get().equals("Жилое") ? 2.7 : 1.7;

            // Генерируем случайное целевое значение в диапазоне
            double targetN50 = ThreadLocalRandom.current().nextDouble(minN50, maxN50);

            // Рассчитываем коэффициент
            double initialQ50Avg = Arrays.stream(INITIAL_Q_VALUES[0]).average().orElse(0);
            double coefficient = (targetN50 * volume) / initialQ50Avg;
            room.setCoefficient(coefficient);

            // Рассчитываем все значения Qm
            List<Double> calculatedQValues = new ArrayList<>();
            for (double[] row : INITIAL_Q_VALUES) {
                for (double value : row) {
                    calculatedQValues.add(value * coefficient);
                }
            }
            room.setQValues(calculatedQValues);

            // Рассчитываем фактическое n50 для проверки
            double q50Avg = calculatedQValues.subList(0, 5).stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
            room.setN50(q50Avg / volume);

            buildingTypeProperty().addListener((obs, oldVal, newVal) -> {
                setAirExchangeNorm("Общественное".equals(newVal) ? 2 : 4);
            });
        }

    }

    public ObservableList<RoomData> getRooms() {
        return rooms;
    }

    public LocalDate getTestDate() {
        return testDate.get();
    }

    public ObjectProperty<LocalDate> testDateProperty() {
        return testDate;
    }

    public void setTestDate(LocalDate testDate) {
        this.testDate.set(testDate);
    }

    public String getProtocolNumber() {
        return protocolNumber.get();
    }

    public StringProperty protocolNumberProperty() {
        return protocolNumber;
    }

    public String getCustomerAddress() {
        return customerAddress.get();
    }

    public StringProperty customerAddressProperty() {
        return customerAddress;
    }
    public String getWallType() { return wallTypeProperty.get(); }
    public void setWallType(String value) { wallTypeProperty.set(value); }
    public StringProperty wallTypeProperty() { return wallTypeProperty; }

    public String getWindowType() { return windowTypeProperty.get(); }
    public void setWindowType(String value) { windowTypeProperty.set(value); }
    public StringProperty windowTypeProperty() { return windowTypeProperty; }

    public String getVentilationType() { return ventilationTypeProperty.get(); }
    public void setVentilationType(String value) { ventilationTypeProperty.set(value); }
    public StringProperty ventilationTypeProperty() { return ventilationTypeProperty; }

    public int getAirExchangeNorm() { return airExchangeNormProperty.get(); }
    public void setAirExchangeNorm(int value) { airExchangeNormProperty.set(value); }
    public IntegerProperty airExchangeNormProperty() { return airExchangeNormProperty; }
    public void setCustomerAddress(String customerAddress) {
        this.customerAddress.set(customerAddress);
    }
    public void setProtocolNumber(String protocolNumber) {
        this.protocolNumber.set(protocolNumber);
    }

    public String getCustomer() {
        return customer.get();
    }

    public StringProperty customerProperty() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer.set(customer);
    }

    public String getObjectName() {
        return objectName.get();
    }

    public StringProperty objectNameProperty() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName.set(objectName);
    }

    public String getBuildingType() {
        return buildingType.get();
    }

    public StringProperty buildingTypeProperty() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType.set(buildingType);
    }


}
