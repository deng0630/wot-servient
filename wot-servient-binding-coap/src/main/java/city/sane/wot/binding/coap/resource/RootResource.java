package city.sane.wot.binding.coap.resource;

import city.sane.wot.binding.coap.WotCoapServer;
import city.sane.wot.content.Content;
import city.sane.wot.content.ContentCodecException;
import city.sane.wot.content.ContentManager;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint for listing all Things from the {@link city.sane.wot.Servient}.
 */
public class RootResource extends AbstractResource {
    private static final Logger log = LoggerFactory.getLogger(RootResource.class);
    private final WotCoapServer server;

    public RootResource(WotCoapServer server) {
        super("");
        this.server = server;
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        log.debug("Handle GET to '{}'", getURI());

        String requestContentFormat = getOrDefaultRequestContentType(exchange);

        if (ensureSupportedContentFormat(exchange, requestContentFormat)) {
            try {
                Content content = ContentManager.valueToContent(server.getProtocolServer().getThings(), requestContentFormat);
                int contentFormat = MediaTypeRegistry.parse(content.getType());

                exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
            }
            catch (ContentCodecException e) {
                log.warn("Exception", e);
                exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.getMessage());
            }
        }
    }
}
