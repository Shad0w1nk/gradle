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

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.XmlProvider;
import org.gradle.api.internal.AbstractBuildableModelElement;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.XmlConfigFile;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.language.rc.WindowsResourceSet;
import org.gradle.nativeplatform.NativeComponentSpec;

import java.io.File;
import java.util.*;

public class DefaultVisualStudioProject extends AbstractBuildableModelElement implements VisualStudioProject {
    private final String name;
    private final DefaultConfigFile projectFile;
    private final DefaultConfigFile filtersFile;
    private NativeComponentSpec component;
    private final List<File> additionalFiles = new ArrayList<File>();
    final Set<LanguageSourceSet> sources = new LinkedHashSet<LanguageSourceSet>();
    //private final Map<NativeBinarySpec, VisualStudioProjectConfiguration> configurations = [:]
    private final NamedDomainObjectContainer<VisualStudioProjectConfiguration> configurations;

    public DefaultVisualStudioProject(String name, FileResolver fileResolver, Instantiator instantiator) {
        this.name = name;
        projectFile = instantiator.newInstance(DefaultConfigFile.class, fileResolver, String.format("%s.vcxproj", name));
        filtersFile = instantiator.newInstance(DefaultConfigFile.class, fileResolver, String.format("%s.vcxproj.filters", name));
        configurations = new FactoryNamedDomainObjectContainer<VisualStudioProjectConfiguration>(VisualStudioProjectConfiguration.class, instantiator);
    }

    public void setComponent(NativeComponentSpec component) {
        this.component = component;
    }

    public String getName() {
        return name;
    }

    public DefaultConfigFile getProjectFile() {
        return projectFile;
    }

    public DefaultConfigFile getFiltersFile() {
        return filtersFile;
    }

    public NativeComponentSpec getComponent() {
        return component;
    }

    public void addSourceFile(File sourceFile) {
        additionalFiles.add(sourceFile);
    }

    public String getUuid() {
        String projectPath = component.getProjectPath();
        String vsComponentPath = String.format("%s:%s", projectPath, name);
        return String.format("{%s}", UUID.nameUUIDFromBytes(vsComponentPath.getBytes()).toString().toUpperCase());
    }

    public void source(Collection<LanguageSourceSet> sources) {
        this.sources.addAll(sources);
        builtBy(sources);
    }

    public List<File> getSourceFiles() {
        Set<File> allSource = new HashSet<File>();
        allSource.addAll(additionalFiles);
        for (LanguageSourceSet sourceSet : sources) {
            if (!(sourceSet instanceof WindowsResourceSet)) {
                allSource.addAll(sourceSet.getSource().getFiles());
            }
        }
        return new ArrayList<File>(allSource);
    }

    public List<File> getResourceFiles() {
        Set<File> allResources = new HashSet<File>();
        for (LanguageSourceSet sourceSet : sources) {
            if (sourceSet instanceof WindowsResourceSet) {
                allResources.addAll(sourceSet.getSource().getFiles());
            }
        }
        return new ArrayList<File>(allResources);
    }

    public List<File> getHeaderFiles() {
        Set<File> allHeaders = new HashSet<File>();
        for (LanguageSourceSet sourceSet : sources) {
            if (sourceSet instanceof HeaderExportingSourceSet) {
                allHeaders.addAll(((HeaderExportingSourceSet) sourceSet).getExportedHeaders().getFiles());
                allHeaders.addAll(((HeaderExportingSourceSet) sourceSet).getImplicitHeaders().getFiles());
            }
        }
        return new ArrayList<File>(allHeaders);
    }

//    public List<VisualStudioProjectConfiguration> getConfigurations() {
//        return CollectionUtils.toList(configurations.values())
//    }

//    public void addConfiguration(NativeBinarySpec nativeBinary, VisualStudioProjectConfiguration configuration) {
//        configurations[nativeBinary] = configuration
//        def specInternal = nativeBinary as NativeBinarySpecInternal
//        source specInternal.inputs
//    }

    //    public VisualStudioProjectConfiguration getConfiguration(NativeBinarySpec nativeBinary) {
//        return configurations[nativeBinary]
//    }
    public NamedDomainObjectContainer<VisualStudioProjectConfiguration> getConfigurations() {
        return configurations;
    }

    public static class DefaultConfigFile implements XmlConfigFile {
        private final List<Action<? super XmlProvider>> actions = new ArrayList<Action<? super XmlProvider>>();
        private final FileResolver fileResolver;
        private Object location;

        public DefaultConfigFile(FileResolver fileResolver, String defaultLocation) {
            this.fileResolver = fileResolver;
            this.location = defaultLocation;
        }

        public File getLocation() {
            return fileResolver.resolve(location);
        }

        public void setLocation(Object location) {
            this.location = location;
        }

        public void withXml(Action<? super XmlProvider> action) {
            actions.add(action);
        }

        public List<Action<? super XmlProvider>> getXmlActions() {
            return actions;
        }
    }
}
