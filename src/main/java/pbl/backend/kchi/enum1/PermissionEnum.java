package pbl.backend.kchi.enum1;

public enum PermissionEnum {

    USER_CATALOGUE("user_catalogue"),
    PERMISSION("permission"),
    USER("user"),
    NOTIFICATIONS("notifications"),
    CLASSES_USER("classes"),
    ASSIGNMENT("assignments"),
    CONVERSATIONS("conversations"),
    MESSAGE("messages"),
    SUBMISSION("submissions"),
    UPLOAD("upload");

    private final String prefix;

    PermissionEnum(String prefix){
        this.prefix = prefix;
    }

    public String getPrefix(){
        return prefix;
    }


}