package pbl.backend.kchi.modules.classes.services.interfaces;


import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;

import pbl.backend.kchi.services.interfaces.BaseServiceInterface;




public interface ClassServiceInterface extends BaseServiceInterface<Classes, StoreClassRequest, UpdateClassRequest> {

}

