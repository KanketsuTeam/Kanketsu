# 性能

## 测试环境
- **CPU**: AMD Ryzen 7 2700 (8C/16T, 3.2GHz Base, Zen+ 架构)
- **操作系统**: Windows 11 + WSL 2 (Native Image 测试) / Windows Native (JMH 测试)
- **JDK**: GraalVM JDK 21.0.12 (Java HotSpot VM)
- **构建工具**: Maven + Native Image (GraalVM CE)

## GraalVM Native Image WSL 模拟Linux理论启动极限

### 实测结果
```bash
$ time ./KanketsuTest
HI

real    0m0.006s
user    0m0.000s
sys     0m0.006s
```

### 性能换算
| 指标 | 数值   | 解析     |
| :--- |:-----|:--|
| **real** | 6ms  | **物理世界总响应时间。** |
| **user** | 0ms  | **代码逻辑已达物理测量极限。** |
| **sys** | 6ms | **系统调用与虚拟化开销。** |
>因为**Linux**的**time**已经无法捕捉`user`的时间开销，四舍五入为0ms。（实际应为<**0.5ms**）



## JMH 稳态吞吐量压测 (JIT 峰值性能)

> 模拟生产环境长时间运行下的框架吞吐上限，命令组合完全随机。

### 实测结果
```bash
Benchmark                      Mode  Cnt     Score    Error   Units
KanketsuStressTest.testParse  thrpt   40  6464.102 ± 64.673  ops/ms
```

### 性能换算
| 指标 | 数值               | 说明 |
| :--- |:-----------------| :- |
| **16 线程并发吞吐量** | **646 万 ops/s**  | 基于 16 线程测试，反映整体处理能力，但不可直接换算延迟。|
| **单线程吞吐量** | **	285 万 ops/s** | 基于 1 线程测试。 |
| **单线程平均延迟** | **~351 ns/op**   | 基于 1 线程测试，代表单次解析的真实耗时。 |

> **解读**：  
> 在包含 `git remote add`、`git config set` 等深度嵌套子命令（最深 3 级）的场景下，单线程下每次解析耗时约 351 纳秒，16 线程并发总吞吐量可达 646 万次/秒，远超常规 CLI 解析器（通常需 1~5 微秒），足以支撑高频指令交互场景。
> 该性能已远超常规 CLI 解析器（通常需 1~5 微秒），足以支撑高频指令交互场景（如实时 REPL、自动化脚本引擎）。


## 测试代码

### GraalVM Native Image的极简代码
Simple:
```Java
import io.github.fascesaedi.kanketsu.core.CLI;

public class Simple {
    public static void main(String[] args) {
        CLI cli = CLIHolder.cli;
        cli.execute("hi");
    }
}

```
CLIHolder:
```Java
import io.github.fascesaedi.kanketsu.core.CLI;

public class CLIHolder {
    public static CLI cli = CLI
            .builder()
            .command("hi", hi -> hi
                    .action(cxt -> {
                        System.out.println("HI");
                    })).build();
}
```
>编译时将注册命令部分移至编译期

