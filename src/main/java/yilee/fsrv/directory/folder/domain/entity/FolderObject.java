package yilee.fsrv.directory.folder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;
import yilee.fsrv.directory.folder.exception.DomainException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "folder_object",
indexes = @Index(name = "idx_folder_parent", columnList = "parent_id"),
uniqueConstraints = @UniqueConstraint(name = "uk_parent_name", columnNames = {"parent_id", "name"}))
@Getter @EqualsAndHashCode(of = "id")
@Builder @NoArgsConstructor @AllArgsConstructor
public class FolderObject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_object_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FolderObject parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("name asc")
    private List<FolderObject> child = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private FolderScope scope;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    public boolean isSharedFolder() {
        return scope == FolderScope.SHARED;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateParent(FolderObject parentFolder) {
        if (parentFolder == this) {
            throw new IllegalArgumentException("A folder cannot be its own parent");
        }

        if (parentFolder != null && parentFolder.isDescendantOf(this)) {
            throw new IllegalStateException("Cycle detected");
        }

        if (parentFolder != null && this.scope != parentFolder.scope) {
            throw new DomainException("Folder scope mismatch");
        }

        if (Objects.equals(this.parent, parentFolder)) return;

        if (this.parent != null) {
            this.parent.child.remove(this);
        }

        this.parent = parentFolder;

        if (parentFolder != null && !parentFolder.child.contains(this)) {
            parentFolder.child.add(this);
        }
    }

    public boolean isDescendantOf(FolderObject other) {
        for (FolderObject cur = this.parent; cur != null; cur = cur.parent) {
            if (cur == other) return true;
        }
        return false;
    }

    public void addChild(FolderObject childFolder) {
        if (childFolder == null | childFolder == this) return;
        childFolder.updateParent(this);
    }

    public void removeChild(FolderObject childFolder) {
        if (childFolder == null) return;
        if (this.child.contains(childFolder)) {
            childFolder.updateParent(null);
        }
    }

    public void updateScope(FolderScope scope) {
        this.scope = scope;
    }

    public void markDeleted() {
        if (!isDeleted) {
            this.isDeleted = true;
            deletedAt = LocalDateTime.now();
        }
    }

    public void unmarkDeleted() {
        if (isDeleted) {
            this.isDeleted = false;
            deletedAt = null;
        }
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
