package yilee.fsrv.directory.file.repository;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.file.domain.dto.FileSummaryDto;
import yilee.fsrv.directory.file.domain.entity.FileObject;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileObject, Long>, FileQueryRepository {
    @Query("select max(f.version) from FileObject f where f.folder.id = :folderId and f.originalFilename = :name")
    Optional<Integer> findMaxVersion(@Param("folderId") Long folderId, @Param("name") String originalName);

    @Query("""
      select new yilee.fsrv.directory.file.domain.dto.FileSummaryDto(
          count(f),
          coalesce(sum(f.fileSize), 0),

          sum(case when f.fileType = :normal    then 1 else 0 end),
          coalesce(sum(case when f.fileType = :normal    then f.fileSize else 0 end), 0),

          sum(case when f.fileType = :install   then 1 else 0 end),
          coalesce(sum(case when f.fileType = :install   then f.fileSize else 0 end), 0),

          sum(case when f.fileType = :uninstall then 1 else 0 end),
          coalesce(sum(case when f.fileType = :uninstall then f.fileSize else 0 end), 0)
      )
      from FileObject f
        join f.folder fo
      where f.deleted = false
        and fo.isDeleted = false
        and (:folderId is null or fo.id = :folderId)
        and (
              fo.scope = 'SHARED'
           or (:viewerId is not null and (
                  fo.ownerId = :viewerId
               or exists (
                    select 1 from FolderAcl a
                    where a.folder.id = fo.id
                      and a.subjectId = :viewerId
                      and a.permission in :rwPerms
               )
           ))
        )
    """)
    FileSummaryDto summarizeVisibleJPQL(@Param("folderId") Long folderId,
                                        @Param("viewerId") Long viewerId,
                                        @Param("normal")    FileType normal,
                                        @Param("install")   FileType install,
                                        @Param("uninstall") FileType uninstall,
                                        @Param("rwPerms")   Collection<FolderPermission> rwPerms);

    // ✅ 목록: 동일한 가시성 조건, 커서 페이징
    @Query("""
      select f
      from FileObject f
        join f.folder fo
      where f.deleted = false
        and fo.isDeleted = false
        and (:folderId is null or fo.id = :folderId)
        and (:fileType is null or f.fileType = :fileType)
        and (
              fo.scope = 'SHARED'
           or (:viewerId is not null and (
                  fo.ownerId = :viewerId
               or exists (
                    select 1 from FolderAcl a
                    where a.folder.id = fo.id
                      and a.subjectId = :viewerId
                      and a.permission in :rwPerms
               )
           ))
        )
        and (:cursorId is null or f.id < :cursorId)
      order by f.id desc
    """)
    List<FileObject> findVisibleByTypeJPQL(@Param("folderId") Long folderId,
                                           @Param("viewerId") Long viewerId,
                                           @Param("fileType") FileType fileType,
                                           @Param("cursorId") Long cursorId,
                                           @Param("rwPerms") Collection<FolderPermission> rwPerms,
                                           Pageable pageable);
//
//    @Query("""
//  select new yilee.fsrv.directory.file.domain.dto.FileSummaryDto(
//      count(f),
//      coalesce(sum(f.fileSize), 0),
//
//      sum(case when f.fileType = :normal    then 1 else 0 end),
//      coalesce(sum(case when f.fileType = :normal    then f.fileSize else 0 end), 0),
//
//      sum(case when f.fileType = :install   then 1 else 0 end),
//      coalesce(sum(case when f.fileType = :install   then f.fileSize else 0 end), 0),
//
//      sum(case when f.fileType = :uninstall then 1 else 0 end),
//      coalesce(sum(case when f.fileType = :uninstall then f.fileSize else 0 end), 0)
//  )
//  from FileObject f
//    join f.folder fo
//  where f.deleted = false
//    and fo.isDeleted = false
//    and (:folderId is null or fo.id = :folderId)
//    and (
//          fo.scope = :sharedScope
//       or (:viewerId is not null and (
//              fo.ownerId = :viewerId
//           or exists (
//                select 1 from FolderAcl a
//                where a.folder.id = fo.id
//                  and a.subjectId = :viewerId
//                  and a.permission in :rwPerms
//           )
//       ))
//    )
//""")
//    FileSummaryDto summarizeVisibleJPQL(@Param("folderId") Long folderId,
//                                        @Param("viewerId") Long viewerId,
//                                        @Param("normal")    FileType normal,
//                                        @Param("install")   FileType install,
//                                        @Param("uninstall") FileType uninstall,
//                                        @Param("sharedScope") FolderScope sharedScope,
//                                        @Param("rwPerms")   java.util.Collection<FolderPermission> rwPerms);
//
//
//    @Query("""
//  select f
//  from FileObject f
//    join f.folder fo
//  where f.deleted = false
//    and fo.isDeleted = false
//    and (:folderId is null or fo.id = :folderId)
//    and (:fileType is null or f.fileType = :fileType)
//    and (
//          fo.scope = :sharedScope
//       or (:viewerId is not null and (
//              fo.ownerId = :viewerId
//           or exists (
//                select 1 from FolderAcl a
//                where a.folder.id = fo.id
//                  and a.subjectId = :viewerId
//                  and a.permission in :rwPerms
//              )
//          ))
//    )
//    and (:cursorId is null or f.id < :cursorId)
//  order by f.id desc
//""")
//    List<FileObject> findVisibleByTypeJPQL(
//            @Param("folderId")    Long folderId,
//            @Param("viewerId")    Long viewerId,
//            @Param("fileType")    FileType fileType,
//            @Param("cursorId")    Long cursorId,
//            @Param("sharedScope") FolderScope sharedScope,
//            @Param("rwPerms")     java.util.Collection<FolderPermission> rwPerms,
//            Pageable pageable
//    );
}
