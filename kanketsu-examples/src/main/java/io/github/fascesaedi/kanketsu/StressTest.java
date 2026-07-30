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
package io.github.fascesaedi.kanketsu;

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.core.CommandContext;
import io.github.fascesaedi.kanketsu.spi.Logger;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class StressTest {
    private static CLI buildCli() {
        return buildCli.buildCLI();
    }

    private static String[] generateRandomArgs(ThreadLocalRandom rnd) {
        int cmdIdx = rnd.nextInt(15);
        List<String> tokens = new ArrayList<>();
        tokens.add("git");

        switch (cmdIdx) {
            // 0: remote add
            case 0 -> {
                tokens.addAll(List.of("remote", "add"));
                tokens.add("-n");
                tokens.add(randomString(rnd, 5, 15));
                tokens.add("-u");
                tokens.add("git@github.com:" + randomString(rnd, 5, 10) + "/repo.git");
                if (rnd.nextBoolean()) { tokens.add("-t"); tokens.add(randomString(rnd, 3, 10)); }
                if (rnd.nextDouble() < 0.3) tokens.add("-m");
            }
            // 1: remote remove
            case 1 -> {
                tokens.addAll(List.of("remote", "remove"));
                tokens.add("-n");
                tokens.add(randomString(rnd, 5, 15));
            }
            // 2: remote set-url
            case 2 -> {
                tokens.addAll(List.of("remote", "set-url"));
                tokens.add("-n");
                tokens.add(randomString(rnd, 5, 15));
                tokens.add("-u");
                tokens.add("git@github.com:" + randomString(rnd, 5, 10) + "/new-repo.git");
                if (rnd.nextBoolean()) tokens.add("-p");
            }
            // 3: config get
            case 3 -> {
                tokens.addAll(List.of("config", "get"));
                tokens.add("-k");
                tokens.add(randomString(rnd, 3, 10));
                if (rnd.nextBoolean()) { tokens.add("-s"); tokens.add(rnd.nextBoolean() ? "local" : "global"); }
            }
            // 4: config set
            case 4 -> {
                tokens.addAll(List.of("config", "set"));
                tokens.add("-k");
                tokens.add(randomString(rnd, 3, 10));
                tokens.add("-v");
                tokens.add(randomString(rnd, 3, 20));
                if (rnd.nextBoolean()) { tokens.add("-s"); tokens.add(rnd.nextBoolean() ? "local" : "global"); }
            }
            // 5: config list
            case 5 -> {
                tokens.addAll(List.of("config", "list"));
                if (rnd.nextBoolean()) { tokens.add("-s"); tokens.add(rnd.nextBoolean() ? "local" : "global"); }
                if (rnd.nextBoolean()) tokens.add("-g");
            }
            // 6: log show
            case 6 -> {
                tokens.addAll(List.of("log", "show"));
                tokens.add("-c");
                tokens.add(randomString(rnd, 7, 10));
                if (rnd.nextBoolean()) tokens.add("-p");
                if (rnd.nextBoolean()) tokens.add("-s");
            }
            // 7: log diff
            case 7 -> {
                tokens.addAll(List.of("log", "diff"));
                tokens.add("-f");
                tokens.add(randomString(rnd, 7, 10));
                tokens.add("-t");
                tokens.add(randomString(rnd, 7, 10));
                if (rnd.nextBoolean()) tokens.add("-w");
            }
            // 8: stash push
            case 8 -> {
                tokens.addAll(List.of("stash", "push"));
                if (rnd.nextBoolean()) { tokens.add("-m"); tokens.add(randomString(rnd, 5, 20)); }
                if (rnd.nextBoolean()) tokens.add("-u");
                if (rnd.nextBoolean()) tokens.add("-k");
            }
            // 9: stash pop
            case 9 -> {
                tokens.addAll(List.of("stash", "pop"));
                if (rnd.nextBoolean()) { tokens.add("-i"); tokens.add(String.valueOf(rnd.nextInt(0, 5))); }
                if (rnd.nextBoolean()) tokens.add("-f");
            }
            // 10: stash list
            case 10 -> {
                tokens.addAll(List.of("stash", "list"));
                if (rnd.nextBoolean()) { tokens.add("-p"); tokens.add(rnd.nextBoolean() ? "oneline" : "full"); }
            }
            // 11: stash apply
            case 11 -> {
                tokens.addAll(List.of("stash", "apply"));
                if (rnd.nextBoolean()) { tokens.add("-i"); tokens.add(String.valueOf(rnd.nextInt(0, 5))); }
            }
            // 12: tag create
            case 12 -> {
                tokens.addAll(List.of("tag", "create"));
                tokens.add("-n");
                tokens.add(randomString(rnd, 3, 10));
                if (rnd.nextBoolean()) { tokens.add("-m"); tokens.add(randomString(rnd, 5, 15)); }
                if (rnd.nextBoolean()) tokens.add("-f");
            }
            // 13: tag delete
            case 13 -> {
                tokens.addAll(List.of("tag", "delete"));
                tokens.add("-n");
                tokens.add(randomString(rnd, 3, 10));
                if (rnd.nextBoolean()) tokens.add("-f");
            }
            // 14: tag list
            case 14 -> {
                tokens.addAll(List.of("tag", "list"));
                if (rnd.nextBoolean()) { tokens.add("-p"); tokens.add(randomString(rnd, 3, 8)); }
            }
        }

        if (tokens.size() > 1) {
            String first = tokens.remove(0);
            Collections.shuffle(tokens, rnd);
            tokens.add(0, first);
        }
        return tokens.toArray(new String[0]);
    }

    private static String randomString(ThreadLocalRandom rnd, int minLen, int maxLen) {
        int len = rnd.nextInt(minLen, maxLen + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int type = rnd.nextInt(3);
            char c;
            if (type == 0) c = (char) ('a' + rnd.nextInt(26));
            else if (type == 1) c = (char) ('A' + rnd.nextInt(26));
            else c = (char) ('0' + rnd.nextInt(10));
            sb.append(c);
        }
        return sb.toString();
    }

    private static void printSystemMetrics(String phase) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        double load = osBean.getSystemLoadAverage();
        String cpuStr = (load < 0) ? "N/A" : String.format("%.2f", load);

        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMB = heapUsage.getUsed() / (1024 * 1024);
        long maxMB = heapUsage.getMax() / (1024 * 1024);

        long gcCount = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            gcCount += gc.getCollectionCount();
        }

        System.out.printf("[%s] CPU load: %s, Heap memory: %dMB / %dMB, GC count: %d%n",
                phase, cpuStr, usedMB, maxMB, gcCount);
    }

    public static void main(String[] args) throws InterruptedException {
        CLI cli = buildCli();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int totalCases = 50_000;
        String[][] testCases = new String[totalCases][];
        for (int i = 0; i < totalCases; i++) {
            testCases[i] = generateRandomArgs(rnd);
        }

        int warmupIterations = 30_000;
        System.out.println("Warming up (single thread) ...");
        for (int i = 0; i < warmupIterations; i++) {
            cli.execute(testCases[i % totalCases]);
        }

        int threadCount = 8;
        int tasksPerThread = 20000;
        int totalTasks = threadCount * tasksPerThread;

        System.out.printf("Concurrent stress test: threads=%d, tasks per thread=%d, total tasks=%d%n",
                threadCount, tasksPerThread, totalTasks);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalTasks);

        List<Double> allTimes = Collections.synchronizedList(new ArrayList<>(totalTasks));
        AtomicLong successCount = new AtomicLong(0);

        for (int i = 0; i < totalTasks; i++) {
            String[] cmd = testCases[rnd.nextInt(totalCases)];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long start = System.nanoTime();
                    cli.execute(cmd);
                    long end = System.nanoTime();
                    allTimes.add((end - start) / 1_000_000.0);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    endLatch.countDown();
                }
            });
        }

        printSystemMetrics("Before stress test");

        long overallStart = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        long overallEnd = System.currentTimeMillis();

        printSystemMetrics("After stress test");
        executor.shutdown();

        double totalSeconds = (overallEnd - overallStart) / 1000.0;
        double tps = successCount.get() / totalSeconds;

        Collections.sort(allTimes);
        int size = allTimes.size();
        if (size == 0) {
            System.out.println("No successful tasks");
            return;
        }

        double avg = allTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = allTimes.get(0);
        double max = allTimes.get(size - 1);
        double median = allTimes.get(size / 2);
        double p90 = allTimes.get((int) (size * 0.90));
        double p99 = allTimes.get((int) (size * 0.99));
        double p999 = allTimes.get((int) (size * 0.999));

        double variance = allTimes.stream().mapToDouble(t -> Math.pow(t - avg, 2)).average().orElse(0);
        double std = Math.sqrt(variance);

        System.out.println("========== Stress Test Report ==========");
        System.out.printf("Total tasks: %d, Success: %d, Failed: %d%n", totalTasks, successCount.get(), totalTasks - successCount.get());
        System.out.printf("Total time: %.2f seconds, Overall TPS: %.2f%n", totalSeconds, tps);
        System.out.printf("Average response time: %.4f ms, Std dev: %.4f ms%n", avg, std);
        System.out.printf("Min: %.4f ms, Max: %.4f ms%n", min, max);
        System.out.printf("Median: %.4f ms, P90: %.4f ms, P99: %.4f ms, P99.9: %.4f ms%n",
                median, p90, p99, p999);
        System.out.println("=========================================");
    }
}