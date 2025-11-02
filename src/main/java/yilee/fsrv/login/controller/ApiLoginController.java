package yilee.fsrv.login.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import yilee.fsrv.login.domain.dto.*;
import yilee.fsrv.login.helper.JwtUtils;
import yilee.fsrv.login.service.MemberService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiLoginController {
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        SignUpResponse response = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInDto dto) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(dto.username(), dto.password());

        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        MemberContext principal = (MemberContext) authenticate.getPrincipal();

        Map<String, Object> claims = principal.getClaims();

        String accessToken = JwtUtils.generateToken(claims, 10);
        String refreshToken = JwtUtils.generateToken(claims, 60 * 24);

        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);

        return ResponseEntity.ok(claims);
    }


}
