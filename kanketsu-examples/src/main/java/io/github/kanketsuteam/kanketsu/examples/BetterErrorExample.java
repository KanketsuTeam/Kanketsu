package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.core.CLI;
import io.github.kanketsuteam.kanketsu.repl.REPL;
import io.github.kanketsuteam.kanketsu.spi.Logger;

import java.io.IOException;

public class BetterErrorExample {
    static class logger implements Logger{
        @Override
        public void log(String message) {
            System.out.println(message);
        }

        @Override
        public void error(String message, Throwable t, int index, String[] rawValues){
            if (index == -1){
                log(message);
            }else {
                log(message + " rawValue:" + rawValues[index] + " index:" + index);
            }
        }
    }

    public static void main(String[] args){
        Logger logger = new BetterErrorExample.logger();

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
                                            logger.log(ctx.getOptionValueAs("name", String.class));
                                            logger.log(ctx.getOptionValueAs("url", String.class));
                                            logger.log(ctx.getOptionValueAs("track", String.class));
                                            logger.log(ctx.getOptionValueAs("mirror", String.class));
                                        })
                                )
                                .command("remove", remove -> remove
                                        .option("name", opt -> opt
                                                .shortOpt("n")
                                                .description("Remote name")
                                                .hasArg(true))
                                        .action(ctx -> {
                                            logger.log(ctx.getOptionValueAs("name", String.class));
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
                                            logger.log(ctx.getOptionValueAs("name", String.class));
                                            logger.log(ctx.getOptionValueAs("url", String.class));
                                            logger.log(ctx.getOptionValueAs("push", String.class));
                                        })
                                )
                        )
                )
                .build();

        REPL repl;
        try {
            repl = new REPL(cli);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Kanketsu REPL with Tab completion. Type 'exit' to quit.");
        while (true) {
            String input = repl.readLine("kanketsu> ");
            if (input == null || "exit".equalsIgnoreCase(input.trim())) break;
            String[] argsArray = input.trim().split("\\s+");
            if (argsArray.length > 0 && !argsArray[0].isEmpty()) {
                cli.execute(argsArray);
            }
        }
    }
}
