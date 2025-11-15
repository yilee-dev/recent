package yilee.fsrv.directory.folder.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import yilee.fsrv.directory.acl.entity.QFolderAcl;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.folder.domain.dto.CursorPageResponse;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;
import yilee.fsrv.directory.folder.domain.dto.FolderDto;
import yilee.fsrv.directory.folder.domain.entity.QFolderMetrics;
import yilee.fsrv.directory.folder.domain.entity.QFolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import java.util.List;

@Repository
public class FolderQueryRepositoryImpl implements FolderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public FolderQueryRepositoryImpl(EntityManager entityManager) {
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public CursorResult<FolderDto> findChild(Long folderId, Long viewerId, Long nextCursor, int size) {

        QFolderObject folder = QFolderObject.folderObject;
        QFolderAcl acl = QFolderAcl.folderAcl;
        QFolderMetrics fm = QFolderMetrics.folderMetrics; // ✅ metrics

        BooleanBuilder condition = new BooleanBuilder().and(folder.isDeleted.isFalse());
        if (folderId == null) condition.and(folder.parent.isNull());
        else condition.and(folder.parent.id.eq(folderId));
        if (nextCursor != null) condition.and(folder.id.lt(nextCursor));

        BooleanBuilder visibility = new BooleanBuilder()
                .or(folder.scope.eq(FolderScope.SHARED));
        if (viewerId != null) {
            visibility.or(folder.scope.eq(FolderScope.PRIVATE).and(folder.ownerId.eq(viewerId)));
            visibility.or(
                    JPAExpressions.selectOne().from(acl)
                            .where(acl.folder.id.eq(folder.id)
                                    .and(acl.subjectId.eq(viewerId))
                                    .and(acl.permission.in(FolderPermission.READ, FolderPermission.WRITE)))
                            .exists()
            );
        }
        condition.and(visibility);

        List<FolderDto> lists = queryFactory.select(
                        Projections.constructor(
                                FolderDto.class,
                                folder.id,
                                folder.parent.id,
                                folder.ownerId,
                                folder.name,
                                folder.scope,
                                folder.createdAt,
                                folder.updatedAt,
                                folder.deletedAt,
                                folder.isDeleted,
                                fm.directFileCount.coalesce(0L),   // ✅ 파일 수
                                fm.directFolderCount.coalesce(0L)  // ✅ 폴더 수
                        )
                )
                .from(folder)
                .leftJoin(fm).on(fm.folderId.eq(folder.id)) // ✅ metrics 조인
                .where(condition)
                .orderBy(folder.id.desc())
                .limit(size + 1)
                .fetch();

        boolean hasNextCursor = lists.size() > size;
        if (hasNextCursor) lists = lists.subList(0, size);
        Long nc = lists.isEmpty() ? null : lists.get(lists.size() - 1).id();

        return new CursorResult<>(lists, nc, hasNextCursor);
    }
}
