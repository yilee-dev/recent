package yilee.fsrv.login.filter;

import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import yilee.fsrv.login.domain.dto.MemberDto;
import yilee.fsrv.login.exception.CustomJwtException;
import yilee.fsrv.login.helper.JwtUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtCheckFilter extends OncePerRequestFilter {
    private static Map<String, Set<String>> WHITE_LIST = Map.of(
            "/", Set.of("GET"),
            "/api/sign-up", Set.of("POST"),
            "/api/sign-in", Set.of("POST"),
            "/api/tokens/refresh", Set.of("GET", "POST"),
            "/css/*", Set.of("GET"),
            "/js/*", Set.of("GET"),
            "/*.ico",Set.of("GET")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        return isWhiteListed(requestURI, method);
    }

    private boolean isWhiteListed(String uri, String method) {
        for (Map.Entry<String, Set<String>> entry : WHITE_LIST.entrySet()) {
            String whitePath = entry.getKey();
            Set<String> allowedMethods = entry.getValue();

            if (PatternMatchUtils.simpleMatch(whitePath, uri) && allowedMethods.contains(method)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorization.substring("Bearer ".length()).trim();

            Map<String, Object> claims = JwtUtils.validateToken(token);
            log.info("JWT Claims: {}", claims);

            String username = (String) claims.get("username");
            Long userId = claims.get("id") instanceof Number n ? n.longValue() : null;
            List<String> roles = (List<String>) claims.get("roles");
            Boolean isDisabled = (Boolean) claims.get("isDisabled");

            if (Boolean.TRUE.equals(isDisabled)) {
                throw new CustomJwtException("DISABLED_ACCOUNT");
            }

            MemberDto dto = MemberDto.builder()
                    .id(userId)
                    .username(username)
                    .roles(roles != null ? roles : List.of())
                    .isDisabled(isDisabled != null ? isDisabled : false)
                    .build();

            List<GrantedAuthority> authorities = dto.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(dto, null, authorities);

            SecurityContextHolder.getContextHolderStrategy().getContext().setAuthentication(usernamePasswordAuthenticationToken);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Jwt Check Error...", e);

            Gson gson = new GsonHttpMessageConverter().getGson();
            String jsonStr = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            PrintWriter writer = response.getWriter();
            writer.println(jsonStr);
            writer.close();
        }
    }
}