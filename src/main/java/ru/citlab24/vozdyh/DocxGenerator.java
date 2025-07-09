package ru.citlab24.vozdyh;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.*;
import ru.citlab24.vozdyh.service.WeatherService;

public class DocxGenerator {
    public static File generateDocument(MainModel model, String templatePath) throws IOException {
        WeatherService.WeatherData weather = WeatherService.getKrasnoyarskWeather();
        String rawProtocol = model.getProtocolNumber();
        boolean useDefaultProtocol = rawProtocol == null || rawProtocol.isBlank();
        String protocolNumber = useDefaultProtocol ? "10" : rawProtocol;
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            throw new FileNotFoundException("Файл шаблона не найден: " + templatePath);
        }
        String[] timeIntervals = generateTimeIntervals(model.getTimeOfDay());
        model.setPressure(weather.getPressure());
        model.setWindSpeed(weather.getWindSpeed());
        model.setTemperature(weather.getTemperature());

        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(templatePath))) {
            LocalDate date = model.getTestDate();
            LocalDate nextDay = date.plusDays(1);
            int year = date.getYear() % 100;

            // Форматированные значения
            String nextDayStr = String.format("%02d", nextDay.getDayOfMonth());
            String currentMonth = date.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"));
            String fullYear = String.valueOf(date.getYear());
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            // Основные замены
            replaceCommonPlaceholders(doc, model, protocolNumber, year, nextDayStr, currentMonth, fullYear, formattedDate);

            // Обработка комнат
            processRooms(doc, model);

            // +++ ДОБАВЛЕНО: Расчет статистик +++
            calculateAndReplaceSummary(doc, model);
            replaceTimePlaceholders(doc, timeIntervals);

            // Генерация уникального имени файла
            return saveDocument(doc);

        }
    }
    private static String[] generateTimeIntervals(String timeOfDay) {
        String[] intervals = new String[6];
        Random random = new Random();

        // Базовое время в зависимости от выбора
        int baseHour;
        if ("вечер".equalsIgnoreCase(timeOfDay)) {
            baseHour = 14; // Для вечера начинаем в 14 часов
        } else {
            baseHour = 9; // Для утра - в 9 часов
        }

        // Случайные минуты: 0, 5 или 10
        int baseMinute = random.nextInt(3) * 5;

        // Первое измерение
        LocalTime start1 = LocalTime.of(baseHour, baseMinute);
        int duration1 = 40 + random.nextInt(11); // 40-50 минут
        LocalTime end1 = start1.plusMinutes(duration1);

        // Второе измерение
        int gap1 = 10 + random.nextInt(11); // 10-20 минут
        LocalTime start2 = end1.plusMinutes(gap1);
        int duration2 = 40 + random.nextInt(11); // 40-50 минут
        LocalTime end2 = start2.plusMinutes(duration2);

        // Третье измерение
        int gap2 = 10 + random.nextInt(11); // 10-20 минут
        LocalTime start3 = end2.plusMinutes(gap2);
        int duration3 = 40 + random.nextInt(11); // 40-50 минут
        LocalTime end3 = start3.plusMinutes(duration3);

        // Форматирование в строки HH:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        intervals[0] = start1.format(formatter);
        intervals[1] = end1.format(formatter);
        intervals[2] = start2.format(formatter);
        intervals[3] = end2.format(formatter);
        intervals[4] = start3.format(formatter);
        intervals[5] = end3.format(formatter);

        return intervals;
    }
    private static void replaceTimePlaceholders(XWPFDocument doc, String[] intervals) {
        replaceAllText(doc, "[время утро/вечер начало 1]", intervals[0]);
        replaceAllText(doc, "[время утро/вечер конец 1]", intervals[1]);
        replaceAllText(doc, "[время утро/вечер начало 2]", intervals[2]);
        replaceAllText(doc, "[время утро/вечер конец 2]", intervals[3]);
        replaceAllText(doc, "[время утро/вечер начало 3]", intervals[4]);
        replaceAllText(doc, "[время утро/вечер конец 3]", intervals[5]);
    }

    private static void calculateAndReplaceSummary(XWPFDocument doc, MainModel model) {
        List<RoomData> rooms = model.getRooms();
        if (rooms.isEmpty()) return;

        // Собираем значения n50 и M (расчетную величину)
        List<Double> n50Values = new ArrayList<>();

        for (RoomData room : rooms) {
            n50Values.add(room.getN50());
        }

        // Рассчитываем статистики
        double minN = Collections.min(n50Values);
        double maxN = Collections.max(n50Values);
        double avgN = n50Values.stream().mapToDouble(d -> d).average().orElse(0);


        // Заменяем плейсхолдеры
        replaceAllText(doc, "[minN]", formatDouble(minN));
        replaceAllText(doc, "[maxN]", formatDouble(maxN));
        replaceAllText(doc, "[avgN]", formatDouble(avgN));
    }

    private static void replaceCommonPlaceholders(XWPFDocument doc, MainModel model,
                                                  String protocolNumber, int year,
                                                  String nextDayStr, String currentMonth,
                                                  String fullYear, String formattedDate) {
        // Основные замены
        replaceAllText(doc, "[номер/год]", protocolNumber + "/" + year);
        replaceAllText(doc, "[следующий день]", nextDayStr);
        replaceAllText(doc, "[текущий месяц]", currentMonth);
        replaceAllText(doc, "[текущий год]", fullYear);
        replaceAllText(doc, "[текущая дата]", formattedDate);
        replaceAllText(doc, "[ProtocolNumber]", protocolNumber);
        replaceAllText(doc, "[TestDate]", formattedDate);

        // Замены связанные с объектом
        replaceAllText(doc, "[Customer]", getSafeValue(model.getCustomer()));
        replaceAllText(doc, "[ObjectName]", getSafeValue(model.getObjectName()));
        replaceAllText(doc, "[наименование объекта]", getSafeValue(model.getObjectName()));
        replaceAllText(doc, "[Наименование заказчика]", getSafeValue(model.getCustomer()));
        replaceAllText(doc, "[юридический адрес заказчика]", getSafeValue(model.getCustomerAddress()));

        // Технические параметры
        replaceAllText(doc, "[вид стен и их толщина]", getSafeValue(model.getWallType()));
        replaceAllText(doc, "[вид оконных блоков]", getSafeValue(model.getWindowType()));
        replaceAllText(doc, "[естественная/искусственная]", getSafeValue(model.getVentilationType()));
        replaceAllText(doc, "[Класс]", getSafeValue(model.getBuildingClass()));
        replaceAllText(doc, "[2/4 (два для общественных зданий / 4 для жилых домов]",
                String.valueOf(model.getAirExchangeNorm()));
        replaceCheMinusOneWithSuperscript(doc);
        replaceAllText(doc, "[давление]", formatDouble(model.getPressure()));
        replaceAllText(doc, "[скорость ветра]", formatDouble(model.getWindSpeed()) + " м/с");
        replaceAllText(doc, "[температура улица]", "+ " + formatDouble(model.getTemperature()));
    }

    private static void replaceCheMinusOneWithSuperscript(XWPFDocument doc) {
        // Обработка всех элементов документа
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceInParagraphCheMinusOne(p);
        }

        // Обработка таблиц
        for (XWPFTable tbl : doc.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraphCheMinusOne(p);
                    }
                }
            }
        }

        // Обработка колонтитулов
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                replaceInParagraphCheMinusOne(p);
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                replaceInParagraphCheMinusOne(p);
            }
        }
    }

    private static void replaceInParagraphCheMinusOne(XWPFParagraph p) {
        List<XWPFRun> runs = p.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            String text = run.getText(0);
            if (text != null && text.contains("ч-1")) {
                // Сохраняем стиль
                String color = run.getColor();
                String fontFamily = run.getFontFamily();
                int fontSize = run.getFontSize();
                boolean bold = run.isBold();
                boolean italic = run.isItalic();
                UnderlinePatterns ul = run.getUnderline();

                // Разбиваем текст на части
                String[] parts = text.split("ч-1", -1);

                // Удаляем исходный run
                p.removeRun(i);

                // Вставляем новые runs для всех частей
                for (int k = 0; k < parts.length; k++) {
                    if (!parts[k].isEmpty()) {
                        XWPFRun r = p.insertNewRun(i++);
                        r.setText(parts[k]);
                        copyStyle(r, color, fontFamily, fontSize, bold, italic, ul);
                    }

                    if (k < parts.length - 1) {
                        // Вставляем "ч" как обычный текст
                        XWPFRun chRun = p.insertNewRun(i++);
                        chRun.setText("ч");
                        copyStyle(chRun, color, fontFamily, fontSize, bold, italic, ul);

                        // Вставляем "−1" как верхний индекс
                        XWPFRun supRun = p.insertNewRun(i++);
                        supRun.setText("−1");
                        supRun.setSubscript(VerticalAlign.SUPERSCRIPT);
                        copyStyle(supRun, color, fontFamily, fontSize, bold, italic, ul);
                    }
                }
                i--; // Корректируем индекс после вставки
            }
        }
    }

    // Вспомогательный метод для восстановления стиля run'а
    private static void copyStyle(XWPFRun target,
                                  String color, String fontFamily, int fontSize,
                                  boolean bold, boolean italic, UnderlinePatterns ul) {
        if (color      != null)       target.setColor(color);
        if (fontFamily != null)       target.setFontFamily(fontFamily);
        if (fontSize   > 0)           target.setFontSize(fontSize);
        target.setBold(bold);
        target.setItalic(italic);
        target.setUnderline(ul);
    }

    private static String getSafeValue(String value) {
        return value != null ? value : "";
    }

    private static void processRooms(XWPFDocument doc, MainModel model) {
        List<RoomData> rooms = model.getRooms();

        for (int i = 0; i < rooms.size(); i++) {
            RoomData room = rooms.get(i);
            int roomIndex = i + 1;

            // Основные параметры комнаты
            replaceRoomData(doc, room, roomIndex);
        }
    }

    private static void replaceRoomData(XWPFDocument doc, RoomData room, int roomIndex) {
        // Основные параметры
        replaceAllText(doc, "[Room" + roomIndex + ".Name]", getSafeValue(room.getFullName()));
        replaceAllText(doc, "[Room" + roomIndex + ".Floor]", getSafeValue(room.getFloor()));
        replaceAllText(doc, "[Room" + roomIndex + ".Area]", formatDouble(room.getArea()));
        replaceAllText(doc, "[Room" + roomIndex + ".Height]", formatDouble(room.getHeight()));
        replaceAllText(doc, "[Room" + roomIndex + ".Volume]", formatDouble(room.getVolume()));
        replaceAllText(doc, "[Room" + roomIndex + ".WindowArea]", formatDouble(room.getWindowArea()));

        // Расчетные параметры
        double avg = calculateAverage(room.getQValues());
        replaceAllText(doc, "[Room" + roomIndex + ".расчетная величина]", formatDouble(avg));
        replaceAllText(doc, "[Room" + roomIndex + ".n50]", formatDouble(room.getN50()));
        replaceAllText(doc, "[Room" + roomIndex + ".QmAvg]", formatDouble(avg));
        replaceAllText(doc, "[Room" + roomIndex + ".N50]", formatDouble(room.getN50()));

        // Дополнительные замены
        replaceAllText(doc, "[угловая/рядовая/торцевая квартира №]", getSafeValue(room.getFullName()));
        replaceAllText(doc, "[номер этажа]", getSafeValue(room.getFloor()));
        replaceAllText(doc, "[пло-щадь внешней стены]", formatDouble(room.getArea()));
        replaceAllText(doc, "[площадь окон]", formatDouble(room.getWindowArea()));

        // Для каждой комнаты заменяем только один плейсхолдер
        if (roomIndex == 1) {
            replaceAllText(doc, "[площадь стен1]", formatDouble(room.getWallArea()));
        } else if (roomIndex == 2) {
            replaceAllText(doc, "[площадь стен2]", formatDouble(room.getWallArea()));
        } else if (roomIndex == 3) {
            replaceAllText(doc, "[площадь стен3]", formatDouble(room.getWallArea()));
        }

        // Также заменяем специфичные плейсхолдеры для каждой комнаты
        replaceAllText(doc, "[Room" + roomIndex + ".WallArea]", formatDouble(room.getWallArea()));
    }

    private static String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private static double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        return values.stream()
                .limit(5)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private static File saveDocument(XWPFDocument doc) throws IOException {
        String baseName = "Протокол_испытаний";
        String extension = ".docx";
        File outputFile;
        int counter = 1;

        do {
            String fileName = counter == 1
                    ? baseName + extension
                    : baseName + "_" + counter + extension;
            outputFile = new File(fileName);
            counter++;
        } while (outputFile.exists());

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            doc.write(out);
        }
        return outputFile;
    }
    private static class RunStyle {
        String color;
        String fontFamily;
        int fontSize;
        boolean isBold;
        boolean isItalic;
        UnderlinePatterns underline;
    }

    private static RunStyle extractRunStyle(XWPFRun run) {
        RunStyle style = new RunStyle();
        try { style.color = run.getColor(); } catch (Exception e) { style.color = null; }
        try { style.fontFamily = run.getFontFamily(); } catch (Exception e) { style.fontFamily = null; }
        try { style.fontSize = run.getFontSize(); } catch (Exception e) { style.fontSize = -1; }
        try { style.isBold = run.isBold(); } catch (Exception e) { style.isBold = false; }
        try { style.isItalic = run.isItalic(); } catch (Exception e) { style.isItalic = false; }
        try { style.underline = run.getUnderline(); } catch (Exception e) { style.underline = UnderlinePatterns.NONE; }
        return style;
    }

    private static void applyRunStyle(XWPFRun target, RunStyle style) {
        if (style == null) return;
        if (style.color != null) target.setColor(style.color);
        if (style.fontFamily != null) target.setFontFamily(style.fontFamily);
        if (style.fontSize > 0) target.setFontSize(style.fontSize);
        target.setBold(style.isBold);
        target.setItalic(style.isItalic);
        target.setUnderline(style.underline);
    }
    private static void replaceInParagraphSimple(XWPFParagraph p, String placeholder, String replacement) {
        String fullText = p.getText();
        if (fullText == null || !fullText.contains(placeholder)) return;

        // Сохраняем все стили runs
        List<RunStyle> styles = new ArrayList<>();
        List<String> runTexts = new ArrayList<>();

        for (XWPFRun run : p.getRuns()) {
            styles.add(extractRunStyle(run));
            runTexts.add(run.getText(0));
        }

        // Очищаем параграф
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        // Восстанавливаем текст с заменой
        String newText = fullText.replace(placeholder, replacement);

        // Разбиваем новый текст на части, соответствующие оригинальным runs
        int currentPos = 0;
        for (int i = 0; i < runTexts.size(); i++) {
            String originalRunText = runTexts.get(i);
            if (originalRunText == null) continue;

            int endPos = currentPos + originalRunText.length();
            if (endPos > newText.length()) endPos = newText.length();

            String part = newText.substring(currentPos, endPos);
            currentPos = endPos;

            XWPFRun newRun = p.createRun();
            newRun.setText(part);
            applyRunStyle(newRun, styles.get(i)); // Применяем оригинальный стиль
        }
        if (currentPos < newText.length()) {
            String remaining = newText.substring(currentPos);
            XWPFRun newRun = p.createRun();
            newRun.setText(remaining);

            // Применяем стиль последнего run'а
            if (!styles.isEmpty()) {
                RunStyle lastStyle = styles.get(styles.size() - 1);
                lastStyle.isBold = false; // Убираем жирный
                applyRunStyle(newRun, lastStyle);
            }
        }
    }

    private static void copyRunStyle(XWPFRun target, XWPFRun source) {
        if (source == null) return;

        target.setColor(source.getColor());
        target.setFontFamily(source.getFontFamily());
        target.setFontSize(source.getFontSize());
        target.setBold(source.isBold());
        target.setItalic(source.isItalic());
        target.setUnderline(source.getUnderline());
        // Копирование других важных свойств
        target.setStrike(source.isStrike());
        target.setTextPosition(source.getTextPosition());
    }

    private static void replaceAllText(XWPFDocument doc, String placeholder, String replacement) {
        // Заменяем null на пустую строку
        String safeReplacement = replacement == null ? "" : replacement;

        // Обработка основного контента
        replaceInBody(doc, placeholder, safeReplacement);

        // Обработка колонтитулов
        for (XWPFHeader header : doc.getHeaderList()) {
            replaceInBody(header, placeholder, safeReplacement);
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            replaceInBody(footer, placeholder, safeReplacement);
        }

    }

    private static void replaceInBody(IBody body, String placeholder, String replacement) {
        // Обработка параграфов
        for (XWPFParagraph p : body.getParagraphs()) {
            replaceInParagraphSimple(p, placeholder, replacement);
        }

        // Обработка таблиц
        for (XWPFTable table : body.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraphSimple(p, placeholder, replacement);
                    }
                }
            }
        }
    }

    private static void replaceAnchoredTableCells(XWPFDocument doc, List<RoomData> rooms) {
        // rooms.get(0) → первая комната → таблица index=1, rooms.get(1)→таблица index=2 и т.д.
        for (int t = 0; t < rooms.size(); t++) {
            List<Double> qValues = rooms.get(t).getQValues();
            // для каждой из 5 строк и 5 столбцов
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    // номер таблицы: t+1
                    // строка: r+1, столбец c+1
                    String placeholder = String.format("[Qm%d-%d-%d]", t+1, r+1, c+1);
                    int idx = r * 5 + c;
                    String replacement = idx < qValues.size()
                            ? String.format("%.1f", qValues.get(idx))
                            : "";
                    // простая замена текста (в параграфах и в таблицах)
                    replaceAllText(doc, placeholder, replacement);
                }
            }
        }
    }
    private static void replaceTableDataSafe(XWPFDocument doc, String tableTitle, List<Double> values) {
        if (values == null || values.isEmpty()) return;

        for (XWPFTable table : doc.getTables()) {
            if (table.getText().contains(tableTitle)) {
                int startRow = 1; // предположим: первая строка — заголовок таблицы (с ΔP Qm Qm Qm...)
                int valuesIndex = 0;

                for (int i = 0; i < 5; i++) { // 5 строк по ΔP (50, 40, 30, 20, 10)
                    if ((startRow + i) >= table.getRows().size()) break;
                    XWPFTableRow row = table.getRow(startRow + i);

                    // Столбцы 1-5: Qm (0 — это ΔP, пропускаем)
                    for (int j = 1; j <= 5; j++) {
                        if (valuesIndex >= values.size()) break;
                        if (j >= row.getTableCells().size()) break;

                        XWPFTableCell cell = row.getCell(j);
                        cell.removeParagraph(0);
                        XWPFParagraph p = cell.addParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(String.format("%.1f", values.get(valuesIndex++)));
                    }
                }
                break; // таблицу нашли и заполнили — выходим
            }
        }
    }
}