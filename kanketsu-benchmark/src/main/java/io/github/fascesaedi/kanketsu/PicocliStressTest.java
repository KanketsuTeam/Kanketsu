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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(4)
@Threads(1)
public class PicocliStressTest {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        String[][] allCommands;
        int totalCommands;

        @Setup(Level.Trial)
        public void setup() {
            int totalCases = 50_000;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            allCommands = new String[totalCases][];
            for (int i = 0; i < totalCases; i++) {
                allCommands[i] = generateRandomArgs(rnd);
            }
            totalCommands = allCommands.length;
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
        CommandLine cmd;
        int index = 0;

        @Setup(Level.Trial)
        public void setup() {
            Git git = new Git();
            cmd = new CommandLine(git);

            Remote remote = new Remote();
            cmd.addSubcommand("remote", remote);
            CommandLine remoteCmd = cmd.getSubcommands().get("remote");
            remoteCmd.addSubcommand("add", new Remote.Add());
            remoteCmd.addSubcommand("remove", new Remote.Remove());
            remoteCmd.addSubcommand("set-url", new Remote.SetUrl());

            Config config = new Config();
            cmd.addSubcommand("config", config);
            CommandLine configCmd = cmd.getSubcommands().get("config");
            configCmd.addSubcommand("get", new Config.Get());
            configCmd.addSubcommand("set", new Config.Set());
            configCmd.addSubcommand("list", new Config.List());

            Log log = new Log();
            cmd.addSubcommand("log", log);
            CommandLine logCmd = cmd.getSubcommands().get("log");
            logCmd.addSubcommand("show", new Log.Show());
            logCmd.addSubcommand("diff", new Log.Diff());

            Stash stash = new Stash();
            cmd.addSubcommand("stash", stash);
            CommandLine stashCmd = cmd.getSubcommands().get("stash");
            stashCmd.addSubcommand("push", new Stash.Push());
            stashCmd.addSubcommand("pop", new Stash.Pop());
            stashCmd.addSubcommand("list", new Stash.List());
            stashCmd.addSubcommand("apply", new Stash.Apply());

            Tag tag = new Tag();
            cmd.addSubcommand("tag", tag);
            CommandLine tagCmd = cmd.getSubcommands().get("tag");
            tagCmd.addSubcommand("create", new Tag.Create());
            tagCmd.addSubcommand("delete", new Tag.Delete());
            tagCmd.addSubcommand("list", new Tag.List());
        }
    }

