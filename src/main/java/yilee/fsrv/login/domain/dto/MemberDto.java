package yilee.fsrv.login.domain.dto;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import yilee.fsrv.login.domain.enums.MemberRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class MemberDto {
    private Long id;

    @NotBlank
    @Length(min = 4, max = 20)
    private String username;

    @NotBlank
    @Length(min = 8, max = 20)
    private String password;

    @NotBlank
    @Length(min = 4, max = 20)
    private String nickname;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    private LocalDateTime joinDate;

    private LocalDateTime lastLoginDate;

    @Builder.Default
    private Boolean isDisabled = false;

    private LocalDateTime disabledAt;

}
