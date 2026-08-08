/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.converter.Converters;
import io.github.kanketsuteam.kanketsu.core.exception.CommandBuildException;
import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KanketsuAdditionalTest {

    private ByteArrayOutputStream systemOut;
    private PrintStream originalOut;
    private TestLogger testLogger;

    static class TestLogger implements Logger {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();
        private final List<String> debugMessages = new ArrayList<>();
        private boolean debugEnabled = true;

        @Override public void log(String message) {}
        @Override public void info(String msg) { infoMessages.add(msg); }
        @Override public void warn(String msg) { warnMessages.add(msg); }
        @Override public void error(String msg) { errorMessages.add(msg); }
        @Override public void error(String msg, Throwable t) { errorMessages.add(msg); }
        @Override public void debug(String msg) { debugMessages.add(msg); }
        @Override public boolean isDebugEnabled() { return debugEnabled; }

        public List<String> getInfoMessages() { return infoMessages; }
        public List<String> getWarnMessages() { return warnMessages; }
        public List<String> getErrorMessages() { return errorMessages; }
        public List<String> getDebugMessages() { return debugMessages; }
    }

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        systemOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOut));
        testLogger = new TestLogger();
        System.setProperty("kanketsu.autoHelp", "true");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.clearProperty("kanketsu.autoHelp");
    }

    @Test
    void globalOptionRequiredIsIgnored() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .option(Option.builder("global")
                        .shortOpt("g")
                        .hasArg(true)
                        .required(true)
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd
                        .action(ctx -> System.out.println("executed")))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("executed");
        assertThat(testLogger.getErrorMessages()).isEmpty();
    }

    @Test
    void globalOptionDefaultValueIsIgnored() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .option(Option.builder("global")
                        .shortOpt("g")
                        .hasArg(true)
                        .defaultValue("default")
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd
                        .action(ctx -> {
                            String val = ctx.getOptionValue("global", String.class);
                            System.out.println("val=" + val);
                        }))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("val=null");
    }

    @Test
    void globalOptionAfterSubcommandCausesUnknownOptionError() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .option(Option.builder("global")
                        .shortOpt("g")
                        .hasArg(true)
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd
                        .action(ctx -> {
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println("pos=" + pos);
                        }))
                .build();

        int exit = cli.execute("sub", "arg1", "-g", "value");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown option: -g"));
    }

    @Test
    void customHelpGeneratorIsUsedForCommandHelp() {
        HelpGenerator custom = new HelpGenerator() {
            @Override
            public void output(String helpText) {
            }

            @Override
            public String generateDetailedHelp(java.util.Map<String, io.github.kanketsuteam.kanketsu.core.command.Command> roots, String path) {
                return "CUSTOM HELP FOR " + path;
            }
        };

        CLI cli = CLI.builder()
                .logger(testLogger)
                .helpGenerator(custom)
                .command("sub", "A sub command", cmd -> cmd
                        .option("opt", opt -> opt.description("an option"))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "--help");
        assertThat(exit).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.equals("CUSTOM HELP FOR sub"));
    }

    @Test
    void typeConverterPriorityRespectsRegistrationOrder() {
        TypeConverter intConverter = new TypeConverter() {
            @Override public boolean supports(Class<?> targetType) { return targetType == Integer.class; }
            @Override public Object convert(String source, CommandContext context) {
                return Integer.parseInt(source) + 100;
            }
        };

        CLI cli = CLI.builder()
                .converter(intConverter)
                .command("sub", cmd -> cmd
                        .option("val", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            Integer i = ctx.getOptionValueAs("val", Integer.class);
                            System.out.println(i);
                        }))
                .build();

        int exit = cli.execute("sub", "--val", "123");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("223");
    }

    @Test
    void getOptionValueAsFailsWhenRawValueNotStringAndTargetMismatch() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt.hasArg(true).converter(Converters.INT))
                        .action(ctx -> {
                            Long val = ctx.getOptionValueAs("num", Long.class);
                            System.out.println(val);
                        }))
                .build();

        int exit = cli.execute("sub", "--num", "123");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Cannot convert non-string raw value"));
    }

    @Test
    void actionThrowsRuntimeException_returnsErrorCodeAndLogs() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .action(ctx -> { throw new RuntimeException("test exception"); }))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(1);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("test exception"));
    }

    @Test
    void duplicateSubcommandNameThrowsCommandBuildException() {
        assertThatThrownBy(() -> {
            CLI.builder()
                    .command("sub", cmd -> cmd
                            .command("inner", inner -> {})
                            .command("inner", inner -> {}))
                    .build();
        }).isInstanceOf(CommandBuildException.class)
                .hasMessageContaining("Duplicate subcommand: inner");
    }

    @Test
    void optionLongOptOverrideIsAllowedAndShortMapIsUpdated() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("opt", o -> o.shortOpt("o").hasArg(false))
                        .option("opt", o -> o.shortOpt("x").hasArg(true))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("opt", String.class);
                            System.out.println(val);
                        }))
                .build();

        int exit = cli.execute("sub", "-x", "hello");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("hello");

        int exit2 = cli.execute("sub", "-o");
        assertThat(exit2).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown option: -o"));
    }

    @Test
    void helpOptionAfterDoubleDashIsStillRecognizedAsHelp() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .action(ctx -> {
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println("pos=" + pos);
                        }))
                .build();

        int exit = cli.execute("sub", "--", "--help");
        assertThat(exit).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Usage") && msg.contains("sub"));
    }

    @Test
    void defaultValueMismatchTypeThrowsAtRuntime() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT)
                                .defaultValue("abc"))
                        .action(ctx -> {
                            int val = ctx.getOptionValue("num", Integer.class);
                            System.out.println(val);
                        }))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Invalid value 'abc' for option --num"));
    }
}