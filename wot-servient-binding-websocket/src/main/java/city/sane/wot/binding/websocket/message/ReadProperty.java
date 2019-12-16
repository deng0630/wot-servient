package city.sane.wot.binding.websocket.message;

import java.io.Reader;

public class ReadProperty extends AbstractMessage {
    private String thingId;
    private String name;

    private ReadProperty() {
        this.thingId = null;
        this.name = null;
    }

    public ReadProperty(String thingId, String name) {
        this.thingId = thingId;
        this.name = name;
    }


    public String getThingId() {
        return thingId;
    }

    public String getName() {
        return name;
    }
}
