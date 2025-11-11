package pbl.backend.kchi.modules.users.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pbl.backend.kchi.modules.users.entities.Permission;

@Repository("permissionRepository")
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

}