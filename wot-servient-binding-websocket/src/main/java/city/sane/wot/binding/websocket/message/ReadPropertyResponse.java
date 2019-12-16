package city.sane.wot.binding.websocket.message;

public class ReadPropertyResponse extends AbstractMessage {
    private Object value;

    private ReadPropertyResponse() {
        this.value = null;
    }

    public ReadPropertyResponse(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
