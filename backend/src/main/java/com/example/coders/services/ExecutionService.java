package com.example.coders.services;

import com.example.coders.dtos.ExecuteDto;
import com.example.coders.dtos.FileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ExecutionService {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${EXECUTION_FILE_PATH}")
    private String filePath;

    public String executeCode(ExecuteDto executeDto) throws Exception {
        byte[] fileData = fileStorageService.getFileContents(fileStorageService.calcFilePath(executeDto.getFileDto()));
        String code = new String(fileData, StandardCharsets.UTF_8);

        if (code.isEmpty() || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
        String fileName = writeCodeToFile(executeDto.getFileDto(), code);

        String[] dockerCommand = buildDockerCommand(executeDto, fileName);

        ProcessBuilder processBuilder = new ProcessBuilder(dockerCommand);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        cleanupFiles();
        if (exitCode != 0) {
            return "Execution failed with exit code " + exitCode + ": " + output.toString();
        }

        return output.toString();
    }

    private String writeCodeToFile(FileDto fileDto, String code) throws IOException {
        String fileName = "/app/code_storage/" + fileDto.getFileName();
        Files.write(Paths.get(fileName), code.getBytes());
        return fileName;
    }

    private String[] buildDockerCommand(ExecuteDto executeDto, String fileName) {
        if (executeDto.getLanguage().equalsIgnoreCase("python"))
            return python(fileName);
        if (executeDto.getLanguage().equalsIgnoreCase("java"))
            return java(fileName);
        return null;
    }

    private String[] java(String fileName) {
        Path path = Paths.get(fileName);
        String javaFile = path.getFileName().toString();

        return new String[]{"docker", "run", "--rm", "-v",
                filePath +
                ":/usr/src/app", "-w", "/usr/src/app", "openjdk:21", "bash", "-c", "javac "
                + javaFile + " && java " + javaFile.substring(0, javaFile.length() - 5)
            };
    }

    private String[] python(String fileName) {
        Path path = Paths.get(fileName);
        String pythonScript = path.getFileName().toString();
        return new String[]{"docker", "run", "--rm", "-v ",
                filePath + ":/usr/src/app", "python:3", "python", "/usr/src/app/"
                + pythonScript
            };
    }

    private void cleanupFiles() {
        try {
            File folder = new File("/app/code_storage/");
            for (File file : folder.listFiles()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        }
    }
}