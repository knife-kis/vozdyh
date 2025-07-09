package ru.citlab24.vozdyh;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;

public class RoomData {
    private final StringProperty name = new SimpleStringProperty("Квартира");
    private final DoubleProperty area = new SimpleDoubleProperty(0);
    private final DoubleProperty height = new SimpleDoubleProperty(0);
    private final DoubleProperty volume = new SimpleDoubleProperty(0);
    private final DoubleProperty coefficient = new SimpleDoubleProperty(0);
    private final DoubleProperty n50 = new SimpleDoubleProperty(0);
    private final DoubleProperty windowArea = new SimpleDoubleProperty(0);
    private final List<Double> qValues = new ArrayList<>();
    private StringProperty floor = new SimpleStringProperty("");
    private final StringProperty apartmentType = new SimpleStringProperty("");
    private final DoubleProperty wallArea = new SimpleDoubleProperty();
    private final DoubleProperty wallArea1 = new SimpleDoubleProperty();
    private final DoubleProperty wallArea2 = new SimpleDoubleProperty();
    private final DoubleProperty wallArea3 = new SimpleDoubleProperty();
    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public double getArea() {
        return area.get();
    }

    public DoubleProperty areaProperty() {
        return area;
    }
    public String getFloor() {
        return floor.get();
    }
    public double getWindowArea() {
        return windowArea.get();
    }
    public String getFullName() {
        String type = getApartmentType();
        String base = getName();

        if (type == null || type.isEmpty())
            return base;

        return type + " " + base;
    }
    public String getApartmentType() { return apartmentType.get(); }
    public void setApartmentType(String value) { apartmentType.set(value); }
    public StringProperty apartmentTypeProperty() { return apartmentType; }
    public DoubleProperty windowAreaProperty() {
        return windowArea;
    }

    public void setWindowArea(double windowArea) {
        this.windowArea.set(windowArea);
    }

    public StringProperty floorProperty() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor.set(floor);
    }
    public void setArea(double area) {
        this.area.set(area);
    }

    public double getHeight() {
        return height.get();
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public void setHeight(double height) {
        this.height.set(height);
    }

    public double getVolume() {
        return volume.get();
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume.set(volume);
    }

    public double getCoefficient() {
        return coefficient.get();
    }

    public DoubleProperty coefficientProperty() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient.set(coefficient);
    }

    public double getN50() {
        return n50.get();
    }

    public DoubleProperty n50Property() {
        return n50;
    }

    public void setN50(double n50) {
        this.n50.set(n50);
    }

    public List<Double> getqValues() {
        return qValues;
    }

    public RoomData() {
    }

    public List<Double> getQValues() {
        return qValues;
    }

    public void setQValues(List<Double> calculatedQValues) {
        qValues.clear();
        qValues.addAll(calculatedQValues);
    }

    public double getWallArea1() {
        return wallArea1.get();
    }

    public DoubleProperty wallArea1Property() {
        return wallArea1;
    }

    public void setWallArea1(double wallArea1) {
        this.wallArea1.set(wallArea1);
    }

    public double getWallArea2() {
        return wallArea2.get();
    }

    public DoubleProperty wallArea2Property() {
        return wallArea2;
    }

    public void setWallArea2(double wallArea2) {
        this.wallArea2.set(wallArea2);
    }

    public double getWallArea3() {
        return wallArea3.get();
    }

    public DoubleProperty wallArea3Property() {
        return wallArea3;
    }

    public void setWallArea3(double wallArea3) {
        this.wallArea3.set(wallArea3);
    }

    public double getWallArea() {
        return wallArea.get();
    }

    public DoubleProperty wallAreaProperty() {
        return wallArea;
    }

    public void setWallArea(double wallArea) {
        this.wallArea.set(wallArea);
    }
}
