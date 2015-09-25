/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.ide.visualstudio.tasks;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Transformer;
import org.gradle.api.XmlProvider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.internal.DefaultVisualStudioProject;
import org.gradle.ide.visualstudio.tasks.internal.RelativeFileNameTransformer;
import org.gradle.ide.visualstudio.tasks.internal.VisualStudioProjectFile;
import org.gradle.plugins.ide.api.XmlGeneratorTask;

import java.io.File;
import java.util.concurrent.Callable;

@Incubating
class GenerateProjectFileTask extends XmlGeneratorTask<VisualStudioProjectFile> {
    private DefaultVisualStudioProject vsProject;
    private String gradleExe;
    private String gradleArgs;

    public void setGradleExe(String gradleExe) {
        this.gradleExe = gradleExe;
    }

    @Input
    public String getGradleExe() {
        return gradleExe;
    }

    public void setGradleArgs(String gradleArgs) {
        this.gradleArgs = gradleArgs;
    }

    @Input
    @Optional
    public String getGradleArgs() {
        return gradleArgs;
    }

    public void initGradleCommand() {
        final File gradlew = getProject().getRootProject().file("gradlew.bat");
        getConventionMapping().map("gradleExe", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String rootDir = getTransformer().transform(getProject().getRootDir());
                String args = "";
                if (rootDir != ".") {
                    args = " -p \"" + rootDir + "\"";
                }
                if (gradlew.isFile()) {
                    return getTransformer().transform(gradlew) + args;
                }
                return "gradle" + args;
            }
        });
    }

    public Transformer<String, File> getTransformer() {
        return RelativeFileNameTransformer.forFile(getProject().getRootDir(), vsProject.getProjectFile().getLocation());
    }

    public void setVisualStudioProject(VisualStudioProject vsProject) {
        this.vsProject = (DefaultVisualStudioProject) vsProject;
    }

    public VisualStudioProject getVisualStudioProject() {
        return vsProject;
    }

    @Override
    public File getInputFile() {
        return null;
    }

    @Override
    public File getOutputFile() {
        return vsProject.getProjectFile().getLocation();
    }

    @Override
    protected VisualStudioProjectFile create() {
        return new VisualStudioProjectFile(getXmlTransformer(), getTransformer());
    }

    @Override
    protected void configure(VisualStudioProjectFile projectFile) {
        projectFile.setGradleCommand(buildGradleCommand());
        projectFile.setProjectUuid(vsProject.getUuid());
        for (File it : vsProject.getSourceFiles()) {
            projectFile.addSourceFile(it);
        }
        for (File it : vsProject.getResourceFiles()) {
            projectFile.addResource(it);
        }
        for (File it : vsProject.getHeaderFiles()) {
            projectFile.addHeaderFile(it);
        }

//        vsProject.configurations.each {
//            projectFile.addConfiguration(it)
//        }

        for (Action<? super XmlProvider> it : vsProject.getProjectFile().getXmlActions()) {
            getXmlTransformer().addAction(it);
        }
    }

    private String buildGradleCommand() {
        String exe = getGradleExe();
        String args = getGradleArgs();
        if (args == null || args.trim().length() == 0) {
            return exe;
        } else {
            return exe + " " + args.trim();
        }
    }
}
