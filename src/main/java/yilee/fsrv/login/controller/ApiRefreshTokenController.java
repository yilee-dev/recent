package yilee.fsrv.login.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yilee.fsrv.login.exception.CustomJwtException;
import yilee.fsrv.login.helper.JwtUtils;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiRefreshTokenController {

    @PreAuthorize("permitAll()")
    @PostMapping("/api/tokens/refresh")
    public Map<String, Object> refresh(@RequestHeader("Authorization") String authHeader, @RequestParam("refreshToken") String refreshToken) {
        if (refreshToken == null) {
            throw new CustomJwtException("EMPTY_REFRESH_TOKEN");
        }

        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            throw new CustomJwtException("INVALID_ACCESS_TOKEN");
        }

        String[] split = authHeader.split(" ");

        if (!checkExpiredToken(split[1])) {
            return Map.of("accessToken", split[1], "refreshToken", refreshToken);
        }

        Map<String, Object> claims = JwtUtils.validateToken(refreshToken);
        String newAccessToken = JwtUtils.generateToken(claims, 15);
        String newRefreshToken = checkTime((Long) claims.get("exp")) == true ? JwtUtils.generateToken(claims, 24 * 60) : refreshToken;

        log.info("exp: {}", (Long)claims.get("exp"));

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);
    }

    private boolean checkTime(Long exp) {
        Date expDate = new Date((long) exp * (1000));

        log.info("expDate: {}", expDate);

        long gap = expDate.getTime() - System.currentTimeMillis();

        long leftMin = gap / (1000 * 60);

        return leftMin < 5;
    }

    private boolean checkExpiredToken(String token) {
        try {
            JwtUtils.validateToken(token);
        } catch (Exception e) {
            if (e.getClass() == CustomJwtException.class) {
                if (e.getMessage().equals("Expired")) {
                    return true;
                }
            }
        }

        return false;
    }

}
