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

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.repl.REPL;
import io.github.fascesaedi.kanketsu.spi.Logger;

import java.io.IOException;

public class ReplTest {
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.system();

        CLI cli = CLI.builder()
                .logger(logger)
                .command("git", git -> git
                        .command("remote", remote -> remote
                                .command("add", add -> add
                                        .option("name", opt -> opt
                                                .shortOpt("n")
                                                .description("Remote name")
                                                .hasArg(true))
                                        .option("url", opt -> opt
                                                .shortOpt("u")
                                                .description("Remote URL")
                                                .hasArg(true))
                                        .option("track", opt -> opt
                                                .shortOpt("t")
                                                .description("Track branch")
                                                .hasArg(true)
                                                .defaultValue("main"))
                                        .option("mirror", opt -> opt
                                                .shortOpt("m")
                                                .description("Mirror"))
                                        .action(ctx -> {
                                            logger.log((String) ctx.getOption("name"));
                                            logger.log((String)ctx.getOption("url"));
                                            logger.log((String)ctx.getOption("track"));
                                            logger.log((String)ctx.getOption("mirror"));
                                        })
                                )
                                .command("remove", remove -> remove
                                        .option("name", opt -> opt
                                                .shortOpt("n")
                                                .description("Remote name")
                                                .hasArg(true))
                                        .action(ctx -> {
                                            logger.log((String)ctx.getOption("name"));
                                        })
                                )
                                .command("set-url", setUrl -> setUrl
                                        .option("name", opt -> opt
                                                .shortOpt("n")
                                                .description("Remote name")
                                                .hasArg(true))
                                        .option("url", opt -> opt
                                                .shortOpt("u")
                                                .description("New URL")
                                                .hasArg(true))
                                        .option("push", opt -> opt
                                                .shortOpt("p")
                                                .description("Set push URL")
                                                .hasArg(false))
                                        .action(ctx -> {
                                            logger.log((String)ctx.getOption("name"));
                                            logger.log((String)ctx.getOption("url"));
                                            logger.log((String)ctx.getOption("push"));
                                        })
                                )
                        )
                )
                .build();
        REPL repl = new REPL(cli);

        System.out.println("Kanketsu REPL with Tab completion. Type 'exit' to quit.");
        while (true) {
            String input = repl.readLine("kanketsu");
            if (input == null || "exit".equalsIgnoreCase(input.trim())) break;
            String[] argsArray = input.trim().split("\\s+");
            if (argsArray.length > 0 && !argsArray[0].isEmpty()) {
                cli.execute(argsArray);
            }
        }
    }
}