### JMH 测试代码
KanketsuStressTest:
```Java
package com.example;

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
@Fork(4)                           
@Threads(16)                        
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
                                            .option("name", "n", "Remote name", true)
                                            .option("url", "u", "Remote URL", true)
                                            .option("track", "t", "Track branch", false, "main")
                                            .option("mirror", "m", "Mirror")
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("remove", remove -> remove
                                            .option("name", "n", "Remote name", true)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("set-url", setUrl -> setUrl
                                            .option("name", "n", "Remote name", true)
                                            .option("url", "u", "New URL", true)
                                            .option("push", "p", "Set push URL", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("config", config -> config
                                    .command("get", get -> get
                                            .option("key", "k", "Config key", true)
                                            .option("scope", "s", "Scope", false, "local")
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("set", set -> set
                                            .option("key", "k", "Config key", true)
                                            .option("value", "v", "Config value", true)
                                            .option("scope", "s", "Scope", false, "local")
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("scope", "s", "Scope", false, "local")
                                            .option("global", "g", "Global only", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("log", log -> log
                                    .command("show", show -> show
                                            .option("commit", "c", "Commit hash", true)
                                            .option("patch", "p", "Show patch", false)
                                            .option("stat", "s", "Show statistics", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("diff", diff -> diff
                                            .option("from", "f", "From commit", true)
                                            .option("to", "t", "To commit", true)
                                            .option("ignore-space", "w", "Ignore whitespace", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("stash", stash -> stash
                                    .command("push", push -> push
                                            .option("message", "m", "Stash message", false)
                                            .option("include-untracked", "u", "Include untracked", false)
                                            .option("keep-index", "k", "Keep index", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("pop", pop -> pop
                                            .option("index", "i", "Stash index", false, "0")
                                            .option("force", "f", "Force pop", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("pretty", "p", "Pretty format", false, "oneline")
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("apply", apply -> apply
                                            .option("index", "i", "Stash index", false, "0")
                                            .action(KanketsuStressTest::noopAction)
                                    )
                            )
                            .command("tag", tag -> tag
                                    .command("create", create -> create
                                            .option("name", "n", "Tag name", true)
                                            .option("message", "m", "Tag message", false)
                                            .option("force", "f", "Force create", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("delete", delete -> delete
                                            .option("name", "n", "Tag name", true)
                                            .option("force", "f", "Force delete", false)
                                            .action(KanketsuStressTest::noopAction)
                                    )
                                    .command("list", list -> list
                                            .option("pattern", "p", "List tags matching pattern", false)
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
```
#### 完整测试结果：
16线程测试结果：
```bash
C:\graalvm\bin\java.exe "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\lib\idea_rt.jar=50416:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath D:\CLI\test\my-jmh-benchmark\target\classes;C:\Users\Administrator\.m2\repository\org\openjdk\jmh\jmh-core\1.37\jmh-core-1.37.jar;C:\Users\Administrator\.m2\repository\net\sf\jopt-simple\jopt-simple\5.0.4\jopt-simple-5.0.4.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-math3\3.6.1\commons-math3-3.6.1.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-core\1.0.0\kanketsu-core-1.0.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-repl\1.0.0\kanketsu-repl-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jline\jline\3.29.0\jline-3.29.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-logging\1.0.0\kanketsu-logging-1.0.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\imperium\1.0.0\imperium-1.0.0.jar;C:\Users\Administrator\.m2\repository\info\picocli\picocli\4.7.5\picocli-4.7.5.jar com.example.KanketsuStressTest
# JMH version: 1.37
# VM version: JDK 21.0.12, Java HotSpot(TM) 64-Bit Server VM, 21.0.12+7-LTS-jvmci-23.1-b96
# VM invoker: C:\graalvm\bin\java.exe
# VM options: -XX:ThreadPriorityPolicy=1 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCIProduct -XX:-UnlockExperimentalVMOptions -javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\lib\idea_rt.jar=50416:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\bin -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
# Blackhole mode: compiler (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 5 iterations, 2 s each
# Measurement: 10 iterations, 2 s each
# Timeout: 10 min per iteration
# Threads: 16 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.example.KanketsuStressTest.testParse

# Run progress: 0.00% complete, ETA 00:02:00
# Fork: 1 of 4
# Warmup Iteration   1: 5033.011 ops/ms
# Warmup Iteration   2: 5827.456 ops/ms
# Warmup Iteration   3: 6083.640 ops/ms
# Warmup Iteration   4: 6191.914 ops/ms
# Warmup Iteration   5: 6353.677 ops/ms
Iteration   1: 6114.853 ops/ms
Iteration   2: 6256.762 ops/ms
Iteration   3: 6449.659 ops/ms
Iteration   4: 6352.548 ops/ms
Iteration   5: 6352.122 ops/ms
Iteration   6: 6456.934 ops/ms
Iteration   7: 6423.180 ops/ms
Iteration   8: 6427.219 ops/ms
Iteration   9: 6523.733 ops/ms
Iteration  10: 6493.507 ops/ms

# Run progress: 25.00% complete, ETA 00:01:36
# Fork: 2 of 4
# Warmup Iteration   1: 5417.582 ops/ms
# Warmup Iteration   2: 5851.896 ops/ms
# Warmup Iteration   3: 6252.184 ops/ms
# Warmup Iteration   4: 6510.453 ops/ms
# Warmup Iteration   5: 6405.447 ops/ms
Iteration   1: 6366.597 ops/ms
Iteration   2: 6459.871 ops/ms
Iteration   3: 6328.496 ops/ms
Iteration   4: 6559.833 ops/ms
Iteration   5: 6579.245 ops/ms
Iteration   6: 6561.777 ops/ms
Iteration   7: 6525.748 ops/ms
Iteration   8: 6466.110 ops/ms
Iteration   9: 6512.779 ops/ms
Iteration  10: 6520.577 ops/ms

# Run progress: 50.00% complete, ETA 00:01:03
# Fork: 3 of 4
# Warmup Iteration   1: 5385.153 ops/ms
# Warmup Iteration   2: 5766.637 ops/ms
# Warmup Iteration   3: 6360.101 ops/ms
# Warmup Iteration   4: 6475.681 ops/ms
# Warmup Iteration   5: 6318.029 ops/ms
Iteration   1: 6365.117 ops/ms
Iteration   2: 6294.589 ops/ms
Iteration   3: 6567.981 ops/ms
Iteration   4: 6483.743 ops/ms
Iteration   5: 6524.421 ops/ms
Iteration   6: 6583.647 ops/ms
Iteration   7: 6610.338 ops/ms
Iteration   8: 6403.724 ops/ms
Iteration   9: 6535.679 ops/ms
Iteration  10: 6490.031 ops/ms

# Run progress: 75.00% complete, ETA 00:00:31
# Fork: 4 of 4
# Warmup Iteration   1: 5438.827 ops/ms
# Warmup Iteration   2: 5690.449 ops/ms
# Warmup Iteration   3: 6373.604 ops/ms
# Warmup Iteration   4: 6211.093 ops/ms
# Warmup Iteration   5: 6509.615 ops/ms
Iteration   1: 6210.404 ops/ms
Iteration   2: 6547.977 ops/ms
Iteration   3: 6600.587 ops/ms
Iteration   4: 6516.568 ops/ms
Iteration   5: 6557.814 ops/ms
Iteration   6: 6625.753 ops/ms
Iteration   7: 6437.334 ops/ms
Iteration   8: 6379.348 ops/ms
Iteration   9: 6555.771 ops/ms
Iteration  10: 6541.700 ops/ms


Result "com.example.KanketsuStressTest.testParse":
  6464.102 ±(99.9%) 64.673 ops/ms [Average]
  (min, avg, max) = (6114.853, 6464.102, 6625.753), stdev = 114.956
  CI (99.9%): [6399.429, 6528.775] (assumes normal distribution)


# Run complete. Total time: 00:02:06

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

NOTE: Current JVM experimentally supports Compiler Blackholes, and they are in use. Please exercise
extra caution when trusting the results, look into the generated code to check the benchmark still
works, and factor in a small probability of new VM bugs. Additionally, while comparisons between
different JVMs are already problematic, the performance difference caused by different Blackhole
modes can be very significant. Please make sure you use the consistent Blackhole mode for comparisons.

Benchmark                      Mode  Cnt     Score    Error   Units
KanketsuStressTest.testParse  thrpt   40  6464.102 ± 64.673  ops/ms

进程已结束，退出代码为 0
```
1线程测试结果：
```bash
C:\graalvm\bin\java.exe "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\lib\idea_rt.jar=59264:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath D:\CLI\test\my-jmh-benchmark\target\classes;C:\Users\Administrator\.m2\repository\org\openjdk\jmh\jmh-core\1.37\jmh-core-1.37.jar;C:\Users\Administrator\.m2\repository\net\sf\jopt-simple\jopt-simple\5.0.4\jopt-simple-5.0.4.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-math3\3.6.1\commons-math3-3.6.1.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-core\1.0.0\kanketsu-core-1.0.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-repl\1.0.0\kanketsu-repl-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jline\jline\3.29.0\jline-3.29.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\kanketsu-logging\1.0.0\kanketsu-logging-1.0.0.jar;C:\Users\Administrator\.m2\repository\io\github\fascesaedi\imperium\1.0.0\imperium-1.0.0.jar;C:\Users\Administrator\.m2\repository\info\picocli\picocli\4.7.5\picocli-4.7.5.jar com.example.KanketsuStressTest
# JMH version: 1.37
# VM version: JDK 21.0.12, Java HotSpot(TM) 64-Bit Server VM, 21.0.12+7-LTS-jvmci-23.1-b96
# VM invoker: C:\graalvm\bin\java.exe
# VM options: -XX:ThreadPriorityPolicy=1 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCIProduct -XX:-UnlockExperimentalVMOptions -javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\lib\idea_rt.jar=59264:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.7\bin -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
# Blackhole mode: compiler (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 5 iterations, 2 s each
# Measurement: 10 iterations, 2 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.example.KanketsuStressTest.testParse

# Run progress: 0.00% complete, ETA 00:02:00
# Fork: 1 of 4
# Warmup Iteration   1: 1931.670 ops/ms
# Warmup Iteration   2: 2705.616 ops/ms
# Warmup Iteration   3: 2877.081 ops/ms
# Warmup Iteration   4: 2885.745 ops/ms
# Warmup Iteration   5: 2870.852 ops/ms
Iteration   1: 2856.387 ops/ms
Iteration   2: 2895.880 ops/ms
Iteration   3: 2863.065 ops/ms
Iteration   4: 2891.185 ops/ms
Iteration   5: 2884.021 ops/ms
Iteration   6: 2865.467 ops/ms
Iteration   7: 2879.371 ops/ms
Iteration   8: 2866.698 ops/ms
Iteration   9: 2883.142 ops/ms
Iteration  10: 2885.956 ops/ms

# Run progress: 25.00% complete, ETA 00:01:34
# Fork: 2 of 4
# Warmup Iteration   1: 2500.969 ops/ms
# Warmup Iteration   2: 2724.244 ops/ms
# Warmup Iteration   3: 2859.247 ops/ms
# Warmup Iteration   4: 2854.119 ops/ms
# Warmup Iteration   5: 2806.032 ops/ms
Iteration   1: 2838.992 ops/ms
Iteration   2: 2846.001 ops/ms
Iteration   3: 2842.731 ops/ms
Iteration   4: 2830.404 ops/ms
Iteration   5: 2844.262 ops/ms
Iteration   6: 2868.946 ops/ms
Iteration   7: 2832.884 ops/ms
Iteration   8: 2836.457 ops/ms
Iteration   9: 2840.345 ops/ms
Iteration  10: 2858.965 ops/ms

# Run progress: 50.00% complete, ETA 00:01:02
# Fork: 3 of 4
# Warmup Iteration   1: 2497.686 ops/ms
# Warmup Iteration   2: 2733.041 ops/ms
# Warmup Iteration   3: 2831.670 ops/ms
# Warmup Iteration   4: 2810.990 ops/ms
# Warmup Iteration   5: 2803.088 ops/ms
Iteration   1: 2859.741 ops/ms
Iteration   2: 2804.522 ops/ms
Iteration   3: 2855.810 ops/ms
Iteration   4: 2845.805 ops/ms
Iteration   5: 2799.768 ops/ms
Iteration   6: 2830.225 ops/ms
Iteration   7: 2827.351 ops/ms
Iteration   8: 2841.788 ops/ms
Iteration   9: 2849.995 ops/ms
Iteration  10: 2855.121 ops/ms

# Run progress: 75.00% complete, ETA 00:00:31
# Fork: 4 of 4
# Warmup Iteration   1: 2471.090 ops/ms
# Warmup Iteration   2: 2711.747 ops/ms
# Warmup Iteration   3: 2848.670 ops/ms
# Warmup Iteration   4: 2812.350 ops/ms
# Warmup Iteration   5: 2853.811 ops/ms
Iteration   1: 2847.013 ops/ms
Iteration   2: 2843.104 ops/ms
Iteration   3: 2847.183 ops/ms
Iteration   4: 2837.951 ops/ms
Iteration   5: 2850.179 ops/ms
Iteration   6: 2845.685 ops/ms
Iteration   7: 2847.426 ops/ms
Iteration   8: 2848.002 ops/ms
Iteration   9: 2796.908 ops/ms
Iteration  10: 2855.283 ops/ms


Result "com.example.KanketsuStressTest.testParse":
  2850.000 ±(99.9%) 12.541 ops/ms [Average]
  (min, avg, max) = (2796.908, 2850.000, 2895.880), stdev = 22.291
  CI (99.9%): [2837.460, 2862.541] (assumes normal distribution)


# Run complete. Total time: 00:02:05

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

NOTE: Current JVM experimentally supports Compiler Blackholes, and they are in use. Please exercise
extra caution when trusting the results, look into the generated code to check the benchmark still
works, and factor in a small probability of new VM bugs. Additionally, while comparisons between
different JVMs are already problematic, the performance difference caused by different Blackhole
modes can be very significant. Please make sure you use the consistent Blackhole mode for comparisons.

Benchmark                      Mode  Cnt     Score    Error   Units
KanketsuStressTest.testParse  thrpt   40  2850.000 ± 12.541  ops/ms

进程已结束，退出代码为 0
```