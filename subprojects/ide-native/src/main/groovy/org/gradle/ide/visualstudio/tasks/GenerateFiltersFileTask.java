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
import org.gradle.api.XmlProvider;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.internal.DefaultVisualStudioProject;
import org.gradle.ide.visualstudio.tasks.internal.RelativeFileNameTransformer;
import org.gradle.ide.visualstudio.tasks.internal.VisualStudioFiltersFile;
import org.gradle.plugins.ide.api.XmlGeneratorTask;

import java.io.File;

@Incubating
public class GenerateFiltersFileTask extends XmlGeneratorTask<VisualStudioFiltersFile> {
    private DefaultVisualStudioProject vsProject;

    public void setVisualStudioProject(VisualStudioProject vsProject) {
        this.vsProject = (DefaultVisualStudioProject)vsProject;
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
        return vsProject.getFiltersFile().getLocation();
    }

    @Override
    protected void configure(VisualStudioFiltersFile filtersFile) {
        for (File it : vsProject.getSourceFiles()) {
            filtersFile.addSource(it);
        }
        for (File it : vsProject.getHeaderFiles()) {
            filtersFile.addHeader(it);
        }

        for (Action<? super XmlProvider> it : vsProject.getFiltersFile().getXmlActions()) {
            getXmlTransformer().addAction(it);
        }
    }

    @Override
    protected VisualStudioFiltersFile create() {
        return new VisualStudioFiltersFile(getXmlTransformer(), RelativeFileNameTransformer.forFile(getProject().getRootDir(), vsProject.getFiltersFile().getLocation()));
    }
}
