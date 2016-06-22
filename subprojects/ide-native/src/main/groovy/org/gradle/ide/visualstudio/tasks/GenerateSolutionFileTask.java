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

import org.gradle.api.Incubating;
import org.gradle.ide.visualstudio.VisualStudioSolution;
import org.gradle.ide.visualstudio.internal.DefaultVisualStudioSolution;
import org.gradle.ide.visualstudio.tasks.internal.VisualStudioSolutionFile;
import org.gradle.plugins.ide.api.GeneratorTask;
import org.gradle.plugins.ide.internal.generator.generator.PersistableConfigurationObjectGenerator;

import java.io.File;

@Incubating
class GenerateSolutionFileTask extends GeneratorTask<VisualStudioSolutionFile> {
    private DefaultVisualStudioSolution solution;

    public GenerateSolutionFileTask() {
        generator = new ConfigurationObjectGenerator();
    }

    public void setVisualStudioSolution(VisualStudioSolution solution) {
        this.solution = (DefaultVisualStudioSolution) solution;

        dependsOn(solution.getProjects());
    }

    public VisualStudioSolution getSolution() {
        return solution;
    }

    @Override
    public File getInputFile() {
        return null;
    }

    @Override
    public File getOutputFile() {
        return this.solution.getSolutionFile().getLocation();
    }

    private class ConfigurationObjectGenerator extends PersistableConfigurationObjectGenerator<VisualStudioSolutionFile> {
        public VisualStudioSolutionFile create() {
            return new VisualStudioSolutionFile();
        }

        public void configure(VisualStudioSolutionFile solutionFile) {
            DefaultVisualStudioSolution solution = (DefaultVisualStudioSolution) getSolution();
            //solutionFile.setMainProject(solution.rootProject)
//            solution.getSolutionConfigurations.each {
//                solutionConfig ->
//                    solutionFile.addSolutionConfiguration(solutionConfig.name, solution.getProjectConfigurations(solutionConfig))
//            }

            solutionFile.getActions().addAll(solution.getSolutionFile().getTextActions());
        }
    }
}
