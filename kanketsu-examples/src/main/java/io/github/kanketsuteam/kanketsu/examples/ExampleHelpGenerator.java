package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;

public class ExampleHelpGenerator implements HelpGenerator {
    @Override
    public void output(String helpText) {

    }

    @Override
    public int getCommandWidth() {
        return 30;  // 为长命令名预留更多空间
    }

    @Override
    public int getOptionWidth() {
        return 50;
    }
}
