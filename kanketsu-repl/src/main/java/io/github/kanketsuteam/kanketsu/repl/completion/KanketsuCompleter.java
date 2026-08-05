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
package io.github.kanketsuteam.kanketsu.repl.completion;

import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.Option;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Map;

public class KanketsuCompleter implements Completer {

    private final Map<String, Command> roots;

    public KanketsuCompleter(Map<String, Command> roots) {
        this.roots = roots;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.word();
        List<String> words = line.words();
        int wordIndex = line.wordIndex();
        if (words.isEmpty() || wordIndex == 0) {
            for (String rootName : roots.keySet()) {
                if (rootName.startsWith(buffer)) {
                    candidates.add(new Candidate(rootName));
                }
            }
            return;
        }

        String rootName = null;
        int firstArgIndex = 0;
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            if (!w.startsWith("-")) {
                rootName = w;
                firstArgIndex = i;
                break;
            }
        }
        if (rootName == null || !roots.containsKey(rootName)) {
            return;
        }

        Command current = roots.get(rootName);
        for (int i = firstArgIndex + 1; i < wordIndex; i++) {
            String word = words.get(i);
            if (!word.startsWith("-")) {
                Command child = current.getChildren().get(word);
                if (child != null) {
                    current = child;
                } else {
                    return;
                }
            }
        }

        if (buffer.startsWith("-")) {
            for (Option opt : current.getOptions().values()) {
                String longOpt = "--" + opt.getLongOpt();
                if (longOpt.startsWith(buffer)) {
                    candidates.add(new Candidate(longOpt, longOpt, null, opt.getDescription(), null, null, true));
                }
                String shortOpt = opt.getShortOpt();
                if (shortOpt != null && !shortOpt.isEmpty()) {
                    String shortOptStr = "-" + shortOpt;
                    if (shortOptStr.startsWith(buffer)) {
                        candidates.add(new Candidate(shortOptStr, shortOptStr, null, opt.getDescription(), null, null, true));
                    }
                }
            }
        } else {
            for (Command child : current.getChildren().values()) {
                String name = child.getName();
                if (name.startsWith(buffer)) {
                    candidates.add(new Candidate(name));
                }
            }
        }
    }
}