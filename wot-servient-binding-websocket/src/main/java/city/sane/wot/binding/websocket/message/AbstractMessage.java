package city.sane.wot.binding.websocket.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReadProperty.class, name = "readProperty"),
        @JsonSubTypes.Type(value = ReadPropertyResponse.class, name = "readPropertyResponse"),
        @JsonSubTypes.Type(value = WriteProperty.class, name = "writeProperty"),
        @JsonSubTypes.Type(value = WritePropertyResponse.class, name = "writePropertyResponse"),
        @JsonSubTypes.Type(value = SubscribeProperty.class, name = "subscribeProperty"),
        @JsonSubTypes.Type(value = SubscribePropertyResponse.class, name = "subscribePropertyResponse")
})
public abstract class AbstractMessage {
    private final String id;

    public AbstractMessage() {
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

}