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

package org.gradle.nativeplatform.test.cunit.plugins;

import org.gradle.api.*;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.Actions;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.base.internal.DefaultFunctionalSourceSet;
import org.gradle.language.c.CSourceSet;
import org.gradle.language.c.internal.DefaultCSourceSet;
import org.gradle.language.c.plugins.CLangPlugin;
import org.gradle.language.nativeplatform.internal.DefaultPreprocessingTool;
import org.gradle.model.*;
import org.gradle.model.collection.CollectionBuilder;
import org.gradle.nativeplatform.*;
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal;
import org.gradle.nativeplatform.internal.configure.*;
import org.gradle.nativeplatform.internal.resolve.NativeDependencyResolver;
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal;
import org.gradle.nativeplatform.test.NativeTestSuiteSpec;
import org.gradle.platform.base.test.TestSuiteContainer;
import org.gradle.nativeplatform.test.cunit.CUnitTestSuiteBinarySpec;
import org.gradle.nativeplatform.test.cunit.CUnitTestSuiteSpec;
import org.gradle.nativeplatform.test.cunit.internal.DefaultCUnitTestSuiteBinary;
import org.gradle.nativeplatform.test.cunit.internal.DefaultCUnitTestSuiteSpec;
import org.gradle.nativeplatform.test.cunit.internal.DefaultNativeCUnitBinariesFactory;
import org.gradle.nativeplatform.test.cunit.tasks.GenerateCUnitLauncher;
import org.gradle.nativeplatform.test.plugins.NativeBinariesTestPlugin;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainInternal;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;
import org.gradle.platform.base.*;
import org.gradle.platform.base.component.BaseComponentSpec;
import org.gradle.platform.base.internal.*;
import org.gradle.platform.base.test.TestSuiteSpec;

import java.io.File;

/**
 * A plugin that sets up the infrastructure for testing native binaries with CUnit.
 */
