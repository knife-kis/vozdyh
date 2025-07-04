package ru.citlab24.vozdyh;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.VerticalAlign;

import org.apache.poi.xwpf.usermodel.*;

public class DocxGenerator {
    public static File generateDocument(MainModel model, String templatePath) throws IOException {
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            throw new FileNotFoundException("Файл шаблона не найден: " + templatePath);
        }
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(templatePath))) {

            // Основные замены
            String protocolNumber = model.getProtocolNumber();
            int year = model.getTestDate().getYear() % 100;
            LocalDate date = model.getTestDate();
            LocalDate nextDay = date.plusDays(1);

            // Форматы
            String nextDayStr = String.format("%02d", nextDay.getDayOfMonth());
            String currentMonth = date.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"));
            String fullYear = String.valueOf(date.getYear());

            // Замена новых спец. полей
            replaceAllText(doc, "[номер/год]", protocolNumber + "/" + year);
            replaceAllText(doc, "[следующий день]", nextDayStr);
            replaceAllText(doc, "[текущий месяц]", currentMonth);
            replaceAllText(doc, "[текущий год]", fullYear);
            replaceAllText(doc, "[текущая дата]", date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            replaceAllText(doc, "[ProtocolNumber]", protocolNumber);
            replaceAllText(doc, "[TestDate]", date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            replaceAllText(doc, "[Customer]", model.getCustomer());
            replaceAllText(doc, "[ObjectName]", model.getObjectName());
            replaceAllText(doc, "[наименование объекта]", model.getObjectName());
            replaceAllText(doc, "[следующий день]", nextDayStr);
            replaceAllText(doc, "[текущий месяц]", currentMonth);
            replaceAllText(doc, "[текущий год]", fullYear);
            replaceAllText(doc, "[Наименование заказчика]", model.getCustomer());
            replaceAllText(doc, "[юридический адрес заказчика]", model.getCustomerAddress());
            replaceAllText(doc, "[наименование объекта]", model.getObjectName());
            replaceAllText(doc, "[вид стен и их толщина]", model.getWallType());
            replaceAllText(doc, "[вид оконных блоков]", model.getWindowType());
            replaceAllText(doc, "[естественная/искусственная]", model.getVentilationType());
            replaceAllText(doc, "[2/4 (два для общественных зданий / 4 для жилых домов]",
                    String.valueOf(model.getAirExchangeNorm()));
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            replaceAllText(doc, "[текущая дата]", formattedDate);
            System.out.println("Customer: " + model.getCustomer());
            System.out.println("Address: " + model.getCustomerAddress());
            System.out.println("Date: " + formattedDate);

            // Обработка комнат с проверкой размера списка
            List<RoomData> rooms = model.getRooms();
            for (int i = 0; i < rooms.size(); i++) {
                RoomData room = rooms.get(i);
                int roomIndex = i + 1;

                replaceAllText(doc, "[Room" + roomIndex + ".Name]", room.getName());
                replaceAllText(doc, "[Room" + roomIndex + ".Area]", String.format("%.2f", room.getArea()));
                replaceAllText(doc, "[Room" + roomIndex + ".Height]", String.format("%.2f", room.getHeight()));
                replaceAllText(doc, "[Room" + roomIndex + ".Volume]", String.format("%.2f", room.getVolume()));

                // Безопасный расчет среднего
                List<Double> qValues = room.getQValues();
                double avg = 0;
                if (qValues != null && !qValues.isEmpty()) {
                    avg = qValues.stream()
                            .limit(5)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0);
                }
                replaceAllText(doc, "[Room" + roomIndex + ".QmAvg]", String.format("%.2f", avg));
                replaceAllText(doc, "[Room" + roomIndex + ".N50]", String.format("%.2f", room.getN50()));

                // Безопасная замена табличных данных
                replaceTableDataSafe(doc, "Таблица " + (i + 2), qValues);
            }

            // Генерация уникального имени файла
            String baseName = "Протокол_испытаний";
            String extension = ".docx";
            File outputFile;
            int counter = 1;

            do {
                String fileName = counter == 1 ?
                        baseName + extension :
                        baseName + "_" + counter + extension;
                outputFile = new File(fileName);
                counter++;
            } while (outputFile.exists());

            // Сохранение результата
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
            return outputFile;

        }
    }

    private static void replaceSpecialPlaceholder(XWPFDocument doc, String placeholder,
                                                  String[] parts, VerticalAlign[] formats) {
        // Обработка в параграфах
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceSpecialInParagraph(p, placeholder, parts, formats); // Исправленный вызов
        }

        // Обработка в таблицах
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceSpecialInParagraph(p, placeholder, parts, formats); // Исправленный вызов
                    }
                }
            }
        }
    }
    private static void replaceSpecialInParagraph(XWPFParagraph p, String placeholder,
                                                  String[] parts, VerticalAlign[] formats) {
        List<XWPFRun> runs = p.getRuns();

        for (int i = 0; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            String runText = run.getText(0);
            if (runText == null) continue;

            int idx = runText.indexOf(placeholder);
            if (idx >= 0) {
                String before = runText.substring(0, idx);
                String after = runText.substring(idx + placeholder.length());

                // Сохраняем стиль ДО удаления run
                RunStyle style = extractRunStyle(run);

                p.removeRun(i);
                int newRunIdx = i;

                if (!before.isEmpty()) {
                    XWPFRun beforeRun = p.insertNewRun(newRunIdx++);
                    beforeRun.setText(before);
                    applyRunStyle(beforeRun, style);
                }

                for (int j = 0; j < parts.length; j++) {
                    XWPFRun partRun = p.insertNewRun(newRunIdx++);
                    partRun.setText(parts[j]);
                    partRun.setVerticalAlignment(String.valueOf(formats[j]));

                    // Копируем стиль, но убираем жирное начертание
                    RunStyle nonBoldStyle = style;
                    nonBoldStyle.isBold = false;
                    applyRunStyle(partRun, nonBoldStyle);
                }

                if (!after.isEmpty()) {
                    XWPFRun afterRun = p.insertNewRun(newRunIdx++);
                    afterRun.setText(after);
                    applyRunStyle(afterRun, style);
                }
                break;
            }
        }
    }
    // Вспомогательный класс для сохранения стиля
    private static class RunStyle {
        String color;
        String fontFamily;
        int fontSize;
        boolean isBold;
        boolean isItalic;
        UnderlinePatterns underline;
    }
    // Безопасное извлечение стиля
    private static RunStyle extractRunStyle(XWPFRun run) {
        RunStyle style = new RunStyle();
        try {
            style.color = run.getColor();
        } catch (Exception e) { style.color = null; }

        try {
            style.fontFamily = run.getFontFamily();
        } catch (Exception e) { style.fontFamily = null; }

        try {
            style.fontSize = run.getFontSize();
        } catch (Exception e) { style.fontSize = -1; }

        try {
            style.isBold = run.isBold();
        } catch (Exception e) { style.isBold = false; }

        try {
            style.isItalic = run.isItalic();
        } catch (Exception e) { style.isItalic = false; }

        try {
            style.underline = run.getUnderline();
        } catch (Exception e) { style.underline = UnderlinePatterns.NONE; }

        return style;
    }

    // Безопасное применение стиля
    private static void applyRunStyle(XWPFRun target, RunStyle style) {
        if (style == null) return;

        if (style.color != null) target.setColor(style.color);
        if (style.fontFamily != null) target.setFontFamily(style.fontFamily);
        if (style.fontSize > 0) target.setFontSize(style.fontSize);

        // Убираем жирное начертание для заменяемых полей
        target.setBold(false);

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
        // Обработка основного контента
        replaceInBody(doc, placeholder, replacement);

        // Обработка колонтитулов
        for (XWPFHeader header : doc.getHeaderList()) {
            replaceInBody(header, placeholder, replacement);
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            replaceInBody(footer, placeholder, replacement);
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

    private static void applySpecialFormatting(XWPFRun run, String text) {
        // Настройки для специальных символов
        if (text.contains("inf")) {
            // Подстрочный текст
            run.setSubscript(VerticalAlign.SUBSCRIPT);
        }

        if (text.contains("des") || text.contains("reg")) {
            // Надстрочный текст
            run.setSubscript(VerticalAlign.SUPERSCRIPT);
        }

        if (text.contains("ч") && text.contains("-1")) {
            // Надстрочный текст для ч⁻¹
            run.setText(text.replace("-1", ""));
            XWPFRun superscript = run.getParagraph().createRun();
            superscript.setSubscript(VerticalAlign.SUPERSCRIPT);
            superscript.setText("−1"); // Используем длинное тире
        }
    }


    private static void replaceTableDataSafe(XWPFDocument doc, String tableTitle, List<Double> values) {
        if (values == null || values.isEmpty()) return;

        for (XWPFTable table : doc.getTables()) {
            if (table.getText().contains(tableTitle)) {
                int startRow = 7; // Начало данных в таблице
                int valuesIndex = 0;

                for (int i = 0; i < 5; i++) { // 5 строк данных
                    if ((startRow + i) >= table.getRows().size()) break;

                    XWPFTableRow row = table.getRow(startRow + i);
                    for (int j = 1; j <= 5; j++) { // 5 столбцов Qm
                        if (valuesIndex >= values.size()) break;
                        if (j >= row.getTableCells().size()) break;

                        XWPFTableCell cell = row.getCell(j);
                        cell.removeParagraph(0);
                        XWPFParagraph p = cell.addParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(String.format("%.1f", values.get(valuesIndex++)));
                    }
                }
                break;
            }
        }
    }
}