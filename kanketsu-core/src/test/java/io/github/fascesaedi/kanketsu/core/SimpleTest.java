/*
 *
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
package io.github.fascesaedi.kanketsu.core;

import io.github.fascesaedi.kanketsu.spi.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTest {
    private ByteArrayOutputStream systemOut;
    private PrintStream originalOut;
    private TestLogger testLogger;

    static class TestLogger implements Logger {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();
        private final List<String> debugMessages = new ArrayList<>();
        private boolean debugEnabled = true;

        @Override
        public void log(String message) {
        }

        @Override
        public void info(String msg) {
            infoMessages.add(msg);
        }

        @Override
        public void warn(String msg) {
            warnMessages.add(msg);
        }

        @Override
        public void error(String msg) {
            errorMessages.add(msg);
        }

        public void debug(String msg) {
            debugMessages.add(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        @Override
        public PrintStream getPrintStream() {
            return System.err;
        }

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
    void testSimpleCommand() {
        CLI cli = CLI.builder()
                .command("hi", hi -> hi
                        .action(ctx -> System.out.println("HI")))
                .build();

        cli.execute("hi");
        assertThat(systemOut.toString().trim()).isEqualTo("HI");
    }

    @Test
    void testCommandWithOption() {
        CLI cli = CLI.builder()
                .command("echo", cmd -> cmd
                        .option("message", opt -> opt
                                .shortOpt("m")
                                .description("Message")
                                .hasArg(true))
                        .action(ctx -> {
                            String msg = (String) ctx.getOption("message");
                            System.out.println(msg);
                        }))
                .build();

        cli.execute(new String[]{"echo", "--message", "Hello Kanketsu"});
        assertThat(systemOut.toString().trim()).isEqualTo("Hello Kanketsu");
    }

    @Test
    void testUnknownCommandLogsWarning() {
        TestLogger logger = new TestLogger();
        CLI cli = CLI.builder()
                .logger(logger)
                .build();

        cli.execute("i_dont_exist");

        assertThat(logger.warnMessages)
                .as("应记录警告信息")
                .anyMatch(msg -> msg.contains("Unknown command"));
    }

    @Test
    void defaultValueShouldBeUsedWhenOptionNotProvided() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file")
                                .hasArg(true)
                                .defaultValue("default.txt"))
                        .action(ctx -> System.out.println(ctx.getOption("file")))
                ).build();
        cli.execute("sub");
        assertThat(systemOut.toString().trim()).isEqualTo("default.txt");
    }

    @Test
    void noArguments() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", "A test command", cmd -> cmd.action(ctx -> System.out.println("executed")))
                .build();

        int exitCode = cli.execute();
        assertThat(exitCode).isEqualTo(1);
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("No command provided"));
    }

    @Test
    void globalHelpLong() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", "A test command", cmd -> cmd.action(ctx -> {}))
                .command("other", "Another command", cmd -> cmd.action(ctx -> {}))
                .build();

        int exitCode = cli.execute("--help");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Usage: <command>"));
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("test") && msg.contains("A test command"));
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("other") && msg.contains("Another command"));
    }

    @Test
    void globalHelpShort() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", "A test command", cmd -> cmd.action(ctx -> {}))
                .build();

        int exitCode = cli.execute("-h");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Usage: <command>"));
    }

    @Test
    void unknownRootCommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", "A test command", cmd -> cmd.action(ctx -> {}))
                .build();

        int exitCode = cli.execute("unknown");
        assertThat(exitCode).isEqualTo(1);
        assertThat(testLogger.getWarnMessages()).anyMatch(msg -> msg.contains("Unknown command: unknown"));
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Use --help"));
    }

    @Test
    void requiredOptionMissing() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(true)
                                .required(true))
                        .action(ctx -> System.out.println("executed"))
                )
                .build();

        int exitCode = cli.execute("sub");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getWarnMessages()).anyMatch(msg -> msg.contains("Missing required option: --file"));
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Usage"));
    }

    @Test
    void validCommandWithOptions() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(true))
                        .action(ctx -> {
                            String file = (String) ctx.getOption("file");
                            System.out.println("File: " + file);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--file", "a.txt");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("File: a.txt");
    }

    @Test
    void subCommandHelp() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .command("cmd", "Inner command", sub -> sub
                                .option("verbose", opt -> opt
                                        .shortOpt("v")
                                        .description("verbose")
                                        .hasArg(false))
                                .action(ctx -> {})
                        )
                )
                .build();

        int exitCode = cli.execute("sub", "cmd", "--help");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Inner command") && msg.contains("--verbose"));
    }

    @Test
    void unknownLongOption() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(false))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "--unknown");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Unknown option: --unknown"));
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("Usage"));
    }

    @Test
    void unknownShortOptionSingle() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(false))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "-x");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Unknown option: -x"));
    }

    @Test
    void unknownShortOptionInCombination() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("all", opt -> opt
                                .shortOpt("a")
                                .description("all")
                                .hasArg(false))
                        .option("build", opt -> opt
                                .shortOpt("b")
                                .description("build")
                                .hasArg(false))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "-abx");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Unknown short option: -x"));
    }

    @Test
    void longOptionMissingValue() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(true))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "--file");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Option --file requires a value"));
    }

    @Test
    void shortOptionMissingValue() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(true))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "-f");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Option -f requires a value"));
    }

    @Test
    void shortOptionEqualsForm() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file option")
                                .hasArg(true))
                        .action(ctx -> {
                            String file = (String) ctx.getOption("file");
                            System.out.println("File: " + file);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "-f=my.txt");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("File: my.txt");
    }

    @Test
    void combinedShortOptionWithValue() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .description("verbose")
                                .hasArg(true))
                        .option("all", opt -> opt
                                .shortOpt("a")
                                .description("all")
                                .hasArg(false))
                        .action(ctx -> {
                            String v = (String) ctx.getOption("verbose");
                            System.out.println("verbose=" + v);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "-vfile");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("verbose=file");
    }

    @Test
    void combinedShortOptionsAllBoolean() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("all", opt -> opt
                                .shortOpt("a")
                                .description("all")
                                .hasArg(false)
                                .category(Category.BOOLEAN))
                        .option("build", opt -> opt
                                .shortOpt("b")
                                .description("build")
                                .hasArg(false)
                                .category(Category.BOOLEAN))
                        .option("clean", opt -> opt
                                .shortOpt("c")
                                .description("clean")
                                .hasArg(false)
                                .category(Category.BOOLEAN))
                        .action(ctx -> {
                            boolean a = (boolean)(ctx.getOption("all"));
                            boolean b = (boolean)(ctx.getOption("build"));
                            boolean c = (boolean)(ctx.getOption("clean"));
                            System.out.printf("a=%b, b=%b, c=%b", a, b, c);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "-abc");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("a=true, b=true, c=true");
    }

    @Test
    void combinedShortOptionWithRequiredValue() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("all", opt -> opt
                                .shortOpt("a")
                                .description("all")
                                .hasArg(false))
                        .option("build", opt -> opt
                                .shortOpt("b")
                                .description("build")
                                .hasArg(true))
                        .option("clean", opt -> opt
                                .shortOpt("c")
                                .description("clean")
                                .hasArg(false))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "-abc");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("Parameter error: Option -b requires a value"));
    }

    @Test
    void doubleDashSeparator() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file")
                                .hasArg(true))
                        .action(ctx -> {
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println("Positional: " + String.join(",", pos));
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--file", "a.txt", "--", "--extra", "x");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("Positional: --extra,x");
    }

    @Test
    void actionIsNull() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "Sub command", cmd -> {
                    // 故意不设置 action
                })
                .build();

        int exitCode = cli.execute("sub");
        assertThat(exitCode).isEqualTo(1);
        assertThat(testLogger.getErrorMessages()).anyMatch(msg -> msg.contains("has no action defined"));
    }

    @Test
    void autoHelpDisabledButHelpStillWorks() {
        System.setProperty("kanketsu.autoHelp", "false");

        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", "A sub command", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .description("file")
                                .hasArg(false))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "--help");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages()).anyMatch(msg -> msg.contains("A sub command") && msg.contains("--file"));
    }
}