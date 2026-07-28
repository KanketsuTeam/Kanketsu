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
package io.github.fascesaedi.kanketsu.core;

import io.github.fascesaedi.kanketsu.spi.Logger;

import java.util.*;

public class CLI {
    private static final String TRUE_VALUE = "true";
    private static final String DOUBLE_DASH = "--";

    private final Map<String, Command> roots;
    private final Logger logger;

    private CLI(Builder builder) {
        this.roots = builder.roots;
        this.logger = builder.logger != null ? builder.logger : Logger.system();
    }

    public void execute(String... args) {
        if (args.length == 0) return;

        Command current = roots.get(args[0]);
        if (current == null) {
            logger.warn("Unknown command: " + args[0]);
            return;
        }

        int idx = 1;
        while (idx < args.length) {
            Command next = current.getChildren().get(args[idx]);
            if (next == null) break;
            current = next;
            idx++;
        }

        String[] remaining = Arrays.copyOfRange(args, idx, args.length);
        CommandContext ctx = parseOptions(remaining, current);

        for (Option opt : current.getOptions().values()) {
            if (opt.isRequired() && !ctx.hasOption(opt.getLongOpt())) {
                logger.warn("Missing required option: --" + opt.getLongOpt());
                return;
            }
        }

        current.run(ctx);
    }

    private CommandContext parseOptions(String[] args, Command command) {
        Map<String, Option> optionDefs = command.getOptions();
        Map<String, String> shortToLong = buildShortToLongMap(optionDefs);
        Map<String, String> parsed = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();

        for (Option opt : optionDefs.values()) {
            if (opt.getDefaultValue() != null) {
                parsed.put(opt.getLongOpt(), opt.getDefaultValue());
            }
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals(DOUBLE_DASH)) {
                for (int j = i + 1; j < args.length; j++) {
                    positional.add(args[j]);
                }
                break;
            }

            if (arg.startsWith(DOUBLE_DASH)) {
                i = parseLongOption(arg, i, args, optionDefs, parsed, positional);
            } else if (arg.startsWith("-") && arg.length() > 1) {
                i = parseShortOption(arg, i, args, optionDefs, shortToLong, parsed, positional);
            } else {
                positional.add(arg);
            }
        }

        return new CommandContext(parsed, positional);
    }

    private Map<String, String> buildShortToLongMap(Map<String, Option> optionDefs) {
        Map<String, String> map = new HashMap<>();
        for (Option opt : optionDefs.values()) {
            if (opt.getShortOpt() != null) {
                map.put(opt.getShortOpt(), opt.getLongOpt());
            }
        }
        return map;
    }

    private int parseLongOption(String arg, int i, String[] args,
                                Map<String, Option> optionDefs,
                                Map<String, String> parsed,
                                List<String> positional) {
        String key = arg.substring(2);
        Option def = optionDefs.get(key);
        if (def == null) {
            positional.add(arg);
            logger.warn("Unknown option: --" + key);
            return i;
        }

        if (def.hasArg()) {
            int pos = arg.indexOf('=');
            if (pos != -1) {
                parsed.put(key, arg.substring(pos + 1));
            } else if (i + 1 < args.length) {
                parsed.put(key, args[i + 1]);
                return i + 1; // 跳过值
            } else {
                logger.warn("Option --" + key + " requires a value");
            }
        } else {
            parsed.put(key, TRUE_VALUE);
        }
        return i;
    }

    private int parseShortOption(String arg, int i, String[] args,
                                 Map<String, Option> optionDefs,
                                 Map<String, String> shortToLong,
                                 Map<String, String> parsed,
                                 List<String> positional) {
        String optStr = arg.substring(1);

        if (optStr.length() == 1) {
            String longKey = shortToLong.get(optStr);
            if (longKey == null) {
                positional.add(arg);
                logger.warn("Unknown option: -" + optStr);
                return i;
            }
            Option def = optionDefs.get(longKey);
            if (def.hasArg()) {
                if (i + 1 < args.length) {
                    parsed.put(longKey, args[i + 1]);
                    return i + 1;
                } else {
                    logger.warn("Option -" + optStr + " requires a value");
                }
            } else {
                parsed.put(longKey, TRUE_VALUE);
            }
            return i;
        }

        String firstChar = optStr.substring(0, 1);
        String firstLongKey = shortToLong.get(firstChar);
        Option firstDef = (firstLongKey != null) ? optionDefs.get(firstLongKey) : null;

        if (firstDef != null && firstDef.hasArg()) {
            parsed.put(firstLongKey, optStr.substring(1));
            return i;
        }

        boolean allValid = true;
        for (char ch : optStr.toCharArray()) {
            String key = shortToLong.get(String.valueOf(ch));
            if (key == null) {
                logger.warn("Unknown short option: -" + ch);
                allValid = false;
                continue;
            }
            Option def = optionDefs.get(key);
            if (def == null) {
                logger.warn("Unknown short option: -" + ch);
                allValid = false;
                continue;
            }
            if (def.hasArg()) {
                // 在复合短选项中，如果某个选项需要值，但值没有以当前位置提供，警告并跳过
                logger.warn("Option -" + ch + " requires a value, but it is in a combined short option without a value; skipping.");
                allValid = false;
                continue;
            }
            parsed.put(key, TRUE_VALUE);
        }

        if (!allValid) {
            positional.add(arg);
        }
        return i;
    }

    // ========== Builder ==========

    public static class Builder {
        private final Map<String, Command> roots = new LinkedHashMap<>();
        private Logger logger;

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder command(String name, java.util.function.Consumer<CommandBuilder> consumer) {
            CommandBuilder cb = new CommandBuilder(name);
            consumer.accept(cb);
            roots.put(name, cb.build());
            return this;
        }

        public CLI build() {
            return new CLI(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}