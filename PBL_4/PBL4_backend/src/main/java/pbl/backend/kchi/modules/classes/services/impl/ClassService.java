package pbl.backend.kchi.modules.classes.services.impl;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;

import pbl.backend.kchi.services.BaseService;


@Service
public class ClassService extends BaseService<
        Classes,
        ClassMapper,
        StoreClassRequest,
        UpdateClassRequest,
        ClassRepository
        > implements ClassServiceInterface {

    private final ClassMapper classMapper;

    @Autowired
    private ClassRepository classRepository;

    public ClassService(
            ClassMapper classMapper
    ){
        this.classMapper = classMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{"permissions"};
    }

    @Override
    protected ClassRepository getRepository(){
        return classRepository;
    }

    @Override
    protected ClassMapper getMapper(){
        return classMapper;
    }


}