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
 * 真正的随机模糊测试器。
 * 生成随机参数组合，包括：
 * - 随机选取命令（git, commit, push）
 * - 随机加入已知选项（及其短/长形式），随机是否带参数
 * - 随机加入未知选项和随机参数值（含特殊字符、超长字符串等）
 * - 随机打乱顺序
 */
public class KanketsuRandomFuzzTest {

    static class FuzzLogger implements Logger {
        private final List<String> crashInputs = new ArrayList<>();
        private String[] currentArgs;

        @Override
        public void log(String message) { /* 静默 */ }

        @Override
        public void error(String message, Throwable t) {
            if (t instanceof CommandException) {
                return; // 预期异常，忽略
            }
            String combo = currentArgs != null ? String.join(" ", currentArgs) : "unknown";
            crashInputs.add("💥 异常: " + t.getClass().getSimpleName()
                    + " | 消息: " + message
                    + " | 输入: " + combo);
        }

        @Override
        public void error(String message) { /* 忽略单参数调用 */ }

        @Override public void debug(String message) {}
        @Override public void info(String message) {}
        @Override public void warn(String message) {}
        @Override public void success(String message) {}
        @Override public boolean isDebugEnabled() { return false; }
    }

    private static final Random RANDOM = new Random();
    private static final int MAX_ARGS = 15;          // 最大参数个数
    private static final int TOTAL_RUNS = 1_000_000; // 运行轮数
    private static final int PROGRESS_INTERVAL = 50_000;

    // 已知选项定义（用于生成有效输入）
    private static final List<OptionDef> KNOWN_OPTIONS = Arrays.asList(
            new OptionDef("--verbose", "-v", false),
            new OptionDef("--message", "-m", true),
            new OptionDef("--amend", null, false),
            new OptionDef("--force", "-f", false)
    );

    // 命令层次结构：根命令 git，子命令 commit/push
    private static final List<String> COMMANDS = Arrays.asList("git", "commit", "push");

    // 变异值池
    private static final List<String> MUTATION_VALUES = Arrays.asList(
            "",                     // 空字符串
            " ",                    // 空格
            "\t",                   // 制表符
            "\n",                   // 换行
            "你好",                 // Unicode
            "12345678901234567890", // 数字
            "--%!@#$%^&*()",        // 特殊符号
            "a".repeat(1000),       // 超长字符串
            "-a",                   // 短选项风格的值
            "--",                   // 单独的双横线
            "-",                    // 单独的单横线
            "null",                 // 字符串 null
            "true",                 // 布尔
            "1.2e-3"                // 科学计数法
    );

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
        System.out.println("🎲 启动 Kanketsu 随机模糊测试（" + TOTAL_RUNS + " 轮）");

        FuzzLogger fuzzLogger = new FuzzLogger();
        CLI cli = buildCLI(fuzzLogger);

