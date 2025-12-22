package pbl.backend.kchi.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

// === CÁC IMPORT CẦN THIẾT CHO LỖI JWT ===
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
// =====================================

import pbl.backend.kchi.modules.users.services.impl.CustomUserDetailService;
import pbl.backend.kchi.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final CustomUserDetailService CustomUserDetailsService;
    private final ObjectMapper objectMapper;


    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ){
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/refresh") ||
                path.startsWith("/api/v1/auth/register") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-ui/**") ||
                path.startsWith("/v3/api-docs" ) ||
                path.startsWith("/swagger-resources/**" ) ||
                path.startsWith("/webjars/**") ||
                path.startsWith("/api-docs/swagger-config") ||
                path.startsWith("/api-docs")
                ;
    }


    @Override
    public void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // Nếu header rỗng, hãy để nó đi tiếp.
        // Controller (với hàm me() đã sửa) sẽ xử lý và trả về 401.
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // === LOGIC XÁC THỰC MỚI ===

            // 1. Lấy User ID (Hàm này sẽ parse token, nếu lỗi sẽ ném Exception)
            userId = jwtService.getUserIdFromJwt(jwt);

            // 2. Kiểm tra Issuer (Nguồn phát hành)
            if (!jwtService.isIssuerToken(jwt)) {
                throw new MalformedJwtException("Nguồn Token không hợp lệ.");
            }

            // 3. Kiểm tra Token có bị khóa (blacklist) không
            if (jwtService.isBlacklistedToken(jwt)) {
                throw new MalformedJwtException("Token bị khóa.");
            }

            // 4. Nếu mọi thứ OK và user chưa được xác thực, hãy set Authentication
            if(userId != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = CustomUserDetailsService.loadUserByUsername(userId);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                request.setAttribute("userId", Long.parseLong(userId));
                logger.info("Xác thực tài khoản thành công: " + userDetails.getUsername());
            }

            // Cho request đi tiếp
            filterChain.doFilter(request, response);

        }
        // === KHỐI CATCH MỚI, SẼ BẮT LỖI CỤ THỂ ===
        catch (ExpiredJwtException e) {
            logger.warn("JWT Token đã hết hạn: {}", e);
            sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Xác thực không thành công", "Token đã hết hạn.");
        } catch (SignatureException e) {
            logger.warn("JWT Chữ ký không hợp lệ: {}", e);
            sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Xác thực không thành công", "Chữ ký không hợp lệ.");
        } catch (MalformedJwtException e) {
            logger.warn("JWT Token không đúng định dạng/Issuer/Blacklist: {}", e);
            sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Xác thực không thành công", e.getMessage());
        } catch (Exception e) {
            logger.error("!!! Lỗi filter không xác định: {}", e);
            e.printStackTrace(); // In ra lỗi để debug
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi máy chủ", "Lỗi không xác định trong quá trình xác thực.");
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

        Map<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURI());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}