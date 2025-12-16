package pbl.backend.kchi.modules.assignments.services.impl;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;
import pbl.backend.kchi.modules.assignments.mapper.SubmissionMapper;
import pbl.backend.kchi.modules.assignments.repositories.SubmittedRepository;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.UpdateSubmittedRequest;
import pbl.backend.kchi.modules.assignments.services.interfaces.SubmittedServiceInterface;

import pbl.backend.kchi.services.BaseService;



@Service
public class SubmittedService extends BaseService<
        AssignmentSubmission,
        SubmissionMapper,
        StoreSubmittedRequest,
        UpdateSubmittedRequest,
        SubmittedRepository
        > implements SubmittedServiceInterface {

    private final SubmissionMapper submissionMapper;

    @Autowired
    private SubmittedRepository submittedRepository;

    public SubmittedService(
            SubmissionMapper submissionMapper
    ){
        this.submissionMapper =submissionMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{};
    }

    @Override
    protected SubmittedRepository getRepository(){
        return submittedRepository;
    }

    @Override
    protected SubmissionMapper getMapper(){
        return submissionMapper;
    }


}