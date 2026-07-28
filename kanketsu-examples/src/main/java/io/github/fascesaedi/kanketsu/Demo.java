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