package city.sane.wot.cli;

import org.apache.commons.cli.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class CliTest {
    @Test
    public void help() throws ParseException, IOException {
        new Cli(new String[]{ "--help" });
    }

    @Test
    public void version() throws ParseException, IOException {
        new Cli(new String[]{ "--version" });
    }

    @Test
    public void runGivenScript() throws ParseException, IOException {
        new Cli(new String[]{
                "examples/scripts/example-dynamic.groovy"
        });
    }
}