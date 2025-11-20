package pbl.backend.kchi.modules.assignments.mapper;

import org.mapstruct.*;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.requests.UpdateAssignmentRequest;
import pbl.backend.kchi.modules.assignments.resources.AssignmentResource;


@Mapper(componentModel = "spring")
public interface AssignmentMapper extends BaseMapper<Assignments, AssignmentResource, StoreAssignmentRequest, UpdateAssignmentRequest> {

    @Override
    @BaseMapperAnnotation
//    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Assignments toEntity(StoreAssignmentRequest createRequest);

    @Override
    @BaseMapperAnnotation
//    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateAssignmentRequest updateRequest, @MappingTarget Assignments entity);
}