package yilee.fsrv.login.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import yilee.fsrv.login.domain.enums.MemberRole;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "member")
@Getter @EqualsAndHashCode(of = "id")
@Builder @NoArgsConstructor @AllArgsConstructor
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String username;

    @Size(min = 4, max = 13)
    @Column(length = 13, nullable = false, unique = true)
    private String nickname;

    private String password;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_role_list",
            joinColumns = @JoinColumn(name = "member_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "role"})
    )
    @Column(name = "role")
    private Set<MemberRole> memberRoleList = new HashSet<>();

    private LocalDateTime joinDate;

    private LocalDateTime lastLoginDate;

    private boolean isDisabled;

    private LocalDateTime disabledAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void addRole(MemberRole... roles) {
        // Set Data Structure
        if (roles == null || roles.length == 0) return;
        EnumSet<MemberRole> validRoles = Arrays.stream(roles)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(MemberRole.class)));
        memberRoleList.addAll(validRoles);
    }

    public void updateLastLoginDate() {
        this.lastLoginDate = LocalDateTime.now();
    }

    public void markDisabled() {
        if (!this.isDisabled) {
            this.isDisabled = true;
            disabledAt = LocalDateTime.now();
        }
    }

    public void unmarkDisabled() {
        if (this.isDisabled) {
            this.isDisabled = false;
            disabledAt = null;
        }
    }

    @PrePersist
    public void onCreate() {
        this.joinDate = LocalDateTime.now();
    }
}
