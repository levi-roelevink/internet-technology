package shared.messages;

// TODO: replace status and code with enums
public record LoginResponseMessage(String status, int code) {

    public LoginResponseMessage(String status) {
        this(status, 0);
    }
}
