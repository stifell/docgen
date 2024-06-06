package com.docgen.paper.util;

import com.docgen.paper.model.TagMap;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;

/**
 * @author Денис on 05.06.2024
 */
public class WordDOCX {
    private final TagMap tagMap;
    private File file;

    public WordDOCX(TagMap tagMap, File file) {
        this.tagMap = tagMap;
        this.file = file;
    }

    /**
     * Метод changeFile() выполняет замену текста в документе и сохранение изменений.
     * @throws IOException если возникают проблемы при чтении или записи файла.
     */
    public void changeFile(OutputStream outputStream) throws IOException {
        // inputStream - входной поток данных, FileInputStream - чтения байтов из файла
        try (InputStream inputStream = new FileInputStream(file)){
            // создание объект для работы с .docx
            XWPFDocument doc = new XWPFDocument(inputStream);
            // замена текста в docx и сохранение изменений
            doc = replaceText(doc);
            saveFile(outputStream, doc);
            doc.close();
        }
    }

    /**
     * Метод replaceText() выполняет замену указанного тега на соответствующее слово в документе.
     * @param doc объект XWPFDocument, представляющий документ, в котором нужно выполнить замену.
     * @return XWPFDocument с выполненной заменой.
     */
    private XWPFDocument replaceText(XWPFDocument doc) {
        // обработка всех абзацев в документе
        iterationAllParagraphs(doc.getParagraphs());
        // при наличии таблиц
        for (XWPFTable table : doc.getTables()) {
            // обработка каждой строки в таблице
            for (XWPFTableRow row : table.getRows()) {
                // обработка каждой ячейки в строке
                for (XWPFTableCell cell : row.getTableCells()) {
                    // обработка всех абзацев в ячейке
                    iterationAllParagraphs(cell.getParagraphs());
                }
            }
        }
        return doc;
    }

    /**
     * Метод iterationAllParagraphs() выполняет итерацию по всем абзацам в списке абзацев документа
     * и передает каждый абзац в метод iterateThroughRuns() для замены текста.
     * @param paragraphs список абзацев, которые нужно обработать.
     */
    private void iterationAllParagraphs(List<XWPFParagraph> paragraphs){
        paragraphs.forEach(this::iterateThroughRuns);
    }

    /**
     * Итерирует через все объекты XWPFRun в заданном абзаце
     * и меняет теги на значения
     * @param paragraph абзац, содержащий объекты XWPFRun.
     */
    private void iterateThroughRuns(XWPFParagraph paragraph){
        List<XWPFRun> runs = paragraph.getRuns();
        // проверка наличия объектов XWPFRun в абзаце
        if (runs == null)
            return;
        int index = 0;
        int runsSize = runs.size();
        // вычисляет индекс новой позиции, начиная с которой будет продолжено замена тегов
        int newIndex = checkTextParagraph(runs, index, runsSize);
        // если новый индекс меньше текущего, то тега в абзаце нет
        if (index > newIndex)
            return;
        // обновляет индекс
        index = newIndex;
        while (index < runsSize) {
            XWPFRun run = runs.get(index);
            StringBuilder runText = new StringBuilder(run.getText(0));
            // проверяет, не пуст ли текст текущего объекта
            if (runText != null) {
                // если объект содержит полноценный тег, заменяет его
                if (checkTag(runText.toString())) {
                    replaceWord(run, runText);
                    newIndex = checkTextParagraph(runs, index, runsSize);
                    if (index > newIndex)
                        return;
                    index = newIndex;
                } else if (runText.toString().contains("$")) {
                    XWPFRun nextRun;
                    String nextRunText;
                    // пока не встретится закрывающая скобка, склеивает тег
                    do {
                        index++;
                        nextRun = runs.get(index);
                        nextRunText = nextRun.getText(0);
                        runText.append(checkOptions(nextRun, nextRunText));
                    } while (!nextRunText.contains("}"));
                    // заменяет текст текущего объекта
                    replaceWord(run, runText);
                    newIndex = checkTextParagraph(runs, index, runsSize);
                    if (index > newIndex)
                        return;
                    index = newIndex;
                }
            }
        }
    }

