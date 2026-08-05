# 🚀 Getting Started

This guide walks you through building your first command-line application with Kanketsu.

By the end of this guide, you'll know how to:

* 🧩 Create a CLI application.
* 🌳 Register commands.
* ⚙️ Define options.
* 🧾 Access user input.

---

## 📋 Requirements

Before getting started, make sure you have:

* ☕ Java 21 or later
* 📦 Maven

Add the core dependency:

```xml
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## 👋 Your First Command

Every Kanketsu application starts with a `CLI` builder.

```java
CLI.builder()
    .command("hello", cmd -> cmd
        .action(ctx -> System.out.println("Hello, Kanketsu!")))
    .build()
    .execute("hello");
```

Running the application prints:

```text
Hello, Kanketsu!
```

Congratulations! You've created your first Kanketsu application.

---

## 🌳 Building a Command Tree

Real-world CLI applications usually contain multiple commands.

Commands can be nested to form a command tree.

```java
CLI cli = CLI.builder()
    .command("git", git -> git
        .command("commit", commit -> commit
            .action(ctx -> System.out.println("Commit executed"))))
    .build();

cli.execute("git", "commit");
```

This structure represents:

```text
git
└── commit
```

Each command can contain child commands, options, and an action.

---

## ⚙️ Adding Options

Options modify the behavior of a command.

```java
CLI cli = CLI.builder()
    .command("echo", command -> command
        .option("message", option -> option
            .shortOpt("m")
            .hasArg(true)
            .converter(Converters.STRING))
        .action(ctx -> {
            String message = ctx.getOptionValue("message", String.class);
            System.out.println(message);
        }))
    .build();
```

Execute:

```text
echo --message "Hello"
```

or

```text
echo -m "Hello"
```

Both produce the same result.

---

## 📍 Positional Arguments

Arguments that are not parsed as options become positional arguments.

```text
copy source.txt destination.txt
```

They can be accessed through the command context.

```java
List<String> args = ctx.getPositionalArgs();

String source = args.get(0);
String destination = args.get(1);
```

---

## 🚀 What's Next?

You now know how to:

* 🧩 Create a CLI application.
* 🌳 Register commands.
* ⚙️ Define options.
* 📍 Access positional arguments.

Continue with the following guides:

* 💡 **Examples** — Practical CLI examples.
* 🧠 **Concepts** — Learn how Kanketsu works internally.
* 🧩 **Modules** — Explore optional modules such as REPL and Logging.