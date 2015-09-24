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

import org.gradle.api.Transformer;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.ide.visualstudio.VisualStudioProject;
import org.gradle.ide.visualstudio.VisualStudioSolution;
import org.gradle.internal.Factories;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.model.ModelMap;
import org.gradle.model.collection.internal.PolymorphicModelMapProjection;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;
import org.gradle.model.internal.manage.schema.SpecializedMapSchema;
import org.gradle.model.internal.registry.RuleContext;
import org.gradle.model.internal.type.ModelType;
import org.gradle.model.internal.type.ModelTypes;

import java.util.Collections;

public class DefaultVisualStudioExtension implements VisualStudioExtensionInternal {
    private final MutableModelNode solutions;
    private final MutableModelNode projects;
    private final MutableModelNode modelNode;
    //private Project project;

    public DefaultVisualStudioExtension(MutableModelNode modelNode, final Instantiator instantiator, final FileResolver fileResolver, ModelSchemaStore schemaStore) {
        this.modelNode = modelNode;

        modelNode.addLink(
            createNode(modelNode.getPath().child("solutions"), VisualStudioSolution.class, VisualStudioSolutionRegistry.class, ((SpecializedMapSchema<VisualStudioSolutionRegistry>) schemaStore.getSchema(ModelType.of(VisualStudioSolutionRegistry.class))).getImplementationType().asSubclass(VisualStudioSolutionRegistry.class), new SimpleModelRuleDescriptor(modelNode.getPath() + ".solutions"), instantiator, fileResolver));


//        modelNode.addLink(
//            ModelCreators.of(
//                modelNode.getPath().child("solutions"), Actions.doNothing())
//                .descriptor(modelNode.getDescriptor(), ".solutions")
//                .withProjection(
//                    ModelMapModelProjection.unmanaged(
//                        VisualStudioSolution.class,
//                        NodeBackedModelMap.createUsingParentNode(new Transformer<NamedEntityInstantiator<VisualStudioSolution>, MutableModelNode>() {
//                            @Override
//                            public NamedEntityInstantiator<VisualStudioSolution> transform(MutableModelNode mutableModelNode) {
//                                return new NamedEntityInstantiator<VisualStudioSolution>() {
//                                    @Override
//                                    public <S extends VisualStudioSolution> S create(String name, Class<S> type) {
//                                        return instantiator.newInstance(type, name, fileResolver, instantiator);
//                                    }
//                                };
//                            }
//                        })
//                    )
//                )
//                .build()
//        );
        solutions = modelNode.getLink("solutions");

//        ModelType<VisualStudioProjectRegistry> projectRegistryType = ModelType.of(VisualStudioProjectRegistry.class);
//        ModelType<VisualStudioProject> projectType = ModelType.of(VisualStudioProject.class);
//        ChildNodeInitializerStrategy<VisualStudioProject> projectFactory = NodeBackedModelMap.createUsingParentNode(new Transformer<NamedEntityInstantiator<VisualStudioProject>, MutableModelNode>() {
//            @Override
//            public NamedEntityInstantiator<VisualStudioProject> transform(MutableModelNode mutableModelNode) {
//                return new NamedEntityInstantiator<VisualStudioProject>() {
//                    @Override
//                    public <S extends VisualStudioProject> S create(String name, Class<S> type) {
//                        return instantiator.newInstance(type, name, fileResolver, instantiator);
//                    }
//                };
//            }
//        });


//        modelNode.addLink(
//            ModelCreators.of(
//                modelNode.getPath().child("projects"), Actions.doNothing())
//                .descriptor(modelNode.getDescriptor(), ".projects")
//                .withProjection(
//                    ModelMapModelProjection.unmanaged(
//                        VisualStudioProject.class,
//                        NodeBackedModelMap.createUsingParentNode(new Transformer<NamedEntityInstantiator<VisualStudioProject>, MutableModelNode>() {
//                            @Override
//                            public NamedEntityInstantiator<VisualStudioProject> transform(MutableModelNode mutableModelNode) {
//                                return new NamedEntityInstantiator<VisualStudioProject>() {
//                                    @Override
//                                    public <S extends VisualStudioProject> S create(String name, Class<S> type) {
//                                        return instantiator.newInstance(type, name, fileResolver, instantiator);
//                                    }
//                                };
//                            }
//                        })
//                    )
//                ).withProjection(UnmanagedModelProjection.of(VisualStudioProjectRegistry.class))
//                //.withProjection(new SpecializedModelMapProjection<VisualStudioProjectRegistry, VisualStudioProject>(ModelType.of(VisualStudioProjectRegistry.class), ModelType.of(VisualStudioProject.class), ModelType.of(VisualStudioProjectRegistry.class), null)))
//                .build()
//        );
        modelNode.addLink(
            createNode(
                modelNode.getPath().child("projects"),
                VisualStudioProject.class,
                VisualStudioProjectRegistry.class,
                ((SpecializedMapSchema<VisualStudioProjectRegistry>) schemaStore.getSchema(ModelType.of(VisualStudioProjectRegistry.class))).getImplementationType().asSubclass(VisualStudioProjectRegistry.class),
                new SimpleModelRuleDescriptor(modelNode.getPath() + ".projects"),
                instantiator,
                fileResolver));

        projects = modelNode.getLink("projects");
    }

    private static <T, C extends ModelMap<T>> ModelCreator createNode(ModelPath path, final Class<T> typeClass, Class<C> containerClass, Class<? extends C> viewClass, ModelRuleDescriptor descriptor, final Instantiator instantiator, final FileResolver fileResolver) {
        ModelType<C> registryType = ModelType.of(containerClass);
        ModelType<T> modelType = ModelType.of(typeClass);
        ChildNodeInitializerStrategy<T> factory = NodeBackedModelMap.createUsingParentNode(new Transformer<NamedEntityInstantiator<T>, MutableModelNode>() {
            @Override
            public NamedEntityInstantiator<T> transform(MutableModelNode modelNode) {
                return new NamedEntityInstantiator<T>() {
                    @Override
                    public <S extends T> S create(String name, Class<S> type) {
                        return instantiator.newInstance(type, name, fileResolver, instantiator);
                    }
                };
            }
        });
        return ModelCreators.of(
            ModelReference.of(path, registryType), Factories.<C>constantNull())
            .descriptor(descriptor)
            .withProjection(new SpecializedModelMapProjection<C, T>(registryType, modelType, viewClass, factory))
            .withProjection(PolymorphicModelMapProjection.of(modelType, factory))
            .build();
    }

    public ModelMap<VisualStudioProject> getProjects() {
        projects.ensureUsable();
        return projects.asWritable(
            ModelTypes.modelMap(VisualStudioProject.class),
            RuleContext.nest(modelNode.toString() + ".getProjects()"),
            Collections.<ModelView<?>>emptyList()
        ).getInstance();
    }

    public ModelMap<VisualStudioSolution> getSolutions() {
        solutions.ensureUsable();
        return solutions.asWritable(
            ModelTypes.modelMap(VisualStudioSolution.class),
            RuleContext.nest(modelNode.toString() + ".getSolutions()"),
            Collections.<ModelView<?>>emptyList()
        ).getInstance();
    }

//    public boolean isRoot() {
//        return project.getParent() == null;
//    }
//
//    public Project getProject() {
//        return project;
//    }
//
//    public void setProject(Project project) {
//        this.project = project;
//    }
}
