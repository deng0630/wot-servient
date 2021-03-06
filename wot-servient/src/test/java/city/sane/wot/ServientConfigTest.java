package city.sane.wot;

import city.sane.Pair;
import city.sane.wot.binding.ProtocolClient;
import city.sane.wot.binding.ProtocolClientFactory;
import city.sane.wot.binding.ProtocolServer;
import city.sane.wot.content.Content;
import city.sane.wot.thing.ExposedThing;
import city.sane.wot.thing.Thing;
import city.sane.wot.thing.filter.ThingFilter;
import city.sane.wot.thing.form.Form;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.reactivex.rxjava3.core.Observable;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ServientConfigTest {
    private Config config;

    @Before
    public void setUp() {
        config = mock(Config.class);
    }

    @Test
    public void initializeServer() throws ServientConfigException {
        ProtocolServer server = ServientConfig.initializeServer(config, MyProtocolServer.class.getName());

        assertThat(server, instanceOf(MyProtocolServer.class));
    }

    @Test(expected = ServientConfigException.class)
    public void initializeServerWithoutImplementation() throws ServientConfigException {
        ServientConfig.initializeServer(config, MyBadMissingImplementationProtocolServer.class.getName());
    }

    @Test(expected = ServientConfigException.class)
    public void initializeServerMissingConstructor() throws ServientConfigException {
        ServientConfig.initializeServer(config, MyBadMissingConstructorProtocolServer.class.getName());
    }

    @Test
    public void initializeClientFactory() throws ServientConfigException {
        Pair<String, ProtocolClientFactory> pair = ServientConfig.initializeClientFactory(config, MyProtocolClientFactory.class.getName());

        assertThat(pair.first(), is("test"));
        assertThat(pair.second(), instanceOf(MyProtocolClientFactory.class));
    }

    @Test(expected = ServientConfigException.class)
    public void initializeClientFactoryWithoutImplementation() throws ServientConfigException {
        ServientConfig.initializeClientFactory(config, MyBadMissingImplementationProtocolClientFactory.class.getName());
    }

    @Test(expected = ServientConfigException.class)
    public void initializeClientFactoryMissingConstructor() throws ServientConfigException {
        ServientConfig.initializeClientFactory(config, MyBadMissingConstructorProtocolClientFactory.class.getName());
    }

    @Test
    public void constructorWithCredentials() throws ServientException {
        Config config = ConfigFactory
                .parseString("wot.servient.credentials { \"counter\" = \"mySecret\" }")
                .withFallback(ConfigFactory.load());
        ServientConfig servientConfig = new ServientConfig(config);

        assertThat(servientConfig.getCredentialStore(), hasKey("counter"));
    }

    public static class MyProtocolServer implements ProtocolServer {
        @Override
        public CompletableFuture<Void> start(Servient servient) {
            return completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> stop() {
            return completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> expose(ExposedThing thing) {
            return completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> destroy(ExposedThing thing) {
            return completedFuture(null);
        }
    }

    public static class MyProtocolClientFactory implements ProtocolClientFactory {
        @Override
        public String getScheme() {
            return "test";
        }

        @Override
        public ProtocolClient getClient() {
            return new MyProtocolClient();
        }
    }

    static class MyProtocolClient implements ProtocolClient {
        public MyProtocolClient() {
        }

        @Override
        public CompletableFuture<Content> readResource(Form form) {
            String json = null;
            switch (form.getHref()) {
                case "test:/":
                    json = "{\"counter\": {\"id\": \"counter\"}}";
                    break;

                case "test:/counter":
                    json = "{\"id\": \"counter\"}";
                    break;
            }
            return completedFuture(new Content("application/json", json.getBytes()));
        }

        @Override
        public Observable<Thing> discover(ThingFilter filter) {
            return Observable.empty();
        }
    }

    static class MyBadMissingImplementationProtocolServer {
        MyBadMissingImplementationProtocolServer(Config config) {

        }
    }

    static class MyBadMissingConstructorProtocolServer implements ProtocolServer {
        /**
         * Starts the server (e.g. HTTP server) and makes it ready for requests to the exposed
         * things.
         *
         * @param servient
         * @return
         */
        @Override
        public CompletableFuture<Void> start(Servient servient) {
            return null;
        }

        /**
         * Stops the server (e.g. HTTP server) and ends the exposure of the Things
         *
         * @return
         */
        @Override
        public CompletableFuture<Void> stop() {
            return null;
        }

        /**
         * Exposes <code>thing</code> and allows interaction with it.
         *
         * @param thing
         * @return
         */
        @Override
        public CompletableFuture<Void> expose(ExposedThing thing) {
            return null;
        }

        /**
         * Stops the exposure of <code>thing</code> and allows no further interaction with the
         * thing.
         *
         * @param thing
         * @return
         */
        @Override
        public CompletableFuture<Void> destroy(ExposedThing thing) {
            return null;
        }
    }

    static class MyBadMissingImplementationProtocolClientFactory {
        MyBadMissingImplementationProtocolClientFactory(Config config) {

        }
    }

    static class MyBadMissingConstructorProtocolClientFactory implements ProtocolClientFactory {
        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public ProtocolClient getClient() {
            return null;
        }
    }
}