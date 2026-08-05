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
package io.github.fascesaedi.kanketsu.json;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.core.converter.Converters;
import io.github.fascesaedi.kanketsu.core.exception.OptionValueInvalidException;
import io.github.fascesaedi.kanketsu.spi.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonTypeConverterTest {

    private ByteArrayOutputStream systemOut;
    private PrintStream originalOut;
    private TestLogger testLogger;
    private Path tempJsonFile;

    static class TestLogger implements Logger {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();

        @Override
        public void log(String message) {}

        @Override
        public void info(String msg) { infoMessages.add(msg); }

        @Override
        public void warn(String msg) { warnMessages.add(msg); }

        @Override
        public void error(String msg) { errorMessages.add(msg); }

        @Override
        public boolean isDebugEnabled() { return false; }

        public List<String> getInfoMessages() { return infoMessages; }
        public List<String> getWarnMessages() { return warnMessages; }
        public List<String> getErrorMessages() { return errorMessages; }
    }

    @BeforeEach
    void setUp() throws Exception {
        originalOut = System.out;
        systemOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOut));
        testLogger = new TestLogger();
        System.setProperty("kanketsu.autoHelp", "true");

        tempJsonFile = Files.createTempFile("kanketsu-test-", ".json");
        Files.writeString(tempJsonFile, "{\"host\":\"localhost\",\"port\":8080}");
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.clearProperty("kanketsu.autoHelp");
        if (tempJsonFile != null) {
            Files.deleteIfExists(tempJsonFile);
        }
    }

    @Test
    void convertJsonStringToJsonObject() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                            String host = (String) config.get("host");
                            System.out.println("host=" + host);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "{\"host\":\"example.com\"}");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("host=example.com");
    }

    @Test
    void convertJsonStringToJsonArray() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("items", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            JsonArray array = ctx.getOptionValueAs("items", JsonArray.class);
                            int size = array.size();
                            System.out.println("size=" + size);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--items", "[1,2,3]");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("size=3");
    }

    @Test
    void convertJsonStringWithWhitespace() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                            String name = (String) config.get("name");
                            System.out.println("name=" + name);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "  { \"name\": \"test\" }  ");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("name=test");
    }

    @Test
    void convertJsonFileToJsonObject() throws Exception {
        Path customFile = Files.createTempFile("custom-", ".json");
        Files.writeString(customFile, "{\"env\":\"prod\",\"debug\":true}");
        try {
            CLI cli = CLI.builder()
                    .logger(testLogger)
                    .converter(JsonTypeConverter.INSTANCE)
                    .command("test", cmd -> cmd
                            .option("config", opt -> opt
                                    .hasArg(true)
                                    .converter(Converters.STRING)
                                    .required(true))
                            .action(ctx -> {
                                JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                                String env = (String) config.get("env");
                                System.out.println("env=" + env);
                            })
                    )
                    .build();

            int exitCode = cli.execute("test", "--config", customFile.toString());
            assertThat(exitCode).isEqualTo(0);
            assertThat(systemOut.toString().trim()).isEqualTo("env=prod");
        } finally {
            Files.deleteIfExists(customFile);
        }
    }

    @Test
    void convertJsonFileToJsonArray() throws Exception {
        Path arrayFile = Files.createTempFile("array-", ".json");
        Files.writeString(arrayFile, "[100,200,300]");
        try {
            CLI cli = CLI.builder()
                    .logger(testLogger)
                    .converter(JsonTypeConverter.INSTANCE)
                    .command("test", cmd -> cmd
                            .option("data", opt -> opt
                                    .hasArg(true)
                                    .converter(Converters.STRING)
                                    .required(true))
                            .action(ctx -> {
                                JsonArray array = ctx.getOptionValueAs("data", JsonArray.class);
                                int sum = 0;
                                for (Object v : array) {
                                    sum += ((Number) v).intValue();
                                }
                                System.out.println("sum=" + sum);
                            })
                    )
                    .build();

            int exitCode = cli.execute("test", "--data", arrayFile.toString());
            assertThat(exitCode).isEqualTo(0);
            assertThat(systemOut.toString().trim()).isEqualTo("sum=600");
        } finally {
            Files.deleteIfExists(arrayFile);
        }
    }

    @Test
    void invalidJsonStringThrowsException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("config", JsonObject.class);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "{invalid}");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Invalid JSON") && msg.contains("position"));
    }

    @Test
    void emptyJsonStringThrowsException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("config", JsonObject.class);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("Parameter error: JSON source cannot be null or empty"));
    }

    @Test
    void jsonFileNotFoundThrowsException() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .required(true))
                        .action(ctx -> {
                            ctx.getOptionValueAs("config", JsonObject.class);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "/non/existent/file.json");
        assertThat(exitCode).isEqualTo(2);
        assertThat(testLogger.getErrorMessages())
                .anyMatch(msg -> msg.contains("JSON file not found: /non/existent/file.json"));
    }

    @Test
    void invalidJsonFileContentThrowsException() throws Exception {
        Path invalidFile = Files.createTempFile("invalid-", ".json");
        Files.writeString(invalidFile, "{\"broken\": }");
        try {
            CLI cli = CLI.builder()
                    .logger(testLogger)
                    .converter(JsonTypeConverter.INSTANCE)
                    .command("test", cmd -> cmd
                            .option("config", opt -> opt
                                    .hasArg(true)
                                    .converter(Converters.STRING)
                                    .required(true))
                            .action(ctx -> {
                                ctx.getOptionValueAs("config", JsonObject.class);
                            })
                    )
                    .build();

            int exitCode = cli.execute("test", "--config", invalidFile.toString());
            assertThat(exitCode).isEqualTo(2);
            assertThat(testLogger.getErrorMessages())
                    .anyMatch(msg -> msg.contains("Invalid JSON") && msg.contains("position"));
        } finally {
            Files.deleteIfExists(invalidFile);
        }
    }

    @Test
    void nullSourceThrowsException() {
        JsonTypeConverter converter = JsonTypeConverter.INSTANCE;
        assertThatThrownBy(() -> converter.convert(null, null))
                .isInstanceOf(OptionValueInvalidException.class)
                .hasMessageContaining("JSON source cannot be null");
    }

    @Test
    void supportsJsonObjectAndArray() {
        JsonTypeConverter converter = JsonTypeConverter.INSTANCE;
        assertThat(converter.supports(JsonObject.class)).isTrue();
        assertThat(converter.supports(JsonArray.class)).isTrue();
        assertThat(converter.supports(String.class)).isFalse();
        assertThat(converter.supports(Integer.class)).isFalse();
    }

    @Test
    void converterWorksWithRequiredAndDefault() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .defaultValue("{\"default\":true}"))
                        .action(ctx -> {
                            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                            boolean val = (Boolean) config.get("default");
                            System.out.println("default=" + val);
                        })
                )
                .build();

        int exitCode = cli.execute("test");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("default=true");
    }

    @Test
    void converterOverridesDefault() {
        CLI cli = CLI.builder()
                .logger(testLogger)
                .converter(JsonTypeConverter.INSTANCE)
                .command("test", cmd -> cmd
                        .option("config", opt -> opt
                                .hasArg(true)
                                .converter(Converters.STRING)
                                .defaultValue("{\"default\":true}"))
                        .action(ctx -> {
                            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
                            boolean val = (Boolean) config.get("override");
                            System.out.println("override=" + val);
                        })
                )
                .build();

        int exitCode = cli.execute("test", "--config", "{\"override\":true}");
        assertThat(exitCode).isEqualTo(0);
        assertThat(systemOut.toString().trim()).isEqualTo("override=true");
    }
}