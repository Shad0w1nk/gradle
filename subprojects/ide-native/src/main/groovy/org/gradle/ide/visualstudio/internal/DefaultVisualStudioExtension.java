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
package org.gradle.ide.visualstudio.internal;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.resolve.ProjectModelResolver;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.VisualStudioSolution;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.model.Managed;

public class DefaultVisualStudioExtension implements VisualStudioExtensionInternal {
    private final VisualStudioProjectRegistry projectRegistry;
    private final VisualStudioSolutionRegistry solutionRegistry;
    private final VisualStudioProjectRegistryEx projectRegistryEx;
    private final ProjectInternal project;

    public DefaultVisualStudioExtension(Instantiator instantiator, ProjectModelResolver projectModelResolver, FileResolver fileResolver, ProjectInternal project) {
        VisualStudioProjectMapper projectMapper = new VisualStudioProjectMapper();
        projectRegistry = new VisualStudioProjectRegistry(fileResolver, projectMapper, instantiator);
        projectRegistryEx = new VisualStudioProjectRegistryEx(fileResolver, projectMapper, instantiator);
        VisualStudioProjectResolver projectResolver = new VisualStudioProjectResolver(projectModelResolver);
        solutionRegistry = new VisualStudioSolutionRegistry(fileResolver, projectResolver, instantiator);
        this.project = project;
    }

    public NamedDomainObjectSet<? extends VisualStudioProject> getProjects() {
        return projectRegistry;
    }

    public VisualStudioProjectRegistry getProjectRegistry() {
        return projectRegistry;
    }

    public NamedDomainObjectSet<? extends VisualStudioSolution> getSolutions() {
        return solutionRegistry;
    }

    public VisualStudioSolutionRegistry getSolutionRegistry() {
        return solutionRegistry;
    }

    @Override
    public VisualStudioProjectRegistryEx getProjectRegistryEx() {
        return projectRegistryEx;
    }

    public boolean isRoot() {
        return project.getParent() == null;
    }

    public ProjectInternal getRootProject() {
        return project.getRootProject();
    }
}
