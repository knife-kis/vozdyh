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
            replaceAllText(doc, "[Наименование заказчика]", model.getCustomer());
            replaceAllText(doc, "[юридический адрес заказчика]", model.getCustomerAddress());
            replaceAllText(doc, "[вид стен и их толщина]", model.getWallType());
            replaceAllText(doc, "[вид оконных блоков]", model.getWindowType());
            replaceAllText(doc, "[естественная/искусственная]", model.getVentilationType());
            replaceAllText(doc, "[2/4 (два для общественных зданий / 4 для жилых домов]",
                    String.valueOf(model.getAirExchangeNorm()));
            replaceAllText(doc, "[наименование объекта]", model.getObjectName());

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

    private static void replaceAllText(XWPFDocument doc, String placeholder, String replacement) {
        // Замена в обычных параграфах
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceInParagraph(p, placeholder, replacement);
        }

        // Замена в таблицах
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, placeholder, replacement);
                    }
                }
            }
        }

        // Замена в колонтитулах
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                replaceInParagraph(p, placeholder, replacement);
            }
        }

        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                replaceInParagraph(p, placeholder, replacement);
            }
        }
    }
    private static void replaceInParagraph(XWPFParagraph paragraph, String placeholder, String replacement) {
        List<XWPFRun> runs = paragraph.getRuns();
        StringBuilder fullText = new StringBuilder();
        List<XWPFRun> runList = new ArrayList<>(runs);

        // Собираем полный текст и проверяем наличие плейсхолдера
        for (XWPFRun run : runList) {
            String text = run.getText(0);
            if (text != null) {
                fullText.append(text);
            }
        }

        if (!fullText.toString().contains(placeholder)) {
            return;
        }

        // Очищаем параграф
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Воссоздаём runs с сохранением форматирования
        String[] parts = fullText.toString().split(Pattern.quote(placeholder), -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // Добавляем текст части
            if (!part.isEmpty()) {
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(part);

                // Копируем форматирование из оригинального run (если возможно)
                if (!runList.isEmpty()) {
                    XWPFRun sampleRun = runList.get(0);
                    newRun.setFontFamily(sampleRun.getFontFamily());
                    newRun.setFontSize(sampleRun.getFontSize());
                    newRun.setBold(sampleRun.isBold());
                    newRun.setItalic(sampleRun.isItalic());
                    newRun.setUnderline(sampleRun.getUnderline());

                    // Исправление для версии POI 5.4.0
                    Object vertAlign = sampleRun.getVerticalAlignment();
                    if (vertAlign != null) {
                        String alignStr = vertAlign.toString();
                        if ("SUPERSCRIPT".equals(alignStr)) {
                            newRun.setSubscript(VerticalAlign.SUPERSCRIPT);
                        } else if ("SUBSCRIPT".equals(alignStr)) {
                            newRun.setSubscript(VerticalAlign.SUBSCRIPT);
                        }
                    }
                }
            }

            // Добавляем замену между частями
            if (i < parts.length - 1) {
                XWPFRun replacementRun = paragraph.createRun();
                replacementRun.setText(replacement);
            }
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