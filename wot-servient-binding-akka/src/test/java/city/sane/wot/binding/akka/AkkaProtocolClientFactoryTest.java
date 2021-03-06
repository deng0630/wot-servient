package city.sane.wot.binding.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import city.sane.RefCountResource;
import city.sane.Triple;
import city.sane.wot.thing.ExposedThing;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AkkaProtocolClientFactoryTest {
    private RefCountResource<Triple<ActorSystem, Map<String, ExposedThing>, ActorRef>> actorSystemProvider;
    private Duration askTimeout;
    private Duration discoverTimeout;
    private Triple<ActorSystem, Map<String, ExposedThing>, ActorRef> triple;

    @Before
    public void setUp() {
        actorSystemProvider = mock(RefCountResource.class);
        askTimeout = Duration.ofSeconds(60);
        discoverTimeout = Duration.ofSeconds(5);
        triple = mock(Triple.class);
    }

    @Test
    public void getSchemeShouldReturnCorrectScheme() {
        AkkaProtocolClientFactory factory = new AkkaProtocolClientFactory(actorSystemProvider, askTimeout, discoverTimeout, triple);
        assertEquals("akka", factory.getScheme());
    }

    @Test
    public void getClientShouldReturnCorrectClient() {
        AkkaProtocolClientFactory factory = new AkkaProtocolClientFactory(actorSystemProvider, askTimeout, discoverTimeout, triple);
        factory.init();

        assertThat(factory.getClient(), instanceOf(AkkaProtocolClient.class));
    }
}
