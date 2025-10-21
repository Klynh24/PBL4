package pbl.backend.kchi.modules.session_policies.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "session_policies")
public class Session_policies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", updatable = false)
    private Long id;

    private Boolean allowScreenshare;
    private Boolean allowChat;
    private Boolean muteOnjoin;
    private Boolean lockRoom;

    public Long getId() { return id;}

    public void setId(Long id) { this.id = id; }

    public Boolean getAllowScreenshare() { return allowScreenshare; }

    public void setAllowScreenshare(Boolean allowScreenshare) { this.allowScreenshare = allowScreenshare; }

    public Boolean getAllowChat() { return allowChat; }

    public void setAllowChat(Boolean allowChat) { this.allowChat = allowChat; }

    public Boolean getMuteOnjoin() { return muteOnjoin; }

    public void setMuteOnjoin(Boolean muteOnjoin) { this.muteOnjoin = muteOnjoin; }

    public Boolean getLockRoom() { return lockRoom; }

    public void setLockRoom(Boolean lockRoom) { this.lockRoom = lockRoom; }
}
