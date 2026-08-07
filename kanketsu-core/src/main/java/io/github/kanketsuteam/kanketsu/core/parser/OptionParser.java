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
package io.github.kanketsuteam.kanketsu.core.parser;

import io.github.kanketsuteam.kanketsu.core.CLI;
import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.CommandException;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueInvalidException;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueMissingException;
import io.github.kanketsuteam.kanketsu.core.exception.UnknownOptionException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses command-line tokens into options and their values.
 * <p>
 * This class handles the detailed parsing logic for both long options (--foo)
 * and short options (-f), including combined short options (-abc) and
 * value assignment via '=' or space separation.
 * </p>
 * <p>
 * It is used internally by {@link io.github.kanketsuteam.kanketsu.core.CLI}
 * and is not intended to be used directly by end users.
 * </p>
 */
public class OptionParser {
    private final List<TypeConverter> converters;

    /**
     * Constructs an OptionParser with a list of TypeConverters for dynamic conversion.
     *
     * @param converters the list of converters to use for option values
     */
    public OptionParser(List<TypeConverter> converters) {
        this.converters = converters;
    }

    /**
     * Parses a single option token and updates the parsed map.
     * <p>
     * This method handles long options (--key=value, --key value), short options
     * (-k=value, -k value), and combined short options (-abc).
     * </p>
     *
     * @param token       the token to parse (e.g., "--foo", "-f", "--foo=bar")
     * @param i           the current index in the argument array
     * @param args        the full argument array
     * @param optionDefs  a map from long option names to {@link Option} definitions
     * @param shortToLong a map from short option names to long names
     * @param parsed      the map where parsed options and values are stored
     * @return the new index after consuming the token and optional value
     * @throws UnknownOptionException      if the option is not recognized
     * @throws OptionValueMissingException if a value is required but not provided
     * @throws OptionValueInvalidException if the value cannot be converted
     */
    public int parseOptionToken(String token, int i, String[] args,
                                Map<String, Option> optionDefs,
                                Map<String, String> shortToLong,
                                Map<Option, Object> parsed) {
        // --long option handling
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
                throw new UnknownOptionException("Unknown option: --" + key, i);
            }
            if (def.hasArg()) {
                if (value != null) {
                    parsed.put(def, convertOptionValue(def, value, "--" + key, i));
                    return i + 1;
                } else if (i + 1 < args.length) {
                    String nextValue = args[i + 1];
                    parsed.put(def, convertOptionValue(def, nextValue, "--" + key, i));
                    return i + 2;
                } else {
                    throw new OptionValueMissingException("Option --" + key + " requires a value", i);
                }
            } else {
                if (value != null) {
                    throw new UnknownOptionException("Option --" + key + " does not take a value", i);
                }
                parsed.put(def, true);
                return i + 1;
            }
        }

        // short option handling
        if (token.startsWith("-") && token.length() > 1) {
            String optStr = token.substring(1);
            int eqIndex = optStr.indexOf('=');

            // -f=value or -fvalue style (using '=' or concatenated)
            if (eqIndex != -1) {
                String optionChar = optStr.substring(0, 1);
                String value = optStr.substring(eqIndex + 1);
                String longKey = shortToLong.get(optionChar);
                if (longKey == null) {
                    throw new UnknownOptionException("Unknown option: -" + optionChar, i);
                }
                Option def = optionDefs.get(longKey);
                if (def == null) {
                    throw new UnknownOptionException("Unknown option: -" + optionChar, i);
                }
                if (!def.hasArg()) {
                    throw new UnknownOptionException("Option -" + optionChar + " does not take a value", i);
                }
                if (value.isEmpty()) {
                    throw new OptionValueMissingException("Option -" + optionChar + " requires a value", i);
                }
                parsed.put(def, convertOptionValue(def, value, "--" + longKey, i));
                return i + 1;
            }

            // Single short option
            if (optStr.length() == 1) {
                String longKey = shortToLong.get(optStr);
                if (longKey == null) {
                    throw new UnknownOptionException("Unknown option: -" + optStr, i);
                }
                Option def = optionDefs.get(longKey);
                if (def.hasArg()) {
                    if (i + 1 < args.length) {
                        String value = args[i + 1];
                        parsed.put(def, convertOptionValue(def, value, "-" + optStr, i));
                        return i + 2;
                    } else {
                        throw new OptionValueMissingException("Option -" + optStr + " requires a value", i);
                    }
                } else {
                    parsed.put(def, true);
                    return i + 1;
                }
            }

            // Combined short options (e.g., -abc)
            // Check if the first character is an option that takes a value, e.g., -fvalue
            String firstChar = optStr.substring(0, 1);
            String firstLongKey = shortToLong.get(firstChar);
            Option firstDef = (firstLongKey != null) ? optionDefs.get(firstLongKey) : null;
            if (firstDef != null && firstDef.hasArg()) {
                String value = optStr.substring(1);
                if (value.isEmpty()) {
                    throw new OptionValueMissingException("Option -" + firstChar + " requires a value", i);
                }
                parsed.put(firstDef, convertOptionValue(firstDef, value, "-" + firstChar, i));
                return i + 1;
            }

            // Otherwise, each character is a flag option (no values)
            for (char ch : optStr.toCharArray()) {
                String key = shortToLong.get(String.valueOf(ch));
                if (key == null) {
                    throw new UnknownOptionException("Unknown short option: -" + ch, i);
                }
                Option def = optionDefs.get(key);
                if (def == null) {
                    throw new UnknownOptionException("Unknown short option: -" + ch, i);
                }
                if (def.hasArg()) {
                    throw new OptionValueMissingException(
                            "Option -" + ch + " requires a value, but is in a combined short option.", i
                    );
                }
                parsed.put(def, true);
            }
            return i + 1;
        }

        throw new UnknownOptionException("Invalid option token: " + token, i);
    }

    /**
     * Convenience method to parse an option token for a specific command.
     *
     * @param token     the token to parse
     * @param i         the current index
     * @param args      the full argument array
     * @param cmd       the command whose options are used
     * @param preParsed the map to store parsed options
     * @return the new index
     */
    public int parseOptionToken(String token, int i, String[] args,
                                Command cmd, Map<Option, Object> preParsed) {
        return parseOptionToken(token, i, args, cmd.getOptions(), cmd.getShortToLongMap(), preParsed);
    }

    /**
     * Converts a value using the option's converter, handling conversion exceptions.
     *
     * @param def          the option definition
     * @param value        the raw string value
     * @param optionDisplay a display name for error messages (e.g., "--foo")
     * @param position     the position in the argument array
     * @return the converted object
     * @throws OptionValueInvalidException if conversion fails
     */
    private Object convertOptionValue(Option def, String value, String optionDisplay, int position) {
        try {
            return def.convert(value);
        } catch (NumberFormatException e) {
            throw new OptionValueInvalidException(
                    "Invalid value '" + value + "' for option " + optionDisplay + " (expected numeric type)", position);
        } catch (Exception e) {
            throw new OptionValueInvalidException("Invalid value for option " + optionDisplay + ": " + e.getMessage(), position);
        }
    }

    /**
     * Parses the remaining arguments after the command and subcommands have been resolved.
     * <p>
     * It separates options from positional arguments, applies default values, and
     * constructs a {@link CommandContext}.
     * </p>
     *
     * @param args          the remaining arguments
     * @param cmd           the final command to execute
     * @param initial       initial parsed options (e.g., from global options)
     * @param globalOptions global option definitions
     * @return a fully populated {@link CommandContext}
     */
    public CommandContext parseOptions(String[] args, Command cmd,
                                       Map<Option, Object> initial,
                                       Map<String, Option> globalOptions) {
        Map<String, Option> allOptions = new LinkedHashMap<>(cmd.getOptions());
        allOptions.putAll(globalOptions);

        Map<Option, Object> parsed = new LinkedHashMap<>();
        // Apply default values for command options that are not already set
        for (Option opt : cmd.getOptions().values()) {
            if (opt.getDefaultValue() != null && !initial.containsKey(opt)) {
                parsed.put(opt, convertOptionValue(opt, opt.getDefaultValue(), "--" + opt.getLongOpt(), CommandException.POSITION_UNKNOWN));
            }
        }
        parsed.putAll(initial);

        List<String> positional = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals(CLI.DOUBLE_DASH)) {
                // Everything after -- is positional
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
}