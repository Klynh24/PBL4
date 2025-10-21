package pbl.backend.kchi.modules.users.resources;

public class LoginResources {

    private final String token;
    private final UserResource user;

    public LoginResources(
            String token,
            UserResource user
    ) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResource getUser() {
        return user;
    }
}
