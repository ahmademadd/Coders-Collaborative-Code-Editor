package com.example.coders.repositories;

import com.example.coders.entities.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    Optional<Folder> findByFolderPath(String folderPath);
    void deleteByFolderPath(String folderPath);
}
