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
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.command.CommandBuilder;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.*;
import io.github.kanketsuteam.kanketsu.core.exception.*;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.util.*;

public class CLI {
    private static final String DOUBLE_DASH = "--";

    private final Map<String, Command> roots;
    private final Logger logger;
    private final List<TypeConverter> converters;
    private final boolean autoHelp;

    private CLI(Builder builder) {
        this.roots = builder.roots;
        this.logger = builder.logger != null ? builder.logger : Logger.system();
        this.converters = Collections.unmodifiableList(builder.converters);
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

        if (autoHelp && args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            logger.info(generateGlobalHelp());
            return 0;
        }

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
                    logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
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
                        logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
                        return 0;
                    }
                }
            }

            if (current.getAction() == null) {
                String fullPath = String.join(" ", path);
                logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
                return 0;
            }

            CommandContext ctx = parseOptions(remaining, current, preParsed);

            for (Option opt : current.getOptions().values()) {
                if (opt.isRequired() && !ctx.hasOption(opt)) {
                    throw new MissingRequiredOptionException("Missing required option: --" + opt.getLongOpt());
                }
            }

            return current.run(ctx);

        } catch (CommandException e) {
            logger.error("Parameter error: " + e.getMessage());
            String fullPath = path.isEmpty() ? "" : String.join(" ", path);
            logger.info(HelpGenerator.generateDetailedHelp(roots, fullPath));
            if (e.getCode() == 2) {
                return 2;
            } else {
                return 1;
            }
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private Object convertOptionValue(Option def, String value, String optionDisplay) {
        try {
            return def.convert(value);
        } catch (NumberFormatException e) {
            throw new OptionValueInvalidException(
                    "Invalid value '" + value + "' for option " + optionDisplay + " (expected numeric type)"
            );
        } catch (Exception e) {
            throw new OptionValueInvalidException(
                    "Invalid value for option " + optionDisplay + ": " + e.getMessage()
            );
        }
    }

    private int parseOptionToken(String token, int i, String[] args,
                                 Command cmd, Map<Option, Object> preParsed) {
        Map<String, Option> optionDefs = cmd.getOptions();
        Map<String, String> shortToLong = cmd.getShortToLongMap();

        if (token.startsWith("--")) {
            String key = token.substring(2);
            Option def = optionDefs.get(key);
            if (def == null) {
                throw new UnknownOptionException("Unknown option: --" + key);
            }

            if (def.hasArg()) {
                int eqPos = token.indexOf('=');
                if (eqPos != -1) {
                    String value = token.substring(eqPos + 1);
                    preParsed.put(def, convertOptionValue(def, value, "--" + key));
                    return i + 1;
                } else if (i + 1 < args.length) {
                    String value = args[i + 1];
                    preParsed.put(def, convertOptionValue(def, value, "--" + key));
                    return i + 2;
                } else {
                    throw new OptionValueMissingException("Option --" + key + " requires a value");
                }
            } else {
                preParsed.put(def, true);
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
                preParsed.put(def, convertOptionValue(def, value, "--" + longKey));
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
                        preParsed.put(def, convertOptionValue(def, value, "-" + optStr));
                        return i + 2;
                    } else {
                        throw new OptionValueMissingException("Option -" + optStr + " requires a value");
                    }
                } else {
                    preParsed.put(def, true);
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
                preParsed.put(firstDef, convertOptionValue(firstDef, value, "-" + firstChar));
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
                preParsed.put(def, true);
            }
            return i + 1;
        }

        throw new UnknownOptionException("Invalid option token: " + token);
    }

    private CommandContext parseOptions(String[] args, Command cmd, Map<Option, Object> initial) {
        Map<Option, Object> parsed = new LinkedHashMap<>();
        for (Option opt : cmd.getOptions().values()) {
            if (opt.getDefaultValue() != null) {
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

        return new CommandContext(parsed, positional, cmd.getOptions(), this.converters);
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

    // ========== Builder ==========

    public static class Builder {
        private final Map<String, Command> roots = new LinkedHashMap<>();
        private Logger logger;
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