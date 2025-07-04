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
        public void start(Stage primaryStage) throws Exception {
            // Загружаем основное FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/citlab24/vozdyh/main.fxml"));
            Parent root = loader.load();

            // Получаем контроллер
            MainController controller = loader.getController();

            // Инициализируем с 1 комнатой по умолчанию
            controller.initializeRooms(1);

            // Настраиваем сцену
            Scene scene = new Scene(root);
            primaryStage.setTitle("Протокол испытаний");
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        public static void main(String[] args) {
            launch(args);
        }
}