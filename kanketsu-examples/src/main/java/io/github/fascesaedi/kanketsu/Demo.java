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
package io.github.fascesaedi.kanketsu;

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.spi.Logger;

public class Demo {
    public static void main(String[] args) {
        Logger logger = Logger.system();

        CLI cli = CLI.builder()
                .logger(logger)
                .command("calc", calc -> calc
                        .option("add", "a", "First number", true)
                        .option("base", "b", "Second number", true)
                        .option("operator", "o", "Operator (+, -)", true, "+")
                        .action(ctx -> {
                            int a = Integer.parseInt(ctx.getOption("add"));
                            int b = Integer.parseInt(ctx.getOption("base"));
                            String op = ctx.getOption("operator");
                            int result = switch (op) {
                                case "+" -> a + b;
                                case "-" -> a - b;
                                default -> throw new IllegalArgumentException("Unsupported op");
                            };
                            logger.success("Result: " + result);
                        })
                )
                .build();

        cli.execute(new String[]{"calc", "--add", "10", "--base", "20", "--operator", "+"});
    }
}