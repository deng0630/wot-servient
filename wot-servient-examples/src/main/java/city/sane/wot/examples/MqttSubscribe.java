package city.sane.wot.examples;

import city.sane.wot.DefaultWot;
import city.sane.wot.Wot;
import city.sane.wot.WotException;
import city.sane.wot.thing.ConsumedThing;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Interacts with the thing produces by {@link MqttSubscribe}. Reads the counter and resets it
 * periodically.
 */
class MqttSubscribe {
    public static void main(String[] args) throws IOException, WotException {
        // Setup MQTT broker address/port details in application.json!

        // create wot
        Config config = ConfigFactory
                .parseString("wot.servient.client-factories = [\"city.sane.wot.binding.mqtt.MqttProtocolClientFactory\"]")
                .withFallback(ConfigFactory.load());
        Wot wot = DefaultWot.clientOnly(config);

        String thing = "{\n" +
                "    \"@context\": \"https://www.w3.org/2019/td/v1\",\n" +
                "    \"title\": \"MQTT Counter\",\n" +
                "    \"id\": \"urn:dev:wot:mqtt:counter\",\n" +
                "    \"actions\" : {\n" +
                "        \"resetCounter\": {\n" +
                "            \"forms\": [\n" +
                "                {\"href\": \"mqtt://iot.eclipse.org/MQTT-Test/actions/resetCounter\",  \"mqtt:qos\":  0, \"mqtt:retain\" : false}\n" +
                "            ]\n" +
                "        }\n" +
                "    }, \n" +
                "    \"events\": {\n" +
                "        \"temperature\": {\n" +
                "            \"data\": {\n" +
                "                \"type\": \"integer\"\n" +
                "            },\n" +
                "            \"forms\": [\n" +
                "                {\"href\": \"mqtt://iot.eclipse.org/MQTT-Test/events/counterEvent\",  \"mqtt:qos\":  0, \"mqtt:retain\" : false}\n" +
                "            ]\n" +
                "        } \n" +
                "    } \n" +
                "}";

        ConsumedThing consumedThing = wot.consume(thing);
        System.out.println("=== TD ===");
        System.out.println(consumedThing.toJson(true));
        System.out.println("==========");

        consumedThing.getEvent("temperature").observer().subscribe(
                next -> System.out.println("MqttSubscribe: next = " + next),
                ex -> System.out.println("MqttSubscribe: error = " + ex.toString()),
                () -> System.out.println("MqttSubscribe: completed!")
        );
        System.out.println("MqttSubscribe: Subscribed");

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("MqttSubscribe: Reset counter!");
                consumedThing.getAction("resetCounter").invoke();
            }
        }, 0, 20000);

        System.out.println("Press ENTER to exit the client");
        System.in.read();
    }
}
