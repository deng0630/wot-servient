package city.sane.wot;

import city.sane.wot.binding.*;
import city.sane.wot.thing.ExposedThing;
import city.sane.wot.thing.Thing;
import city.sane.wot.thing.filter.DiscoveryMethod;
import city.sane.wot.thing.filter.SparqlThingQuery;
import city.sane.wot.thing.filter.ThingFilter;
import city.sane.wot.thing.filter.ThingQueryException;
import city.sane.wot.thing.form.Form;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ServientTest {
    private ServientConfig config;
    private ProtocolServer server;
    private ProtocolClientFactory clientFactory;
    private ProtocolClient client;
    private ExposedThing exposedThing;
    private Thing thing;

    @Before
    public void setUp() {
        config = mock(ServientConfig.class);
        server = mock(ProtocolServer.class);
        clientFactory = mock(ProtocolClientFactory.class);
        client = mock(ProtocolClient.class);
        exposedThing = mock(ExposedThing.class);
        thing = mock(Thing.class);
    }

    @Test
    public void start() throws ExecutionException, InterruptedException {
        Servient servient = new Servient(config);

        assertNull(servient.start().get());
    }

    @Test(expected = ProtocolServerException.class)
    public void startFails() throws Throwable {
        when(server.start()).thenReturn(CompletableFuture.failedFuture(new ProtocolServerException()));
        when(config.getServers()).thenReturn(List.of(server));

        Servient servient = new Servient(config);

        try {
            servient.start().get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void shutdown() throws ExecutionException, InterruptedException {
        Servient servient = new Servient(config);

        assertNull(servient.shutdown().get());
    }

    @Test(expected = ProtocolServerException.class)
    public void shutdownFails() throws Throwable {
        when(server.stop()).thenReturn(CompletableFuture.failedFuture(new ProtocolServerException()));
        when(config.getServers()).thenReturn(List.of(server));

        Servient servient = new Servient(config);

        try {
            servient.shutdown().get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void expose() throws InterruptedException, ExecutionException {
        when(server.expose(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(server), Map.of(), Map.of(), Map.of("counter", exposedThing));

        assertThat(servient.expose("counter").get(), is(exposedThing));
        verify(server, times(1)).expose(exposedThing);
    }

    @Test(expected = ServientException.class)
    public void exposeWithoutServers() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of("counter", exposedThing));

        try {
            servient.expose("counter").get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = ServientException.class)
    public void exposeUnknownThing() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.expose("counter").get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void destroy() throws ExecutionException, InterruptedException {
        when(server.destroy(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(server), Map.of(), Map.of(), Map.of("counter", exposedThing));

        assertThat(servient.destroy("counter").get(), is(exposedThing));
        verify(server, times(1)).destroy(exposedThing);
    }

    @Test(expected = ServientException.class)
    public void destroyWithoutServers() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of("counter", exposedThing));

        try {
            servient.destroy("counter").get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = ServientException.class)
    public void destroyUnknownThing() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.destroy("counter").get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void fetch() throws URISyntaxException, ExecutionException, InterruptedException, ProtocolClientException {
        when(clientFactory.getClient()).thenReturn(client);
        when(client.readResource(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.fetch("test:/counter").get();

        verify(client, times(1)).readResource(any());
    }

    @Test(expected = ServientException.class)
    public void fetchMissingScheme() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.fetch("test:/counter").get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void fetchDirectory() throws URISyntaxException, ExecutionException, InterruptedException, ProtocolClientException {
        when(clientFactory.getClient()).thenReturn(client);
        when(client.readResource(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.fetchDirectory(new URI("test:/")).get();

        verify(client, times(1)).readResource(any());
    }

    @Test
    public void fetchDirectoryString() throws URISyntaxException, ExecutionException, InterruptedException, ProtocolClientException {
        when(clientFactory.getClient()).thenReturn(client);
        when(client.readResource(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.fetchDirectory("test:/").get();

        verify(client, times(1)).readResource(any());
    }

    @Test
    public void discover() throws ExecutionException, InterruptedException, ProtocolClientException {
        when(clientFactory.getClient()).thenReturn(client);
        when(client.discover(any())).thenReturn(CompletableFuture.completedFuture(List.of(thing)));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.discover().get();

        verify(client, times(1)).discover(any());
    }

    @Test
    public void discoverWithQuery() throws ExecutionException, InterruptedException, ThingQueryException, ProtocolClientException {
        ThingFilter filter = new ThingFilter().setQuery(new SparqlThingQuery("?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.w3.org/2019/wot/td##Thing> ."));
        when(clientFactory.getClient()).thenReturn(client);
        when(client.discover(any())).thenReturn(CompletableFuture.completedFuture(List.of(thing)));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.discover(filter).get();

        verify(client, times(1)).discover(filter);
    }

    @Test
    public void discoverLocal() throws ExecutionException, InterruptedException, ProtocolClientException {
        ThingFilter filter = new ThingFilter(DiscoveryMethod.LOCAL);

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of("counter", exposedThing));

        assertThat(servient.discover(filter).get(), hasItem(exposedThing));
    }

    @Test
    public void discoverDirectory() throws ExecutionException, InterruptedException, URISyntaxException, ProtocolClientException {
        ThingFilter filter = new ThingFilter().setMethod(DiscoveryMethod.DIRECTORY).setUrl(new URI("test:/"));
        when(clientFactory.getClient()).thenReturn(client);
        when(client.readResource(any())).thenReturn(CompletableFuture.completedFuture(null));

        Servient servient = new Servient(List.of(), Map.of("test", clientFactory), Map.of(), Map.of());
        servient.discover(filter).get();

        verify(client, times(1)).readResource(new Form.Builder().setHref("test:/").build());
    }

    @Test(expected = ProtocolClientNotImplementedException.class)
    public void discoverWithNoClientImplementsDiscover() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.discover().get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = ServientException.class)
    public void runScriptWithNoEngine() throws ServientException {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        servient.runScript(new File("foo.bar"), null);
    }

    @Test
    public void getAddresses() {
        Servient.getAddresses();

        // should not fail
        assertTrue(true);
    }

    @Test
    public void addThingWithoutId() {
        when(exposedThing.getId()).thenReturn(null);

        Servient servient = new Servient(List.of(), Map.of(), Map.of(), new HashMap<>());
        servient.addThing(exposedThing);

        verify(exposedThing, times(1)).setId(any());
    }

    @Test
    public void getClientForNegative() throws ProtocolClientException {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        assertNull(servient.getClientFor("test"));
    }

    @Test(expected = ServientException.class)
    public void register() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.register("test://foo/bar", null).get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = ServientException.class)
    public void unregister() throws Throwable {
        Servient servient = new Servient(List.of(), Map.of(), Map.of(), Map.of());

        try {
            servient.unregister("test://foo/bar", null).get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}