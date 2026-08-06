/*
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
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.command.CommandBuilder;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.*;
import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.util.*;

public class CLI {
    private static final String DOUBLE_DASH = "--";

    private final Map<String, Command> roots;
    private final Logger logger;
    private final List<TypeConverter> converters;
    private final HelpGenerator helpGenerator;
    private final boolean autoHelp;

    private final Map<String, Option> globalOptions;
    private final Map<String, String> globalShortToLong;

    private CLI(Builder builder) {
        this.roots = builder.roots;
        this.logger = builder.logger != null ? builder.logger : Logger.system();
        this.helpGenerator = builder.helpGenerator != null ? builder.helpGenerator : HelpGenerator.system();
        this.converters = Collections.unmodifiableList(builder.converters);
        this.autoHelp = Boolean.parseBoolean(System.getProperty("kanketsu.autoHelp", "true"));

        this.globalOptions = Collections.unmodifiableMap(builder.globalOptions);
        Map<String, String> shortMap = new LinkedHashMap<>();
        for (Option opt : globalOptions.values()) {
            if (opt.getShortOpt() != null) {
                shortMap.put(opt.getShortOpt(), opt.getLongOpt());
            }
        }
        this.globalShortToLong = Collections.unmodifiableMap(shortMap);
    }

    public Map<String, Command> getRootCommands() {
        return Collections.unmodifiableMap(roots);
    }

    public int execute(String... args) {
        if (args == null) {
            throw new CommandException(1, "Arguments array cannot be null");
        }
        if (args.length == 0) {
            logger.info("No command provided. Use --help for usage.");
            return 1;
        }

        Map<Option, Object> globalParsed = new LinkedHashMap<>();
        int idx = 0;
        try {
            while (idx < args.length) {
                String token = args[idx];
                if (token.equals(DOUBLE_DASH)) {
                    break;
                }
                if (token.startsWith("-")) {
                    Option matchedGlobal = findGlobalOption(token);
                    if (matchedGlobal == null) {
                        break;
                    }
                    int newIdx = parseOptionToken(token, idx, args, globalOptions, globalShortToLong, globalParsed);
                    if (newIdx == idx) break;
                    idx = newIdx;
                } else {
                    break;
                }
            }
        } catch (CommandException e) {
            logger.error("Global option error: " + e.getMessage(), e);
            return 1;
        }

        String[] remaining = Arrays.copyOfRange(args, idx, args.length);

        if (remaining.length == 0) {
            logger.info(generateGlobalHelp());
            return 0;
        }
        if (autoHelp && remaining.length == 1 && ("--help".equals(remaining[0]) || "-h".equals(remaining[0]))) {
            logger.info(generateGlobalHelp());
            return 0;
        }

        return executeCommand(remaining, globalParsed);
    }

    private Option findGlobalOption(String token) {
        if (token.startsWith("--")) {
            String key = token.substring(2);
            int eq = key.indexOf('=');
            if (eq != -1) key = key.substring(0, eq);
            return globalOptions.get(key);
        } else if (token.startsWith("-") && token.length() > 1) {
            String optStr = token.substring(1);
            int eqIndex = optStr.indexOf('=');
            if (eqIndex != -1) {
                String firstChar = optStr.substring(0, 1);
                String longKey = globalShortToLong.get(firstChar);
                if (longKey != null) {
                    return globalOptions.get(longKey);
                }
                return null;
            }
            if (optStr.length() == 1) {
                String longKey = globalShortToLong.get(optStr);
                if (longKey != null) {
                    return globalOptions.get(longKey);
                }
                return null;
            }
            for (char ch : optStr.toCharArray()) {
                String key = globalShortToLong.get(String.valueOf(ch));
                if (key == null || !globalOptions.containsKey(key)) {
                    return null;
                }
            }
            String firstLongKey = globalShortToLong.get(optStr.substring(0, 1));
            return globalOptions.get(firstLongKey);
        }
        return null;
    }

    private int executeCommand(String[] args, Map<Option, Object> globalParsed) {
        Command current;
        List<String> path = new ArrayList<>();
        Map<Option, Object> preParsed = new LinkedHashMap<>();
        int i = 1;

        try {
            current = roots.get(args[0]);
            if (current == null) {
                throw new UnknownCommandException("Unknown command: " + args[0]);
            }
            path.add(args[0]);

            while (i < args.length) {
                String token = args[i];

                if (token.equals(DOUBLE_DASH)) {
                    break;
                }

                if (autoHelp && ("--help".equals(token) || "-h".equals(token))) {
                    String fullPath = String.join(" ", path);
                    logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                    return 0;
                }

                if (token.startsWith("-")) {
                    i = parseOptionToken(token, i, args, current, preParsed);
                    continue;
                }

                Command next = current.getChildren().get(token);
                if (next != null) {
                    current = next;
                    path.add(token);
                    i++;
                } else {
                    if (!current.getChildren().isEmpty()) {
                        throw new UnknownSubcommandException("Unknown subcommand: " + token);
                    }
                    break;
                }
            }

            String[] remaining = Arrays.copyOfRange(args, i, args.length);

            if (autoHelp) {
                for (String arg : remaining) {
                    if ("--help".equals(arg) || "-h".equals(arg)) {
                        String fullPath = String.join(" ", path);
                        logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                        return 0;
                    }
                }
            }

            if (current.getAction() == null) {
                String fullPath = String.join(" ", path);
                logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                return 0;
            }

            Map<Option, Object> initial = new LinkedHashMap<>(globalParsed);
            initial.putAll(preParsed);

            CommandContext ctx = parseOptions(remaining, current, initial, globalOptions);

            for (Option opt : current.getOptions().values()) {
                if (opt.isRequired() && !ctx.hasOption(opt)) {
                    throw new MissingRequiredOptionException("Missing required option: --" + opt.getLongOpt());
                }
            }

            return current.run(ctx);

        } catch (CommandException e) {
            logger.error("Parameter error: " + e.getMessage(), e);
            String fullPath = path.isEmpty() ? "" : String.join(" ", path);
            logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
            return e.getCode() == 2 ? 2 : 1;
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage(), e);
            e.printStackTrace();
            return 1;
        }
    }

    private int parseOptionToken(String token, int i, String[] args,
                                 Map<String, Option> optionDefs,
                                 Map<String, String> shortToLong,
                                 Map<Option, Object> parsed) {
        if (token.startsWith("--")) {
            String full = token.substring(2);
            String key = full;
            String value = null;
            int eqPos = full.indexOf('=');
            if (eqPos != -1) {
                key = full.substring(0, eqPos);
                value = full.substring(eqPos + 1);
            }
            Option def = optionDefs.get(key);
            if (def == null) {
                throw new UnknownOptionException("Unknown option: --" + key);
            }
            if (def.hasArg()) {
                if (value != null) {
                    parsed.put(def, convertOptionValue(def, value, "--" + key));
                    return i + 1;
                } else if (i + 1 < args.length) {
                    String nextValue = args[i + 1];
                    parsed.put(def, convertOptionValue(def, nextValue, "--" + key));
                    return i + 2;
                } else {
                    throw new OptionValueMissingException("Option --" + key + " requires a value");
                }
            } else {
                if (value != null) {
                    throw new UnknownOptionException("Option --" + key + " does not take a value");
                }
                parsed.put(def, true);
                return i + 1;
            }
        }

        if (token.startsWith("-") && token.length() > 1) {
            String optStr = token.substring(1);
            int eqIndex = optStr.indexOf('=');

            if (eqIndex != -1) {
                String optionChar = optStr.substring(0, 1);
                String value = optStr.substring(eqIndex + 1);
                String longKey = shortToLong.get(optionChar);
                if (longKey == null) {
                    throw new UnknownOptionException("Unknown option: -" + optionChar);
                }
                Option def = optionDefs.get(longKey);
                if (def == null) {
                    throw new UnknownOptionException("Unknown option: -" + optionChar);
                }
                if (!def.hasArg()) {
                    throw new UnknownOptionException("Option -" + optionChar + " does not take a value");
                }
                if (value.isEmpty()) {
                    throw new OptionValueMissingException("Option -" + optionChar + " requires a value");
                }
                parsed.put(def, convertOptionValue(def, value, "--" + longKey));
                return i + 1;
            }

            if (optStr.length() == 1) {
                String longKey = shortToLong.get(optStr);
                if (longKey == null) {
                    throw new UnknownOptionException("Unknown option: -" + optStr);
                }
                Option def = optionDefs.get(longKey);
                if (def.hasArg()) {
                    if (i + 1 < args.length) {
                        String value = args[i + 1];
                        parsed.put(def, convertOptionValue(def, value, "-" + optStr));
                        return i + 2;
                    } else {
                        throw new OptionValueMissingException("Option -" + optStr + " requires a value");
                    }
                } else {
                    parsed.put(def, true);
                    return i + 1;
                }
            }

            String firstChar = optStr.substring(0, 1);
            String firstLongKey = shortToLong.get(firstChar);
            Option firstDef = (firstLongKey != null) ? optionDefs.get(firstLongKey) : null;
            if (firstDef != null && firstDef.hasArg()) {
                String value = optStr.substring(1);
                if (value.isEmpty()) {
                    throw new OptionValueMissingException("Option -" + firstChar + " requires a value");
                }
                parsed.put(firstDef, convertOptionValue(firstDef, value, "-" + firstChar));
                return i + 1;
            }

            for (char ch : optStr.toCharArray()) {
                String key = shortToLong.get(String.valueOf(ch));
                if (key == null) {
                    throw new UnknownOptionException("Unknown short option: -" + ch);
                }
                Option def = optionDefs.get(key);
                if (def == null) {
                    throw new UnknownOptionException("Unknown short option: -" + ch);
                }
                if (def.hasArg()) {
                    throw new OptionValueMissingException("Option -" + ch + " requires a value, but is in a combined short option.");
                }
                parsed.put(def, true);
            }
            return i + 1;
        }

        throw new UnknownOptionException("Invalid option token: " + token);
    }

    private int parseOptionToken(String token, int i, String[] args,
                                 Command cmd, Map<Option, Object> preParsed) {
        return parseOptionToken(token, i, args, cmd.getOptions(), cmd.getShortToLongMap(), preParsed);
    }

    private Object convertOptionValue(Option def, String value, String optionDisplay) {
        try {
            return def.convert(value);
        } catch (NumberFormatException e) {
            throw new OptionValueInvalidException(
                    "Invalid value '" + value + "' for option " + optionDisplay + " (expected numeric type)");
        } catch (Exception e) {
            throw new OptionValueInvalidException("Invalid value for option " + optionDisplay + ": " + e.getMessage());
        }
    }

    private CommandContext parseOptions(String[] args, Command cmd,
                                        Map<Option, Object> initial,
                                        Map<String, Option> globalOptions) {
        Map<String, Option> allOptions = new LinkedHashMap<>(cmd.getOptions());
        allOptions.putAll(globalOptions);

        Map<Option, Object> parsed = new LinkedHashMap<>();
        for (Option opt : cmd.getOptions().values()) {
            if (opt.getDefaultValue() != null && !initial.containsKey(opt)) {
                parsed.put(opt, convertOptionValue(opt, opt.getDefaultValue(), "--" + opt.getLongOpt()));
            }
        }
        parsed.putAll(initial);

        List<String> positional = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals(DOUBLE_DASH)) {
                for (int j = i + 1; j < args.length; j++) {
                    positional.add(args[j]);
                }
                break;
            }
            if (arg.startsWith("-")) {
                i = parseOptionToken(arg, i, args, cmd, parsed);
            } else {
                positional.add(arg);
                i++;
            }
        }

        return new CommandContext(parsed, positional, allOptions, this.converters);
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

    public static class Builder {
        private final Map<String, Command> roots = new LinkedHashMap<>();
        private final Map<String, Option> globalOptions = new LinkedHashMap<>();
        private Logger logger;
        private HelpGenerator helpGenerator;
        private final List<TypeConverter> converters = new ArrayList<>();

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder converter(TypeConverter converter) {
            this.converters.add(converter);
            return this;
        }

        public Builder converters(List<TypeConverter> converters) {
            this.converters.addAll(converters);
            return this;
        }

        public Builder helpGenerator(HelpGenerator hg) {
            this.helpGenerator = hg;
            return this;
        }

        public Builder option(Option option) {
            if (option.isGlobal()) {
                globalOptions.put(option.getLongOpt(), option);
            } else {
                throw new CommandBuildException(
                        "Option '" + option.getLongOpt() + "' is not global. " +
                                "Only global options can be registered at CLI level. " +
                                "Please set .global(true) in Option builder."
                );
            }
            return this;
        }

        public Builder command(String name, String description, java.util.function.Consumer<CommandBuilder> consumer) {
            CommandBuilder cb = new CommandBuilder(name, description);
            consumer.accept(cb);
            roots.put(name, cb.build());
            return this;
        }

        public Builder command(String name, java.util.function.Consumer<CommandBuilder> consumer) {
            return command(name, "No description", consumer);
        }

        public CLI build() {
            return new CLI(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}