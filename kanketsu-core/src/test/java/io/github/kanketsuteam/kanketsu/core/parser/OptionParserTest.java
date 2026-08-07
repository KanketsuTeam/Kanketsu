package io.github.kanketsuteam.kanketsu.core.parser;

import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.command.CommandBuilder;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.converter.Converter;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueInvalidException;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueMissingException;
import io.github.kanketsuteam.kanketsu.core.exception.UnknownOptionException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionParserTest {

    private OptionParser parser;
    private Command cmd;
    private Map<String, Option> globalOptions;
    private Map<Option, Object> initial;

    static class IntegerConverter implements Converter {
        @Override
        public Object convert(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid port number");
            }
        }
    }

    @BeforeEach
    void setUp() {
        cmd = new CommandBuilder("test", "Test command")
                .option(Option.builder("verbose")
                        .shortOpt("v")
                        .description("Enable verbose mode")
                        .build())
                .option(Option.builder("file")
                        .shortOpt("f")
                        .hasArg(true)
                        .description("Input file")
                        .build())
                .option(Option.builder("port")
                        .shortOpt("p")
                        .hasArg(true)
                        .defaultValue("8080")
                        .converter(new IntegerConverter())
                        .description("Port number")
                        .build())
                .option(Option.builder("debug")
                        .shortOpt("d")
                        .description("Debug mode")
                        .build())
                .option(Option.builder("help")
                        .shortOpt("h")
                        .description("Show help")
                        .build())
                .build();

        globalOptions = new LinkedHashMap<>();
        globalOptions.put("global-flag", Option.builder("global-flag")
                .shortOpt("g")
                .description("Global flag")
                .build());

        initial = new LinkedHashMap<>();
        List<TypeConverter> converters = Collections.emptyList();
        parser = new OptionParser(converters);
    }

    @Test
    void testParseLongOptionWithEquals() {
        String[] args = {"--file=test.txt"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
        assertThat(ctx.getPositionalArgs()).isEmpty();
    }

    @Test
    void testParseLongOptionWithSpace() {
        String[] args = {"--file", "test.txt"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
        assertThat(ctx.getPositionalArgs()).isEmpty();
    }

    @Test
    void testParseShortOptionWithSpace() {
        String[] args = {"-f", "test.txt"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
    }

    @Test
    void testParseShortOptionWithEquals() {
        String[] args = {"-f=test.txt"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
    }

    @Test
    void testParseLongFlagOption() {
        String[] args = {"--verbose"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
    }

    @Test
    void testParseShortFlagOption() {
        String[] args = {"-v"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
    }

    @Test
    void testParseCombinedShortFlags() {
        String[] args = {"-vd"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
        assertThat(ctx.getOptionValue("debug", Boolean.class)).isTrue();
    }

    @Test
    void testParseCombinedShortFlagsWithArgThrowsException() {
        String[] args = {"-vf"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(OptionValueMissingException.class)
                .hasMessageContaining("Option -f requires a value, but is in a combined short option.");
    }

    @Test
    void testParseLongOptionMissingValueThrowsException() {
        String[] args = {"--file"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(OptionValueMissingException.class)
                .hasMessageContaining("Option --file requires a value");
    }

    @Test
    void testParseShortOptionMissingValueThrowsException() {
        String[] args = {"-f"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(OptionValueMissingException.class)
                .hasMessageContaining("Option -f requires a value");
    }

    @Test
    void testParseLongOptionInvalidValueThrowsException() {
        String[] args = {"--port=abc"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(OptionValueInvalidException.class)
                .hasMessageContaining("Invalid value 'abc' for option --port");
    }

    @Test
    void testParseShortOptionInvalidValueThrowsException() {
        String[] args = {"-p=xyz"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(OptionValueInvalidException.class)
                .hasMessageContaining("Invalid value 'xyz' for option --port");
    }

    @Test
    void testParseUnknownLongOptionThrowsException() {
        String[] args = {"--unknown"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(UnknownOptionException.class)
                .hasMessageContaining("Unknown option: --unknown");
    }

    @Test
    void testParseUnknownShortOptionThrowsException() {
        String[] args = {"-x"};
        assertThatThrownBy(() -> parser.parseOptions(args, cmd, initial, globalOptions))
                .isInstanceOf(UnknownOptionException.class)
                .hasMessageContaining("Unknown option: -x");
    }

    @Test
    void testParseDoubleDashStopsParsing() {
        String[] args = {"--verbose", "--", "--file", "test.txt"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
        assertThat(ctx.getPositionalArgs()).containsExactly("--file", "test.txt");
        assertThat(ctx.getOptionValue("file", String.class)).isNull();
    }

    @Test
    void testParsePositionalArgumentsMixed() {
        String[] args = {"arg1", "-v", "arg2", "--file=test.txt", "arg3"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
        assertThat(ctx.getPositionalArgs()).containsExactly("arg1", "arg2", "arg3");
    }

    @Test
    void testParseWithDefaultValue() {
        String[] args = {};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("port", Integer.class)).isEqualTo(8080);
    }

    @Test
    void testParseWithInitialValuesOverride() {
        Option portOpt = cmd.getOptions().get("port");
        initial.put(portOpt, 9999);
        String[] args = {};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("port", Integer.class)).isEqualTo(9999);
    }

    @Test
    void testParseMultipleOptions() {
        String[] args = {"-v", "--file=test.txt", "-p", "9090", "--debug"};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.getOptionValue("verbose", Boolean.class)).isTrue();
        assertThat(ctx.getOptionValue("file", String.class)).isEqualTo("test.txt");
        assertThat(ctx.getOptionValue("port", Integer.class)).isEqualTo(9090);
        assertThat(ctx.getOptionValue("debug", Boolean.class)).isTrue();
        assertThat(ctx.getPositionalArgs()).isEmpty();
    }

    @Test
    void testInitialContainsGlobalOption() {
        Option globalOpt = globalOptions.get("global-flag");
        initial.put(globalOpt, true);
        String[] args = {};
        CommandContext ctx = parser.parseOptions(args, cmd, initial, globalOptions);
        assertThat(ctx.hasOption(globalOpt)).isTrue();
        assertThat(ctx.getOptionValue("global-flag", Boolean.class)).isTrue();
    }
}