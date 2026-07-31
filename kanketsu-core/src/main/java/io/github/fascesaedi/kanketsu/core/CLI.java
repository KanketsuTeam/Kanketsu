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
    private final boolean autoHelp;

    private CLI(Builder builder) {
        this.roots = builder.roots;
        this.logger = builder.logger != null ? builder.logger : Logger.system();
        this.autoHelp = Boolean.parseBoolean(
                System.getProperty("kanketsu.autoHelp", "true")
        );
    }

    public Map<String, Command> getRootCommands() {
        return Collections.unmodifiableMap(roots);
    }

    public int execute(String... args) {
        if (args.length == 0) {
            logger.info("No command provided. Use --help for usage.");
            return 1;
        }

        if (args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            logger.info(generateGlobalHelp());
            return 0;
        }

        Command current = roots.get(args[0]);
        if (current == null) {
            logger.warn("Unknown command: " + args[0]);
            logger.info("Use --help to see available commands.");
            return 1;
        }

        List<String> path = new ArrayList<>();
        path.add(args[0]);
        int idx = 1;
        while (idx < args.length) {
            Command next = current.getChildren().get(args[idx]);
            if (next == null) break;
            current = next;
            path.add(args[idx]);
            idx++;
        }

        String[] remaining = Arrays.copyOfRange(args, idx, args.length);

        for (String arg : remaining) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                String fullPath = String.join(" ", path);
                logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
                return 0;
            }
        }

        try {
            CommandContext ctx = parseOptions(remaining, current);

            for (Option opt : current.getOptions().values()) {
                if (opt.isRequired() && !ctx.hasOption(opt.getLongOpt())) {
                    String fullPath = String.join(" ", path);
                    logger.warn("Missing required option: --" + opt.getLongOpt());
                    logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
                    return 2;
                }
            }

            if (current.getAction() == null) {
                logger.error("Command '" + String.join(" ", path) + "' has no action defined.");
                return 1;
            }
            return current.run(ctx);

        } catch (IllegalArgumentException e) {
            logger.error("Parameter error: " + e.getMessage());
            String fullPath = String.join(" ", path);
            logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
            return 2;
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
            if (logger.isDebugEnabled()) {
                e.printStackTrace(logger.getPrintStream());
            }
            return 1;
        }
    }

    private String generateGlobalHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: <command> [options] [arguments]\n");
        sb.append("Available commands:\n");
        int maxLen = 0;
        for (String name : roots.keySet()) {
            if (name.length() > maxLen) maxLen = name.length();
        }
        String format = "  %-" + maxLen + "s  %s\n";
        for (Map.Entry<String, Command> entry : roots.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue().getDescription();
            if (desc == null) desc = "";
            sb.append(String.format(format, name, desc));
        }
        sb.append("\nUse '<command> --help' for more information on a specific command.");
        return sb.toString();
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
            }
            else if (arg.startsWith("-") && arg.length() > 1) {
                i = parseShortOption(arg, i, args, optionDefs, shortToLong, parsed, positional);
            }
            else {
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
            throw new IllegalArgumentException("Unknown option: --" + key);
        }

        if (def.hasArg()) {
            int pos = arg.indexOf('=');
            if (pos != -1) {
                parsed.put(key, arg.substring(pos + 1));
            } else if (i + 1 < args.length) {
                parsed.put(key, args[i + 1]);
                return i + 1;
            } else {
                throw new IllegalArgumentException("Option --" + key + " requires a value");
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
        int eqIndex = optStr.indexOf('=');

        if (eqIndex != -1) {
            String optionChar = optStr.substring(0, 1);
            String value = optStr.substring(eqIndex + 1);
            String longKey = shortToLong.get(optionChar);
            if (longKey == null) {
                throw new IllegalArgumentException("Unknown option: -" + optionChar);
            }
            Option def = optionDefs.get(longKey);
            if (def == null) {
                throw new IllegalArgumentException("Unknown option: -" + optionChar);
            }
            if (!def.hasArg()) {
                throw new IllegalArgumentException("Option -" + optionChar + " does not take a value");
            }
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Option -" + optionChar + " requires a value");
            }
            parsed.put(longKey, value);
            return i;
        }

        if (optStr.length() == 1) {
            String longKey = shortToLong.get(optStr);
            if (longKey == null) {
                throw new IllegalArgumentException("Unknown option: -" + optStr);
            }
            Option def = optionDefs.get(longKey);
            if (def.hasArg()) {
                if (i + 1 < args.length) {
                    parsed.put(longKey, args[i + 1]);
                    return i + 1;
                } else {
                    throw new IllegalArgumentException("Option -" + optStr + " requires a value");
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

        for (char ch : optStr.toCharArray()) {
            String key = shortToLong.get(String.valueOf(ch));
            if (key == null) {
                throw new IllegalArgumentException("Unknown short option: -" + ch);
            }
            Option def = optionDefs.get(key);
            if (def == null) {
                throw new IllegalArgumentException("Unknown short option: -" + ch);
            }
            if (def.hasArg()) {
                throw new IllegalArgumentException("Option -" + ch + " requires a value, but is in a combined short option without a value.");
            }
            parsed.put(key, TRUE_VALUE);
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

        public Builder command(String name,String description, java.util.function.Consumer<CommandBuilder> consumer) {
            CommandBuilder cb = new CommandBuilder(name, description);
            consumer.accept(cb);
            roots.put(name, cb.build());
            return this;
        }

        public Builder command(String name, java.util.function.Consumer<CommandBuilder> consumer){
            return command(name,"No description", consumer);
        }

        public CLI build() {
            return new CLI(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}