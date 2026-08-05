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
package io.github.fascesaedi.kanketsu.repl;

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.repl.completion.KanketsuCompleter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class REPL {
    private final Terminal terminal;
    private final LineReader reader;

    public REPL(CLI cli) throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .provider("ffm")
                .build();
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new KanketsuCompleter(cli.getRootCommands()))
                .build();
    }

    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    public String readLine() {
        return readLine("> ");
    }

    public void close() {
        try {
            terminal.close();
        } catch (IOException ignored) {}
    }

    public Terminal getTerminal(){
        return terminal;
    }
}