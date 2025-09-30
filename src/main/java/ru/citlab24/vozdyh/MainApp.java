package ru.citlab24.vozdyh;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.citlab24.vozdyh.controllers.MainController;

import java.io.InputStream;
import java.util.Properties;

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

// читаем версию из pom.properties
            String appVersion = null;

// 1) Пытаемся взять из app.properties (работает и в IDE)
            try (InputStream in = MainApp.class.getResourceAsStream("/app.properties")) {
                if (in != null) {
                    Properties p = new Properties();
                    p.load(in);
                    appVersion = p.getProperty("app.version");
                }
            } catch (Exception ignore) {}

// 2) Пытаемся взять из манифеста JAR (работает в упакованном jar/exe)
            if (appVersion == null || appVersion.isBlank()) {
                Package pkg = MainApp.class.getPackage();
                if (pkg != null) {
                    appVersion = pkg.getImplementationVersion();
                }
            }

// 3) Пытаемся взять из pom.properties (тоже из JAR)
            if (appVersion == null || appVersion.isBlank()) {
                try (InputStream pin = MainApp.class.getResourceAsStream(
                        "/META-INF/maven/ru.citlab24/Vozdyh/pom.properties")) {
                    if (pin != null) {
                        Properties pp = new Properties();
                        pp.load(pin);
                        appVersion = pp.getProperty("version");
                    }
                } catch (Exception ignore) {}
            }

// 4) Дефолт
            if (appVersion == null || appVersion.isBlank()) {
                appVersion = "dev";
            }

            String v = (appVersion == null || appVersion.isBlank()) ? "dev" : appVersion.trim();
            primaryStage.setTitle("Протокол испытаний v" + v);
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        public static void main(String[] args) {
            launch(args);
        }
}