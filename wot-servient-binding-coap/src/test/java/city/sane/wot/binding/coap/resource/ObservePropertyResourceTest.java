package city.sane.wot.binding.coap.resource;

import city.sane.wot.Servient;
import city.sane.wot.ServientException;
import city.sane.wot.binding.ProtocolServerException;
import city.sane.wot.content.Content;
import city.sane.wot.content.ContentCodecException;
import city.sane.wot.content.ContentManager;
import city.sane.wot.thing.ExposedThing;
import city.sane.wot.thing.action.ThingAction;
import city.sane.wot.thing.event.ThingEvent;
import city.sane.wot.thing.property.ExposedThingProperty;
import city.sane.wot.thing.property.ThingProperty;
import city.sane.wot.thing.schema.NumberSchema;
import city.sane.wot.thing.schema.ObjectSchema;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class ObservePropertyResourceTest {
    private Servient servient;

    @Before
    public void setup() throws ServientException {
        Config config = ConfigFactory
                .parseString("wot.servient.servers = [\"city.sane.wot.binding.coap.CoapProtocolServer\"]\n" +
                        "wot.servient.client-factories = [\"city.sane.wot.binding.coap.CoapProtocolClientFactory\"]")
                .withFallback(ConfigFactory.load());

        servient = new Servient(config);
        servient.start().join();
    }

    @After
    public void teardown() {
        servient.shutdown().join();
    }

    @Test(timeout = 20 * 1000)
    public void observeProperty() throws ExecutionException, InterruptedException, ContentCodecException {
        ExposedThing thing = getCounterThing();
        servient.addThing(thing);
        servient.expose(thing.getId()).join();

        CompletableFuture<Content> result = new CompletableFuture<>();
        CoapClient client = new CoapClient("coap://localhost:5683/counter/properties/count/observable");
        client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                client.shutdown();
                String type = MediaTypeRegistry.toString(response.getOptions().getContentFormat());
                byte[] body = response.getPayload();
                Content output = new Content(type, body);
                result.complete(output);
            }

            @Override
            public void onError() {
                client.shutdown();
                result.completeExceptionally(new ProtocolServerException("Error while observe '" + client.getURI() + "'"));
            }
        });

        // wait until client establish subscription
        // TODO: This is error-prone. We need a client that notifies us when the observation is active.
        Thread.sleep(5 * 1000L);

        // write new value
        ExposedThingProperty property = thing.getProperty("count");
        property.write(1337).get();

        // wait until new value is received
        Content content = result.get();

        Object newValue = ContentManager.contentToValue(content, property);

        assertEquals(1337, newValue);
    }

    private ExposedThing getCounterThing() {
        ExposedThing thing = new ExposedThing(servient)
                .setId("counter")
                .setTitle("counter")
                .setDescription("counter example Thing");

        ThingProperty counterProperty = new ThingProperty.Builder()
                .setType("integer")
                .setDescription("current counter value")
                .setObservable(true)
                .setUriVariables(Map.of(
                        "step", Map.of(
                                "type", "integer",
                                "minimum", 1,
                                "maximum", 250
                        )
                ))
                .build();

        ThingProperty lastChangeProperty = new ThingProperty.Builder()
                .setType("string")
                .setDescription("last change of counter value")
                .setObservable(true)
                .setReadOnly(true)
                .build();

        thing.addProperty("count", counterProperty, 42);
        thing.addProperty("lastChange", lastChangeProperty, new Date().toString());

        thing.addAction("increment", new ThingAction.Builder()
                .setInput(new ObjectSchema())
                .setOutput(new NumberSchema())
                .build(), (input, options) -> {
            return thing.getProperty("count").read().thenApply(value -> {
                int step;
                if ((input instanceof Map) && ((Map) input).containsKey("step")) {
                    step = (int) ((Map) input).get("step");
                }
                else {
                    step = 1;
                }
                int newValue = ((Integer) value) + step;
                thing.getProperty("count").write(newValue);
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return newValue;
            });
        });

        thing.addAction("decrement", new ThingAction(), (input, options) -> {
            return thing.getProperty("count").read().thenApply(value -> {
                int newValue = ((Integer) value) - 1;
                thing.getProperty("count").write(newValue);
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return newValue;
            });
        });

        thing.addAction("reset", new ThingAction(), (input, options) -> {
            return thing.getProperty("count").write(0).thenApply(value -> {
                thing.getProperty("lastChange").write(new Date().toString());
                thing.getEvent("change").emit();
                return 0;
            });
        });

        thing.addEvent("change", new ThingEvent());

        return thing;
    }
}