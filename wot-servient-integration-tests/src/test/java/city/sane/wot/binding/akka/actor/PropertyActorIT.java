package city.sane.wot.binding.akka.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import city.sane.wot.binding.akka.Messages;
import city.sane.wot.binding.akka.Messages.Read;
import city.sane.wot.binding.akka.Messages.Subscribe;
import city.sane.wot.binding.akka.Messages.SubscriptionNext;
import city.sane.wot.binding.akka.Messages.Write;
import city.sane.wot.content.ContentCodecException;
import city.sane.wot.content.ContentManager;
import city.sane.wot.thing.ExposedThing;
import city.sane.wot.thing.action.ThingAction;
import city.sane.wot.thing.event.ThingEvent;
import city.sane.wot.thing.property.ExposedThingProperty;
import city.sane.wot.thing.property.ThingProperty;
import city.sane.wot.thing.schema.IntegerSchema;
import city.sane.wot.thing.schema.ObjectSchema;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class PropertyActorIT {
    private ActorSystem system;

    @Before
    public void setUp() {
        Config config = ConfigFactory.parseString("akka.loglevel = DEBUG").withFallback(ConfigFactory.load());
        system = ActorSystem.create("my-system", config);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void readProperty() throws ContentCodecException {
        TestKit testKit = new TestKit(system);

        ExposedThing thing = getExposedCounterThing();
        Props props = PropertyActor.props("count", thing.getProperty("count"));
        ActorRef actorRef = system.actorOf(props);

        actorRef.tell(new Read(), testKit.getRef());

        Messages.RespondRead msg = testKit.expectMsgClass(Messages.RespondRead.class);
        int count = ContentManager.contentToValue(msg.content, new IntegerSchema());

        assertEquals(42, count);
    }

    @Test
    public void writeProperty() throws ContentCodecException {
        TestKit testKit = new TestKit(system);

        ExposedThing thing = getExposedCounterThing();
        Props props = PropertyActor.props("count", thing.getProperty("count"));
        ActorRef actorRef = system.actorOf(props);

        actorRef.tell(new Write(ContentManager.valueToContent(1337)), testKit.getRef());

        testKit.expectMsgClass(Messages.Written.class);
    }

    @Test
    public void subscribeProperty() throws ExecutionException, InterruptedException {
        TestKit testKit = new TestKit(system);

        ExposedThing thing = getExposedCounterThing();
        ExposedThingProperty property = thing.getProperty("count");
        Props props = PropertyActor.props("count", property);
        ActorRef actorRef = system.actorOf(props);

        actorRef.tell(new Subscribe(), testKit.getRef());

        // wait until client establish subcription
        // TODO: This is error-prone. We need a feature that notifies us when the subscription is active.
        Thread.sleep(5 * 1000L);

        property.write(23).get();

        testKit.expectMsgClass(SubscriptionNext.class);
    }

    private ExposedThing getExposedCounterThing() {
        ThingProperty counterProperty = new ThingProperty.Builder()
                .setType("integer")
                .setDescription("current counter content")
                .setObservable(true)
                .build();

        ThingProperty lastChangeProperty = new ThingProperty.Builder()
                .setType("string")
                .setDescription("last change of counter content")
                .setObservable(true)
                .setReadOnly(true)
                .build();

        ExposedThing thing = new ExposedThing(null)
                .setId("counter")
                .setTitle("counter");

        thing.addProperty("count", counterProperty, 42);
        thing.addProperty("lastChange", lastChangeProperty, new Date().toString());

        thing.addAction("increment",
                new ThingAction.Builder()
                        .setDescription("Incrementing counter content with optional step content as uriVariable")
                        .setUriVariables(Map.of(
                                "step", Map.of(
                                        "type", "integer",
                                        "minimum", 1,
                                        "maximum", 250
                                )
                        ))
                        .setInput(new ObjectSchema())
                        .setOutput(new IntegerSchema())
                        .build(),
                (input, options) -> {
                    return thing.getProperty("count").read().thenApply(value -> {
                        int step;
                        if (input != null && ((Map) input).containsKey("step")) {
                            step = (Integer) ((Map) input).get("step");
                        }
                        else if (options.containsKey("uriVariables") && ((Map) options.get("uriVariables")).containsKey("step")) {
                            step = (int) ((Map) options.get("uriVariables")).get("step");
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