@Incubating
public class CUnitPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.getPluginManager().apply(NativeBinariesTestPlugin.class);
        project.getPluginManager().apply(CLangPlugin.class);
    }

    /**
     * Model rules.
     */
    @SuppressWarnings("UnusedDeclaration")
    @RuleSource
    public static class Rules {

        private static final String CUNIT_LAUNCHER_SOURCE_SET = "cunitLauncher";

        @ComponentType
        void cUnitExecutable(ComponentTypeBuilder<CUnitTestSuiteSpec> builder) {
            builder.defaultImplementation(DefaultCUnitTestSuiteSpec.class);
        }

        @BinaryType
        void cUnitBinary(BinaryTypeBuilder<CUnitTestSuiteBinarySpec> builder) {
            builder.defaultImplementation(DefaultCUnitTestSuiteBinary.class);
        }

        @Model
        PolymorphicDomainObjectContainer<CUnitTestSuiteSpec> cUnitTestSuites(PolymorphicDomainObjectContainer<NativeTestSuiteSpec> testSuites) {
            return testSuites.containerWithType(CUnitTestSuiteSpec.class);
        }

        @ComponentBinaries // Do we correctly template the collection builder???
        public void createCUnitLibraries(CollectionBuilder<NativeBinarySpec> binaries, final CUnitTestSuiteSpec component,
                                         LanguageRegistry languages, NativeToolChainRegistryInternal toolChains,
                                         PlatformContainer platforms, BuildTypeContainer buildTypes, FlavorContainer flavors,
                                         ServiceRegistry serviceRegistry, @Path("buildDir") final File buildDir) {
            if (component.getTestedComponent() == null) {
                NativeDependencyResolver resolver = serviceRegistry.get(NativeDependencyResolver.class);
                Action<NativeBinarySpec> configureBinaryAction = new NativeBinarySpecInitializer(buildDir);
                Action<NativeBinarySpec> setToolsAction = new ToolSettingNativeBinaryInitializer(languages);
                @SuppressWarnings("unchecked") Action<NativeBinarySpec> initAction = Actions.composite(configureBinaryAction, setToolsAction, new MarkBinariesBuildable());

                NativeBinariesFactory factory = new DefaultNativeCUnitBinariesFactory(binaries, initAction, resolver);

                BinaryNamingSchemeBuilder namingSchemeBuilder = new DefaultBinaryNamingSchemeBuilder();
                Action<NativeComponentSpec> createBinariesAction =
                        new NativeComponentSpecInitializer(factory, namingSchemeBuilder, toolChains, platforms, buildTypes, flavors);

                createBinariesAction.execute(component);
            } else {
                for (final NativeBinarySpec testedBinary : component.getTestedComponent().getNativeBinaries().withType(StaticLibraryBinarySpec.class)) {
                    // TODO(daniel): Create naming scheme that include dimensions.
                    final BinaryNamingScheme namingScheme = new DefaultBinaryNamingSchemeBuilder(((NativeBinarySpecInternal) testedBinary).getNamingScheme())
                            .withComponentName(component.getBaseName())
                            .withTypeString("CUnitExe").build();
                    binaries.create(namingScheme.getLifecycleTaskName(), CUnitTestSuiteBinarySpec.class, new Action<NativeBinarySpec>() {
                        public void execute(NativeBinarySpec s) {
                            DefaultCUnitTestSuiteBinary spec = (DefaultCUnitTestSuiteBinary) s;
                            spec.setTestedBinary((NativeBinarySpecInternal) testedBinary);
                            spec.setNamingScheme(namingScheme);
                            spec.setComponent(component);
                            //spec.setResolver();  // TODO(daniel): Should we set this?
                            configure(spec, buildDir);
                        }
                    });
                }
            }
        }

        private FunctionalSourceSet createCUnitSources(final Instantiator instantiator, final String suiteName, ProjectSourceSet projectSourceSet, final FileResolver fileResolver) {
            final FunctionalSourceSet functionalSourceSet = instantiator.newInstance(DefaultFunctionalSourceSet.class, suiteName, instantiator, projectSourceSet);
            functionalSourceSet.registerFactory(CSourceSet.class, new NamedDomainObjectFactory<CSourceSet>() {
                public CSourceSet create(String name) {
                    return BaseLanguageSourceSet.create(DefaultCSourceSet.class, name, suiteName, fileResolver, instantiator);
                }
            });
            return functionalSourceSet;
        }

        @Finalize
        // TODO(daniel): Same as before CUnit can have c++, c and ASM source too
        public void configureCUnitTestSuiteSources(PolymorphicDomainObjectContainer<CUnitTestSuiteSpec> testSuites, @Path("buildDir") File buildDir) {
            for (final CUnitTestSuiteSpec suite : testSuites) {
                FunctionalSourceSet suiteSourceSet = ((ComponentSpecInternal) suite).getSources();
                CSourceSet launcherSources = suiteSourceSet.maybeCreate(CUNIT_LAUNCHER_SOURCE_SET, CSourceSet.class);
                File baseDir = new File(buildDir, String.format("src/%s/cunitLauncher", suite.getName()));
                launcherSources.getSource().srcDir(new File(baseDir, "c"));
                launcherSources.getExportedHeaders().srcDir(new File(baseDir, "headers"));

                CSourceSet testSources = suiteSourceSet.maybeCreate("c", CSourceSet.class);
                testSources.getSource().srcDir(String.format("src/%s/%s", suite.getName(), "c"));
                testSources.getExportedHeaders().srcDir(String.format("src/%s/headers", suite.getName()));

                testSources.lib(launcherSources);
            }
        }

        @Mutate
        // TODO(daniel): Should we keep the launcher code in the base plugin
        public void createCUnitLauncherTasks(TaskContainer tasks, PolymorphicDomainObjectContainer<CUnitTestSuiteSpec> testSuites) {
            for (final CUnitTestSuiteSpec suite : testSuites) {

                String taskName = suite.getName() + "CUnitLauncher";
                GenerateCUnitLauncher skeletonTask = tasks.create(taskName, GenerateCUnitLauncher.class);

                CSourceSet launcherSources = findLaucherSources(suite);
                skeletonTask.setSourceDir(launcherSources.getSource().getSrcDirs().iterator().next());
                skeletonTask.setHeaderDir(launcherSources.getExportedHeaders().getSrcDirs().iterator().next());
                launcherSources.builtBy(skeletonTask);
            }
        }

        private CSourceSet findLaucherSources(CUnitTestSuiteSpec suite) {
            return suite.getSource().withType(CSourceSet.class).matching(new Spec<CSourceSet>() {
                public boolean isSatisfiedBy(CSourceSet element) {
                    return element.getName().equals(CUNIT_LAUNCHER_SOURCE_SET);
                }
            }).iterator().next();
        }

//        //@Mutate
//        public void createCUnitTestBinaries(final BinaryContainer binaries, TestSuiteContainer testSuites, @Path("buildDir") File buildDir, ServiceRegistry serviceRegistry) {
//            for (final CUnitTestSuiteSpec cUnitTestSuite : testSuites.withType(CUnitTestSuiteSpec.class)) {
//                for (NativeBinarySpec testedBinary : cUnitTestSuite.getTestedComponent().getNativeBinaries()) {
//
//                    DefaultCUnitTestSuiteBinary testBinary = createTestBinary(serviceRegistry, cUnitTestSuite, testedBinary);
//
//                    configure(testBinary, buildDir);
//
//                    cUnitTestSuite.getBinaries().add(testBinary);
//                    binaries.add(testBinary);
//                }
//            }
//        }

//        private DefaultCUnitTestSuiteBinary createTestBinary(ServiceRegistry serviceRegistry, CUnitTestSuiteSpec cUnitTestSuite, NativeBinarySpec testedBinary) {
////            BinaryNamingScheme namingScheme = new DefaultBinaryNamingSchemeBuilder(((NativeBinarySpecInternal) testedBinary).getNamingScheme())
////                    .withComponentName(cUnitTestSuite.getBaseName())
////                    .withTypeString("CUnitExe").build();
////
////            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
////            NativeDependencyResolver resolver = serviceRegistry.get(NativeDependencyResolver.class);
////            return DefaultCUnitTestSuiteBinary.create(cUnitTestSuite, (NativeBinarySpecInternal) testedBinary, namingScheme, resolver, instantiator);
//            return null;
//        }

        // TODO(daniel): Should be able to use the generic binary setup since you can use
        // the test suite framework with c++ too if you want. Even asm if you really want.
        private void configure(DefaultCUnitTestSuiteBinary testBinary, File buildDir) {
            // TODO(daniel): Done in NativeBinarySpecInitializer
            BinaryNamingScheme namingScheme = testBinary.getNamingScheme();
            PlatformToolProvider toolProvider = testBinary.getPlatformToolProvider();
            File binaryOutputDir = new File(new File(buildDir, "binaries"), namingScheme.getOutputDirectoryBase());
            String baseName = testBinary.getComponent().getBaseName();

            // TODO(daniel): Done in NativeBinarySpecInitializer
            testBinary.setExecutableFile(new File(binaryOutputDir, toolProvider.getExecutableName(baseName)));

            // TODO(daniel): Done at higer level... needed? don't think so as C++ code can be used in a C program.
            ((ExtensionAware) testBinary).getExtensions().create("cCompiler", DefaultPreprocessingTool.class);
        }
    }

    private static class MarkBinariesBuildable implements Action<NativeBinarySpec> {
        public void execute(NativeBinarySpec nativeBinarySpec) {
            NativeToolChainInternal toolChainInternal = (NativeToolChainInternal) nativeBinarySpec.getToolChain();
            boolean canBuild = toolChainInternal.select((NativePlatformInternal) nativeBinarySpec.getTargetPlatform()).isAvailable();
            ((NativeBinarySpecInternal) nativeBinarySpec).setBuildable(canBuild);
        }
    }
}
