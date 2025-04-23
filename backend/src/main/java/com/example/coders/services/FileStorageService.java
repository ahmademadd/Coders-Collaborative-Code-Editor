package com.example.coders.services;

import com.example.coders.dtos.FileDto;
import com.example.coders.entities.File;
import com.example.coders.entities.Folder;
import com.example.coders.entities.Project;
import com.example.coders.repositories.FileRepository;
import com.example.coders.repositories.FolderRepository;
import com.example.coders.repositories.ProjectRepository;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private FolderRepository folderRepository;

    @Transactional
    public void createFolder(String folderPath, String folderName, Integer projectId) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("coders")
                            .object(folderPath.endsWith("/") ? folderPath : folderPath + "/")
                            .stream(new ByteArrayInputStream(new byte[0]), 0, 0)
                            .contentType("application/octet-stream")
                            .build()
            );

            // Save folder in the database
            Folder folder = new Folder();
            folder.setFolderName(folderName);
            folder.setFolderPath(folderPath);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setLastModified(LocalDateTime.now());
            folder.setProject(projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found")));

            folderRepository.save(folder);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder", e);
        }
    }

    @Transactional
    public void copyFolder(String oldFolderPath, String newFolderPath) {
        try {
            Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket("coders")
                    .prefix(oldFolderPath)
                    .recursive(true)
                    .build());

            for (Result<Item> result : items) {
                Item item = result.get();
                String newObjectPath = newFolderPath + item.objectName().substring(oldFolderPath.length());

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket("coders")
                                .source(CopySource.builder().bucket("coders").object(item.objectName()).build())
                                .object(newObjectPath)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy folder", e);
        }
    }

    @Transactional
    public void updateFolder(String oldFolderPath, String newFolderName) {
        try {
            // Copy the folder in Minio
            copyFolder(oldFolderPath, newFolderName);

            // Update folder name in the database
            Folder folder = folderRepository.findByFolderPath(oldFolderPath)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            String newFolderPath = folder.getFolderPath().replace(oldFolderPath, newFolderName);
            folder.setFolderName(newFolderName);
            folder.setFolderPath(newFolderPath);
            folder.setLastModified(LocalDateTime.now());

            folderRepository.save(folder);

            // Delete old folder after updating
            deleteFolder(oldFolderPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update folder name", e);
        }
    }

    @Transactional
    public void deleteFolder(String folderPath) {
        try {
            // List all objects in the folder
            Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket("coders")
                    .prefix(folderPath) // Prefix for the folder
                    .recursive(true)    // Recursively delete contents
                    .build());

            // Delete each object
            for (Result<Item> result : items) {
                Item item = result.get();
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket("coders")
                        .object(item.objectName())
                        .build());
            }

            // Optionally delete folder metadata from your database
            folderRepository.deleteByFolderPath(folderPath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder", e);
        }
    }

    @Transactional
    public void createFileInFolder(String folderPath, String fileName, byte[] fileData, String contentType, long fileSize, Integer projectId) {
        try {
            String filePath = folderPath.endsWith("/") ? folderPath + fileName : folderPath + "/" + fileName;

            boolean fileExists = fileRepository.existsByFilePath(filePath);
            if (fileExists) {
                throw new RuntimeException("File already exists at this path: " + filePath);
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("coders")
                            .object(filePath)
                            .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                            .contentType(contentType)
                            .build()
            );

            // Save file in the database
            File file = new File();
            file.setFileName(fileName);
            file.setFilePath(filePath);
            file.setFileType(contentType);
            file.setFileSize(fileSize);
            file.setCreatedAt(LocalDateTime.now());
            file.setLastModified(LocalDateTime.now());
            file.setLastModifiedBy(authenticationService.authenticatedUser().getId());
            file.setProject(projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found")));

            fileRepository.save(file);

            // Update project size
            Optional<Project> p = projectRepository.findById(projectId);
            p.get().setSize(p.get().getSize() + fileSize);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }


    @Transactional
    public void updateFileName(String oldFilePath, String newFileName) {
        try {
            String newFilePath = oldFilePath.substring(0, oldFilePath.lastIndexOf("/") + 1) + newFileName;

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("coders")
                            .source(CopySource.builder().bucket("coders").object(oldFilePath).build())
                            .object(newFilePath)
                            .build()
            );

            File file = fileRepository.findByFilePath(oldFilePath)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            file.setFileName(newFileName);
            file.setFilePath(newFilePath);
            file.setLastModified(LocalDateTime.now());
            file.setLastModifiedBy(authenticationService.authenticatedUser().getId());
            deleteFile(oldFilePath);
            fileRepository.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update file name", e);
        }
    }

    @Transactional
    public void updateFileContents(String filePath, byte[] newFileData, String newContentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("coders")
                            .object(filePath)
                            .stream(new ByteArrayInputStream(newFileData), newFileData.length, -1)
                            .contentType(newContentType)
                            .build()
            );

            File file = fileRepository.findByFilePath(filePath)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            file.setFileType(newContentType);
            file.setLastModified(LocalDateTime.now());
            file.setLastModifiedBy(authenticationService.authenticatedUser().getId());

            fileRepository.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update file contents", e);
        }
    }

    @Transactional
    public void deleteFile(String filePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("coders")
                            .object(filePath)
                            .build()
            );

            // Remove file from the database
            fileRepository.deleteByFilePath(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Transactional
    public byte[] getFileContents(String filePath) {
        try {
            // Download the file as an InputStream from Minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket("coders")
                    .object(filePath)
                    .build();
            try (var inputStream = minioClient.getObject(getObjectArgs)) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file contents", e);
        }
    }

    @Transactional
    public String getFileContentType(String filePath) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket("coders")
                    .object(filePath)
                    .build());

            return stat.contentType();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file content type", e);
        }
    }

    public String calcFilePath(FileDto fileDto){
        try {
            Project project = projectRepository.findBySlug(fileDto.getProjectSlug()).orElseThrow(() -> new RuntimeException("Project not found"));
            Integer ownerId = project.getOwner().getId();

            return "projects/" + ownerId + "/" + project.getName() +
                    (fileDto.getFilePath().isEmpty() ? "" : "/" + fileDto.getFilePath()) +
                    "/" + fileDto.getFileName();
        }
        catch (Exception e){
            throw new RuntimeException("Can't calculate the file path");
        }

    }
}
