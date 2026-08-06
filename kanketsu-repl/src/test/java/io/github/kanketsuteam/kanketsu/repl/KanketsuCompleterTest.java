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
package io.github.kanketsuteam.kanketsu.repl;

import io.github.kanketsuteam.kanketsu.core.CLI;
import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.converter.Converters;
import io.github.kanketsuteam.kanketsu.repl.completion.KanketsuCompleter;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KanketsuCompleterTest {

    private Map<String, Command> roots;
    private KanketsuCompleter completer;

    @BeforeEach
    void setUp() {
        CLI cli = CLI.builder()
                .command("git", git -> git
                        .command("remote", remote -> remote
                                .command("add", add -> add
                                        .option("verbose", opt -> opt
                                                .shortOpt("v")
                                                .hasArg(false)
                                                .converter(Converters.BOOLEAN)
                                                .description("verbose mode"))
                                        .option("force", opt -> opt
                                                .shortOpt("f")
                                                .hasArg(false)
                                                .converter(Converters.BOOLEAN)
                                                .description("force push"))
                                        .option("mirror", opt -> opt
                                                .shortOpt("m")
                                                .hasArg(true)
                                                .converter(Converters.STRING)
                                                .description("mirror url"))
                                        .action(ctx -> {})
                                )
                        )
                        .command("commit", commit -> commit
                                .option("message", opt -> opt
                                        .shortOpt("m")
                                        .hasArg(true)
                                        .converter(Converters.STRING)
                                        .description("commit message"))
                                .option("all", opt -> opt
                                        .shortOpt("a")
                                        .hasArg(false)
                                        .converter(Converters.BOOLEAN)
                                        .description("add all"))
                                .action(ctx -> {})
                        )
                )
                .command("other", other -> other
                        .action(ctx -> {})
                )
                .build();

        roots = cli.getRootCommands();
        completer = new KanketsuCompleter(roots);
    }

    // ---- Helper to create ParsedLine ----
    private ParsedLine parsedLine(String word, List<String> words, int wordIndex) {
        return new ParsedLine() {
            @Override
            public String word() { return word; }

            @Override
            public int wordCursor() { return word.length(); }

            @Override
            public int wordIndex() { return wordIndex; }

            @Override
            public List<String> words() { return words; }

            @Override
            public String line() { return String.join(" ", words); }

            @Override
            public int cursor() { return line().length(); }
        };
    }

    @Test
    void shouldCompleteRootCommandsWhenEmpty() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("", List.of(), 0), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactlyInAnyOrder("git", "other");
    }

    @Test
    void shouldCompleteGitWhenPrefixG() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("g", List.of("g"), 0), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("git");
    }

    @Test
    void shouldCompleteOtherWhenPrefixO() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("o", List.of("o"), 0), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("other");
    }

    @Test
    void shouldReturnEmptyWhenNoRootMatch() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("x", List.of("x"), 0), candidates);
        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldCompleteSubcommandsAfterGit() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("", List.of("git", ""), 1), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactlyInAnyOrder("remote", "commit");
    }

    @Test
    void shouldCompleteRemoteWhenPrefixR() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("r", List.of("git", "r"), 1), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("remote");
    }

    @Test
    void shouldCompleteAddAfterRemote() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("", List.of("git", "remote", ""), 2), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("add");
    }

    @Test
    void shouldCompleteAddWhenPrefixA() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("a", List.of("git", "remote", "a"), 2), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("add");
    }

    @Test
    void shouldReturnEmptyWhenNoSubcommandUnderAdd() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("", List.of("git", "remote", "add", ""), 3), candidates);
        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldCompleteAllLongOptionsAfterDoubleDash() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("--", List.of("git", "remote", "add", "--"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactlyInAnyOrder("--verbose", "--force", "--mirror");
    }

    @Test
    void shouldCompleteVerboseLongOptionWhenPrefixV() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("--v", List.of("git", "remote", "add", "--v"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("--verbose");
    }

    @Test
    void shouldCompleteShortOptionV() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("-v", List.of("git", "remote", "add", "-v"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("-v");
    }

    @Test
    void shouldCompleteShortOptionF() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("-f", List.of("git", "remote", "add", "-f"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("-f");
    }

    @Test
    void shouldCompleteShortOptionM() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("-m", List.of("git", "remote", "add", "-m"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("-m");
    }

    @Test
    void shouldCompleteMirrorLongOption() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("--mirror", List.of("git", "remote", "add", "--mirror"), 3), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("--mirror");
    }

    @Test
    void shouldCompleteCommitLongOptions() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("--", List.of("git", "commit", "--"), 2), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactlyInAnyOrder("--message", "--all");
    }

    @Test
    void shouldCompleteCommitShortOptionM() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("-m", List.of("git", "commit", "-m"), 2), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("-m");
    }

    @Test
    void shouldCompleteCommitShortOptionA() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("-a", List.of("git", "commit", "-a"), 2), candidates);
        assertThat(candidates).extracting(Candidate::value)
                .containsExactly("-a");
    }

    @Test
    void shouldReturnEmptyAfterOptionWithSpace() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("", List.of("git", "remote", "add", "--verbose", ""), 4), candidates);
        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPositionalArgProvided() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("origin", List.of("git", "remote", "add", "origin"), 3), candidates);
        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldIncludeDescriptionInCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine("--", List.of("git", "remote", "add", "--"), 3), candidates);
        assertThat(candidates).extracting(Candidate::descr)
                .containsExactlyInAnyOrder("verbose mode", "force push", "mirror url");
    }
}