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
package io.github.kanketsuteam.cmp;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Mojo(name = "generate")
public class GenerateCompletionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "cliClass", required = true)
    private String cliClass;

    @Parameter(property = "cliField", defaultValue = "cli")
    private String cliField;

    @Parameter(property = "shell", defaultValue = "bash")
    private String shell;

    @Parameter(property = "outputFile")
    private File outputFile;

    @Parameter(property = "commandName", defaultValue = "")
    private String commandName;

    private static class OptionInfo {
        String longOpt;
        String shortOpt;
        String description;
        OptionInfo(String longOpt, String shortOpt, String desc) {
            this.longOpt = longOpt;
            this.shortOpt = shortOpt;
            this.description = desc;
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating " + shell + " completion script...");
        if (commandName == null || commandName.isEmpty()) {
            commandName = project.getArtifactId();
        }

        if (outputFile == null) {
            outputFile = new File(project.getBuild().getDirectory(), commandName + "_completion." + shell);
        }
        outputFile.getParentFile().mkdirs();

        ClassLoader projectClassLoader = createProjectClassLoader();

        try {
            Class<?> clazz = Class.forName(cliClass, true, projectClassLoader);
            Field field = clazz.getField(cliField);
            Object cliInstance = field.get(null);

            Method getRoots = cliInstance.getClass().getMethod("getRootCommands");
            Object rootsObj = getRoots.invoke(cliInstance);
            @SuppressWarnings("unchecked")
            Map<String, Object> roots = (Map<String, Object>) rootsObj;

            Map<String, List<String>> childrenMap = new HashMap<>();
            Map<String, List<OptionInfo>> optionsMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : roots.entrySet()) {
                String rootName = entry.getKey();
                Object rootCmd = entry.getValue();
                traverse(rootName, rootCmd, childrenMap, optionsMap);
            }

            String scriptContent;
            switch (shell.toLowerCase()) {
                case "bash":
                    scriptContent = generateBash(roots.keySet(), childrenMap, optionsMap);
                    break;
                case "zsh":
                    scriptContent = generateZsh(childrenMap, optionsMap);
                    break;
                case "fish":
                    scriptContent = generateFish(childrenMap, optionsMap);
                    break;
                default:
                    throw new MojoExecutionException("Unsupported shell: " + shell);
            }

            scriptContent = scriptContent.replaceAll("\\bmyapp\\b", commandName);

            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(scriptContent);
            }
            getLog().info("Completion script generated: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate completion", e);
        }
    }

    private void traverse(String path, Object cmd,
                          Map<String, List<String>> children,
                          Map<String, List<OptionInfo>> options) throws Exception {
        Method getChildren = cmd.getClass().getMethod("getChildren");
        Object childMap = getChildren.invoke(cmd);
        if (childMap instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) childMap;
            List<String> childNames = new ArrayList<>();
            for (Object key : map.keySet()) {
                childNames.add(key.toString());
            }
            children.put(path, childNames);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childName = entry.getKey().toString();
                Object childCmd = entry.getValue();
                String childPath = path.isEmpty() ? childName : path + " " + childName;
                traverse(childPath, childCmd, children, options);
            }
        } else {
            children.put(path, Collections.emptyList());
        }

        Method getOptions = cmd.getClass().getMethod("getOptions");
        Object optMap = getOptions.invoke(cmd);
        if (optMap instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) optMap;
            List<OptionInfo> optInfos = new ArrayList<>();
            for (Object opt : map.values()) {
                Method getLong = opt.getClass().getMethod("getLongOpt");
                Method getShort = opt.getClass().getMethod("getShortOpt");
                Method getDesc = opt.getClass().getMethod("getDescription");
                String longOpt = (String) getLong.invoke(opt);
                String shortOpt = (String) getShort.invoke(opt);
                String desc = (String) getDesc.invoke(opt);
                optInfos.add(new OptionInfo(longOpt, shortOpt, desc));
            }
            options.put(path, optInfos);
        } else {
            options.put(path, Collections.emptyList());
        }
    }

    private ClassLoader createProjectClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            List<URL> urls = new ArrayList<>();
            for (String element : classpathElements) {
                File file = new File(element);
                if (file.exists()) {
                    urls.add(file.toURI().toURL());
                }
            }
            return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create project class loader", e);
        }
    }

    private String generateBash(Set<String> rootNames,
                                Map<String, List<String>> childrenMap,
                                Map<String, List<OptionInfo>> optionsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");
        sb.append("_myapp_completion() {\n");
        sb.append("    local cur=${COMP_WORDS[COMP_CWORD]}\n");
        sb.append("    local words=(\"${COMP_WORDS[@]}\")\n");
        sb.append("    local wordIndex=$COMP_CWORD\n");
        sb.append("    local cmdPath=\"\"\n");
        sb.append("    for ((i=1; i<wordIndex; i++)); do\n");
        sb.append("        local w=\"${words[i]}\"\n");
        sb.append("        if [[ ! \"$w\" =~ ^- ]]; then\n");
        sb.append("            if [[ -z \"$cmdPath\" ]]; then\n");
        sb.append("                cmdPath=\"$w\"\n");
        sb.append("            else\n");
        sb.append("                cmdPath=\"$cmdPath $w\"\n");
        sb.append("            fi\n");
        sb.append("        fi\n");
        sb.append("    done\n\n");

        sb.append("    if [[ -z \"$cmdPath\" ]]; then\n");
        sb.append("        local roots=\"").append(String.join(" ", rootNames)).append("\"\n");
        sb.append("        COMPREPLY=($(compgen -W \"$roots\" -- \"$cur\"))\n");
        sb.append("        return 0\n");
        sb.append("    fi\n\n");

        sb.append("    if [[ \"$cur\" =~ ^- ]]; then\n");
        sb.append("        local opts=$(get_options \"$cmdPath\")\n");
        sb.append("        COMPREPLY=($(compgen -W \"$opts\" -- \"$cur\"))\n");
        sb.append("    else\n");
        sb.append("        local children=$(get_children \"$cmdPath\")\n");
        sb.append("        COMPREPLY=($(compgen -W \"$children\" -- \"$cur\"))\n");
        sb.append("    fi\n");
        sb.append("}\n\n");

        sb.append("get_children() {\n");
        sb.append("    case \"$1\" in\n");
        for (Map.Entry<String, List<String>> entry : childrenMap.entrySet()) {
            String path = entry.getKey();
            if (path.isEmpty()) continue;
            List<String> children = entry.getValue();
            sb.append("        \"").append(path).append("\") echo \"").append(String.join(" ", children)).append("\" ;;\n");
        }
        sb.append("        *) echo \"\" ;;\n");
        sb.append("    esac\n");
        sb.append("}\n\n");

        sb.append("get_options() {\n");
        sb.append("    case \"$1\" in\n");
        for (Map.Entry<String, List<OptionInfo>> entry : optionsMap.entrySet()) {
            String path = entry.getKey();
            List<OptionInfo> opts = entry.getValue();
            if (opts.isEmpty()) {
                sb.append("        \"").append(path).append("\") echo \"\" ;;\n");
            } else {
                List<String> optStrs = new ArrayList<>();
                for (OptionInfo o : opts) {
                    if (o.longOpt != null && !o.longOpt.isEmpty()) optStrs.add("--" + o.longOpt);
                    if (o.shortOpt != null && !o.shortOpt.isEmpty()) optStrs.add("-" + o.shortOpt);
                }
                sb.append("        \"").append(path).append("\") echo \"").append(String.join(" ", optStrs)).append("\" ;;\n");
            }
        }
        sb.append("        *) echo \"\" ;;\n");
        sb.append("    esac\n");
        sb.append("}\n\n");

        sb.append("complete -F _myapp_completion myapp\n");
        return sb.toString();
    }

    private String generateZsh(Map<String, List<String>> childrenMap,
                               Map<String, List<OptionInfo>> optionsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("#compdef myapp\n\n");
        sb.append("_myapp() {\n");
        sb.append("    local curcontext=\"$curcontext\" state line\n");
        sb.append("    typeset -A opt_args\n");
        sb.append("    local cmdPath=\"\"\n");
        sb.append("    local i\n");
        sb.append("    for ((i=1; i<CURRENT; i++)); do\n");
        sb.append("        local w=\"${words[i]}\"\n");
        sb.append("        if [[ ! \"$w\" =~ ^- ]]; then\n");
        sb.append("            if [[ -z \"$cmdPath\" ]]; then\n");
        sb.append("                cmdPath=\"$w\"\n");
        sb.append("            else\n");
        sb.append("                cmdPath=\"$cmdPath $w\"\n");
        sb.append("            fi\n");
        sb.append("        fi\n");
        sb.append("    done\n\n");

        sb.append("    if [[ \"$words[CURRENT]\" =~ ^- ]]; then\n");
        sb.append("        local opts=$(get_options_zsh \"$cmdPath\")\n");
        sb.append("        compadd -X \"Options\" -- ${(z)opts}\n");
        sb.append("    else\n");
        sb.append("        local children=$(get_children_zsh \"$cmdPath\")\n");
        sb.append("        if [[ -n \"$cmdPath\" ]]; then\n");
        sb.append("            compadd -X \"Subcommands\" -- ${(z)children}\n");
        sb.append("        else\n");
        sb.append("            compadd -X \"Commands\" -- ${(z)children}\n");
        sb.append("        fi\n");
        sb.append("    fi\n");
        sb.append("}\n\n");

        // get_children_zsh
        sb.append("get_children_zsh() {\n");
        sb.append("    case \"$1\" in\n");
        for (Map.Entry<String, List<String>> entry : childrenMap.entrySet()) {
            String path = entry.getKey();
            if (path.isEmpty()) continue;
            List<String> children = entry.getValue();
            sb.append("        \"").append(path).append("\") echo \"").append(String.join(" ", children)).append("\" ;;\n");
        }
        sb.append("        *) echo \"\" ;;\n");
        sb.append("    esac\n");
        sb.append("}\n\n");

        // get_options_zsh
        sb.append("get_options_zsh() {\n");
        sb.append("    case \"$1\" in\n");
        for (Map.Entry<String, List<OptionInfo>> entry : optionsMap.entrySet()) {
            String path = entry.getKey();
            List<OptionInfo> opts = entry.getValue();
            if (opts.isEmpty()) {
                sb.append("        \"").append(path).append("\") echo \"\" ;;\n");
            } else {
                List<String> optStrs = new ArrayList<>();
                for (OptionInfo o : opts) {
                    if (o.longOpt != null && !o.longOpt.isEmpty()) optStrs.add("--" + o.longOpt);
                    if (o.shortOpt != null && !o.shortOpt.isEmpty()) optStrs.add("-" + o.shortOpt);
                }
                sb.append("        \"").append(path).append("\") echo \"").append(String.join(" ", optStrs)).append("\" ;;\n");
            }
        }
        sb.append("        *) echo \"\" ;;\n");
        sb.append("    esac\n");
        sb.append("}\n\n");

        sb.append("compdef _myapp myapp\n");
        return sb.toString();
    }

    private String generateFish(Map<String, List<String>> childrenMap,
                                Map<String, List<OptionInfo>> optionsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Fish completion for myapp\n\n");

        sb.append("function __fish_myapp_using_command\n");
        sb.append("    set -l args (commandline -opc)\n");
        sb.append("    set -e args[1]\n");
        sb.append("    set -l path $argv[1]\n");
        sb.append("    if test (count $path) -eq 0\n");
        sb.append("        if test (count $args) -eq 0\n");
        sb.append("            return 0\n");
        sb.append("        else\n");
        sb.append("            return 1\n");
        sb.append("        end\n");
        sb.append("    end\n");
        sb.append("    if test (count $args) -lt (count $path)\n");
        sb.append("        return 1\n");
        sb.append("    end\n");
        sb.append("    for i in (seq 1 (count $path))\n");
        sb.append("        if test $args[$i] != $path[$i]\n");
        sb.append("            return 1\n");
        sb.append("        end\n");
        sb.append("    end\n");
        sb.append("    return 0\n");
        sb.append("end\n\n");

        for (Map.Entry<String, List<String>> entry : childrenMap.entrySet()) {
            String path = entry.getKey();
            List<String> children = entry.getValue();
            if (children.isEmpty()) continue;
            String condition = "__fish_myapp_using_command '" + path + "'";
            sb.append("complete -c myapp -n \"").append(condition).append("\" -f -a \"");
            sb.append(String.join(" ", children));
            sb.append("\"\n");
        }

        for (Map.Entry<String, List<OptionInfo>> entry : optionsMap.entrySet()) {
            String path = entry.getKey();
            List<OptionInfo> opts = entry.getValue();
            if (opts.isEmpty()) continue;
            String condition = "__fish_myapp_using_command '" + path + "'";
            for (OptionInfo opt : opts) {
                StringBuilder optCmd = new StringBuilder("complete -c myapp -n \"" + condition + "\"");
                if (opt.longOpt != null && !opt.longOpt.isEmpty()) {
                    optCmd.append(" -l ").append(opt.longOpt);
                }
                if (opt.shortOpt != null && !opt.shortOpt.isEmpty()) {
                    optCmd.append(" -s ").append(opt.shortOpt);
                }
                if (opt.description != null && !opt.description.isEmpty()) {
                    optCmd.append(" -d \"").append(opt.description.replace("\"", "\\\"")).append("\"");
                }
                optCmd.append("\n");
                sb.append(optCmd.toString());
            }
        }

        return sb.toString();
    }
}