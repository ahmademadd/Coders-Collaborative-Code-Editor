package com.example.coders.mappings;

import com.example.coders.dtos.ProjectDto;
import com.example.coders.entities.Project;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectDto toProjectDto(Project project);
}
