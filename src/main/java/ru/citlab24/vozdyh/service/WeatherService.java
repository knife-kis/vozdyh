package ru.citlab24.vozdyh.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONObject;

public class WeatherService {

    public static WeatherData getKrasnoyarskWeather() {
        try {
            // Координаты Красноярска: 56.0184° с.ш., 92.8672° в.д.
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=56.0184&longitude=92.8672&current=pressure_msl,wind_speed_10m,temperature_2m");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONObject current = json.getJSONObject("current");

                double pressureKpa = current.getDouble("pressure_msl")/10;
                // Конвертация км/ч → м/с
                double windSpeedKph = current.getDouble("wind_speed_10m");
                double windSpeedMps = windSpeedKph / 3.6;
                // Округление температуры до целого
                double temperature = Math.round(current.getDouble("temperature_2m"));

                return new WeatherData(pressureKpa, windSpeedMps, temperature);
            }
        } catch (Exception e) {
            return new WeatherData(101.3, 0.0, 20.0); // Значения по умолчанию
        }
    }

    public static class WeatherData {
        private final double pressure;
        private final double windSpeed;
        private final double temperature;

        public WeatherData(double pressure, double windSpeed, double temperature) {
            this.pressure = pressure;
            this.windSpeed = windSpeed;
            this.temperature = temperature;
        }

        // Геттеры
        public double getPressure() { return pressure; }
        public double getWindSpeed() { return windSpeed; }
        public double getTemperature() { return temperature; }
    }
}