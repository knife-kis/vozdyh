package ru.citlab24.vozdyh;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // Показываем окно выбора количества комнат
            FXMLLoader countLoader = new FXMLLoader(getClass().getResource("room-count.fxml"));
            Parent countRoot = countLoader.load();

            Stage countStage = new Stage();
            countStage.setTitle("Выберите количество квартир");
            countStage.setScene(new Scene(countRoot, 300, 200));
            countStage.initModality(Modality.APPLICATION_MODAL);

            RoomCountController countController = countLoader.getController();
            countController.setStage(countStage);

            countStage.showAndWait();

            int roomCount = countController.getSelectedCount();

            // Загружаем основное окно
            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));
            Parent root = mainLoader.load();

            MainController mainController = mainLoader.getController();
            mainController.initializeRooms(roomCount);

            Stage mainStage = new Stage();
            mainStage.setTitle("Генератор протоколов испытаний");
            mainStage.setScene(new Scene(root, 1200, 800));
            mainStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}