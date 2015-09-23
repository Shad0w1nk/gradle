/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.ide.visualstudio.plugins;

import groovy.lang.Closure;
import org.gradle.api.*;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectIdentifier;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.resolve.ProjectModelResolver;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.VisualStudioSolution;
import org.gradle.ide.visualstudio.internal.*;
import org.gradle.ide.visualstudio.tasks.GenerateFiltersFileTask;
import org.gradle.ide.visualstudio.tasks.GenerateProjectFileTask;
import org.gradle.ide.visualstudio.tasks.GenerateSolutionFileTask;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.collection.CollectionBuilder;
import org.gradle.model.internal.core.ModelPath;
import org.gradle.model.internal.type.ModelType;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeComponentSpec;
import org.gradle.nativeplatform.plugins.NativeComponentModelPlugin;
import org.gradle.platform.base.BinaryContainer;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;


/**
 * A plugin for creating a Visual Studio solution for a gradle project.
 */
@Incubating
public class VisualStudioPlugin implements Plugin<Project> {
    private final ServiceRegistry serviceRegistry;

    @Inject
    public VisualStudioPlugin(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void apply(final Project project) {
        project.getPluginManager().apply(NativeComponentModelPlugin.class);

        // is root? add solution

        // VS (per root) -> project (per component) -- executable? executable : choose between dll & lib
        Instantiator instantiator = serviceRegistry.get(Instantiator.class);
        ProjectModelResolver projectModelResolver = serviceRegistry.get(ProjectModelResolver.class);
        FileResolver fileResolver = serviceRegistry.get(FileResolver.class);

        VisualStudioExtensionInternal visualStudio = project.getExtensions().create("visualStudio", VisualStudioExtensionInternal.class, instantiator, projectModelResolver, fileResolver);

        VisualStudioProjectResolver projectResolver = serviceRegistry.get(VisualStudioProjectResolver.class);

//        if (isRoot(project)) {
//            // Add default solution
//            visualStudio.getSolutionRegistry().add(instantiator.newInstance(DefaultVisualStudioSolution.class, project.getRootProject(), fileResolver, projectResolver, instantiator));
//
//            // Fill the solution with projects
//            project.getGradle().projectsEvaluated(new Callable<void>() {
//                @Override
//                public void call() throws Exception {
//                    Set<Project> projects = new HashSet<Project>();
//                    for (Project it : project.getRootProject().getAllprojects()) {
//                        if (it.getPlugins().hasPlugin(VisualStudioPlugin.class)) {
//                            projects.add(it);
//                            VisualStudioExtensionInternal visualStudio = ((ProjectInternal) it).getModelRegistry().realize(ModelPath.path("visualStudio"), ModelType.of(VisualStudioExtensionInternal.class));
//                            // Add projects to root solution.
//                            //visualStudio.getProjectRegistry()
//                        }
//                    }
//                }
//            });
//            //visualStudio.getSolutionRegistry().addSolution();
//            // extension.solution().add()
//
//            // rootProject.allprojects.findAll { it.plugins.hasPlugin(VisualStudioPlugin) }
//        }

//        void hookDeduplicationToTheRoot(Project project) {
//            if (isRoot(project)) {
//                project.gradle.projectsEvaluated {
//                    makeSureModuleNamesAreUnique()
//                }
//            }
//        }
    }

    private boolean isRoot(Project project) {
        return project.getParent() == null;
    }

    static class Rules extends RuleSource {
        @Model
        public static VisualStudioExtensionInternal visualStudio(ExtensionContainer extensions) {
            return extensions.getByType(VisualStudioExtensionInternal.class);
        }

        // Rule to add project to the model based on component
        @Mutate
        public static void createDefaultSolution(final VisualStudioExtensionInternal visualStudio) {
            if (visualStudio.isRoot()) {
                visualStudio.getRootProject().getGradle().projectsEvaluated(new Closure<Void>(this) {
                    @Override
                    public Void call() {
                        Set<Project> projects = new HashSet<Project>();
                        for (Project it : visualStudio.getRootProject().getAllprojects()) {
                            if (it.getPlugins().hasPlugin(VisualStudioPlugin.class)) {
                                projects.add(it);
                                VisualStudioExtensionInternal projectVisualStudio = ((ProjectInternal) it).getModelRegistry().realize(ModelPath.path("visualStudio"), ModelType.of(VisualStudioExtensionInternal.class));
                                // Add projects to root solution.
                                //visualStudio.getProjectRegistry()
                            }
                        }
                        return super.call();
                    }
                });
            }
        }

        @Mutate
        public static void createProjects(final VisualStudioExtensionInternal visualStudio, ModelMap<NativeComponentSpec> components, ServiceRegistry serviceRegistry) {
            final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
            final Instantiator instantiator = serviceRegistry.get(Instantiator.class);

            components.all(new Action<NativeComponentSpec>() {
                @Override
                public void execute(NativeComponentSpec component) {
                    System.out.println(component.getProjectPath());
                    DefaultVisualStudioProjectEx vsProject = new DefaultVisualStudioProjectEx("bob", component, fileResolver, instantiator);
                    //visualStudio.getProjectRegistryEx().add(vsProject);
                }
            });
        }

        @Mutate
        public static void includeBuildFileInProject(VisualStudioExtensionInternal visualStudio, final ProjectIdentifier projectIdentifier) {
            visualStudio.getProjects().all(new Action<VisualStudioProject>() {
                public void execute(VisualStudioProject project) {
                    if (projectIdentifier.getBuildFile() != null) {
                        ((DefaultVisualStudioProject) project).addSourceFile(projectIdentifier.getBuildFile());
                    }
                }
            });
        }

        @Mutate
        @SuppressWarnings("GroovyUnusedDeclaration")
        public static void createVisualStudioModelForBinaries(VisualStudioExtensionInternal visualStudioExtension, BinaryContainer binaryContainer) {
            for (NativeBinarySpec binary : binaryContainer.withType(NativeBinarySpec.class)) {
                VisualStudioProjectConfiguration configuration = visualStudioExtension.getProjectRegistry().addProjectConfiguration(binary);

                // Only create a solution if one of the binaries is buildable
                if (binary.isBuildable()) {
                    DefaultVisualStudioProject visualStudioProject = configuration.getProject();
                    visualStudioExtension.getSolutionRegistry().addSolution(visualStudioProject);
                }
            }
        }

        @Mutate
        @SuppressWarnings("GroovyUnusedDeclaration")
        public static void createTasksForVisualStudio(TaskContainer tasks, VisualStudioExtensionInternal visualStudioExtension) {
            for (VisualStudioProject vsProject : visualStudioExtension.getProjects()) {
                vsProject.builtBy(createProjectsFileTask(tasks, vsProject));
                vsProject.builtBy(createFiltersFileTask(tasks, vsProject));
            }

            for (VisualStudioSolution vsSolution : visualStudioExtension.getSolutions()) {
                Task solutionTask = tasks.create(vsSolution.getName() + "VisualStudio");
                solutionTask.setDescription(String.format("Generates the '%s' Visual Studio solution file.", vsSolution.getName()));
                vsSolution.setBuildTask(solutionTask);
                vsSolution.builtBy(createSolutionTask(tasks, vsSolution));

                // Lifecycle task for component
                NativeComponentSpec component = vsSolution.getComponent();
                Task lifecycleTask = tasks.maybeCreate(component.getName() + "VisualStudio");
                lifecycleTask.dependsOn(vsSolution);
                lifecycleTask.setGroup("IDE");
                lifecycleTask.setDescription(String.format("Generates the Visual Studio solution for %s.", component));
            }

            addCleanTask(tasks);
        }

        private static void addCleanTask(TaskContainer tasks) {
            Delete cleanTask = tasks.create("cleanVisualStudio", Delete.class);
            for (Task task : tasks.withType(GenerateSolutionFileTask.class)) {
                cleanTask.delete(task.getOutputs().getFiles());
            }
            for (Task task : tasks.withType(GenerateFiltersFileTask.class)) {
                cleanTask.delete(task.getOutputs().getFiles());
            }
            for (Task task : tasks.withType(GenerateProjectFileTask.class)) {
                cleanTask.delete(task.getOutputs().getFiles());
            }
            cleanTask.setGroup("IDE");
            cleanTask.setDescription("Removes all generated Visual Studio project and solution files");
        }

        private static Task createSolutionTask(TaskContainer tasks, VisualStudioSolution solution) {
            GenerateSolutionFileTask solutionFileTask = tasks.create(solution.getName() + "VisualStudioSolution", GenerateSolutionFileTask.class);
            solutionFileTask.setVisualStudioSolution(solution);
            return solutionFileTask;
        }

        private static Task createProjectsFileTask(TaskContainer tasks, VisualStudioProject vsProject) {
            GenerateProjectFileTask task = tasks.create(vsProject.getName() + "VisualStudioProject", GenerateProjectFileTask.class);
            task.setVisualStudioProject(vsProject);
            task.initGradleCommand();
            return task;
        }

        private static Task createFiltersFileTask(TaskContainer tasks, VisualStudioProject vsProject) {
            GenerateFiltersFileTask task = tasks.create(vsProject.getName() + "VisualStudioFilters", GenerateFiltersFileTask.class);
            task.setVisualStudioProject(vsProject);
            return task;
        }
    }
}
