package city.sane.wot.thing.property;

import city.sane.Pair;
import city.sane.wot.binding.ProtocolClient;
import city.sane.wot.binding.ProtocolClientException;
import city.sane.wot.thing.ConsumedThing;
import city.sane.wot.thing.ConsumedThingException;
import city.sane.wot.thing.content.Content;
import city.sane.wot.thing.content.ContentCodecException;
import city.sane.wot.thing.content.ContentManager;
import city.sane.wot.thing.form.Form;
import city.sane.wot.thing.form.Operation;
import city.sane.wot.thing.observer.Observer;
import city.sane.wot.thing.observer.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Used in combination with {@link ConsumedThing} and allows consuming of a {@link ThingProperty}.
 */
public class ConsumedThingProperty extends ThingProperty {
    final static Logger log = LoggerFactory.getLogger(ConsumedThingProperty.class);

    private final String name;
    private final ConsumedThing thing;

    public ConsumedThingProperty(String name, ThingProperty property, ConsumedThing thing) {
        this.name = name;

        this.objectType = property.getObjectType();
        this.description = property.getDescription();
        this.type = property.getType();
        this.observable = property.isObservable();
        this.readOnly = property.isReadOnly();
        this.writeOnly = property.isWriteOnly();
        this.forms = property.getForms();
        this.uriVariables = property.getUriVariables();
        this.optionalProperties = property.getOptionalProperties();

        this.thing = thing;
    }

    public CompletableFuture<Object> read() {
        try {
            Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.readproperty);
            ProtocolClient client = clientAndForm.first();
            Form form = clientAndForm.second();

            log.debug("Thing '{}' reading Property '{}' from '{}'", thing.getTitle(), name, form.getHref());

            CompletableFuture<Content> result = client.readResource(form);
            return result.thenApply(content -> {
                try {
                    Object value = ContentManager.contentToValue(content, this);
                    return value;
                }
                catch (ContentCodecException e) {
                    e.printStackTrace();
                    throw new CompletionException(new ConsumedThingException("Received invalid writeResource from Thing: " + e.getMessage()));
                }
            });
        }
        catch (ConsumedThingException e) {
            throw new CompletionException(e);
        }
    }

    public CompletableFuture<Object> write(Object value) {
        try {
            Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.writeproperty);
            ProtocolClient client = clientAndForm.first();
            Form form = clientAndForm.second();

            log.debug("ConsumedThing {} reading {}", thing.getTitle(), form.getHref());

            Content input = ContentManager.valueToContent(value, form.getContentType());

            CompletableFuture<Content> result = client.writeResource(form, input);
            return result.thenApply(content -> {
                try {
                    Object output = ContentManager.contentToValue(content, this);
                    return output;
                }
                catch (ContentCodecException e) {
                    e.printStackTrace();
                    throw new CompletionException(new ConsumedThingException("Received invalid writeResource from Thing: " + e.getMessage()));
                }
            });
        }
        catch (ContentCodecException e) {
            throw new CompletionException(new ConsumedThingException("Received invalid input: " + e.getMessage()));
        }
        catch (ConsumedThingException e) {
            throw new CompletionException(e);
        }
    }

    public CompletableFuture<Subscription> subscribe(Observer<Object> observer) throws ConsumedThingException {
        Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.observeproperty);
        ProtocolClient client = clientAndForm.first();
        Form form = clientAndForm.second();

        log.debug("New subscription for '{}'", thing.getTitle());
        try {
            return client.subscribeResource(form,
                    content -> {
                        try {
                            Object value = ContentManager.contentToValue(content, this);
                            observer.next(value);
                        }
                        catch (ContentCodecException e) {
                            observer.error(e);
                        }
                    },
                    observer::error, observer::complete);
        }
        catch (ProtocolClientException e) {
            throw new ConsumedThingException(e);
        }
    }

    public CompletableFuture<Subscription> subscribe(Consumer<Object> next, Consumer<Throwable> error, Runnable complete) throws ConsumedThingException {
        return subscribe(new Observer<>(next, error, complete));
    }

    public CompletableFuture<Subscription> subscribe(Consumer<Object> next) throws ConsumedThingException {
        return subscribe(new Observer<>(next));
    }
}
