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
        String rawProtocol = model.getProtocolNumber();
        boolean useDefaultProtocol = rawProtocol == null || rawProtocol.isBlank();
        String protocolNumber = useDefaultProtocol ? "10" : rawProtocol;

        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            throw new FileNotFoundException("Файл шаблона не найден: " + templatePath);
        }

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

            // Подсветка номера протокола при необходимости
            if (useDefaultProtocol) {
                highlightProtocolNumber(doc, protocolNumber);
            }

            // Генерация уникального имени файла
            return saveDocument(doc);
        }
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
        replaceAllText(doc, "[2/4 (два для общественных зданий / 4 для жилых домов]",
                String.valueOf(model.getAirExchangeNorm()));
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

        // Обработка табличных данных (один раз для всех комнат)
        replaceAnchoredTableCells(doc, rooms);
    }

    private static void replaceRoomData(XWPFDocument doc, RoomData room, int roomIndex) {
        // Основные параметры
        replaceAllText(doc, "[Room" + roomIndex + ".Name]", getSafeValue(room.getName()));
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
        replaceAllText(doc, "[угловая/рядовая/торцевая квартира №]", getSafeValue(room.getName()));
        replaceAllText(doc, "[номер этажа]", getSafeValue(room.getFloor()));
        replaceAllText(doc, "[пло-щадь внешней стены]", formatDouble(room.getArea()));
        replaceAllText(doc, "[площадь окон]", formatDouble(room.getWindowArea()));
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

    private static void highlightProtocolNumber(XWPFDocument doc, String protocolNumber) {
        // Обработка основного текста
        for (XWPFParagraph p : doc.getParagraphs()) {
            highlightRuns(p.getRuns(), protocolNumber);
        }

        // Обработка таблиц
        for (XWPFTable tbl : doc.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        highlightRuns(p.getRuns(), protocolNumber);
                    }
                }
            }
        }

        // Обработка колонтитулов
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                highlightRuns(p.getRuns(), protocolNumber);
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                highlightRuns(p.getRuns(), protocolNumber);
            }
        }
    }

    private static void highlightRuns(List<XWPFRun> runs, String text) {
        for (XWPFRun run : runs) {
            String runText = run.getText(0);
            if (runText != null && runText.equals(text)) {
                run.setTextHighlightColor("yellow");
            }
        }
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