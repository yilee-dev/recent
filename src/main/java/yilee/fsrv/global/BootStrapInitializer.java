package yilee.fsrv.global;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;
import yilee.fsrv.directory.folder.repository.FolderClosureRepository;
import yilee.fsrv.directory.folder.repository.FolderMetricsRepository;
import yilee.fsrv.directory.folder.repository.FolderRepository;
import yilee.fsrv.infra.storage.StorageService;
import yilee.fsrv.login.domain.entity.Member;
import yilee.fsrv.login.domain.enums.MemberRole;
import yilee.fsrv.login.repository.MemberRepository;

import java.util.EnumSet;

@Component
@RequiredArgsConstructor
public class BootStrapInitializer implements ApplicationRunner {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FolderRepository folderRepository;
    private final FolderClosureRepository folderClosureRepository;
    private final FolderMetricsRepository folderMetricsRepository;
    private final StorageService storageService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Member root = memberRepository.findByUsername("root").orElseGet(() -> {
            Member m = Member.builder()
                    .username("root")
                    .password(passwordEncoder.encode("DHD@ngh222"))
                    .nickname("root")
                    .memberRoleList(EnumSet.of(MemberRole.MANAGER, MemberRole.ADMIN))
                    .build();
            return memberRepository.save(m);
        });

        memberRepository.findByUsername("donghee").orElseGet(() -> {
            Member m = Member.builder()
                    .username("donghee")
                    .password(passwordEncoder.encode("DHD@ngh222"))
                    .nickname("donghee")
                    .memberRoleList(EnumSet.of(MemberRole.MANAGER))
                    .build();
            return memberRepository.save(m);
        });
    }
}
