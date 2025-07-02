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
    private final List<Double> qValues = new ArrayList<>();

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

}