    /**
     * Проверяет абзац на наличие частей тега, начиная с указанного индекса.
     * @param runs список объектов XWPFRun, представляющих текст абзаца.
     * @param index индекс, с которого начинается проверка абзаца.
     * @param runsSize размер списка объектов XWPFRun.
     * @return индекс, от которого следует проверять абзац, либо index - 1, если тег не найден.
     */
    private int checkTextParagraph(List<XWPFRun> runs, int index, int runsSize){
        // инициализация флагов для проверки наличия частей тега
        boolean findDollar = false;
        boolean findOpenBrace = false;
        boolean findCloseBrace = false;
        // строка для хранения текста из нескольких объектов
        StringBuilder text = new StringBuilder();
        // новый индекс, который будет возвращен в результате проверки
        int newIndex = 0;
        // проход по объектам, начиная с указанного индекса
        for (int i = index; i < runsSize; i++){
            String runText = runs.get(i).getText(0);
            if (runText == null)
                continue;
            // если текст уже содержит символ $ и текущий объект тоже содержит $,
            // то добавляем текст к уже собранному и проверяем наличие тега
            if (text.toString().contains("$") && runText.contains("$")){
                text.append(runText);
                // если найден тег, возвращаем индекс текущего объекта
                if (checkTag(text.toString()))
                    return newIndex;
                // если был просто $, а не тег, то сбрасываем флаги и очищаем строку
                findDollar = false;
                findOpenBrace = false;
                findCloseBrace = false;
                text.setLength(0);
            }
            if (runText.contains("$")) {
                findDollar = true;
                newIndex = i;
            }
            if (runText.contains("{"))
                findOpenBrace = true;
            if (runText.contains("}"))
                findCloseBrace = true;
            text.append(runText);
            // если найдены все части тега, и тег существует, возвращаем новый индекс
            if (findDollar && findOpenBrace && findCloseBrace && checkTag(text.toString()))
                return newIndex;
        }
        // в абзаце нет тега, выходим из абзаца
        return index - 1;
    }

    /**
     * Проверяет наличие тега в тексте.
     * @param runText текст, в котором производится поиск тега.
     * @return true, если найден хотя бы один тег, иначе - false.
     */
    private boolean checkTag(String runText){
        // проходит по каждой записи в словаре тегов и проверяет их наличие в тексте
        for(HashMap.Entry<String, String> entry: tagMap.getTagMap().entrySet()) {
            String tag = entry.getKey();
            if (runText.contains(tag))
                return true;
        }
        return false;
    }

    /**
     * Проверяет, содержит ли текст следующего объекта XWPFRun символ "}" и "$".
     * Если текст содержит оба символа, удаляет символ "}" из объекта XWPFRun
     * и возвращает его как строку для замены в тексте.
     * "}, ${" -> ", ${"
     * @param nextRun следующий объект XWPFRun для проверки.
     * @param nextRunText текст следующего объекта XWPFRun.
     * @return строка для замены в тексте
     */
    private String checkOptions(XWPFRun nextRun, String nextRunText){
        if (nextRunText.contains("}") && nextRunText.contains("$")) {
            deleteWord(nextRun, nextRunText, "}");
            return "}";
        } else{
            deleteWord(nextRun, nextRunText, nextRunText);
            return nextRunText;
        }
    }

    /**
     * Заменяет указанное слово в тексте объекта XWPFRun на другое слово.
     * @param run объект XWPFRun, содержащий текст, который нужно изменить.
     */
    private void replaceWord(XWPFRun run, StringBuilder text){
        String runText = String.valueOf(text);
        for(HashMap.Entry<String, String> entry: tagMap.getTagMap().entrySet()) {
            // получение ключа
            String tag = entry.getKey();
            // получение значения
            String replaceWord = entry.getValue();
            // замена на тега на его значение
            if (runText.contains(tag)){
                String updatedText = runText.replace(tag, replaceWord);
                run.setText(updatedText, 0);
            }
        }
    }

    /**
     * Заменяет указанный текст в объекте XWPFRun на пустую строку.
     * @param run объект XWPFRun, содержащий текст, который нужно изменить.
     * @param runText текст, который требуется изменить.
     * @param toRemove текст, который нужно удалить из исходного текста.
     */
    private void deleteWord(XWPFRun run, String runText, String toRemove) {
        // заменяет указанный текст на пустую строку
        String updatedText = runText.replace(toRemove, "");
        // устанавливает обновленный текст в объекте
        run.setText(updatedText, 0);
    }

    /**
     * Метод saveFile() сохраняет содержимое документа в файле.
     * @param doc объект XWPFDocument, содержащий измененное содержимое документа
     * @throws IOException если возникает ошибка ввода-вывода при записи файла
     */
    private void saveFile(OutputStream outputStream, XWPFDocument doc) throws IOException {
        // записывание содержимого документа в файл, который был открыт для записи
        doc.write(outputStream);
    }
}