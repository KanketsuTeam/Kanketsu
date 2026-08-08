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
import io.github.kanketsuteam.kanketsu.repl.completion.KanketsuCompleter;
import io.github.kanketsuteam.kanketsu.repl.exception.BuildTerminalFailedException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * Read-Eval-Print Loop (REPL) console that provides interactive command-line
 * input with line editing, history, and tab completion.
 * <p>
 * This class encapsulates a JLine {@link Terminal} and {@link LineReader} to
 * offer a consistent prompt interface. It delegates command completion to a
 * {@link KanketsuCompleter} built from the root command map of the provided CLI.
 * </p>
 * <p>
 * The terminal is either supplied directly or created via {@link Terminals#createDefault()}.
 * If terminal creation fails, a {@link BuildTerminalFailedException} is thrown
 * at construction time when using the default constructor. The other constructor
 * allows the caller to provide an already‑built terminal, giving full control
 * over terminal creation and error handling.
 * </p>
 * <p>
 * If the line reader is {@code null} (which should not happen with a valid terminal),
 * read operations will catch {@link NullPointerException} and return {@code null}
 * gracefully.
 * </p>
 */
public class REPL {
    private Terminal terminal;
    private LineReader reader;
    private final CLI cli;

    /**
     * Constructs a new REPL instance using a provided terminal.
     * <p>
     * The line reader is built with the given terminal and a completer derived
     * from the CLI's root commands. The terminal is assumed to be already
     * initialised and ready for use.
     * </p>
     *
     * @param cli      the CLI instance that provides the root command map for completion
     * @param terminal the terminal to use for input/output
     * @throws NullPointerException if either {@code cli} or {@code terminal} is {@code null}
     */
    public REPL(CLI cli, Terminal terminal) {
        this.cli = cli;
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new KanketsuCompleter(cli.getRootCommands()))
                .build();
    }

    /**
     * Constructs a new REPL instance using the default system terminal.
     * <p>
     * This constructor delegates to {@link #REPL(CLI, Terminal)} after obtaining
     * a terminal via {@link Terminals#createDefault()}. If terminal creation
     * fails, a {@link BuildTerminalFailedException} is thrown.
     * </p>
     *
     * @param cli the CLI instance that provides the root command map for completion
     * @throws BuildTerminalFailedException if the default terminal cannot be created
     * @see Terminals#createDefault()
     */
    public REPL(CLI cli) {
        this(cli, Terminals.createDefault());
    }

    /**
     * Reads a line of input from the user with a custom prompt.
     * <p>
     * The method returns {@code null} if the user interrupts input (Ctrl+C),
     * sends an EOF (Ctrl+D), or if the internal line reader is not available
     * (e.g., due to a terminal build failure or unexpected {@code null} state).
     * </p>
     *
     * @param prompt the prompt string to display
     * @return the entered line, or {@code null} if input is interrupted, EOF, or reader unavailable
     */
    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Reads a line of input using the default prompt {@code "> "}.
     *
     * @return the entered line, or {@code null} if input is interrupted, EOF, or reader unavailable
     * @see #readLine(String)
     */
    public String readLine() {
        return readLine("> ");
    }

    /**
     * Closes the underlying terminal and releases any system resources.
     * <p>
     * Any I/O exceptions or {@code NullPointerException} that occur during
     * closing are logged via the CLI's logger (obtained from {@code cli.getLogger()})
     * but are otherwise silently ignored to avoid disrupting shutdown.
     * </p>
     */
    public void close() {
        try {
            terminal.close();
        } catch (IOException | NullPointerException e) {
            cli.getLogger().error("Close terminal failed:" + e.getMessage(), e);
        }
    }

    /**
     * Returns the underlying terminal instance.
     *
     * @return the terminal used by this REPL, or {@code null} if terminal creation failed
     */
    public Terminal getTerminal() {
        return terminal;
    }
}