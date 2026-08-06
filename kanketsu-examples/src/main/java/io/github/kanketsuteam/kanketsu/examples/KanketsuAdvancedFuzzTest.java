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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高级模糊测试 – 更脏更健壮（已修复边界异常）
 */
public class KanketsuAdvancedFuzzTest {

    static class FuzzLogger implements Logger {
        private final List<String> crashInputs = new ArrayList<>();
        private String[] currentArgs;

        @Override
        public void log(String message) { /* 静默 */ }

        @Override
        public void error(String message, Throwable t) {
            if (t instanceof CommandException) {
                return;
            }
            String combo = currentArgs != null ? String.join(" ", currentArgs) : "unknown";
            crashInputs.add("💥 异常: " + t.getClass().getSimpleName()
                    + " | 消息: " + message
                    + " | 输入: " + combo);
        }

        @Override
        public void error(String message) { /* 忽略 */ }
        @Override public void debug(String message) {}
        @Override public void info(String message) {}
        @Override public void warn(String message) {}
        @Override public void success(String message) {}
        @Override public boolean isDebugEnabled() { return false; }
    }

    private static final Random RANDOM = new Random();
    private static final int MAX_ARGS = 20;
    private static final int TOTAL_RUNS = 1_000_000;
    private static final int PROGRESS_INTERVAL = 50_000;

    private static final List<OptionDef> KNOWN_OPTIONS = Arrays.asList(
            new OptionDef("--verbose", "-v", false),
            new OptionDef("--message", "-m", true),
            new OptionDef("--amend", null, false),
            new OptionDef("--force", "-f", false)
    );

    // 脏数据池（不含空字符串，但空字符串单独保留在列表中供直接使用）
    private static final List<String> DIRTY_VALUES = new ArrayList<>(Arrays.asList(
            "",                     // 空字符串
            " ", "\t", "\n", "\r", "\b",
            "\0", "\u0000", "\uffff",
            "你好", "😊",
            "../../../etc/passwd",
            "|", ";", "&", "$()", "`",
            "a".repeat(1000),
            "a".repeat(10240),
            "-2147483648", "2147483647",
            "NaN", "Infinity",
            "-", "--", "---",
            "''", "\"\"",
            "'\"'", "\"'\"",
            "\\'", "\\\"",
            "*-*", "?", "[", "]",
            "=", "+", "!", "@"
    ));

    // 单独的特殊字符池（用于插入随机字符串中，保证非空）
    private static final char[] SPECIAL_CHARS = {
            '\0', '\n', '\r', '\t', '\b', '|', ';', '&', '$', '`', '\\', '\'', '"', '?', '*', '[', ']', '!', '@', '#', '%', '^'
    };

    private static final List<String> COMMANDS = Arrays.asList("git", "commit", "push");

    static class OptionDef {
        String longOpt;
        String shortOpt;
        boolean hasArg;
        OptionDef(String longOpt, String shortOpt, boolean hasArg) {
            this.longOpt = longOpt;
            this.shortOpt = shortOpt;
            this.hasArg = hasArg;
        }
    }

    public static void main(String[] args) {
        System.out.println("💀 启动高级脏模糊测试（" + TOTAL_RUNS + " 轮）");
        FuzzLogger fuzzLogger = new FuzzLogger();
        CLI cli = buildCLI(fuzzLogger);

        AtomicLong totalRuns = new AtomicLong(0);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_RUNS; i++) {
            // 每10万轮测试空数组
            if (i % 100000 == 0) {
                try {
                    cli.execute(new String[0]);
                } catch (Exception e) {
                    if (!(e instanceof CommandException)) {
                        fuzzLogger.crashInputs.add("🔥 空数组输入异常: " + e);
                    }
                }
                if (i == 0) {
                    try {
                        cli.execute(null);
                    } catch (Exception e) {
                        if (!(e instanceof CommandException)) {
                            fuzzLogger.crashInputs.add("🔥 null 输入异常: " + e);
                        }
                    }
                }
                continue;
            }

            String[] generatedArgs = generateDirtyArgs();
            fuzzLogger.currentArgs = generatedArgs;
            try {
                cli.execute(generatedArgs);
            } catch (Exception e) {
                if (!(e instanceof CommandException)) {
                    fuzzLogger.crashInputs.add("🔥 未被 Logger 捕获: "
                            + e.getClass().getSimpleName()
                            + " | 输入: " + String.join(" ", generatedArgs));
                }
            }

            long runs = totalRuns.incrementAndGet();
            if (runs % PROGRESS_INTERVAL == 0) {
                System.out.printf("⏳ 已完成 %d / %d 轮...\n", runs, TOTAL_RUNS);
            }
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\n========== 高级模糊测试报告 ==========");
        System.out.println("总执行轮次: " + totalRuns.get());
        System.out.println("耗时: " + (endTime - startTime) + " ms");
        System.out.println("意外异常数: " + fuzzLogger.crashInputs.size());

        if (fuzzLogger.crashInputs.isEmpty()) {
            System.out.println("✅ 结论: 所有异常均为 CommandException（预期内），框架在极端脏输入下依然稳健！");
        } else {
            System.out.println("❌ 发现非预期异常，共 " + fuzzLogger.crashInputs.size() + " 个：");
            int limit = Math.min(20, fuzzLogger.crashInputs.size());
            for (int i = 0; i < limit; i++) {
                System.out.println("  " + fuzzLogger.crashInputs.get(i));
            }
            if (fuzzLogger.crashInputs.size() > 20) {
                System.out.println("  ... 还有 " + (fuzzLogger.crashInputs.size() - 20) + " 个未显示");
            }
        }
    }

