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
package io.github.fascesaedi.kanketsu.examples;

import com.github.cliftonlabs.json_simple.JsonObject;
import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.core.converter.Converters;
import io.github.fascesaedi.kanketsu.json.JsonTypeConverter;

/**
 * Example demonstrating the usage of kanketsu-json module.
 * <p>
 * The JSON converter automatically detects whether the input is a JSON string
 * (starting with '{' or '[') or a file path, and handles it accordingly.
 * </p>
 */
public class JsonExample {

    public static void main(String[] args) {
        CLI cli = CLI.builder()
                .converter(JsonTypeConverter.INSTANCE)
                .command("parse", "Parse JSON and display host and port", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true)
                                .description("JSON string or path to JSON file"))
                        .action(ctx -> {
                            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                            String host = config.getOrDefault("host", "unknown").toString();
                            Object portObj = config.getOrDefault("port", 0L);
                            long port = portObj instanceof Number ? ((Number) portObj).longValue() : 0L;
                            System.out.println("Host: " + host);
                            System.out.println("Port: " + port);
                        })
                )
                .build();

        System.exit(cli.execute(args));
    }
}