package pbl.backend.kchi.resources;

import java.util.Map;
import java.util.HashMap;

public class MessageResource {
    private String message;

    public MessageResource(
            String message

    ) {
        this.message = message;

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}
