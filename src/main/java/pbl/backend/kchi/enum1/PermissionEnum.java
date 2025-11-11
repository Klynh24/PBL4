package pbl.backend.kchi.enum1;

public enum PermissionEnum {

    USER_CATALOGUE("user_catalogue"),
    PERMISSION("permission"),
    USER("user"),
    CLASSES_USER("classes");

    private final String prefix;

    PermissionEnum(String prefix){
        this.prefix = prefix;
    }

    public String getPrefix(){
        return prefix;
    }


}