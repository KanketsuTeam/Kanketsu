package io.github.fascesaedi.kanketsu;

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.core.CommandContext;
import io.github.fascesaedi.kanketsu.spi.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(4)                              // 2个独立JVM进程，提高结果可信度
@Threads(16)                           // 并发线程数
public class KanketsuStressTest {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        CLI cli;
        String[][] allCommands;
        int totalCommands;

        @Setup(Level.Trial)
        public void setup() {
            cli = buildCli();
            int totalCases = 50_000;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            allCommands = new String[totalCases][];
            for (int i = 0; i < totalCases; i++) {
                allCommands[i] = generateRandomArgs(rnd);
            }
            totalCommands = allCommands.length;
        }

        private CLI buildCli() {
            Logger logger = Logger.noop();
            return CLI.builder()
                    .logger(logger)
                    .command("git", git -> git
                            .command("remote", remote -> remote
                                    .command("add", add -> add
                                            .option("name", opt -> opt
                                                    .shortOpt("n")
                                                    .description("Remote name")
                                                    .hasArg(true))
                                            .option("url", opt -> opt
                                                    .shortOpt("u")
                                                    .description("Remote URL")
                                                    .hasArg(true))
                                            .option("track", opt -> opt
                                                    .shortOpt("t")
                                                    .description("Track branch")
                                                    .hasArg(false)
                                                    .defaultValue("main"))
                                            .option("mirror", opt -> opt
                                                    .shortOpt("m")
                                                    .description("Mirror")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("remove", remove -> remove
                                            .option("name", opt -> opt
                                                    .shortOpt("n")
                                                    .description("Remote name")
                                                    .hasArg(true))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("set-url", setUrl -> setUrl
                                            .option("name", opt -> opt
                                                    .shortOpt("n")
                                                    .description("Remote name")
                                                    .hasArg(true))
                                            .option("url", opt -> opt
                                                    .shortOpt("u")
                                                    .description("New URL")
                                                    .hasArg(true))
                                            .option("push", opt -> opt
                                                    .shortOpt("p")
                                                    .description("Set push URL")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("config", config -> config
                                    .command("get", get -> get
                                            .option("key", opt -> opt
                                                    .shortOpt("k")
                                                    .description("Config key")
                                                    .hasArg(true))
                                            .option("scope", opt -> opt
                                                    .shortOpt("s")
                                                    .description("Scope")
                                                    .hasArg(false)
                                                    .defaultValue("local"))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("set", set -> set
                                            .option("key", opt -> opt
                                                    .shortOpt("k")
                                                    .description("Config key")
                                                    .hasArg(true))
                                            .option("value", opt -> opt
                                                    .shortOpt("v")
                                                    .description("Config value")
                                                    .hasArg(true))
                                            .option("scope", opt -> opt
                                                    .shortOpt("s")
                                                    .description("Scope")
                                                    .hasArg(false)
                                                    .defaultValue("local"))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("scope", opt -> opt
                                                    .shortOpt("s")
                                                    .description("Scope")
                                                    .hasArg(false)
                                                    .defaultValue("local"))
                                            .option("global", opt -> opt
                                                    .shortOpt("g")
                                                    .description("Global only")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("log", log -> log
                                    .command("show", show -> show
                                            .option("commit", opt -> opt
                                                    .shortOpt("c")
                                                    .description("Commit hash")
                                                    .hasArg(true))
                                            .option("patch", opt -> opt
                                                    .shortOpt("p")
                                                    .description("Show patch")
                                                    .hasArg(false))
                                            .option("stat", opt -> opt
                                                    .shortOpt("s")
                                                    .description("Show statistics")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("diff", diff -> diff
                                            .option("from", opt -> opt
                                                    .shortOpt("f")
                                                    .description("From commit")
                                                    .hasArg(true))
                                            .option("to", opt -> opt
                                                    .shortOpt("t")
                                                    .description("To commit")
                                                    .hasArg(true))
                                            .option("ignore-space", opt -> opt
                                                    .shortOpt("w")
                                                    .description("Ignore whitespace")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("stash", stash -> stash
                                    .command("push", push -> push
                                            .option("message", opt -> opt
                                                    .shortOpt("m")
                                                    .description("Stash message")
                                                    .hasArg(false))
                                            .option("include-untracked", opt -> opt
                                                    .shortOpt("u")
                                                    .description("Include untracked")
                                                    .hasArg(false))
                                            .option("keep-index", opt -> opt
                                                    .shortOpt("k")
                                                    .description("Keep index")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("pop", pop -> pop
                                            .option("index", opt -> opt
                                                    .shortOpt("i")
                                                    .description("Stash index")
                                                    .hasArg(false)
                                                    .defaultValue("0"))
                                            .option("force", opt -> opt
                                                    .shortOpt("f")
                                                    .description("Force pop")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("pretty", opt -> opt
                                                    .shortOpt("p")
                                                    .description("Pretty format")
                                                    .hasArg(false)
                                                    .defaultValue("oneline"))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("apply", apply -> apply
                                            .option("index", opt -> opt
                                                    .shortOpt("i")
                                                    .description("Stash index")
                                                    .hasArg(false)
                                                    .defaultValue("0"))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("tag", tag -> tag
                                    .command("create", create -> create
                                            .option("name", opt -> opt
                                                    .shortOpt("n")
                                                    .description("Tag name")
                                                    .hasArg(true))
                                            .option("message", opt -> opt
                                                    .shortOpt("m")
                                                    .description("Tag message")
                                                    .hasArg(false))
                                            .option("force", opt -> opt
                                                    .shortOpt("f")
                                                    .description("Force create")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("delete", delete -> delete
                                            .option("name", opt -> opt
                                                    .shortOpt("n")
                                                    .description("Tag name")
                                                    .hasArg(true))
                                            .option("force", opt -> opt
                                                    .shortOpt("f")
                                                    .description("Force delete")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("pattern", opt -> opt
                                                    .shortOpt("p")
                                                    .description("List tags matching pattern")
                                                    .hasArg(false))
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                    )
                    .build();
        }

        private String[] generateRandomArgs(ThreadLocalRandom rnd) {
            int cmdIdx = rnd.nextInt(15);
            List<String> tokens = new ArrayList<>();
            tokens.add("git");

            switch (cmdIdx) {
                case 0 -> {
                    tokens.addAll(List.of("remote", "add"));
                    tokens.add("-n");
                    tokens.add(randomString(rnd, 5, 15));
                    tokens.add("-u");
                    tokens.add("git@github.com:" + randomString(rnd, 5, 10) + "/repo.git");
                    if (rnd.nextBoolean()) {
                        tokens.add("-t");
                        tokens.add(randomString(rnd, 3, 10));
                    }
                    if (rnd.nextDouble() < 0.3) tokens.add("-m");
                }
                case 1 -> {
                    tokens.addAll(List.of("remote", "remove"));
                    tokens.add("-n");
                    tokens.add(randomString(rnd, 5, 15));
                }
                case 2 -> {
                    tokens.addAll(List.of("remote", "set-url"));
                    tokens.add("-n");
                    tokens.add(randomString(rnd, 5, 15));
                    tokens.add("-u");
                    tokens.add("git@github.com:" + randomString(rnd, 5, 10) + "/new-repo.git");
                    if (rnd.nextBoolean()) tokens.add("-p");
                }
                case 3 -> {
                    tokens.addAll(List.of("config", "get"));
                    tokens.add("-k");
                    tokens.add(randomString(rnd, 3, 10));
                    if (rnd.nextBoolean()) {
                        tokens.add("-s");
                        tokens.add(rnd.nextBoolean() ? "local" : "global");
                    }
                }
                case 4 -> {
                    tokens.addAll(List.of("config", "set"));
                    tokens.add("-k");
                    tokens.add(randomString(rnd, 3, 10));
                    tokens.add("-v");
                    tokens.add(randomString(rnd, 3, 20));
                    if (rnd.nextBoolean()) {
                        tokens.add("-s");
                        tokens.add(rnd.nextBoolean() ? "local" : "global");
                    }
                }
                case 5 -> {
                    tokens.addAll(List.of("config", "list"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-s");
                        tokens.add(rnd.nextBoolean() ? "local" : "global");
                    }
                    if (rnd.nextBoolean()) tokens.add("-g");
                }
                case 6 -> {
                    tokens.addAll(List.of("log", "show"));
                    tokens.add("-c");
                    tokens.add(randomString(rnd, 7, 10));
                    if (rnd.nextBoolean()) tokens.add("-p");
                    if (rnd.nextBoolean()) tokens.add("-s");
                }
                case 7 -> {
                    tokens.addAll(List.of("log", "diff"));
                    tokens.add("-f");
                    tokens.add(randomString(rnd, 7, 10));
                    tokens.add("-t");
                    tokens.add(randomString(rnd, 7, 10));
                    if (rnd.nextBoolean()) tokens.add("-w");
                }
                case 8 -> {
                    tokens.addAll(List.of("stash", "push"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-m");
                        tokens.add(randomString(rnd, 5, 20));
                    }
                    if (rnd.nextBoolean()) tokens.add("-u");
                    if (rnd.nextBoolean()) tokens.add("-k");
                }
                case 9 -> {
                    tokens.addAll(List.of("stash", "pop"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-i");
                        tokens.add(String.valueOf(rnd.nextInt(0, 5)));
                    }
                    if (rnd.nextBoolean()) tokens.add("-f");
                }
                case 10 -> {
                    tokens.addAll(List.of("stash", "list"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-p");
                        tokens.add(rnd.nextBoolean() ? "oneline" : "full");
                    }
                }
                case 11 -> {
                    tokens.addAll(List.of("stash", "apply"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-i");
                        tokens.add(String.valueOf(rnd.nextInt(0, 5)));
                    }
                }
                case 12 -> {
                    tokens.addAll(List.of("tag", "create"));
                    tokens.add("-n");
                    tokens.add(randomString(rnd, 3, 10));
                    if (rnd.nextBoolean()) {
                        tokens.add("-m");
                        tokens.add(randomString(rnd, 5, 15));
                    }
                    if (rnd.nextBoolean()) tokens.add("-f");
                }
                case 13 -> {
                    tokens.addAll(List.of("tag", "delete"));
                    tokens.add("-n");
                    tokens.add(randomString(rnd, 3, 10));
                    if (rnd.nextBoolean()) tokens.add("-f");
                }
                case 14 -> {
                    tokens.addAll(List.of("tag", "list"));
                    if (rnd.nextBoolean()) {
                        tokens.add("-p");
                        tokens.add(randomString(rnd, 3, 8));
                    }
                }
            }
            return tokens.toArray(new String[0]);
        }

        private static String randomString(ThreadLocalRandom rnd, int minLen, int maxLen) {
            int len = rnd.nextInt(minLen, maxLen + 1);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                int type = rnd.nextInt(3);
                char c = switch (type) {
                    case 0 -> (char) ('a' + rnd.nextInt(26));
                    case 1 -> (char) ('A' + rnd.nextInt(26));
                    default -> (char) ('0' + rnd.nextInt(10));
                };
                sb.append(c);
            }
            return sb.toString();
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        int index = 0;
    }

    private static void noopAction(CommandContext ctx) {
        blackHole(ctx.getPositionalArgs().size() + ctx.getOptions().size());
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static void blackHole(int x) {
        if (x == Integer.MIN_VALUE) {
            System.out.println("unreachable");
        }
    }

    @Benchmark
    public void testParse(BenchmarkState state, ThreadState threadState, Blackhole bh) {
        int idx = threadState.index++ % state.totalCommands;
        String[] cmd = state.allCommands[idx];
        state.cli.execute(cmd);
        bh.consume(cmd);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(KanketsuStressTest.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}