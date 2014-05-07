/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.toolchain.internal.gcc;

import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.nativebinaries.internal.BinaryToolSpec;
import org.gradle.nativebinaries.internal.LinkerSpec;
import org.gradle.nativebinaries.internal.StaticLibraryArchiverSpec;
import org.gradle.nativebinaries.language.c.internal.CCompileSpec;
import org.gradle.nativebinaries.language.cpp.internal.CppCompileSpec;
import org.gradle.nativebinaries.language.objectivec.internal.ObjectiveCCompileSpec;
import org.gradle.nativebinaries.language.objectivecpp.internal.ObjectiveCppCompileSpec;
import org.gradle.nativebinaries.toolchain.internal.*;
import org.gradle.nativebinaries.toolchain.internal.tools.GccCommandLineToolConfigurationInternal;
import org.gradle.nativebinaries.toolchain.internal.tools.ToolRegistry;
import org.gradle.nativebinaries.toolchain.internal.tools.ToolSearchPath;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.util.TreeVisitor;

class GccPlatformToolChain implements PlatformToolChain {
    private final ToolSearchPath toolSearchPath;
    private final ToolRegistry toolRegistry;
    private final ExecActionFactory execActionFactory;
    private final boolean useCommandFile;

    GccPlatformToolChain(ToolSearchPath toolSearchPath, ToolRegistry toolRegistry, ExecActionFactory execActionFactory, boolean useCommandFile) {
        this.toolRegistry = toolRegistry;
        this.toolSearchPath = toolSearchPath;
        this.execActionFactory = execActionFactory;
        this.useCommandFile = useCommandFile;
    }

    public boolean isAvailable() {
        return true;
    }

    public void explain(TreeVisitor<? super String> visitor) {
    }

    public <T extends BinaryToolSpec> Compiler<T> createCppCompiler() {
        GccCommandLineToolConfigurationInternal cppCompilerTool = toolRegistry.getTool(ToolType.CPP_COMPILER);
        CommandLineTool commandLineTool = commandLineTool(cppCompilerTool);
        CppCompiler cppCompiler = new CppCompiler(commandLineTool, commandLineToolInvocation(cppCompilerTool), useCommandFile);
        return (Compiler<T>) new OutputCleaningCompiler<CppCompileSpec>(cppCompiler, getOutputFileSuffix());
    }

    public <T extends BinaryToolSpec> Compiler<T> createCCompiler() {
        GccCommandLineToolConfigurationInternal cCompilerTool = toolRegistry.getTool(ToolType.C_COMPILER);
        CommandLineTool commandLineTool = commandLineTool(cCompilerTool);
        CCompiler cCompiler = new CCompiler(commandLineTool, commandLineToolInvocation(cCompilerTool), useCommandFile);
        return (Compiler<T>) new OutputCleaningCompiler<CCompileSpec>(cCompiler, getOutputFileSuffix());
    }

    public <T extends BinaryToolSpec> Compiler<T> createObjectiveCppCompiler() {
        GccCommandLineToolConfigurationInternal objectiveCppCompilerTool = toolRegistry.getTool(ToolType.OBJECTIVECPP_COMPILER);
        CommandLineTool commandLineTool = commandLineTool(objectiveCppCompilerTool);
        ObjectiveCppCompiler objectiveCppCompiler = new ObjectiveCppCompiler(commandLineTool, commandLineToolInvocation(objectiveCppCompilerTool), useCommandFile);
        return (Compiler<T>) new OutputCleaningCompiler<ObjectiveCppCompileSpec>(objectiveCppCompiler, getOutputFileSuffix());
    }

    public <T extends BinaryToolSpec> Compiler<T> createObjectiveCCompiler() {
        GccCommandLineToolConfigurationInternal objectiveCCompilerTool = toolRegistry.getTool(ToolType.OBJECTIVEC_COMPILER);
        CommandLineTool commandLineTool = commandLineTool(objectiveCCompilerTool);
        ObjectiveCCompiler objectiveCCompiler = new ObjectiveCCompiler(commandLineTool, commandLineToolInvocation(objectiveCCompilerTool), useCommandFile);
        return (Compiler<T>) new OutputCleaningCompiler<ObjectiveCCompileSpec>(objectiveCCompiler, getOutputFileSuffix());
    }

    public <T extends BinaryToolSpec> Compiler<T> createAssembler() {
        GccCommandLineToolConfigurationInternal assemblerTool = toolRegistry.getTool(ToolType.ASSEMBLER);
        CommandLineTool commandLineTool = commandLineTool(assemblerTool);
        return (Compiler<T>) new Assembler(commandLineTool, commandLineToolInvocation(assemblerTool), getOutputFileSuffix());
    }

    public <T extends BinaryToolSpec> Compiler<T> createWindowsResourceCompiler() {
        throw new RuntimeException("Windows resource compiler is not available");
    }

    public <T extends LinkerSpec> Compiler<T> createLinker() {
        GccCommandLineToolConfigurationInternal linkerTool = toolRegistry.getTool(ToolType.LINKER);
        CommandLineTool commandLineTool = commandLineTool(linkerTool);
        return (Compiler<T>) new GccLinker(commandLineTool, commandLineToolInvocation(linkerTool), useCommandFile);
    }

    public <T extends StaticLibraryArchiverSpec> Compiler<T> createStaticLibraryArchiver() {
        GccCommandLineToolConfigurationInternal staticLibArchiverTool = toolRegistry.getTool(ToolType.STATIC_LIB_ARCHIVER);
        return (Compiler<T>) new ArStaticLibraryArchiver(commandLineTool(staticLibArchiverTool), commandLineToolInvocation(staticLibArchiverTool));
    }

    private String getOutputFileSuffix() {
        return OperatingSystem.current().isWindows() ? ".obj" : ".o";
    }

    private CommandLineTool commandLineTool(GccCommandLineToolConfigurationInternal tool) {
        ToolType key = tool.getToolType();
        String exeName = tool.getExecutable();
        return new CommandLineTool(key.getToolName(), toolSearchPath.locate(key, exeName).getTool(), execActionFactory);
    }

    private CommandLineToolInvocation commandLineToolInvocation(GccCommandLineToolConfigurationInternal staticLibArchiverTool) {
        MutableCommandLineToolInvocation baseInvocation = new DefaultCommandLineToolInvocation();
        // MinGW requires the path to be set
        baseInvocation.addPath(toolSearchPath.getPath());
        baseInvocation.addEnvironmentVar("CYGWIN", "nodosfilewarning");

        baseInvocation.addPostArgsAction(staticLibArchiverTool.getArgAction());
        return baseInvocation;
    }
}
