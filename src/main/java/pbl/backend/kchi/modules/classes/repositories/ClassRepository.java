package pbl.backend.kchi.modules.classes.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.classes.entities.Classes;



@Repository
public interface ClassRepository extends JpaRepository<Classes, Long>, JpaSpecificationExecutor<Classes> {

}