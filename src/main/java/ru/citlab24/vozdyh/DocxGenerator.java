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

            finalizeFormatting(doc);
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
        replaceAllText(doc, "[давление]", formatDouble(model.getPressure()));
        replaceAllText(doc, "[скорость ветра]", formatDouble(model.getWindSpeed()) + " м/с");
        replaceAllText(doc, "[температура улица]", "+" + formatTemperature(model.getTemperature()) );
    }

    private static String formatTemperature(double value) {
        return String.format("%d", (int) value);
    }

    // Универсальный обход параграфов в теле и таблицах
    private static void processAllParagraphs(IBody body, java.util.function.Consumer<XWPFParagraph> fn) {
        // Параграфы вне таблиц
        for (XWPFParagraph p : body.getParagraphs()) {
            fn.accept(p);
        }
        // Параграфы в таблицах
        for (XWPFTable tbl : body.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        fn.accept(p);
                    }
                }
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
        int month = model.getTestDate().getMonthValue();
        String buildingType = model.getBuildingType();

        for (int i = 0; i < rooms.size(); i++) {
            RoomData room = rooms.get(i);
            int roomIndex = i + 1;

            // Генерация температуры для помещения
            if (roomIndex <= 3) {
                int temp = generateIndoorTemperature(buildingType, month);
                room.setIndoorTemperature(temp); // Сохраняем температуру в комнате
            }

            // Основные параметры комнаты
            replaceRoomData(doc, room, roomIndex);
        }

        // Обработка табличных данных
        replaceAnchoredTableCells(doc, rooms);
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
        if (roomIndex <= 3) {
            // Используем форматирование со знаком "+"
            String formattedTemp = formatTemperatureSigned(room.getIndoorTemperature());
            replaceAllText(doc, "[температура внутри " + roomIndex + "]", formattedTemp);
        }

        // Также заменяем специфичные плейсхолдеры для каждой комнаты
        replaceAllText(doc, "[Room" + roomIndex + ".WallArea]", formatDouble(room.getWallArea()));
    }

    private static String formatTemperatureSigned(int temperature) {
        return (temperature >= 0 ? "+" : "") + temperature;
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

        if (paragraphHasDrawing(p)) {
            replaceInParagraphNonDestructive(p, placeholder, replacement);
            return;
        }
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
                    replaceAllTextNonDestructive(doc, placeholder, replacement);
                }
            }
        }
    }
    private static int generateIndoorTemperature(String buildingType, int month) {
        Random random = new Random();

        if ("Жилое".equals(buildingType)) {
            switch (month) {
                case 11: case 12: case 1: case 2: case 3: // Ноябрь-Март
                    return random.nextBoolean() ? 20 : 21;
                case 4: case 5: // Апрель-Май
                    return 20 + random.nextInt(3); // 20, 21, 22
                case 6: case 7: case 8: // Июнь-Август
                    return random.nextBoolean() ? 22 : 23;
                case 9: case 10: // Сентябрь-Октябрь
                    return random.nextBoolean() ? 21 : 22;
                default:
                    return 22; // По умолчанию
            }
        } else { // Общественное здание
            switch (month) {
                case 11: case 12: case 1: case 2: case 3: // Ноябрь-Март
                    return 19 + random.nextInt(3); // 19, 20, 21
                case 4: case 5: // Апрель-Май
                    return 20 + random.nextInt(4); // 20, 21, 22, 23
                case 6: case 7: case 8: // Июнь-Август
                    return 21 + random.nextInt(3); // 21, 22, 23
                case 9: case 10: // Сентябрь-Октябрь
                    return 19 + random.nextInt(4); // 19, 20, 21, 22
                default:
                    return 22; // По умолчанию
            }
        }
    }

    // Вызовите ЭТОТ метод САМЫМ ПОСЛЕДНИМ перед saveDocument(doc)
    private static void finalizeFormatting(XWPFDocument doc) {
        processAllParagraphs(doc, DocxGenerator::stylizeAndFixBoldInParagraph);

        for (XWPFHeader h : doc.getHeaderList()) {
            processAllParagraphs(h, DocxGenerator::stylizeAndFixBoldInParagraph);
        }
        for (XWPFFooter f : doc.getFooterList()) {
            processAllParagraphs(f, DocxGenerator::stylizeAndFixBoldInParagraph);
        }
    }
    private static boolean paragraphHasDrawing(XWPFParagraph p) {
        for (XWPFRun r : p.getRuns()) {
            try {
                // inline рисунки (Drawing), старые VML (Pict), OLE (Object)
                if ((r.getCTR().getDrawingList() != null && !r.getCTR().getDrawingList().isEmpty())
                        || (r.getCTR().getPictList()    != null && !r.getCTR().getPictList().isEmpty())
                        || (r.getCTR().getObjectList()  != null && !r.getCTR().getObjectList().isEmpty())) {
                    return true;
                }
            } catch (Exception ignore) {}
        }
        return false;
    }

    private static final String[] BOLD_LABELS = {
            "Методика испытания:",      // без пробела
            "Методика испытания :",     // обычный пробел
            "Методика испытания\u00A0:",// неразрывный пробел

            "Нормативные требования:",
            "Нормативные требования :",
            "Нормативные требования\u00A0:"
    };

    private static void stylizeAndFixBoldInParagraph(XWPFParagraph p) {
        String full = p.getText();
        if (full == null || full.isEmpty()) return;

        if (paragraphHasDrawing(p)) return;

        // нужно ли вообще трогать абзац?
        boolean hasTokens =
                full.contains("[Rinfdes]") || full.contains("[Rinfreg]") ||
                        full.contains("ч-1") || full.contains("м3/ч") || full.contains("m3/ч") ||
                        full.contains("м2") || full.contains("м3") || full.contains("m2") || full.contains("m3");

// ведущие пробелы перед проверкой лейбла
        int lead = 0;
        while (lead < full.length() && Character.isWhitespace(full.charAt(lead))) lead++;

// лейбл (допускаем пробел/неразрывный пробел перед двоеточием)
        boolean hasLabel =
                full.startsWith("Методика испытания:", lead) ||
                        full.startsWith("Методика испытания :", lead) ||
                        full.startsWith("Методика испытания\u00A0:", lead) ||
                        full.startsWith("Нормативные требования:", lead) ||
                        full.startsWith("Нормативные требования :", lead) ||
                        full.startsWith("Нормативные требования\u00A0:", lead);

        if (!hasTokens && !hasLabel) return; // НИЧЕГО НЕ ДЕЛАЕМ — чтобы не снести стрелки
        // Базовый стиль возьмём из первого run'а
        XWPFRun base = p.getRuns().isEmpty() ? null : p.getRuns().get(0);
        String color = base != null ? base.getColor() : null;
        String fontFamily = base != null ? base.getFontFamily() : null;
        int fontSize = base != null ? base.getFontSize() : -1;
        boolean italic = base != null && base.isItalic();
        UnderlinePatterns ul = base != null ? base.getUnderline() : UnderlinePatterns.NONE;

        // Полностью очищаем параграф
        for (int i = p.getRuns().size() - 1; i >= 0; i--) p.removeRun(i);

        // Помощник: добавить текст с нужной "жирностью"
        java.util.function.BiConsumer<String, Boolean> addText = (s, isBold) -> {
            if (s == null || s.isEmpty()) return;
            XWPFRun r = p.createRun();
            r.setText(s);
            copyStyle(r, color, fontFamily, fontSize, isBold, italic, ul);
        };

        // Сначала — ведущие пробелы
        int n = full.length();
        int pos = 0;
        while (pos < n && Character.isWhitespace(full.charAt(pos))) pos++;
        if (pos > 0) addText.accept(full.substring(0, pos), false);

        // Затем — проверяем «жирный префикс»
        boolean hadLabel = false;
        for (String label : BOLD_LABELS) {
            if (full.startsWith(label, pos)) {
                addText.accept(label, true);         // префикс жирный
                pos += label.length();
                hadLabel = true;
                break;
            }
        }

        // Остальное по абзацу — НЕ жирное (даже если шаблон был жирный весь)
        boolean currentBold = false;

        // Дальше идёт посимвольный проход с распознаванием токенов
        while (pos < n) {
            // 1) [Rinfdes] / [Rinfreg]
            if (startsWith(full, pos, "[Rinfdes]")) {
                addRinfComposite(p, color, fontFamily, fontSize, currentBold, italic, ul, "des");
                pos += "[Rinfdes]".length();
                continue;
            }
            if (startsWith(full, pos, "[Rinfreg]")) {
                addRinfComposite(p, color, fontFamily, fontSize, currentBold, italic, ul, "reg");
                pos += "[Rinfreg]".length();
                continue;
            }

            // 2) м3/ч и m3/ч
            if (startsWith(full, pos, "м3/ч") || startsWith(full, pos, "m3/ч")) {
                char mChar = full.charAt(pos); // 'м' или 'm'
                addM3PerH(p, mChar, color, fontFamily, fontSize, currentBold, italic, ul);
                pos += "м3/ч".length();
                continue;
            }

            // 3) одиночные м2/м3/m2/m3
            if (startsWith(full, pos, "м2")) { addMpow(p, 'м', '2', color, fontFamily, fontSize, currentBold, italic, ul); pos += 2; continue; }
            if (startsWith(full, pos, "м3")) { addMpow(p, 'м', '3', color, fontFamily, fontSize, currentBold, italic, ul); pos += 2; continue; }
            if (startsWith(full, pos, "m2")) { addMpow(p, 'm', '2', color, fontFamily, fontSize, currentBold, italic, ul); pos += 2; continue; }
            if (startsWith(full, pos, "m3")) { addMpow(p, 'm', '3', color, fontFamily, fontSize, currentBold, italic, ul); pos += 2; continue; }

            // 4) ч-1
            if (startsWith(full, pos, "ч-1")) {
                XWPFRun ch = p.createRun();
                ch.setText("ч");
                copyStyle(ch, color, fontFamily, fontSize, currentBold, italic, ul);

                XWPFRun sup = p.createRun();
                sup.setText("−1");
                sup.setSubscript(VerticalAlign.SUPERSCRIPT);
                copyStyle(sup, color, fontFamily, fontSize, currentBold, italic, ul);

                pos += 3;
                continue;
            }

            // Иначе — обычный символ
            addText.accept(String.valueOf(full.charAt(pos++)), currentBold);
        }
    }

    private static boolean startsWith(String s, int pos, String token) {
        int len = token.length();
        return pos + len <= s.length() && s.regionMatches(false, pos, token, 0, len);
    }

    private static void addRinfComposite(XWPFParagraph p,
                                         String color, String fontFamily, int fontSize,
                                         boolean bold, boolean italic, UnderlinePatterns ul,
                                         String superscriptSuffix) {
        XWPFRun r = p.createRun();
        r.setText("R");
        copyStyle(r, color, fontFamily, fontSize, bold, italic, ul);

        XWPFRun rInf = p.createRun();
        rInf.setText("inf");
        rInf.setSubscript(VerticalAlign.SUBSCRIPT);
        copyStyle(rInf, color, fontFamily, fontSize, bold, italic, ul);

        XWPFRun rSup = p.createRun();
        rSup.setText(superscriptSuffix);
        rSup.setSubscript(VerticalAlign.SUPERSCRIPT);
        copyStyle(rSup, color, fontFamily, fontSize, bold, italic, ul);
    }

    private static void addM3PerH(XWPFParagraph p, char mChar,
                                  String color, String fontFamily, int fontSize,
                                  boolean bold, boolean italic, UnderlinePatterns ul) {
        XWPFRun rM = p.createRun();
        rM.setText(String.valueOf(mChar));
        copyStyle(rM, color, fontFamily, fontSize, bold, italic, ul);

        XWPFRun r3 = p.createRun();
        r3.setText("3");
        r3.setSubscript(VerticalAlign.SUPERSCRIPT);
        copyStyle(r3, color, fontFamily, fontSize, bold, italic, ul);

        XWPFRun tail = p.createRun();
        tail.setText("/ч");
        copyStyle(tail, color, fontFamily, fontSize, bold, italic, ul);
    }

    private static void addMpow(XWPFParagraph p, char mChar, char powChar,
                                String color, String fontFamily, int fontSize,
                                boolean bold, boolean italic, UnderlinePatterns ul) {
        XWPFRun rM = p.createRun();
        rM.setText(String.valueOf(mChar));
        copyStyle(rM, color, fontFamily, fontSize, bold, italic, ul);

        XWPFRun rPow = p.createRun();
        rPow.setText(String.valueOf(powChar));
        rPow.setSubscript(VerticalAlign.SUPERSCRIPT);
        copyStyle(rPow, color, fontFamily, fontSize, bold, italic, ul);
    }
    // Не удаляет run'ы: просто меняет текст в существующих
    private static void replaceAllTextNonDestructive(XWPFDocument doc, String ph, String repl) {
        replaceInBodyNonDestructive(doc, ph, repl);
        for (XWPFHeader h : doc.getHeaderList()) replaceInBodyNonDestructive(h, ph, repl);
        for (XWPFFooter f : doc.getFooterList()) replaceInBodyNonDestructive(f, ph, repl);
    }

    private static void replaceInBodyNonDestructive(IBody body, String ph, String repl) {
        for (XWPFParagraph p : body.getParagraphs()) replaceInParagraphNonDestructive(p, ph, repl);
        for (XWPFTable t : body.getTables()) {
            for (XWPFTableRow r : t.getRows()) {
                for (XWPFTableCell c : r.getTableCells()) {
                    for (XWPFParagraph p : c.getParagraphs()) replaceInParagraphNonDestructive(p, ph, repl);
                }
            }
        }
    }

    private static void replaceInParagraphNonDestructive(XWPFParagraph p, String ph, String repl) {
        String full = p.getText();
        if (full == null || !full.contains(ph)) return;

        // собираем тексты run'ов (НЕ удаляя их)
        List<XWPFRun> runs = p.getRuns();
        List<String> texts = new ArrayList<>(runs.size());
        for (XWPFRun r : runs) {
            String t = r.getText(0);
            texts.add(t == null ? "" : t);
        }

        String newText = full.replace(ph, repl);

        // распределяем новый текст по тем же run'ам
        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            XWPFRun r = runs.get(i);
            String old = texts.get(i);
            int take = old.length();

            // очистим текущий текст (не трогаем рисунки/shape внутри run'а)
            if (r.getText(0) != null) r.setText("", 0);

            if (take > 0 && pos < newText.length()) {
                String part = newText.substring(pos, Math.min(pos + take, newText.length()));
                r.setText(part, 0);
                pos += part.length();
            }
        }

        // остаток — в последний текстовый run (не создавая новых)
        if (pos < newText.length()) {
            for (int i = runs.size() - 1; i >= 0; i--) {
                if (runs.get(i).getText(0) != null) {
                    String tailOld = runs.get(i).getText(0);
                    runs.get(i).setText((tailOld == null ? "" : tailOld) + newText.substring(pos), 0);
                    break;
                }
            }
        }
    }
}