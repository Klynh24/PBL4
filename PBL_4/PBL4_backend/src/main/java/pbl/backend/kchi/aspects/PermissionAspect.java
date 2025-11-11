package pbl.backend.kchi.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


import jakarta.servlet.http.HttpServletRequest;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.annotations.RequirePermission;
import pbl.backend.kchi.helper.CustomPermissionEvaluator;
import pbl.backend.kchi.modules.users.resources.CustomUserDetail;

@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private CustomPermissionEvaluator customPermissionEvaluator;

    private final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);


    @Before("@annotation(requirePermission)")
    public void checkPermissions(JoinPoint joinPoint, RequirePermission requirePermission){
        logger.info("Aspect Permission Running....");
        Object target  = joinPoint.getTarget();
        if(target instanceof BaseController){
            BaseController<?, ?, ?, ? , ?> controller = (BaseController<?, ?, ?, ? , ?>) target;
            String module = controller.getModule().getPrefix();
            String permission = module + ":" + requirePermission.action();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(!customPermissionEvaluator.hasPermission(authentication, permission)){
                throw new AccessDeniedException("Access Denied");
            }

            if("list".equals(requirePermission.action()) || "pagination".equals(requirePermission.action())){
                handleListPermission(joinPoint, authentication, module, requirePermission.viewAll());
            }

        }

    }

    private void handleListPermission(JoinPoint joinPoint, Authentication authentication, String module, String viewAll){
        Object[] arguments = joinPoint.getArgs();
        logger.info("args: {}", arguments);
        String permission = module + ":" + viewAll;

        Boolean checkViewAll = !viewAll.isEmpty() && customPermissionEvaluator.hasPermission(authentication,  permission);

        if(!checkViewAll){
            for (Object argument : arguments) {
                if(argument instanceof HttpServletRequest request){


                    CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
                    Long userId = userDetails.getId();
                    request.setAttribute("userId", userId);
                }
            }
        }
    }


}