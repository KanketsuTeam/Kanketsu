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
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.core.CLI;
import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.exception.CommandException;
import io.github.kanketsuteam.kanketsu.spi.Logger;

import java.util.*;

public class KanketsuFuzzTest {

    static class TestLogger implements Logger {
        private final List<String> unexpectedErrors = new ArrayList<>();
        private String[] currentArgs;

        @Override
        public void log(String message) { /* 静默 */ }

        @Override
        public void error(String message, Throwable t) {
            if (t instanceof CommandException) {
                return;
            }
            String combo = currentArgs != null ? String.join(" ", currentArgs) : "unknown";
            unexpectedErrors.add("💥 异常: " + t.getClass().getSimpleName()
                    + " | 消息: " + message
                    + " | 组合: " + combo);
        }

        @Override
        public void error(String message) { /* 忽略单参数调用 */ }

        @Override public void debug(String message) {}
        @Override public void info(String message) {}
        @Override public void warn(String message) {}
        @Override public void success(String message) {}
        @Override public boolean isDebugEnabled() { return false; }
    }

    private static long totalRuns = 0;
    private static final long PROGRESS_INTERVAL = 500_000; // 每50万轮打一次

    private static void permuteAndExecute(List<String> tokens, int start,
                                          CLI cli, TestLogger logger) {
        if (start == tokens.size() - 1) {
            String[] args = tokens.toArray(new String[0]);
            logger.currentArgs = args;
            try {
                cli.execute(args);
            } catch (Exception e) {
                logger.unexpectedErrors.add("🔥 未被 Logger 捕获: "
                        + e.getClass().getSimpleName()
                        + " | " + String.join(" ", args));
            }
            totalRuns++;
            if (totalRuns % PROGRESS_INTERVAL == 0) {
                System.out.println("✅ 已执行 " + totalRuns + " 轮 ...");
            }
            return;
        }

        for (int i = start; i < tokens.size(); i++) {
            Collections.swap(tokens, start, i);
            permuteAndExecute(tokens, start + 1, cli, logger);
            Collections.swap(tokens, start, i);
        }
    }

    public static void main(String[] args) {
        System.out.println("🚀 开始 Kanketsu 极限模糊测试 (11! = 39,916,800 种组合)");

        TestLogger testLogger = new TestLogger();
        CLI cli = CLI.builder()
                .logger(testLogger)
                .command("git", cmd -> cmd
                        .option(Option.builder("verbose")
                                .shortOpt("v")
                                .hasArg(false)
                                .global(true)
                                .build())
                        .command("commit", sub -> sub
                                .option(Option.builder("message")
                                        .shortOpt("m")
                                        .hasArg(true)
                                        .required(false)
                                        .build())
                                .option(Option.builder("amend")
                                        .hasArg(false)
                                        .build())
                                .action(ctx -> {}))
                        .command("push", sub -> sub
                                .option(Option.builder("force")
                                        .shortOpt("f")
                                        .hasArg(false)
                                        .build())
                                .action(ctx -> {})))
                .build();

        // 11 个不重复 token（新增 "-a"）
        List<String> tokens = Arrays.asList(
                "git", "commit", "--verbose", "-v",
                "--message", "-m", "--amend", "--force",
                "--", "123", "-a"   // 👈 新增短选项
        );

        long startTime = System.currentTimeMillis();
        permuteAndExecute(tokens, 0, cli, testLogger);
        long endTime = System.currentTimeMillis();

        System.out.println("\n========== 模糊测试报告 ==========");
        System.out.println("总执行轮次: " + totalRuns);
        System.out.println("耗时: " + (endTime - startTime) + " ms");
        System.out.println("意外异常数: " + testLogger.unexpectedErrors.size());

        if (testLogger.unexpectedErrors.isEmpty()) {
            System.out.println("✅ 结论: 所有异常均为 CommandException（预期内），框架解析器在 11 个 token 的极限组合下依然稳健！");
        } else {
            System.out.println("❌ 发现非预期异常（框架漏洞）：");
            for (String err : testLogger.unexpectedErrors) {
                System.out.println("  " + err);
            }
        }
    }
}