        AtomicLong totalRuns = new AtomicLong(0);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_RUNS; i++) {
            // 生成随机参数
            String[] generatedArgs = generateRandomArgs();
            fuzzLogger.currentArgs = generatedArgs;

            try {
                cli.execute(generatedArgs);
            } catch (Exception e) {
                // 如果异常不是 CommandException，且未被 Logger 捕获（比如同步抛出），这里也记录
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

        // 输出报告
        System.out.println("\n========== 随机模糊测试报告 ==========");
        System.out.println("总执行轮次: " + totalRuns.get());
        System.out.println("耗时: " + (endTime - startTime) + " ms");
        System.out.println("意外异常数: " + fuzzLogger.crashInputs.size());

        if (fuzzLogger.crashInputs.isEmpty()) {
            System.out.println("✅ 结论: 所有异常均为 CommandException（预期内），框架在随机变异输入下依然稳健！");
        } else {
            System.out.println("❌ 发现非预期异常（框架漏洞），共 " + fuzzLogger.crashInputs.size() + " 个：");
            // 只打印前10个，避免刷屏
            int limit = Math.min(10, fuzzLogger.crashInputs.size());
            for (int i = 0; i < limit; i++) {
                System.out.println("  " + fuzzLogger.crashInputs.get(i));
            }
            if (fuzzLogger.crashInputs.size() > 10) {
                System.out.println("  ... 还有 " + (fuzzLogger.crashInputs.size() - 10) + " 个异常输入未显示");
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

    /**
     * 生成随机参数数组，策略：
     * 1. 随机决定是否包含命令（至少包含 git，也可能包含子命令）
     * 2. 随机添加选项（已知/未知，带/不带参数）
     * 3. 随机添加随机值（来自变异池）
     * 4. 打乱顺序，增加不确定性
     */
    private static String[] generateRandomArgs() {
        List<String> tokens = new ArrayList<>();

        // 步骤1：添加命令（至少 git，有时带上子命令）
        tokens.add("git");
        if (RANDOM.nextBoolean()) {
            // 随机选择子命令 commit 或 push
            String subCmd = RANDOM.nextBoolean() ? "commit" : "push";
            tokens.add(subCmd);
        }

        // 步骤2：随机添加选项（0~5个）
        int optionCount = RANDOM.nextInt(6); // 0~5
        for (int i = 0; i < optionCount; i++) {
            // 从已知选项中随机选一个，或者生成未知选项
            if (RANDOM.nextBoolean() && !KNOWN_OPTIONS.isEmpty()) {
                // 使用已知选项
                OptionDef def = KNOWN_OPTIONS.get(RANDOM.nextInt(KNOWN_OPTIONS.size()));
                // 随机选择长或短形式
                boolean useLong = RANDOM.nextBoolean();
                String opt;
                if (useLong && def.longOpt != null) {
                    opt = def.longOpt;
                } else if (!useLong && def.shortOpt != null) {
                    opt = def.shortOpt;
                } else {
                    // 后备
                    opt = def.longOpt != null ? def.longOpt : def.shortOpt;
                }
                tokens.add(opt);
                // 如果该选项需要参数，随机决定是否提供（有时故意缺失）
                if (def.hasArg && RANDOM.nextBoolean()) {
                    tokens.add(randomValue());
                }
            } else {
                // 生成未知选项（以 -- 或 - 开头，后跟随机字母数字）
                String unknownOpt;
                if (RANDOM.nextBoolean()) {
                    unknownOpt = "--" + randomString(3, 8);
                } else {
                    unknownOpt = "-" + randomString(1, 3);
                }
                tokens.add(unknownOpt);
                // 随机为未知选项添加参数（即使它不该有，测试健壮性）
                if (RANDOM.nextBoolean()) {
                    tokens.add(randomValue());
                }
            }
        }

        // 步骤3：随机添加一些裸参数（类似位置参数）
        int bareArgCount = RANDOM.nextInt(4); // 0~3
        for (int i = 0; i < bareArgCount; i++) {
            tokens.add(randomValue());
        }

        // 步骤4：偶尔加入 "--" 分隔符
        if (RANDOM.nextBoolean()) {
            tokens.add("--");
            // 后面再加几个随机参数
            int extra = RANDOM.nextInt(3);
            for (int i = 0; i < extra; i++) {
                tokens.add(randomValue());
            }
        }

        // 步骤5：打乱顺序（但确保 git 在第一个？打乱可能会把 git 移到后面，这正好测试解析器对顺序的鲁棒性）
        // 但 CLI 通常期望 git 是第一个命令，如果打乱可能导致 "命令未找到"，但也是有效测试。
        // 我们可以不打乱，或者对前几个元素保留（保留 git 位置）。为了更彻底，我们打乱全部。
        Collections.shuffle(tokens, RANDOM);

        // 但为了不总是产生完全无效的输入（比如 git 不在第一个），我们可以保证至少 git 出现在某个位置，
        // 不过 CLI 解析器遇到无法识别的命令会抛出 CommandException，这算预期异常，所以无所谓。
        // 我们保留打乱以增加变异。

        // 限制长度
        if (tokens.size() > MAX_ARGS) {
            tokens = tokens.subList(0, MAX_ARGS);
        }

        return tokens.toArray(new String[0]);
    }

    private static String randomValue() {
        // 从变异池随机选取，或者生成随机字符串/数字
        int choice = RANDOM.nextInt(4);
        switch (choice) {
            case 0: return MUTATION_VALUES.get(RANDOM.nextInt(MUTATION_VALUES.size()));
            case 1: return String.valueOf(RANDOM.nextInt(10000));
            case 2: return randomString(1, 15);
            case 3: return "'" + randomString(2, 8) + "'"; // 带引号
            default: return "";
        }
    }

    private static String randomString(int minLen, int maxLen) {
        int len = minLen + RANDOM.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = (char) ('a' + RANDOM.nextInt(26));
            if (RANDOM.nextBoolean()) c = Character.toUpperCase(c);
            if (RANDOM.nextBoolean()) c = (char) ('0' + RANDOM.nextInt(10));
            sb.append(c);
        }
        return sb.toString();
    }
}