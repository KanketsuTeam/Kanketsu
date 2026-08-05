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
package io.github.fascesaedi.kanketsu.core.command;

import io.github.fascesaedi.kanketsu.core.Option;
import io.github.fascesaedi.kanketsu.core.exception.OptionValueInvalidException;
import io.github.fascesaedi.kanketsu.spi.TypeConverter;

import java.util.*;

public class CommandContext {
    private final Map<Option, Object> options;
    private final List<String> positionalArgs;
    private final Map<String, Option> optionDefs;
    private final List<TypeConverter> converters;

    public CommandContext(Map<Option, Object> options, List<String> positionalArgs, Map<String, Option> optionDefs, List<TypeConverter> converters) {
        this.options = new LinkedHashMap<>(options);
        this.positionalArgs = new ArrayList<>(positionalArgs);
        this.optionDefs = new LinkedHashMap<>(optionDefs);
        this.converters = converters != null ? new ArrayList<>(converters) : Collections.emptyList();
    }

    public Map<Option, Object> getOptions() { return options; }
    public List<String> getPositionalArgs() { return positionalArgs; }
    public String getPositionalArg(int pos) { return positionalArgs.get(pos); }

    @Deprecated
    public Object getOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        return opt == null ? null : options.get(opt);
    }

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

    public boolean hasOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        return opt != null && options.containsKey(opt);
    }

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

    public <T> T getOptionValueAs(String longOpt, Class<T> targetType, T defaultValue) {
        T value = getOptionValueAs(longOpt, targetType);
        return value != null ? value : defaultValue;
    }

    public boolean hasOption(Option opt) {
        return options.containsKey(opt);
    }
}