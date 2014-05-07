/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.configuration;

import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.file.FileLookup;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.api.internal.initialization.ScriptHandlerFactory;
import org.gradle.api.internal.project.ProjectScript;
import org.gradle.api.plugins.PluginAware;
import org.gradle.groovy.scripts.*;
import org.gradle.groovy.scripts.internal.BuildScriptTransformer;
import org.gradle.groovy.scripts.internal.PluginsAndBuildscriptTransformer;
import org.gradle.groovy.scripts.internal.StatementExtractingScriptTransformer;
import org.gradle.internal.Factory;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.internal.exceptions.LocationAwareException;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.gradle.logging.LoggingManagerInternal;
import org.gradle.plugin.internal.*;
import org.gradle.plugin.resolve.internal.InvalidPluginRequestException;
import org.gradle.plugin.resolve.internal.NoopPluginResolver;
import org.gradle.plugin.resolve.internal.PluginRequest;
import org.gradle.plugin.resolve.internal.PluginResolver;
import org.gradle.util.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultScriptPluginFactory implements ScriptPluginFactory {
    private final ScriptCompilerFactory scriptCompilerFactory;
    private final ImportsReader importsReader;
    private final Factory<LoggingManagerInternal> loggingManagerFactory;
    private final Instantiator instantiator;
    private final ScriptHandlerFactory scriptHandlerFactory;
    private final PluginResolverFactory pluginResolverFactory;
    private final FileLookup fileLookup;

    public DefaultScriptPluginFactory(ScriptCompilerFactory scriptCompilerFactory,
                                      ImportsReader importsReader,
                                      Factory<LoggingManagerInternal> loggingManagerFactory,
                                      Instantiator instantiator,
                                      ScriptHandlerFactory scriptHandlerFactory,
                                      PluginResolverFactory pluginResolverFactory,
                                      FileLookup fileLookup) {
        this.scriptCompilerFactory = scriptCompilerFactory;
        this.importsReader = importsReader;
        this.loggingManagerFactory = loggingManagerFactory;
        this.instantiator = instantiator;
        this.scriptHandlerFactory = scriptHandlerFactory;
        this.pluginResolverFactory = pluginResolverFactory;
        this.fileLookup = fileLookup;
    }

    public ScriptPlugin create(ScriptSource scriptSource, ScriptHandler scriptHandler, ClassLoaderScope classLoaderScope, String classpathClosureName, Class<? extends BasicScript> scriptClass) {
        return new ScriptPluginImpl(scriptSource, scriptHandler, classLoaderScope, classpathClosureName, scriptClass);
    }

    private class ScriptPluginImpl implements ScriptPlugin {
        private final ScriptSource scriptSource;
        private final ClassLoaderScope classLoaderScope;
        private final String classpathClosureName;
        private final Class<? extends BasicScript> scriptType;
        private final ScriptHandler scriptHandler;

        public ScriptPluginImpl(ScriptSource scriptSource, ScriptHandler scriptHandler, ClassLoaderScope classLoaderScope, String classpathClosureName, Class<? extends BasicScript> scriptType) {
            this.scriptSource = scriptSource;
            this.classLoaderScope = classLoaderScope;
            this.classpathClosureName = classpathClosureName;
            this.scriptHandler = scriptHandler;
            this.scriptType = scriptType;
        }


        public ScriptSource getSource() {
            return scriptSource;
        }

        public void apply(final Object target) {
            DefaultServiceRegistry services = new DefaultServiceRegistry();
            services.add(ScriptPluginFactory.class, DefaultScriptPluginFactory.this);
            services.add(ScriptHandlerFactory.class, scriptHandlerFactory);
            services.add(ClassLoaderScope.class, classLoaderScope);
            services.add(LoggingManagerInternal.class, loggingManagerFactory.create());
            services.add(Instantiator.class, instantiator);
            services.add(ScriptHandler.class, scriptHandler);
            services.add(FileLookup.class, fileLookup);

            ScriptSource withImports = importsReader.withImports(scriptSource);

            PluginDependenciesService pluginDependenciesService = new PluginDependenciesService(getSource());
            services.add(PluginDependenciesService.class, pluginDependenciesService);

            ScriptCompiler compiler = scriptCompilerFactory.createCompiler(withImports);
            compiler.setClassloader(classLoaderScope.getBase().getChildClassLoader());

            boolean supportsPluginsBlock = ProjectScript.class.isAssignableFrom(scriptType);
            String onPluginBlockError = supportsPluginsBlock ? null : "Only Project build scripts can contain plugins {} blocks";

            PluginsAndBuildscriptTransformer scriptBlockTransformer = new PluginsAndBuildscriptTransformer(classpathClosureName, onPluginBlockError);

            StatementExtractingScriptTransformer classpathScriptTransformer = new StatementExtractingScriptTransformer(classpathClosureName, scriptBlockTransformer);

            compiler.setTransformer(classpathScriptTransformer);

            ScriptRunner<? extends BasicScript> classPathScriptRunner = compiler.compile(scriptType);
            classPathScriptRunner.getScript().init(target, services);
            classPathScriptRunner.run();

            Configuration classpathConfiguration = scriptHandler.getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION);
            Set<File> files = classpathConfiguration.getFiles();
            ClassPath classPath = new DefaultClassPath(files);

            ClassLoader exportedClassLoader = classLoaderScope.export(classPath);

            List<PluginRequest> pluginRequests = pluginDependenciesService.getRequests();
            if (!pluginRequests.isEmpty()) {

                Map<String, List<PluginRequest>> groupedById = CollectionUtils.groupBy(pluginRequests, new Transformer<String, PluginRequest>() {
                    public String transform(PluginRequest pluginRequest) {
                        return pluginRequest.getId();
                    }
                });

                // Ignore duplicate noops - noop plugin just used for testing
                groupedById.remove(NoopPluginResolver.PLUGIN_ID);

                // Check for duplicates
                for (Map.Entry<String, List<PluginRequest>> entry : groupedById.entrySet()) {
                    List<PluginRequest> pluginRequestsForId = entry.getValue();
                    if (pluginRequestsForId.size() > 1) {
                        PluginRequest first = pluginRequests.get(0);
                        PluginRequest second = pluginRequests.get(1);

                        InvalidPluginRequestException exception = new InvalidPluginRequestException(second, "Plugin with id '" + entry.getKey() + "' was already requested at line " + first.getLineNumber());
                        throw new LocationAwareException(exception, second.getScriptSource(), second.getLineNumber());
                    }
                }

                PluginResolver pluginResolver = pluginResolverFactory.createPluginResolver(exportedClassLoader);
                @SuppressWarnings("ConstantConditions")
                PluginResolutionApplicator resolutionApplicator = new PluginResolutionApplicator((PluginAware) target, classLoaderScope);
                PluginRequestApplicator requestApplicator = new PluginRequestApplicator(pluginResolver, resolutionApplicator);
                requestApplicator.applyPlugin(pluginRequests);
            }

            classLoaderScope.lock();

            compiler.setClassloader(classLoaderScope.getScopeClassLoader());

            compiler.setTransformer(new BuildScriptTransformer("no_" + classpathScriptTransformer.getId(), classpathScriptTransformer.invert()));
            ScriptRunner<? extends BasicScript> runner = compiler.compile(scriptType);

            BasicScript script = runner.getScript();
            script.init(target, services);
            if (target instanceof ScriptAware) {
                ((ScriptAware) target).setScript(script);
            }
            runner.run();
        }

    }
}