    private static CLI buildCLI(FuzzLogger logger) {
        return CLI.builder()
                .logger(logger)
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
    }

    private static String[] generateDirtyArgs() {
        List<String> tokens = new ArrayList<>();

        // 命令组合
        if (RANDOM.nextBoolean()) {
            tokens.add("git");
            int subCount = RANDOM.nextInt(3);
            for (int i = 0; i < subCount; i++) {
                String sub = COMMANDS.get(1 + RANDOM.nextInt(COMMANDS.size() - 1));
                tokens.add(sub);
                if (RANDOM.nextBoolean()) tokens.add(sub);
            }
        } else {
            if (RANDOM.nextBoolean()) {
                tokens.add(randomString(2, 8));
            }
        }

        // 选项
        int optionCount = RANDOM.nextInt(8);
        for (int i = 0; i < optionCount; i++) {
            int type = RANDOM.nextInt(3);
            if (type == 0 && !KNOWN_OPTIONS.isEmpty()) {
                OptionDef def = KNOWN_OPTIONS.get(RANDOM.nextInt(KNOWN_OPTIONS.size()));
                tokens.add(chooseOptForm(def));
                handleOptionArg(def, tokens);
            } else if (type == 1) {
                tokens.add((RANDOM.nextBoolean() ? "--" : "-") + randomString(1, 10));
                if (RANDOM.nextBoolean()) tokens.add(randomDirtyValue());
            } else {
                tokens.add("-" + randomString(2, 5));
                if (RANDOM.nextBoolean()) tokens.add(randomDirtyValue());
            }
        }

        // 裸参数
        int bareCount = RANDOM.nextInt(5);
        for (int i = 0; i < bareCount; i++) {
            tokens.add(randomDirtyValue());
        }

        // 多个 --
        int dashDashCount = RANDOM.nextInt(3);
        for (int i = 0; i < dashDashCount; i++) {
            tokens.add("--");
            if (RANDOM.nextBoolean()) tokens.add(randomDirtyValue());
        }

        // 随机破坏选项格式
        if (RANDOM.nextBoolean() && !tokens.isEmpty()) {
            int idx = RANDOM.nextInt(tokens.size());
            String token = tokens.get(idx);
            if (token.startsWith("-") && !token.equals("--")) {
                tokens.set(idx, "-" + token);
            }
        }

        if (RANDOM.nextBoolean()) Collections.shuffle(tokens, RANDOM);
        if (tokens.size() > MAX_ARGS) tokens = tokens.subList(0, MAX_ARGS);
        return tokens.toArray(new String[0]);
    }

    private static String chooseOptForm(OptionDef def) {
        boolean useLong = RANDOM.nextBoolean();
        if (useLong && def.longOpt != null) return def.longOpt;
        if (!useLong && def.shortOpt != null) return def.shortOpt;
        return def.longOpt != null ? def.longOpt : def.shortOpt;
    }

    private static void handleOptionArg(OptionDef def, List<String> tokens) {
        if (def.hasArg) {
            if (RANDOM.nextDouble() < 0.7) {
                if (RANDOM.nextBoolean()) {
                    tokens.add(randomDirtyValue());
                } else {
                    OptionDef other = KNOWN_OPTIONS.get(RANDOM.nextInt(KNOWN_OPTIONS.size()));
                    tokens.add(chooseOptForm(other));
                }
            }
        } else {
            if (RANDOM.nextDouble() < 0.3) {
                tokens.add(randomDirtyValue());
            }
        }
    }

    private static String randomDirtyValue() {
        int choice = RANDOM.nextInt(5);
        switch (choice) {
            case 0: return DIRTY_VALUES.get(RANDOM.nextInt(DIRTY_VALUES.size()));
            case 1: return randomString(1, 20);
            case 2: return String.valueOf(RANDOM.nextLong());
            case 3: return "'" + randomString(1, 10) + "'";
            case 4: return "\"" + randomString(1, 10) + "\"";
            default: return "";
        }
    }

    private static String randomString(int minLen, int maxLen) {
        int len = minLen + RANDOM.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c;
            if (RANDOM.nextBoolean()) {
                c = (char) ('a' + RANDOM.nextInt(26));
                if (RANDOM.nextBoolean()) c = Character.toUpperCase(c);
            } else {
                c = (char) ('0' + RANDOM.nextInt(10));
            }
            // 插入特殊字符（安全池）
            if (RANDOM.nextDouble() < 0.1) {
                c = SPECIAL_CHARS[RANDOM.nextInt(SPECIAL_CHARS.length)];
            }
            sb.append(c);
        }
        return sb.toString();
    }
}