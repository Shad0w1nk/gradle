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

import org.gradle.api.*;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectIdentifier;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectRegistry;
import org.gradle.api.internal.resolve.ProjectModelResolver;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.ide.visualstudio.VisualStudioExtension;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.VisualStudioSolution;
import org.gradle.ide.visualstudio.internal.*;
import org.gradle.ide.visualstudio.tasks.GenerateFiltersFileTask;
import org.gradle.ide.visualstudio.tasks.GenerateProjectFileTask;
import org.gradle.ide.visualstudio.tasks.GenerateSolutionFileTask;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.*;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.model.internal.type.ModelType;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeComponentSpec;
import org.gradle.nativeplatform.plugins.NativeComponentModelPlugin;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.ComponentSpecContainer;

import javax.inject.Inject;


/**
 * A plugin for creating a Visual Studio solution for a gradle project.
 */
@Incubating
public class VisualStudioPlugin implements Plugin<Project> {
    private final ModelRegistry modelRegistry;
    private final ServiceRegistry serviceRegistry;
    private final Instantiator instantiator;
    private final FileResolver fileResolver;
    private final ModelSchemaStore schemaStore;

    @Inject
    public VisualStudioPlugin(ModelRegistry modelRegistry, ServiceRegistry serviceRegistry, Instantiator instantiator, FileResolver fileResolver, ModelSchemaStore schemaStore) {
        this.modelRegistry = modelRegistry;
        this.serviceRegistry = serviceRegistry;
        this.instantiator = instantiator;
        this.fileResolver = fileResolver;
        this.schemaStore = schemaStore;
    }

