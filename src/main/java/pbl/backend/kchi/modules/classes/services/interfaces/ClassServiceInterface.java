package pbl.backend.kchi.modules.classes.services.interfaces;


import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;

import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;

import java.util.List;


public interface ClassServiceInterface extends BaseServiceInterface<Classes, StoreClassRequest, UpdateClassRequest> {
    void joinClass(String code);
    List<UserResource> getClassMembers(Long classId);
    void addMembersByEmail(Long classId, List<String> userEmails);
}

