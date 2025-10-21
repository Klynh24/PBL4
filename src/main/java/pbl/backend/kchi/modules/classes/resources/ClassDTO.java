package pbl.backend.kchi.modules.classes.resources;

public class ClassDTO {
    private final Long id;
    private final Long userId;

    public ClassDTO(Long id, Long userId) {
        this.id = id;
        this.userId= id;
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId;}
}