    public void apply(final Project project) {
        project.getPluginManager().apply(NativeComponentModelPlugin.class);

        // is root? add solution

        // VS (per root) -> project (per component) -- executable? executable : choose between dll & lib
        //DefaultVisualStudioExtension visualStudio = project.getExtensions().create("visualStudio", DefaultVisualStudioExtension.class);// instantiator, fileResolver, project);


        SimpleModelRuleDescriptor descriptor = new SimpleModelRuleDescriptor(VisualStudioPlugin.class.getSimpleName() + ".apply()");

        //SpecializedMapSchema<VisualStudioExtension> schema = (SpecializedMapSchema<VisualStudioExtension>) schemaStore.getSchema(ModelType.of(VisualStudioExtension.class));
        ModelPath vsPath = ModelPath.path("visualStudio");
        ModelType<VisualStudioExtension> containerType = ModelType.of(VisualStudioExtension.class);
        ModelType<VisualStudioExtensionInternal> modelType = ModelType.of(VisualStudioExtensionInternal.class);
//        ChildNodeInitializerStrategy<T> childFactory = NodeBackedModelMap.createUsingRegistry(modelType, nodeInitializerRegistry);
//        return ModelCreators.of(ModelReference.of(path, containerType), Factories.<C>constantNull())
//            .descriptor(descriptor)
//            .withProjection(new SpecializedModelMapProjection<C, T>(containerType, modelType, viewClass, childFactory))
//            .withProjection(PolymorphicModelMapProjection.of(modelType, childFactory))
//            .inputs(ModelReference.of(NodeInitializerRegistry.class))
//            .build();

        //ModelCreator vsCreator = ModelCreators.of(vsPath).descriptor(descriptor).withProjection(UnmanagedModelProjection.of(modelType)).build();
        //ModelCreator vsCreator = ModelCreators.bridgedInstance(ModelReference.of(vsPath, modelType), visualStudio).build();
        ModelCreator vsCreator = ModelCreators.unmanagedInstanceOf(ModelReference.of(vsPath, modelType), new Transformer<VisualStudioExtensionInternal, MutableModelNode>() {
            @Override
            public VisualStudioExtensionInternal transform(MutableModelNode modelNode) {
                return new DefaultVisualStudioExtension(modelNode, instantiator, fileResolver, schemaStore);
            }
        }).build();
        modelRegistry.create(vsCreator);

        modelRegistry.configure(ModelActionRole.Defaults, DirectNodeNoInputsModelAction.of(ModelReference.of(vsPath), descriptor, new Action<MutableModelNode>() {
            @Override
            public void execute(MutableModelNode modelNode) {
//                ServiceRegistry serviceRegistry = (ServiceRegistry)modelViews.get(0).getInstance();
//                modelNode.addLink(
//                    ModelCreators.of(
//                        modelNode.getPath().child("solutions"), Actions.doNothing())
//                        .descriptor(modelNode.getDescriptor(), ".solutions")
//                        .withProjection(
//                            ModelMapModelProjection.unmanaged(
//                                VisualStudioSolution.class,
//                                NodeBackedModelMap.createUsingRegistry(ModelType.of(VisualStudioSolution.class), serviceRegistry.get(NodeInitializerRegistry.class))
//                            )
//                        )
//                        .build()
//                );
                //binaries = modelNode.getLink("binaries");

                System.out.println("ALLO");
                //modelNode.getPrivateData(DefaultVisualStudioExtension.class);//.setProject(project);
            }
        }));

        //VisualStudioExtensionInternal g = modelRegistry.find(visualStudio, modelType);
        //VisualStudioExtensionInternal g = modelRegistry.realize(visualStudio, modelType);
        //g.setProject(project);

//        ModelCreator componentsCreator = ModelMapCreators.specialized(
//                        components,
//                        ComponentSpec.class,
//                        ComponentSpecContainer.class,
//                        schema.getImplementationType().asSubclass(ComponentSpecContainer.class),
//                        nodeInitializerRegistry,
//                        descriptor
//                    );
//                modelRegistry.create(componentsCreator);

        //

        //VisualStudioProjectResolver projectResolver = serviceRegistry.get(VisualStudioProjectResolver.class);

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


    static class Rules extends RuleSource {
        //@Model
        //public static VisualStudioExtensionInternal visualStudio(ExtensionContainer extensions) {
        //    return extensions.getByType(VisualStudioExtensionInternal.class);
        //}

        //@Model
        //public static void visualStudioRegistry(VisualStudioRegistry vsRegistry) {}

        @Mutate
        public static void bob(VisualStudioExtensionInternal visualStudio) {
            //visualStudio.isRoot();
        }

//        @Model
//        BaseInstanceFactory<VisualStudioExtensionInternal, String> vsFactory(ServiceRegistry serviceRegistry) {
//            final Instantiator instantiator = serviceRegistry.get(Instantiator.class);
//            BaseInstanceFactory<VisualStudioExtensionInternal, String> f = new BaseInstanceFactory<VisualStudioExtensionInternal, String>("blah");
//            f.registerFactory(ModelType.of(VisualStudioExtensionInternal.class), null, new BiFunction<VisualStudioExtensionInternal, String, MutableModelNode>() {
//                @Override
//                public VisualStudioExtensionInternal apply(String s, MutableModelNode mutableModelNode) {
//                    return instantiator.newInstance(DefaultVisualStudioExtension.class, mutableModelNode);
//                }
//            });
//
//            return f;
//        }
//
//        @Defaults
//        public static void a1(InstanceFactoryRegistry instanceFactoryRegistry, BaseInstanceFactory<VisualStudioExtensionInternal, String> vsFactory) {
//            instanceFactoryRegistry.register(ModelType.of(VisualStudioExtensionInternal.class), ModelReference.of(BaseInstanceFactory < VisualStudioExtensionInternal, String >.class));
//        }

        @Mutate
        public static void createDefaultSolution(@Path("visualStudio.solutions") ModelMap<VisualStudioSolution> solutions, @Path("visualStudio.projects") ModelMap<VisualStudioProject> projects, ProjectIdentifier project, ServiceRegistry serviceRegistry) {
            if (isRoot(project)) {
                final ProjectRegistry<ProjectInternal> projectRegistry = serviceRegistry.get(ProjectRegistry.class);
                solutions.create(project.getName(), DefaultVisualStudioSolution.class, new Action<DefaultVisualStudioSolution>() {
                    @Override
                    public void execute(DefaultVisualStudioSolution visualStudioSolution) {
                        for (ProjectInternal project : projectRegistry.getAllProjects()) {
                            // Exclude current project...
                            //project.evaluate();
                            //project.getTasks().discoverTasks();
                            ModelRegistry modelRegistry = project.getModelRegistry();
                            VisualStudioProjectRegistry visualStudio = modelRegistry.realize(ModelPath.path("visualStudio.projects"), ModelType.of(VisualStudioProjectRegistry.class));

                            System.out.println("BOB");
                        }
                        //project.
                        //modelResolver.resolveProjectModel()
                    }
                });
            }
        }

        private static boolean isRoot(ProjectIdentifier project) {
            return project.getParentIdentifier() == null;
        }

        // Rule to add project to the model based on component
//        @Mutate
//        public static void createDefaultSolution(final VisualStudioExtensionInternal visualStudio) {
//            if (visualStudio.isRoot()) {
//                visualStudio.getRootProject().getGradle().projectsEvaluated(new Closure<Void>(this) {
//                    @Override
//                    public Void call() {
//                        Set<Project> projects = new HashSet<Project>();
//                        for (Project it : visualStudio.getRootProject().getAllprojects()) {
//                            if (it.getPlugins().hasPlugin(VisualStudioPlugin.class)) {
//                                projects.add(it);
//                                VisualStudioExtensionInternal projectVisualStudio = ((ProjectInternal) it).getModelRegistry().realize(ModelPath.path("visualStudio"), ModelType.of(VisualStudioExtensionInternal.class));
//                                // Add projects to root solution.
//                                //visualStudio.getProjectRegistry()
//                            }
//                        }
//                        return super.call();
//                    }
//                });
//            }
//        }

        @Mutate
        public static void createProjects(@Path("visualStudio.projects") ModelMap<VisualStudioProject> projects, ModelMap<NativeComponentSpec> components) {
            for (final NativeComponentSpec component : components) {
                projects.create(component.getProjectPath().substring(1).replace(":", "_") + "_" + component.getName(), DefaultVisualStudioProject.class, new Action<DefaultVisualStudioProject>() {
                    @Override
                    public void execute(DefaultVisualStudioProject vsProject) {
                        vsProject.setComponent(component);
                    }
                });
            }
        }

        @Finalize
        public static void fini(@Path("visualStudio.projects") ModelMap<VisualStudioProject> projects) {}

//        //@Mutate
//        public static void createProjects(final VisualStudioExtensionInternal visualStudio, ModelMap<NativeComponentSpec> components, ServiceRegistry serviceRegistry) {
//            final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
//            for (final NativeComponentSpec component : components) {
//                System.out.println(component.getProjectPath());
//                //DefaultVisualStudioProjectEx vsProject = new DefaultVisualStudioProjectEx("bob", component, fileResolver, instantiator);
//                visualStudio.getProjectRegistry().create("bob", new Action<DefaultVisualStudioProject>() {
//                    @Override
//                    public void execute(DefaultVisualStudioProject vsProject) {
//                        vsProject.setComponent(component);
//                        vsProject.getFiltersFile().setLocation(fileResolver.resolve(String.format("%s.vcxproj.filter", vsProject.getName())));
//                        vsProject.getProjectFile().setLocation(fileResolver.resolve(String.format("%s.vcsproj", vsProject.getName())));
//                        createProjectConfigurations(vsProject, component.getBinaries());
//                    }
//                });
//            }
//        }

        @Mutate
        public static void createProjectConfigurations(@Path("visualStudio.projects") ModelMap<VisualStudioProject> projects) {
            projects.all(new Action<VisualStudioProject>() {
                @Override
                public void execute(VisualStudioProject vsProject) {
                    for (BinarySpec binary : vsProject.getComponent().getBinaries()) {
                        // Create config for each buildable binary
                    }
                }
            });
//            for (final BinarySpec binary : vsProject.getComponent().getBinaries()) {
//                if (binary.isBuildable()) {
//                    vsProject.source(binary.getInputs());
//                    vsProject.getConfigurations().create(binary.getName(), new Action<VisualStudioProjectConfiguration>() {
//                        @Override
//                        public void execute(VisualStudioProjectConfiguration vsProjectConfiguration) {
//                            vsProjectConfiguration.setBinary((NativeBinarySpec) binary);
//                        }
//                    });
//                }
//            }
        }

        //
        //@Mutate
        public static void createDefaultSolution(final VisualStudioExtensionInternal visualStudio, ServiceRegistry serviceRegistry/*, ProjectModelResolver modelResolver*/) {
//            final ProjectModelResolver projectModelResolver = serviceRegistry.get(ProjectModelResolver.class);
//            if (visualStudio.isRoot()) {
//                visualStudio.getSolutionRegistry().create(visualStudio.getProject().getName(), new Action<DefaultVisualStudioSolution>() {
//                    @Override
//                    public void execute(DefaultVisualStudioSolution vsSolution) {
//                        //            // Fill the solution with projects
//                        visualStudio.getProject().getGradle().projectsEvaluated(new Closure<Void>(this) {
//                    @Override
//                    public Void call() {
//                        Set<Project> projects = new HashSet<Project>();
//                        for (Project it : visualStudio.getProject().getAllprojects()) {
//                            if (it.getPlugins().hasPlugin(VisualStudioPlugin.class)) {
//                                projects.add(it);
//                                projectModelResolver.resolveProjectModel(it.getPath());
//                                VisualStudioExtensionInternal visualStudioExtension = projectModelResolver.resolveProjectModel(it.getPath()).realize(ModelPath.path("visualStudio"), ModelType.of(VisualStudioExtensionInternal.class));
//                                //VisualStudioExtensionInternal projectVisualStudio = ((ProjectInternal) it).getModelRegistry().realize(ModelPath.path("visualStudio"), ModelType.of(VisualStudioExtensionInternal.class));
//                                System.out.println("NICE " + it.getPath());
//                                // Add projects to root solution.
//                                //visualStudio.getProjectRegistry()
//                            }
//                        }
//                        return super.call();
//                    }
//                });
//                        // Fill the solution with project
//                        //vsSolution.getSolutionFile().setLocation(new File(visualStudio.getProject().getProjectDir(), String.format("%s.sln", name)));
//                    }
//                });
//            }
        }
//
//        //@Mutate
//        public static void includeBuildFileInProject(VisualStudioExtensionInternal visualStudio, final ProjectIdentifier projectIdentifier) {
//            visualStudio.getProjects().all(new Action<VisualStudioProject>() {
//                public void execute(VisualStudioProject project) {
//                    if (projectIdentifier.getBuildFile() != null) {
//                        ((DefaultVisualStudioProject) project).addSourceFile(projectIdentifier.getBuildFile());
//                    }
//                }
//            });
//        }
//
//        //@Mutate
//        @SuppressWarnings("GroovyUnusedDeclaration")
//        public static void createVisualStudioModelForBinaries(VisualStudioExtensionInternal visualStudioExtension, BinaryContainer binaryContainer) {
//            for (NativeBinarySpec binary : binaryContainer.withType(NativeBinarySpec.class)) {
//                VisualStudioProjectConfiguration configuration = visualStudioExtension.getProjectRegistry().addProjectConfiguration(binary);
//
//                // Only create a solution if one of the binaries is buildable
//                if (binary.isBuildable()) {
//                    DefaultVisualStudioProject visualStudioProject = configuration.getProject();
//                    visualStudioExtension.getSolutionRegistry().addSolution(visualStudioProject);
//                }
//            }
//        }

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
                //NativeComponentSpec component = vsSolution.getComponent();
                Task lifecycleTask = tasks.maybeCreate("visualStudio");
                lifecycleTask.dependsOn(vsSolution);
                lifecycleTask.setGroup("IDE");
                lifecycleTask.setDescription(/*String.format(*/"Generates the Visual Studio solution for %s(maybe rootProject name)."/*, component)*/);
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