    @Benchmark
    public void testParse(BenchmarkState state, ThreadState threadState, Blackhole bh) {
        int idx = threadState.index++ % state.totalCommands;
        String[] cmdArgs = state.allCommands[idx];
        String[] argsWithoutGit = Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length);
        threadState.cmd.execute(argsWithoutGit);
        bh.consume(cmdArgs);
    }

    @Command(name = "git")
    public static class Git {

    }

    @Command(name = "remote")
    public static class Remote {
        @Command(name = "add")
        public static class Add implements Runnable {
            @Option(names = {"-n", "--name"}, required = true) String name;
            @Option(names = {"-u", "--url"}, required = true) String url;
            @Option(names = {"-t", "--track"}) String track;
            @Option(names = {"-m", "--mirror"}) boolean mirror;
            @Override public void run() { blackHole(name.length() + url.length() + (track == null ? 0 : track.length()) + (mirror ? 1 : 0)); }
        }
        @Command(name = "remove")
        public static class Remove implements Runnable {
            @Option(names = {"-n", "--name"}, required = true) String name;
            @Override public void run() { blackHole(name.length()); }
        }
        @Command(name = "set-url")
        public static class SetUrl implements Runnable {
            @Option(names = {"-n", "--name"}, required = true) String name;
            @Option(names = {"-u", "--url"}, required = true) String url;
            @Option(names = {"-p", "--push"}) boolean push;
            @Override public void run() { blackHole(name.length() + url.length() + (push ? 1 : 0)); }
        }
    }

    @Command(name = "config")
    public static class Config {
        @Command(name = "get")
        public static class Get implements Runnable {
            @Option(names = {"-k", "--key"}, required = true) String key;
            @Option(names = {"-s", "--scope"}) String scope = "local";
            @Override public void run() { blackHole(key.length() + scope.length()); }
        }
        @Command(name = "set")
        public static class Set implements Runnable {
            @Option(names = {"-k", "--key"}, required = true) String key;
            @Option(names = {"-v", "--value"}, required = true) String value;
            @Option(names = {"-s", "--scope"}) String scope = "local";
            @Override public void run() { blackHole(key.length() + value.length() + scope.length()); }
        }
        @Command(name = "list")
        public static class List implements Runnable {
            @Option(names = {"-s", "--scope"}) String scope = "local";
            @Option(names = {"-g", "--global"}) boolean global;
            @Override public void run() { blackHole(scope.length() + (global ? 1 : 0)); }
        }
    }

    @Command(name = "log")
    public static class Log {
        @Command(name = "show")
        public static class Show implements Runnable {
            @Option(names = {"-c", "--commit"}, required = true) String commit;
            @Option(names = {"-p", "--patch"}) boolean patch;
            @Option(names = {"-s", "--stat"}) boolean stat;
            @Override public void run() { blackHole(commit.length() + (patch ? 1 : 0) + (stat ? 1 : 0)); }
        }
        @Command(name = "diff")
        public static class Diff implements Runnable {
            @Option(names = {"-f", "--from"}, required = true) String from;
            @Option(names = {"-t", "--to"}, required = true) String to;
            @Option(names = {"-w", "--ignore-space"}) boolean ignoreSpace;
            @Override public void run() { blackHole(from.length() + to.length() + (ignoreSpace ? 1 : 0)); }
        }
    }

    @Command(name = "stash")
    public static class Stash {
        @Command(name = "push")
        public static class Push implements Runnable {
            @Option(names = {"-m", "--message"}) String message;
            @Option(names = {"-u", "--include-untracked"}) boolean includeUntracked;
            @Option(names = {"-k", "--keep-index"}) boolean keepIndex;
            @Override public void run() { blackHole((message == null ? 0 : message.length()) + (includeUntracked ? 1 : 0) + (keepIndex ? 1 : 0)); }
        }
        @Command(name = "pop")
        public static class Pop implements Runnable {
            @Option(names = {"-i", "--index"}) String index = "0";
            @Option(names = {"-f", "--force"}) boolean force;
            @Override public void run() { blackHole(index.length() + (force ? 1 : 0)); }
        }
        @Command(name = "list")
        public static class List implements Runnable {
            @Option(names = {"-p", "--pretty"}) String pretty = "oneline";
            @Override public void run() { blackHole(pretty.length()); }
        }
        @Command(name = "apply")
        public static class Apply implements Runnable {
            @Option(names = {"-i", "--index"}) String index = "0";
            @Override public void run() { blackHole(index.length()); }
        }
    }

    @Command(name = "tag")
    public static class Tag {
        @Command(name = "create")
        public static class Create implements Runnable {
            @Option(names = {"-n", "--name"}, required = true) String name;
            @Option(names = {"-m", "--message"}) String message;
            @Option(names = {"-f", "--force"}) boolean force;
            @Override public void run() { blackHole(name.length() + (message == null ? 0 : message.length()) + (force ? 1 : 0)); }
        }
        @Command(name = "delete")
        public static class Delete implements Runnable {
            @Option(names = {"-n", "--name"}, required = true) String name;
            @Option(names = {"-f", "--force"}) boolean force;
            @Override public void run() { blackHole(name.length() + (force ? 1 : 0)); }
        }
        @Command(name = "list")
        public static class List implements Runnable {
            @Option(names = {"-p", "--pattern"}) String pattern;
            @Override public void run() { blackHole(pattern == null ? 0 : pattern.length()); }
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static void blackHole(int x) {
        if (x == Integer.MIN_VALUE) {
            System.out.println("unreachable");
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PicocliStressTest.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}