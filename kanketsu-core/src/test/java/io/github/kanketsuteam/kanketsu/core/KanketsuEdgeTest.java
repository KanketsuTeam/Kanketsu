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

import io.github.kanketsuteam.kanketsu.core.command.CommandBuilder;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.converter.Converter;
import io.github.kanketsuteam.kanketsu.core.converter.Converters;
import io.github.kanketsuteam.kanketsu.core.exception.*;
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

public class KanketsuEdgeTest {

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
    void executeWithNullArguments_throwsCommandException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", cmd -> cmd.action(ctx -> {}))
                .build();

        assertThatThrownBy(() -> cli.execute((String[]) null))
                .isInstanceOf(CommandException.class)
                .hasMessageContaining("Arguments array cannot be null");
    }

    @Test
    void executeWithEmptyArguments_printsNoCommand() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("test", cmd -> cmd.action(ctx -> {}))
                .build();

        int exit = cli.execute();
        assertThat(exit).isEqualTo(1);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("No command provided"));
    }

    @Test
    void optionValueCanBeEmptyString() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("msg", opt -> opt
                                .shortOpt("m")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("msg", String.class);
                            System.out.println("val='" + val + "'");
                        })
                )
                .build();

        int exit = cli.execute("sub", "--msg", "");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("val=''");
    }

    @Test
    void optionValueCanBeJustSpaces() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("msg", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("msg", String.class);
                            System.out.println("val='" + val + "'");
                        })
                )
                .build();

        int exit = cli.execute("sub", "--msg", "   ");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("val='   '");
    }

    @Test
    void veryLongOptionValue() {
        String longVal = "a".repeat(10000);
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("data", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("data", String.class);
                            System.out.println("length=" + val.length());
                        })
                )
                .build();

        int exit = cli.execute("sub", "--data", longVal);
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("length=10000");
    }

    @Test
    void veryLongOptionName() {
        String longOpt = "--" + "x".repeat(500);
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option(longOpt.substring(2), opt -> opt
                                .hasArg(false))
                        .action(ctx -> System.out.println("executed"))
                )
                .build();

        int exit = cli.execute("sub", longOpt);
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("executed");
    }

    @Test
    void optionValueWithControlCharacters() {
        String[] captured = {null};
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("msg", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            captured[0] = ctx.getOptionValue("msg", String.class);
                        })
                )
                .build();

        String val = "hello\nworld\tand\r\0more";
        int exit = cli.execute("sub", "--msg", val);
        assertThat(exit).isEqualTo(0);
        assertThat(captured[0]).isEqualTo(val);
    }

    @Test
    void optionNameWithSpecialChars() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("my-opt!", opt -> opt
                                .hasArg(false))
                        .action(ctx -> System.out.println("executed"))
                )
                .build();

        int exit = cli.execute("sub", "--my-opt!");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("executed");
    }

    @Test
    void repeatedLongOption_lastValueWins() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("x", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String v = ctx.getOptionValue("x", String.class);
                            System.out.println(v);
                        })
                )
                .build();

        int exit = cli.execute("sub", "--x", "first", "--x", "second");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("second");
    }

    @Test
    void repeatedShortOption_lastValueWins() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("x", opt -> opt
                                .shortOpt("x")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String v = ctx.getOptionValue("x", String.class);
                            System.out.println(v);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-x", "first", "-x", "second");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("second");
    }

    @Test
    void booleanOptionRepeated_treatAsSet() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .hasArg(false)
                                .converter(Converters.BOOLEAN))
                        .action(ctx -> {
                            Boolean v = ctx.getOptionValue("verbose", Boolean.class);
                            System.out.println(v);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-v", "-v", "-v");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("true");
    }

    @Test
    void optionWithMultipleValues_onlyFirstConsumed() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("files", opt -> opt
                                .shortOpt("f")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .action(ctx -> {
                            String v = ctx.getOptionValue("files", String.class);
                            System.out.println(v);
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println("pos=" + pos);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-f", "a.txt", "b.txt", "c.txt");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim())
                .contains("a.txt")
                .contains("pos=[b.txt, c.txt]");
    }

    @Test
    void longAndShortOptionSameName_differentOptions() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .hasArg(false))
                        .option("version", opt -> opt
                                .shortOpt("V")
                                .hasArg(false))
                        .action(ctx -> {
                            boolean v = ctx.getOptionValue("verbose", Boolean.class);
                            boolean V = ctx.getOptionValue("version", Boolean.class);
                            System.out.println("v=" + v + ", V=" + V);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-v", "-V");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("v=true, V=true");
    }

    @Test
    void globalOptionOverridesCommandOption() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .option(Option.builder("verbose")
                        .shortOpt("v")
                        .hasArg(false)
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd
                        .option("verbose", opt -> opt
                                .shortOpt("v")
                                .hasArg(false)
                                .global(false))
                        .action(ctx -> {
                            boolean v = ctx.getOptionValue("verbose", Boolean.class);
                            System.out.println("cmd verb=" + v);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-v");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("cmd verb=true");
    }

    private void buildDepth(CommandBuilder parent, int depth, int maxDepth) {
        if (depth == maxDepth) {
            parent.action(ctx -> System.out.println("deep"));
            return;
        }
        String name = "l" + depth;
        parent.command(name, sub -> buildDepth(sub, depth + 1, maxDepth));
    }

    @Test
    void deeplyNestedCommands() {
        CLI.Builder builder = CLI.builder().logger(testLogger);
        builder.command("l0", cmd -> buildDepth(cmd, 1, 20));
        CLI cli = builder.build();

        String[] args = new String[20];
        for (int i = 0; i < 20; i++) {
            args[i] = "l" + i;
        }
        int exit = cli.execute(args);
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("deep");
    }

    @Test
    void positionalArgsWithLeadingDashAfterDoubleDash() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .action(ctx -> {
                            List<String> pos = ctx.getPositionalArgs();
                            System.out.println(String.join(",", pos));
                        })
                )
                .build();

        int exit = cli.execute("sub", "--", "-x", "--y", "-z");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("-x,--y,-z");
    }

    @Test
    void mixedOptionForms() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("a", opt -> opt
                                .shortOpt("a")
                                .hasArg(false))
                        .option("b", opt -> opt
                                .shortOpt("b")
                                .hasArg(true)
                                .converter(Converters.STRING))
                        .option("c", opt -> opt
                                .shortOpt("c")
                                .hasArg(false))
                        .action(ctx -> {
                            boolean a = ctx.getOptionValue("a", Boolean.class);
                            String b = ctx.getOptionValue("b", String.class);
                            boolean c = ctx.getOptionValue("c", Boolean.class);
                            System.out.printf("a=%b, b=%s, c=%b", a, b, c);
                        })
                )
                .build();

        int exit = cli.execute("sub", "-a", "-b", "hello", "-c");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("a=true, b=hello, c=true");
    }

    @Test
    void integerOptionOverflow_throwsError() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {})
                )
                .build();

        int exit = cli.execute("sub", "--num", "999999999999999999");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Invalid value '999999999999999999' for option --num"));
    }

    @Test
    void integerOptionMinValue() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {
                            int val = ctx.getOptionValue("num", Integer.class);
                            System.out.println(val);
                        })
                )
                .build();

        int exit = cli.execute("sub", "--num", "-2147483648");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("-2147483648");
    }

    @Test
    void commandNameWithDash() {
        CLI cli = CLI.builder()
                .command("my-command", cmd -> cmd
                        .action(ctx -> System.out.println("ok")))
                .build();

        int exit = cli.execute("my-command");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("ok");
    }

    @Test
    void commandNameWithUnderscore() {
        CLI cli = CLI.builder()
                .command("my_command", cmd -> cmd
                        .action(ctx -> System.out.println("ok")))
                .build();

        int exit = cli.execute("my_command");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("ok");
    }

    @Test
    void autoHelpDisabledDoesNotInterfereWithCustomHelp() {
        System.setProperty("kanketsu.autoHelp", "false");
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("help", opt -> opt
                                .shortOpt("h")
                                .hasArg(false))
                        .action(ctx -> {
                            if (ctx.getOptionValue("help", Boolean.class)) {
                                System.out.println("custom help");
                            } else {
                                System.out.println("exec");
                            }
                        })
                )
                .build();

        int exit = cli.execute("sub", "-h");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("custom help");
    }

    @Test
    void concurrentExecute_shouldNotThrow() throws InterruptedException {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt
                                .hasArg(true)
                                .converter(Converters.INT))
                        .action(ctx -> {
                            int val = ctx.getOptionValue("num", Integer.class);
                            System.out.println(val);
                        })
                )
                .build();

        int threads = 10;
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            ts[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    cli.execute("sub", "--num", String.valueOf(idx + j));
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        assertThat(true).isTrue();
    }

    @Test
    void optionValueMissing_throwsException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("opt", opt -> opt.hasArg(true))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "--opt");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("requires a value"));
    }

    @Test
    void unknownOption_throwsUnknownOptionException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd.action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "--unknown");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown option"));
    }

    @Test
    void unknownSubcommand_throwsUnknownSubcommandException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", cmd -> cmd
                        .command("commit", sub -> sub.action(ctx -> {}))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("git", "unknown");
        assertThat(exit).isEqualTo(1);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Unknown subcommand"));
    }

    @Test
    void missingRequiredOption_throwsMissingRequiredOptionException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("req", opt -> opt.required(true).hasArg(true))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Missing required option"));
    }

    @Test
    void equalsSyntaxWorks() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("msg", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("msg", String.class);
                            System.out.println(val);
                        }))
                .build();

        int exit = cli.execute("sub", "--msg=hello");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("hello");
    }

    @Test
    void shortOptionCombination() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("a", opt -> opt.shortOpt("a").hasArg(false))
                        .option("b", opt -> opt.shortOpt("b").hasArg(false))
                        .option("c", opt -> opt.shortOpt("c").hasArg(false))
                        .action(ctx -> {
                            boolean a = ctx.getOptionValue("a", Boolean.class);
                            boolean b = ctx.getOptionValue("b", Boolean.class);
                            boolean c = ctx.getOptionValue("c", Boolean.class);
                            System.out.printf("a=%b,b=%b,c=%b", a, b, c);
                        }))
                .build();

        int exit = cli.execute("sub", "-abc");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("a=true,b=true,c=true");
    }

    @Test
    void shortOptionCombinationWithValue() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("c", opt -> opt.shortOpt("c").hasArg(false))
                        .option("v", opt -> opt.shortOpt("v").hasArg(true))
                        .action(ctx -> {
                            boolean c = ctx.getOptionValue("c", Boolean.class);
                            String v = ctx.getOptionValue("v", String.class);
                            System.out.printf("c=%b,v=%s", c, v);
                        }))
                .build();

        int exit = cli.execute("sub", "-c", "-v", "file.txt");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("c=true,v=file.txt");
    }

    @Test
    void globalOptionAfterSubcommand() {
        CLI cli = CLI.builder()
                .option(Option.builder("global")
                        .shortOpt("g")
                        .hasArg(false)
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd.action(ctx -> {
                    boolean g = ctx.getOptionValue("global", Boolean.class);
                    System.out.println("global=" + g);
                }))
                .build();

        int exit = cli.execute("-g", "sub");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("global=true");
    }

    @Test
    void commandWithoutAction_autoHelp() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("opt", opt -> opt.hasArg(false)))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Usage:"));
    }

    @Test
    void customTypeConverter() {
        class Person {
            String name;
            Person(String name) { this.name = name; }
        }
        CLI cli = CLI.builder()
                .converter(new TypeConverter() {
                    @Override
                    public boolean supports(Class<?> targetType) {
                        return targetType == Person.class;
                    }

                    @Override
                    public Object convert(String source, CommandContext context) {
                        return new Person(source);
                    }
                })
                .command("sub", cmd -> cmd
                        .option("person", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            Person p = ctx.getOptionValueAs("person", Person.class);
                            System.out.println(p.name);
                        }))
                .build();

        int exit = cli.execute("sub", "--person", "Alice");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("Alice");
    }

    @Test
    void getOptionValueAs_withDefault() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt.hasArg(true).converter(Converters.INT))
                        .action(ctx -> {
                            int val = ctx.getOptionValueAs("num", Integer.class, 42);
                            System.out.println(val);
                        }))
                .build();

        int exit = cli.execute("sub");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("42");
    }

    @Test
    void repeatedOptionWithEquals_lastValueWins() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("x", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            String v = ctx.getOptionValue("x", String.class);
                            System.out.println(v);
                        }))
                .build();

        int exit = cli.execute("sub", "--x=first", "--x=second");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("second");
    }

    @Test
    void emptyValueAfterEquals_allowedForString() {
        CLI cli = CLI.builder()
                .command("sub", cmd -> cmd
                        .option("opt", opt -> opt.hasArg(true).converter(Converters.STRING))
                        .action(ctx -> {
                            String val = ctx.getOptionValue("opt", String.class);
                            System.out.println("val='" + val + "'");
                        }))
                .build();

        int exit = cli.execute("sub", "--opt=");
        assertThat(exit).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("val=''");
    }

    void onlyGlobalOptions_shouldShowGlobalHelp() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .option(Option.builder("verbose")
                        .shortOpt("v")
                        .hasArg(false)
                        .global(true)
                        .build())
                .command("sub", cmd -> cmd.action(ctx -> {}))
                .build();

        int exit = cli.execute("-v");
        assertThat(exit).isEqualTo(0);
        assertThat(testLogger.getInfoMessages())
                .anyMatch(msg -> msg.contains("Usage: <command>") && msg.contains("Available commands:"));
    }

    @Test
    void longOptionWithEqualsButNoArg_throwsUnknownOption() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("flag", opt -> opt.hasArg(false))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "--flag=value");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Option --flag does not take a value"));
    }

    @Test
    void shortOptionEqualsEmptyValue_throwsOptionValueMissing() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("opt", opt -> opt.shortOpt("o").hasArg(true))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "-o=");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Option -o requires a value"));
    }

    @Test
    void customConverterThrowsException_shouldWrapAndLog() {
        TypeConverter failing = new TypeConverter() {
            @Override public boolean supports(Class<?> targetType) {
                return targetType == Integer.class;
            }
            @Override public Object convert(String source, CommandContext context) {
                throw new RuntimeException("converter failure");
            }
        };
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(failing)
                .command("sub", cmd -> cmd
                        .option("val", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("val", Integer.class);
                        }))
                .build();

        int exit = cli.execute("sub", "--val", "123");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Conversion failed for option 'val' to java.lang.Integer"));
    }

    @Test
    void typeConverterReturnsWrongType_shouldThrow() {
        TypeConverter bad = new TypeConverter() {
            @Override public boolean supports(Class<?> targetType) { return targetType == Integer.class; }
            @Override public Object convert(String source, CommandContext context) {
                return "not an integer";
            }
        };
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(bad)
                .command("sub", cmd -> cmd
                        .option("num", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("num", Integer.class);
                        }))
                .build();

        int exit = cli.execute("sub", "--num", "123");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Converter for java.lang.Integer returned unexpected type"));
    }

    @Test
    void noTypeConverterForTarget_shouldThrow() {
        class Person {}
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("name", opt -> opt.hasArg(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("name", Person.class);
                        }))
                .build();

        int exit = cli.execute("sub", "--name", "Alice");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("No TypeConverter registered for target type: " + Person.class.getName()));
    }

    @Test
    void optionBuilderWithBlankLongOpt_throwsIllegalArgument() {
        assertThatThrownBy(() -> Option.builder("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longOpt cannot be null or empty");
        assertThatThrownBy(() -> Option.builder(" ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longOpt cannot be null or empty");
    }

    @Test
    void convertersOf_createsConverterCorrectly() {
        Converter conv = Converters.of(s -> Integer.parseInt(s) * 2);
        assertThat(conv.convert("10")).isEqualTo(20);
    }

    @Test
    void commandExceptionConstructors_shouldSetCodeAndMessage() {
        CommandException e1 = new CommandException(3, "msg");
        assertThat(e1.getCode()).isEqualTo(3);
        assertThat(e1.getMessage()).isEqualTo("msg");

        CommandException e2 = new CommandException("msg2", new RuntimeException("cause"));
        assertThat(e2.getCode()).isEqualTo(1);
        assertThat(e2.getMessage()).isEqualTo("msg2");

        CommandException e3 = new CommandException(4, "msg3", new RuntimeException("cause"));
        assertThat(e3.getCode()).isEqualTo(4);
        assertThat(e3.getMessage()).isEqualTo("msg3");

        CommandException e4 = new CommandException(5, "msg4", "desc");
        assertThat(e4.getCode()).isEqualTo(5);
        assertThat(e4.getMessage()).isEqualTo("msg4");
    }

    @Test
    void combinedShortOptionsWithValueOptionNotFirst_throws() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("sub", cmd -> cmd
                        .option("all", opt -> opt.shortOpt("a").hasArg(false))
                        .option("verbose", opt -> opt.shortOpt("v").hasArg(true))
                        .action(ctx -> {}))
                .build();

        int exit = cli.execute("sub", "-avfile");
        assertThat(exit).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Option -v requires a value, but is in a combined short option"));
    }
}