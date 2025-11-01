package pbl.backend.kchi.helper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.filter.OncePerRequestFilter;

import pbl.backend.kchi.resources.MessageResource;
import pbl.backend.kchi.services.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.IOException;
import java.util.HashMap;

import pbl.backend.kchi.modules.users.services.impl.CustomUserDetailService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Service
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {



    private final CustomUserDetailService CustomUserDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailService.class);
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/refresh");
    }

    @Override
    public void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userId;

            if(authHeader == null || !authHeader.startsWith("Bearer")) {

//            logger.error("Test");

            sendErrorResponse(response,
                    request,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Xác thực không thành công!",
                    "Không tìm thấy token"
            );
//                filterChain.doFilter(request, response);
               return;

            }
            jwt = authHeader.substring(7);

            if(!jwtService.isTokenFormsValid(jwt)) {
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token không đúng định dạng."
                );
                return;
            }

            if(jwtService.isTokenExpired(jwt)) {
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token đã hết hạn."
                );
                return;
            }


            if(!jwtService.isSignatureValid(jwt)) {
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Chữ ký không hợp lệ."
                );
                return;
            }

            if(!jwtService.isIssuerToken(jwt)) {
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Nguồn Token không hợp lệ."
                );
                return;
            }

            if(jwtService.isBlacklistedToken(jwt)) {
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token bị khóa."
                );
                return;
            }


            userId = jwtService.getUserIdFromJwt(jwt);
            if(userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = CustomUserDetailsService.loadUserByUsername(userId);
                final String emailFromToken = jwtService.getEmailFromJwt(jwt);
                if(!emailFromToken.equals(userDetails.getUsername())) {
                    sendErrorResponse(response,
                            request,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Xác thực không thành công",
                            "User Token không chính xác,"
                    );
                    return;
                }

//





           UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                  userDetails,
                   null,
                   userDetails.getAuthorities()
           );
           authToken.setDetails(
                   new WebAuthenticationDetailsSource().buildDetails(request)
           );

           SecurityContextHolder.getContext().setAuthentication(authToken);
           logger.info("Xác thực tài khoản thành công: ", userDetails.getUsername());

                   logger.info(userDetails.getUsername());

            }
            //     logger.info("userId: {}", userId);

            filterChain.doFilter(request,response);

        } catch (ServletException | IOException e) {
            sendErrorResponse(response,
                    request,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Network Error!",
                    e.getMessage()
                    );

        }


    }

    private void sendErrorResponse(
            @NotNull HttpServletResponse response,
            @NotNull HttpServletRequest request,
            int statusCode,
            String error,
            String message

    ) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object>  errorResponse = new HashMap<>();
        errorResponse.put("timestamp" , System.currentTimeMillis());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURI());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }



}
