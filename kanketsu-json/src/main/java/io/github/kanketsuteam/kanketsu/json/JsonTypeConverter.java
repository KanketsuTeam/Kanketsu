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
package io.github.kanketsuteam.kanketsu.json;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueInvalidException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonTypeConverter implements TypeConverter {

    public static final JsonTypeConverter INSTANCE = new JsonTypeConverter();

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == JsonObject.class || targetType == JsonArray.class;
    }

    @Override
    public Object convert(String source, CommandContext context) {
        if (source == null || source.isBlank()) {
            throw new OptionValueInvalidException("JSON source cannot be null or empty");
        }

        String trimmed = source.trim();

        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        char firstChar = trimmed.charAt(0);

        String content;
        if (firstChar == '{' || firstChar == '[') {
            content = trimmed;
        } else {
            try {
                Path path = Path.of(trimmed);
                if (!Files.exists(path)) {
                    throw new OptionValueInvalidException("JSON file not found: " + trimmed);
                }
                content = Files.readString(path);
            } catch (IOException e) {
                throw new OptionValueInvalidException("Failed to read JSON file: " + trimmed, e);
            }
        }

        try {
            Object parsed = Jsoner.deserialize(content);
            if (parsed instanceof JsonObject || parsed instanceof JsonArray) {
                return parsed;
            } else {
                throw new OptionValueInvalidException("Parsed JSON is not an object or array: " + parsed.getClass().getName());
            }
        } catch (JsonException e) {
            String message = "Invalid JSON";
            int pos = e.getPosition();
            if (pos >= 0) {
                message += " at position " + pos;
            }
            message += ": " + e.getMessage();
            throw new OptionValueInvalidException(message, e);
        } catch (Exception e) {
            throw new OptionValueInvalidException("Failed to parse JSON", e);
        }
    }
}