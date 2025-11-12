package pbl.backend.kchi.helper;
import java.io.IOException;
import java.util.Arrays; // Thêm import này
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    // Danh sách các đường dẫn công khai (Public URLs)
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/",
            "/index.html",
            "/vite.svg",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            // ⭐ Endpoint Đăng ký
            "/api/v1/users",

            // ⭐ Đường dẫn tài nguyên tĩnh và API Docs
            "/assets/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/swagger-resources/",
            "/webjars/",
            "/api-docs/"
    );


    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ){
        final String path = request.getRequestURI();

        // ⭐ Dùng Stream để kiểm tra xem đường dẫn có bắt đầu bằng bất kỳ URL công khai nào không
        return PUBLIC_URLS.stream().anyMatch(publicPath -> path.startsWith(publicPath));
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

            // Nếu request không có Token, nó sẽ bị chặn ở đây (chỉ xảy ra nếu shouldNotFilter hoạt động sai)
            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Không tìm thấy Token."
                );
                return;
            }

            jwt = authHeader.substring(7);

            // ... (Phần kiểm tra JWT logic giữ nguyên)

            if(!jwtService.isTokenFormsValid(jwt)){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token không đúng định dạng."
                );
                return;
            }

            if(jwtService.isTokenExpired(jwt)){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token đã hết hạn."
                );
                return;
            }

            if(!jwtService.isSignatureValid(jwt)){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Chữ ký không hợp lệ."
                );
                return;
            }

            if(!jwtService.isIssuerToken(jwt)){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Nguồn Token không hợp lệ."
                );
                return;
            }

            if(jwtService.isBlacklistedToken(jwt)){
                sendErrorResponse(response,
                        request,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "Token bị khóa."
                );
                return;
            }

            userId = jwtService.getUserIdFromJwt(jwt);
            if(userId != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = CustomUserDetailsService.loadUserByUsername(userId);

                final String emailFromToken = jwtService.getEmailFromJwt(jwt);
                if(!emailFromToken.equals(userDetails.getUsername())){
                    sendErrorResponse(response,
                            request,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Xác thực không thành công",
                            "User Token không chính xác."
                    );
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("Xác thực tài khoản thành công: " + userDetails.getUsername());
            }

            filterChain.doFilter(request, response);

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