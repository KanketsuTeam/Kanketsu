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
package io.github.kanketsuteam.kanketsu.core.command;

import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueInvalidException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.util.*;

/**
 * Contains the parsed result of a command-line invocation for a specific command.
 * <p>
 * It provides access to the parsed option values (both as raw objects and
 * converted types) and positional arguments.
 * </p>
 * <p>
 * Instances of this class are created by the framework and passed to command
 * actions.
 * </p>
 */
public class CommandContext {
    private final Map<Option, Object> options;
    private final List<String> positionalArgs;
    private final Map<String, Option> optionDefs;
    private final List<TypeConverter> converters;

    /**
     * Constructs a new command context.
     *
     * @param options         a map from {@link Option} to its parsed value
     * @param positionalArgs  the list of positional arguments
     * @param optionDefs      a map from option long name to {@link Option} definition
     * @param converters      a list of registered {@link TypeConverter}s for dynamic conversion
     */
    public CommandContext(Map<Option, Object> options, List<String> positionalArgs, Map<String, Option> optionDefs, List<TypeConverter> converters) {
        this.options = new LinkedHashMap<>(options);
        this.positionalArgs = new ArrayList<>(positionalArgs);
        this.optionDefs = new LinkedHashMap<>(optionDefs);
        this.converters = converters != null ? new ArrayList<>(converters) : Collections.emptyList();
    }

    /**
     * Returns the map of parsed options and their values.
     *
     * @return an unmodifiable view of the options map
     */
    public Map<Option, Object> getOptions() { return options; }

    /**
     * Returns the list of positional arguments.
     *
     * @return an unmodifiable list of positional arguments
     */
    public List<String> getPositionalArgs() { return positionalArgs; }

    /**
     * Returns the positional argument at the given index.
     *
     * @param pos the zero-based index
     * @return the positional argument
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String getPositionalArg(int pos) { return positionalArgs.get(pos); }

    /**
     * Returns the raw value of the option with the given long name.
     * <p>
     * This method is deprecated because it returns {@code Object} and does not
     * perform type conversion. Prefer {@link #getOptionValue(String, Class)} or
     * {@link #getOptionValueAs(String, Class)}.
     * </p>
     *
     * @param longOpt the long name of the option
     * @return the parsed value, or {@code null} if the option was not present
     * @deprecated Use {@link #getOptionValue(String, Class)} instead.
     */
    @Deprecated
    public Object getOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        return opt == null ? null : options.get(opt);
    }

    /**
     * Returns the value of the option with the given long name, cast to the specified type.
     * <p>
     * This method only works if the stored value is already an instance of {@code type}.
     * For conversion from a String, use {@link #getOptionValueAs(String, Class)}.
     * </p>
     *
     * @param longOpt the long name of the option
     * @param type    the expected class of the value
     * @param <T>     the type parameter
     * @return the option value, or {@code null} if the option is not present
     * @throws OptionValueInvalidException if the stored value is not of the requested type
     */
    @SuppressWarnings("unchecked")
    public <T> T getOptionValue(String longOpt, Class<T> type) {
        Object val = getOption(longOpt);
        if (val == null) {
            return null;
        }
        if (!type.isInstance(val)) {
            throw new OptionValueInvalidException(
                    "Option '" + longOpt + "' is of type " + val.getClass().getName() +
                            ", cannot cast to " + type.getName()
            );
        }
        return (T) val;
    }

    /**
     * Checks whether the option with the given long name is present (i.e., was specified
     * on the command line or has a default value).
     *
     * @param longOpt the long name of the option
     * @return {@code true} if the option is present, {@code false} otherwise
     */
    public boolean hasOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        return opt != null && options.containsKey(opt);
    }

    /**
     * Returns the value of the option with the given long name, converting it to the
     * target type using the registered {@link TypeConverter}s if necessary.
     * <p>
     * If the stored value is already of the target type, it is returned directly.
     * If it is a String, the first converter that supports the target type is used.
     * </p>
     *
     * @param longOpt    the long name of the option
     * @param targetType the target class to convert to
     * @param <T>        the type parameter
     * @return the converted value, or {@code null} if the option is not present
     * @throws OptionValueInvalidException if conversion fails or no suitable converter is found
     */
    public <T> T getOptionValueAs(String longOpt, Class<T> targetType) {
        Object raw = getOption(longOpt);
        if (raw == null) {
            return null;
        }
        if (targetType.isInstance(raw)) {
            return targetType.cast(raw);
        }
        if (!(raw instanceof String)) {
            throw new OptionValueInvalidException(
                    "Cannot convert non-string raw value for option '" + longOpt + "'"
            );
        }
        for (TypeConverter converter : converters) {
            if (converter.supports(targetType)) {
                try {
                    Object converted = converter.convert((String) raw, this);
                    if (targetType.isInstance(converted)) {
                        return targetType.cast(converted);
                    } else {
                        throw new OptionValueInvalidException(
                                "Converter for " + targetType.getName() +
                                        " returned unexpected type: " + converted.getClass().getName()
                        );
                    }
                } catch (OptionValueInvalidException e) {
                    throw e;
                } catch (Exception e) {
                    throw new OptionValueInvalidException(
                            "Conversion failed for option '" + longOpt + "' to " + targetType.getName()
                    );
                }
            }
        }
        throw new OptionValueInvalidException(
                "No TypeConverter registered for target type: " + targetType.getName()
        );
    }

    /**
     * Returns the value of the option as a specific type, or a default value if the option
     * is not present or conversion yields {@code null}.
     *
     * @param longOpt      the long name of the option
     * @param targetType   the target class to convert to
     * @param defaultValue the value to return if the option is absent
     * @param <T>          the type parameter
     * @return the converted value, or {@code defaultValue} if the option is not present
     * @throws OptionValueInvalidException if conversion fails
     */
    public <T> T getOptionValueAs(String longOpt, Class<T> targetType, T defaultValue) {
        T value = getOptionValueAs(longOpt, targetType);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks whether the given {@link Option} instance is present.
     *
     * @param opt the option to check
     * @return {@code true} if the option is present, {@code false} otherwise
     */
    public boolean hasOption(Option opt) {
        return options.containsKey(opt);
    }
}