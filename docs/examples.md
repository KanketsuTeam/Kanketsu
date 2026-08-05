# 💡 Examples

This guide demonstrates common CLI patterns built with Kanketsu.

Instead of documenting every API, it focuses on solving practical problems you are likely to encounter when building command-line applications.

---

# 👋 Hello World

The smallest possible Kanketsu application.

```java
CLI.builder()
    .command("hello", command -> command
        .action(ctx -> System.out.println("Hello, Kanketsu!")))
    .build()
    .execute("hello");
```

Output:

```text
Hello, Kanketsu!
```

---

# 🌳 Nested Commands

Commands can be nested naturally to build a command tree.

```java
CLI cli = CLI.builder()
        .command("git", git -> git
                .command("commit", commit -> commit
                        .action(ctx -> System.out.println("Commit created"))))
        .build();

cli.execute("git", "commit");
```

This structure represents:

```text
git
└── commit
```

Nested commands are one of the core building blocks of Kanketsu.

---

# ⚙️ Command Options

Options customize how a command behaves.

```java
CLI cli = CLI.builder()
        .command("echo", command -> command
                .option("message", option -> option
                        .shortOpt("m")
                        .converter(Converters.STRING)
                        .hasArg(true)
                        .description("Message to print"))
                .action(ctx -> {
                    String message = ctx.getOptionValue("message", String.class);
                    System.out.println(message);
                }))
        .build();
```

Execute using either the long or short option:

```text
echo --message "Hello"
```

or

```text
echo -m "Hello"
```

---

# 🚩 Boolean Flags

Boolean options act as feature switches.

```java
.option("verbose", option -> option
    .shortOpt("v")
    .converter(Converters.BOOLEAN)
    .description("Enable verbose output"))
```

Example:

```text
tool --verbose
```

or

```text
tool -v
```

Inside the command:

```java
boolean verbose = ctx.getOptionValue("verbose", Boolean.class);
```

---

# ✅ Required Options

Mark important options as required.

```java
.option("title", option -> option
    .shortOpt("t")
    .converter(Converters.STRING)
    .hasArg(true)
    .required(true)
    .description("Issue title"))
```

Running the command without the required option will produce an error before the command is executed.

---

# 🎁 Default Values

Options can provide default values when the user does not specify one.

```java
.option("branch", option -> option
    .converter(Converters.STRING)
    .hasArg(true)
    .defaultValue("main")
    .description("Target branch"))
```

Then:

```java
String branch = ctx.getOptionValue("branch", String.class);
```

returns `"main"` if no value was provided.

---

# 📍 Positional Arguments

Arguments that are not parsed as options become positional arguments.

For example:

```text
copy source.txt destination.txt
```

Inside the command:

```java
List<String> args = ctx.getPositionalArgs();

String source = args.get(0);
String destination = args.get(1);
```

Positional arguments preserve their original order.

---

# 🆘 Built-in Help

Every command automatically provides help output.

For example:

```text
tool --help
```

```text
tool build --help
```

```text
tool issue create --help
```

The generated help includes:

* command usage;
* command description;
* available options;
* required options;
* default values;
* available subcommands.

No additional code is required.

---

# 🚀 What's Next?

Now that you've seen the most common CLI patterns, continue with:

* 🧠 **Concepts** — Learn how Kanketsu models commands and execution.
* 🏛 **Design** — Understand the principles behind the framework.
* 📦 **Modules** — Extend Kanketsu with optional components.