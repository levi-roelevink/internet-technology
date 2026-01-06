package shared.messages;

public record Message(String msg) {
    @Override
    public String toString() {
        return msg;
    }
}
