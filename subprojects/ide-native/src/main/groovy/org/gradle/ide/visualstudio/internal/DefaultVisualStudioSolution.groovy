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

package org.gradle.ide.visualstudio.internal
import org.gradle.api.Action
import org.gradle.api.internal.file.FileResolver
import org.gradle.ide.visualstudio.TextConfigFile
import org.gradle.ide.visualstudio.TextProvider
import org.gradle.ide.visualstudio.VisualStudioProject
import org.gradle.ide.visualstudio.VisualStudioSolution
import org.gradle.internal.reflect.Instantiator
import org.gradle.api.internal.AbstractBuildableModelElement
import org.gradle.model.Managed
import org.gradle.nativeplatform.NativeLibraryBinary
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal

public class DefaultVisualStudioSolution extends AbstractBuildableModelElement implements VisualStudioSolution {
    //final DefaultVisualStudioProject rootProject
    private final String name;
    private SolutionFile solutionFile;
    //private final VisualStudioProjectResolver vsProjectResolver

    DefaultVisualStudioSolution(String name,/*DefaultVisualStudioProject rootProject,*/ FileResolver fileResolver,
                                /*VisualStudioProjectResolver vsProjectResolver,*/ Instantiator instantiator) {
        //this.rootProject = rootProject
        this.name = name;
        //this.vsProjectResolver = vsProjectResolver
        //this.solutionFile = instantiator.newInstance(SolutionFile, fileResolver, String.format("%s.sln", name));
    }

    public String getName() {
        return name;
    }

    public SolutionFile getSolutionFile() {
        return solutionFile;
    }

    //public abstract void setSolutionFile(TextConfigFile configFile);

//    NativeComponentSpec getComponent() {
//        return rootProject.component
//    }

    Set<VisualStudioProject> getProjects() {
        def projects = [] as Set
        solutionConfigurations.each { solutionConfig ->
            getProjectConfigurations(solutionConfig).each { projectConfig ->
                projects << projectConfig.project
            }
        }
        return projects
    }
//
//    List<VisualStudioProjectConfiguration> getSolutionConfigurations() {
//        return null//rootProject.configurations
//    }
//
//    List<VisualStudioProjectConfiguration> getProjectConfigurations(VisualStudioProjectConfiguration solutionConfiguration) {
//        def configurations = [] as Set
//        configurations << solutionConfiguration
//        addDependentConfigurations(configurations, solutionConfiguration)
//        return configurations as List
//    }
//
//    private void addDependentConfigurations(Set configurations, VisualStudioProjectConfiguration configuration) {
//        // TODO: HERE!!!!
//        for (NativeLibraryBinary library : configuration.binary.dependentBinaries) {
//            if (library instanceof NativeBinarySpecInternal) {
//                VisualStudioProjectConfiguration libraryConfiguration = vsProjectResolver.lookupProjectConfiguration(library);
//                if (configurations.add(libraryConfiguration)) {
//                    addDependentConfigurations(configurations, libraryConfiguration)
//                }
//            }
//        }
//    }

    public static class SolutionFile implements TextConfigFile {
        private final List<Action<? super TextProvider>> actions = new ArrayList<Action<? super TextProvider>>();
        private final FileResolver fileResolver;
        private Object location

        public SolutionFile(FileResolver fileResolver, String defaultLocation) {
            this.fileResolver = fileResolver
            this.location = defaultLocation
        }

        public File getLocation() {
            return location;
        }

        public void setLocation(Object location) {
            this.location = location;
        }

        public void withContent(Action<? super TextProvider> action) {
            actions.add(action)
        }

        public List<Action<? super TextProvider>> getTextActions() {
            return actions
        }
    }
}
