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
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.converter.Converters;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Kanketsu20FeaturesTest {

    private ByteArrayOutputStream systemOut;
    private PrintStream originalOut;
    private TestLogger testLogger;

    static class TestLogger implements Logger {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();

        @Override
        public void log(String message) {}

        @Override
        public void info(String msg) { infoMessages.add(msg); }

        @Override
        public void warn(String msg) { warnMessages.add(msg); }

        @Override
        public void error(String msg) { errorMessages.add(msg); }

        @Override
        public boolean isDebugEnabled() { return false; }

        public List<String> getInfoMessages() { return infoMessages; }
        public List<String> getWarnMessages() { return warnMessages; }
        public List<String> getErrorMessages() { return errorMessages; }
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
    void mixedOptionsBelongToCurrentCommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .hasArg(false)
                                .converter(Converters.BOOLEAN))
                        .action(ctx -> {
                            boolean v = ctx.getOptionValue("verbose", Boolean.class);
                            System.out.println("git verbose=" + v);
                        })
                        .command("commit", commit -> commit
                                .action(ctx -> System.out.println("commit")))
                )
                .build();

        int exitCode = cli.execute("git", "--verbose");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("git verbose=true");
    }

    @Test
    void subcommandOptionsBeforeSubcommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .option("message", opt -> opt
                                        .shortOpt("m")
                                        .hasArg(true)
                                        .converter(Converters.STRING))
                                .action(ctx -> {
                                    String msg = ctx.getOptionValue("message", String.class);
                                    System.out.println("msg=" + msg);
                                }))
                )
                .build();

        int exitCode = cli.execute("git", "commit", "--message", "hello");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("msg=hello");
    }

    @Test
    void strictSubcommandMatchingReturnsError() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git", "unknown");
        assertThat(exitCode).isEqualTo(1);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown subcommand: unknown"));
    }

    @Test
    void noActionAutoHelp() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Usage") && msg.contains("commit"));
    }

    @Test
    void autoHelpDisabledGlobal() {
        System.setProperty("kanketsu.autoHelp", "false");

        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("help", opt -> opt
                                .shortOpt("h")
                                .hasArg(false)
                                .converter(Converters.BOOLEAN))
                        .action(ctx -> {
                            if (ctx.getOptionValue("help", Boolean.class)) {
                                System.out.println("Custom help");
                            } else {
                                System.out.println("Executed");
                            }
                        })
                )
                .build();

        int exitCode = cli.execute("--help");
        assertThat(exitCode).isEqualTo(1);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown command: --help"));
    }

    @Test
    void autoHelpDisabledSubcommandWithCustomHelp() {
        System.setProperty("kanketsu.autoHelp", "false");

        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("help", opt -> opt
                                .shortOpt("h")
                                .hasArg(false)
                                .converter(Converters.BOOLEAN))
                        .action(ctx -> {
                            if (ctx.getOptionValue("help", Boolean.class)) {
                                System.out.println("Custom help");
                            } else {
                                System.out.println("Executed");
                            }
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--help");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("Custom help");
    }

    @Test
    void typeSafeOptionValue() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("port", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {
                            int port = ctx.getOptionValue("port", Integer.class);
                            System.out.println(port);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--port", "8080");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("8080");
    }

    @Test
    void typeSafeOptionValueMismatch() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("port", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {
                            ctx.getOptionValue("port", String.class);
                            System.out.println("Should not reach");
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--port", "8080");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Option 'port' is of type java.lang.Integer"));
    }

    @Test
    void defaultValueApplied() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("file", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .defaultValue("default.txt"))
                        .action(ctx -> {
                            String file = ctx.getOptionValue("file", String.class);
                            System.out.println(file);
                        })
                )
                .build();

        int exitCode = cli.execute("sub");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("default.txt");
    }

    @Test
    void defaultValueOverridden() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("file", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .defaultValue("default.txt"))
                        .action(ctx -> {
                            String file = ctx.getOptionValue("file", String.class);
                            System.out.println(file);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--file", "user.txt");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("user.txt");
    }

    @Test
    void invalidIntegerOptionReturnsError() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("port", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {})
                )
                .build();

        int exitCode = cli.execute("sub", "--port", "abc");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Invalid value 'abc' for option --port"));
    }

    @Test
    void combinedShortOptionWithArg() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .option("all", opt -> opt
                                .shortOpt("a")
                                .hasArg(false))
                        .action(ctx -> {
                            String v = ctx.getOptionValue("verbose", String.class);
                            System.out.println("verbose=" + v);
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "-vfile");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("verbose=file");
    }

    @Test
    void doubleDashTerminator() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("file", opt -> opt
                                .shortOpt("f")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println("pos: " + String.join(",", pos));
                        })
                )
                .build();

        int exitCode = cli.execute("sub", "--file", "a.txt", "--", "--extra", "x");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("pos: --extra,x");
    }

    @Test
    void helpInRemainingAfterSubcommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit","Commit changes", commit -> commit
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git", "commit", "--help");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Commit changes") && msg.contains("Usage"));
    }

    @Test
    void helpInMainLoopBeforeSubcommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", "Git command", git -> git
                        .command("commit", commit -> commit
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git", "--help", "commit");
        assertThat(exitCode).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Git command") && msg.contains("commit"));
    }

    @Test
    void unknownOptionInPreParsed() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git", "--unknown", "commit");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown option: --unknown"));
    }

    @Test
    void requiredOptionOnCurrentCommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .option("message", opt -> opt
                                        .shortOpt("m")
                                        .hasArg(true)
                                        .converter(Converters.STRING)
                                        .required(true))
                                .action(ctx -> {
                                    String msg = ctx.getOptionValue("message", String.class);
                                    System.out.println("msg=" + msg);
                                }))
                )
                .build();

        int exitCode = cli.execute("git", "commit", "-m", "hello");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("msg=hello");
    }

    @Test
    void requiredOptionMissingReturnsError() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("commit", commit -> commit
                                .option("message", opt -> opt
                                        .shortOpt("m")
                                        .hasArg(true)
                                        .converter(Converters.STRING)
                                        .required(true))
                                .action(ctx -> {}))
                )
                .build();

        int exitCode = cli.execute("git", "commit");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Missing required option: --message"));
    }

    @Test
    void deepNestedCommandWithPositionalArgs() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", git -> git
                        .command("remote", remote -> remote
                                .command("add", add -> add
                                        .option("verbose", opt -> opt
                                                .shortOpt("v")
                                                .hasArg(false)
                                                .converter(Converters.BOOLEAN))
                                        .action(ctx -> {
                                            List<String> pos = ctx.getPositionalArgs();
                                            if (pos.size() >= 2) {
                                                System.out.println("name=" + pos.get(0) + ", url=" + pos.get(1));
                                            } else {
                                                System.out.println("Not enough positional args");
                                            }
                                            Boolean verbose = ctx.getOptionValue("verbose", Boolean.class);
                                            if (verbose != null && verbose) {
                                                System.out.println("verbose mode");
                                            }
                                        }))
                        )
                )
                .build();

        int exitCode = cli.execute("git", "remote", "add", "--verbose", "origin", "https://github.com/user/repo.git");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim())
                .contains("name=origin, url=https://github.com/user/repo.git")
                .contains("verbose mode");
    }
}