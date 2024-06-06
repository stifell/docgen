package com.docgen.paper.controller;

import com.docgen.paper.model.TagMap;
import com.docgen.paper.service.WordDOCXService;
import com.docgen.paper.util.WordDOCX;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author Денис on 03.06.2024
 */
@Controller
public class WordDocxController {
    @Autowired
    private WordDOCXService wordDOCXService;

    @Autowired
    private TagMap tagMap;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        model.addAttribute("currentURI", request.getRequestURI());
        return "home";
    }

    @GetMapping("/upload")
    public String uploadPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentURI", request.getRequestURI());
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile[] files, RedirectAttributes redirectAttributes,
                                   HttpSession session, Model model) {
        if (files == null || files.length == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Выберите файл для загрузки.");
            return "redirect:/upload";
        }

        Set<String> allTags = new HashSet<>();
        tagMap = new TagMap();
        List<Map.Entry<String, String>> tempFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Выберите файлы для загрузки.");
                return "redirect:/upload";
            }

            String filename = file.getOriginalFilename();
            if (filename != null && !filename.endsWith(".docx")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Можно загружать только файлы с расширением .docx.");
                return "redirect:/upload";
            }

            // Логика сохранения файла
            try {
                File tempFile = File.createTempFile("uploaded-", ".docx");
                file.transferTo(tempFile);

                tempFiles.add(new AbstractMap.SimpleEntry<>(filename, tempFile.getAbsolutePath()));

                // Получение тегов из файла
                Map<String, String> tagsMap = wordDOCXService.writeTagsToSet(new File[]{tempFile});
                for (Map.Entry<String, String> entry : tagsMap.entrySet()) {
                    tagMap.addTag(entry.getKey(), entry.getValue());
                }
                allTags.addAll(tagsMap.keySet());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении файла: " + e.getMessage());
                return "redirect:/upload";
            }
        }
        session.setAttribute("tempFiles", tempFiles);
        model.addAttribute("files", files);
        model.addAttribute("tags", allTags);
        model.addAttribute("fileUploaded", true);
        return "upload";
    }

    @PostMapping("/generate")
    public void generateDocument(@RequestParam Map<String, String> allParams, HttpSession session,
                                   HttpServletResponse response) {
        // Извлечение тегов из параметров
        Map<String, String> tags = tagMap.getTagMap();
        tags.forEach((key, value) -> {
            String paramValue = allParams.get("tag_" + key);
            if (paramValue != null) {
                tags.put(key, paramValue);
            }
        });
        tagMap.setTagMap(tags);

        try {
            List<Map.Entry<String, String>> tempFiles = (List<Map.Entry<String, String>>) session.getAttribute("tempFiles");
            if (tempFiles == null || tempFiles.isEmpty()) {
                throw new IllegalArgumentException("Файлы не найдены.");
            }

            File zipFile = File.createTempFile("documents-", ".zip");
            ZipFile zip = new ZipFile(zipFile);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);

            for (Map.Entry<String, String> tempFileEntry : tempFiles) {
                String originalFilename = tempFileEntry.getKey();
                String tempFilePath = tempFileEntry.getValue();

                File tempFile = new File(tempFilePath);
                WordDOCX wordDOCX = new WordDOCX(tagMap, tempFile);

                // Создаем временный файл для измененного документа
                File modifiedFile = File.createTempFile("modified-", ".docx");
                try (FileOutputStream fos = new FileOutputStream(modifiedFile)) {
                    wordDOCX.changeFile(fos);
                }

                // Добавляем временный файл в ZIP-архив под исходным именем
                zip.addFile(modifiedFile, new ZipParameters() {{
                    setFileNameInZip(originalFilename);
                    setCompressionMethod(CompressionMethod.DEFLATE);
                    setCompressionLevel(CompressionLevel.NORMAL);
                }});
            }

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"documents.zip\"");
            try (InputStream inputStream = new FileInputStream(zipFile)) {
                IOUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
