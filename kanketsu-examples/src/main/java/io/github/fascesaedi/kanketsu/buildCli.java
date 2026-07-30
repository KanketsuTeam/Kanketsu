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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class buildCli {
    private static final Map<String, Integer> RESULT_CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong TOTAL_PROCESSED = new AtomicLong(0);
    private static final int CACHE_MAX_SIZE = 10_000;

    private static final Pattern COMMIT_PATTERN = Pattern.compile("^[a-f0-9]{7,40}$");
    private static final Pattern BRANCH_PATTERN = Pattern.compile("^[a-zA-Z0-9_/.-]+$");

    private static void simulateWork(CommandContext ctx) {
        StringBuilder fullCmd = new StringBuilder("git");
        for (String arg : ctx.getPositionalArgs()) {
            fullCmd.append(' ').append(arg);
        }
        for (Map.Entry<String, String> entry : ctx.getOptions().entrySet()) {
            fullCmd.append(" --").append(entry.getKey());
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                fullCmd.append('=').append(entry.getValue());
            }
        }
        String cmdLine = fullCmd.toString();

        boolean commitValid = true;
        if (ctx.hasOption("commit") || ctx.hasOption("c")) {
            String commit = ctx.getOption("commit") != null ? ctx.getOption("commit") : ctx.getOption("c");
            if (commit != null && !COMMIT_PATTERN.matcher(commit).matches()) {
                commitValid = false;
            }
        }

        boolean nameValid = true;
        if (ctx.hasOption("name") || ctx.hasOption("n")) {
            String name = ctx.getOption("name") != null ? ctx.getOption("name") : ctx.getOption("n");
            if (name != null && !BRANCH_PATTERN.matcher(name).matches()) {
                nameValid = false;
            }
        }

        String output;
        if (commitValid && nameValid) {
            output = "OK: " + cmdLine.substring(0, Math.min(cmdLine.length(), 30));
        } else {
            output = "ERR: " + cmdLine.substring(0, Math.min(cmdLine.length(), 30));
        }

        String cacheKey = output;
        Integer cached = RESULT_CACHE.get(cacheKey);
        if (cached == null) {
            int hash = output.hashCode() ^ cmdLine.length();
            if (RESULT_CACHE.size() < CACHE_MAX_SIZE) {
                RESULT_CACHE.put(cacheKey, hash);
            }
            cached = hash;
        }

        TOTAL_PROCESSED.addAndGet(cached);
        if (TOTAL_PROCESSED.get() == Long.MIN_VALUE) {
            System.out.println("unreachable");
        }
    }

    public static CLI buildCLI(){
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
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("remove", remove -> remove
                                        .option("name", "n", "Remote name", true)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("set-url", setUrl -> setUrl
                                        .option("name", "n", "Remote name", true)
                                        .option("url", "u", "New URL", true)
                                        .option("push", "p", "Set push URL", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                        )
                        .command("config", config -> config
                                .command("get", get -> get
                                        .option("key", "k", "Config key", true)
                                        .option("scope", "s", "Scope", false, "local")
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("set", set -> set
                                        .option("key", "k", "Config key", true)
                                        .option("value", "v", "Config value", true)
                                        .option("scope", "s", "Scope", false, "local")
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("list", list -> list
                                        .option("scope", "s", "Scope", false, "local")
                                        .option("global", "g", "Global only", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                        )
                        .command("log", log -> log
                                .command("show", show -> show
                                        .option("commit", "c", "Commit hash", true)
                                        .option("patch", "p", "Show patch", false)
                                        .option("stat", "s", "Show statistics", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("diff", diff -> diff
                                        .option("from", "f", "From commit", true)
                                        .option("to", "t", "To commit", true)
                                        .option("ignore-space", "w", "Ignore whitespace", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                        )
                        .command("stash", stash -> stash
                                .command("push", push -> push
                                        .option("message", "m", "Stash message", false)
                                        .option("include-untracked", "u", "Include untracked", false)
                                        .option("keep-index", "k", "Keep index", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("pop", pop -> pop
                                        .option("index", "i", "Stash index", false, "0")
                                        .option("force", "f", "Force pop", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("list", list -> list
                                        .option("pretty", "p", "Pretty format", false, "oneline")
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("apply", apply -> apply
                                        .option("index", "i", "Stash index", false, "0")
                                        .action(ctx -> simulateWork(ctx))
                                )
                        )
                        .command("tag", tag -> tag
                                .command("create", create -> create
                                        .option("name", "n", "Tag name", true)
                                        .option("message", "m", "Tag message", false)
                                        .option("force", "f", "Force create", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("delete", delete -> delete
                                        .option("name", "n", "Tag name", true)
                                        .option("force", "f", "Force delete", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                                .command("list", list -> list
                                        .option("pattern", "p", "List tags matching pattern", false)
                                        .action(ctx -> simulateWork(ctx))
                                )
                        )
                )
                .build();
    }
}
