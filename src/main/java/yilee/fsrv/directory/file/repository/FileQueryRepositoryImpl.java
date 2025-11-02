package yilee.fsrv.directory.file.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import yilee.fsrv.directory.acl.entity.QFolderAcl;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.file.domain.dto.FileDto;
import yilee.fsrv.directory.file.domain.entity.QFileObject;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;
import yilee.fsrv.directory.folder.domain.entity.QFolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import java.util.List;
import java.util.Set;

@Repository
public class FileQueryRepositoryImpl implements FileQueryRepository {
    private final JPAQueryFactory queryFactory;

    public FileQueryRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public CursorResult<FileDto> findByFolderVisible(Long folderId, Long viewerId, Long cursor, int size, @Nullable Set<FileType> types) {
        QFileObject f = QFileObject.fileObject;
        QFolderObject fo = QFolderObject.folderObject;
        QFolderAcl a = QFolderAcl.folderAcl;

        // 기본 조건: 대상 폴더의 파일 + 삭제 아님
        BooleanBuilder where = new BooleanBuilder()
                .and(f.folder.id.eq(folderId))
                .and(f.deleted.isFalse());

        if (cursor != null) {
            where.and(f.id.lt(cursor));            // 키셋 커서 (id desc)
        }
        if (types != null && !types.isEmpty()) {
            where.and(f.fileType.in(types));       // 타입 필터
        }

        // 가시성(visibility) 규칙
        BooleanBuilder visibility = new BooleanBuilder()
                .or(fo.scope.eq(FolderScope.SHARED));

        if (viewerId != null) {
            visibility
                    .or( fo.scope.eq(FolderScope.PRIVATE).and(fo.ownerId.eq(viewerId)) )
                    .or( JPAExpressions.selectOne().from(a)
                            .where(
                                    a.folder.id.eq(fo.id)
                                            .and(a.subjectId.eq(viewerId))
                                            .and(a.permission.in(FolderPermission.READ, FolderPermission.WRITE, FolderPermission.DELETE))
                            )
                            .exists()
                    );
        }

        where.and(visibility);

        List<FileDto> rows = queryFactory
                .select(Projections.constructor(
                        FileDto.class,
                        f.id,
                        f.originalFilename,
                        f.fileSize,
                        f.fileType
                ))
                .from(f)
                .join(f.folder, fo)
                .where(where)
                .orderBy(f.id.desc())
                .limit(size + 1)
                .fetch();

        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);

        Long nextCursor = rows.isEmpty() ? null : rows.get(rows.size() - 1).id();

        return new CursorResult<>(rows, nextCursor, hasNext);
    }
}
