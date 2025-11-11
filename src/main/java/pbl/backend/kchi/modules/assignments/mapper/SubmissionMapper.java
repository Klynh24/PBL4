package pbl.backend.kchi.modules.assignments.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.UpdateSubmittedRequest;
import pbl.backend.kchi.modules.assignments.resources.SubmittedResource;

@Mapper(componentModel = "spring")
public interface SubmissionMapper extends BaseMapper<
        AssignmentSubmission,
        SubmittedResource,
        StoreSubmittedRequest,
        UpdateSubmittedRequest
        > {


    @Override
    @BaseMapperAnnotation
    @Mapping(target = "assignment", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    AssignmentSubmission toEntity(StoreSubmittedRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "assignment", ignore = true)
    @Mapping(target = "user", ignore = true)       
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateSubmittedRequest updateRequest, @MappingTarget AssignmentSubmission entity);
}
