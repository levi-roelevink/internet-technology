package shared.messages;

// TODO: replace status and code with enums
public record ResponseMessage(String status, int code) {

    public ResponseMessage(String status) {
        this(status, 0);
    }
}
