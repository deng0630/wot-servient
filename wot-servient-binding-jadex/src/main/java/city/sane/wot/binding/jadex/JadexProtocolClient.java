package city.sane.wot.binding.jadex;

import city.sane.Pair;
import city.sane.Triple;
import city.sane.wot.binding.ProtocolClient;
import city.sane.wot.binding.ProtocolClientException;
import city.sane.wot.content.Content;
import city.sane.wot.thing.Thing;
import city.sane.wot.thing.filter.ThingFilter;
import city.sane.wot.thing.form.Form;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.SFuture;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.search.ServiceQuery;
import jadex.commons.TimeoutException;
import jadex.commons.future.ITerminableIntermediateFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static jadex.commons.future.IFuture.DONE;

/**
 * Allows consuming Things via Jadex Micro Agents.
 * The Jadex Platform created by {@link JadexProtocolClientFactory} is used for this purpose and thus enables interaction with exposed Things on other
 * platforms.
 */
public class JadexProtocolClient implements ProtocolClient {
    static final Logger log = LoggerFactory.getLogger(JadexProtocolClient.class);

    private final IExternalAccess platform;

    public JadexProtocolClient(IExternalAccess platform) {
        this.platform = platform;
    }

    @Override
    public CompletableFuture<Content> readResource(Form form) {
        try {
            Triple<String, String, String> triple = parseAsReadResourceHref(form.getHref());
            String serviceId = triple.first();
            String type = triple.second();
            String name = triple.third();

            CompletableFuture<Content> result = new CompletableFuture<>();
            platform.scheduleStep(ia -> {
                getThingService(ia, serviceId).whenComplete((service, e) -> {
                    if (e == null) {
                        log.trace("Found service {}", service);

                        JadexContent content;
                        if (type.equals("all")) {
                            log.trace("Read properties");
                            content = service.readProperties().get();
                            log.trace("Done reading properties");
                        }
                        else {
                            log.trace("Read property");
                            content = service.readProperty(name).get();
                            log.trace("Done reading property");
                        }
                        result.complete(content.fromJadex());
                    }
                    else {
                        result.completeExceptionally(e);
                    }
                });
                return DONE;
            });
            return result;
        }
        catch (ProtocolClientException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public CompletableFuture<Content> writeResource(Form form, Content content) {
        try {
            log.debug("Write resource '{}'", form.getHref());
            Pair<String, String> pair = parseAsWriteResourceHref(form.getHref());
            String serviceId = pair.first();
            String name = pair.second();

            CompletableFuture<Content> result = new CompletableFuture<>();
            platform.scheduleStep(ia -> {
                getThingService(ia, serviceId).whenComplete((service, e) -> {
                    if (e == null) {
                        log.trace("Found service {}", service);

                        JadexContent input = new JadexContent(content);
                        JadexContent output = service.writeProperty(name, input).get();
                        result.complete(output.fromJadex());
                    }
                    else {
                        result.completeExceptionally(e);
                    }
                });
                return DONE;
            });
            return result;
        }
        catch (ProtocolClientException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public CompletableFuture<Collection<Thing>> discover(ThingFilter filter) {
        CompletableFuture<Collection<Thing>> result = new CompletableFuture<>();

        platform.scheduleStep(ia -> {
            ServiceQuery<ThingService> query = new ServiceQuery<>(ThingService.class, ServiceScope.GLOBAL);

            AtomicInteger ai = new AtomicInteger();
            ai.incrementAndGet(); // start searchServices()
            ITerminableIntermediateFuture<ThingService> search = ia.searchServices(query);
            search.addResultListener(services -> discoverThingServices(filter, result, ai, services), result::completeExceptionally);

            return DONE;
        });

        return result;
    }

    private void discoverThingServices(ThingFilter filter, CompletableFuture<Collection<Thing>> result, AtomicInteger ai, Collection<ThingService> services) {
        Collection<Thing> things = new ArrayList<>();
        services.forEach(service -> {
            ai.incrementAndGet(); // start get()
            service.get().addResultListener(json -> {
                Thing thing = Thing.fromJson(json);
                things.add(thing);
                if (ai.decrementAndGet() < 1) { // end get()
                    if (filter.getQuery() != null) {
                        // TODO: move filter to server-side
                        List<Thing> filtered = filter.getQuery().filter(things);
                        result.complete(filtered);
                    }
                    else {
                        result.complete(things);
                    }
                }
            }, result::completeExceptionally);
        });
        if (ai.decrementAndGet() < 1) { // end searchServices()
            if (filter.getQuery() != null) {
                // TODO: move filter to server-side
                List<Thing> filtered = filter.getQuery().filter(things);
                result.complete(filtered);
            }
            else {
                result.complete(things);
            }
        }
    }

    private Triple<String, String, String> parseAsReadResourceHref(String href) throws ProtocolClientException {
        try {
            URI uri = new URI(href);

            String path = uri.getPath();
            if (path.startsWith("/ThingService_")) {
                String[] pathFragments = path.split("/", 4);
                if (pathFragments.length == 4) {
                    String serviceId = pathFragments[1];
                    String type = pathFragments[2];
                    String name = pathFragments[3];

                    return new Triple<>(serviceId, type, name);
                }
                else {
                    throw new ProtocolClientException("Bad href path: " + path);
                }
            }
            else {
                throw new ProtocolClientException("Href points to unexpected agent: " + uri);
            }
        }
        catch (URISyntaxException e) {
            throw new ProtocolClientException(e);
        }
    }

    private Pair<String, String> parseAsWriteResourceHref(String href) throws ProtocolClientException {
        try {
            URI uri = new URI(href);

            String path = uri.getPath();
            if (path.startsWith("/ThingService_")) {
                String[] pathFragments = path.split("/", 4);
                if (pathFragments.length == 4) {
                    String serviceId = pathFragments[1];
                    String name = pathFragments[3];

                    return new Pair<>(serviceId, name);
                }
                else {
                    throw new ProtocolClientException("Bad href path: " + path);
                }
            }
            else {
                throw new ProtocolClientException("Href points to unexpected agent: " + uri);
            }
        }
        catch (URISyntaxException e) {
            throw new ProtocolClientException(e);
        }
    }

    public CompletableFuture<ThingService> getThingService(IInternalAccess agent, String serviceId) {
        log.debug("Search ThingService with id '{}'", serviceId);
        ServiceQuery<ThingService> query = new ServiceQuery(ThingService.class)
                .setScope(ServiceScope.GLOBAL)
                .setId(serviceId);
        try {
            return CompletableFuture.completedFuture(SFuture.getFirstResultAndTerminate(agent.addQuery(query)));
        }
        catch (TimeoutException e) {
            log.warn("Search ThingService with id '{}' failed: {}", serviceId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
