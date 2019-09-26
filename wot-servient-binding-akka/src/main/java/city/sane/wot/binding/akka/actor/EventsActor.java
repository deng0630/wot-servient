package city.sane.wot.binding.akka.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import city.sane.wot.thing.event.ExposedThingEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static city.sane.wot.binding.akka.CrudMessages.Created;

/**
 * This Actor creates a {@link EventActor} for each {@link ExposedThingEvent}.
 */
public class EventsActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, ExposedThingEvent> events;
    private final Set<ActorRef> children = new HashSet<>();

    public EventsActor(Map<String, ExposedThingEvent> events) {
        this.events = events;
    }

    @Override
    public void preStart() {
        log.info("Started");

        if (!events.isEmpty()) {
            events.forEach((name, event) -> {
                ActorRef propertyActor = getContext().actorOf(EventActor.props(name, event), name);
                children.add(propertyActor);
            });
        }
        else {
            done();
        }
    }

    @Override
    public void postStop() {
        log.info("Stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Created.class, this::eventExposed)
                .build();
    }

    private void eventExposed(Created m) {
        if (children.remove(getSender()) && children.isEmpty()) {
            done();
        }
    }

    private void done() {
        log.info("All events have been exposed");
        getContext().getParent().tell(new Created<>(getSelf()), getSelf());
    }

    static public Props props(Map<String, ExposedThingEvent> properties) {
        return Props.create(EventsActor.class, () -> new EventsActor(properties));
    }
}