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
package org.gradle.api.internal;

import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import org.gradle.api.*;
import org.gradle.api.internal.collections.CollectionEventRegister;
import org.gradle.api.internal.collections.CollectionFilter;
import org.gradle.api.internal.plugins.DefaultConvention;
import org.gradle.api.plugins.Convention;
import org.gradle.internal.Transformers;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.ConfigureUtil;

import java.util.Map;
import java.util.Set;

public abstract class AbstractPolymorphicDomainObjectContainer<T>
        extends AbstractNamedDomainObjectContainer<T> implements PolymorphicDomainObjectContainerInternal<T> {

    private final ContainerElementsDynamicObject elementsDynamicObject = new ContainerElementsDynamicObject();
    private final Convention convention;
    private final DynamicObject dynamicObject;

    protected AbstractPolymorphicDomainObjectContainer(Class<T> type, Instantiator instantiator, Namer<? super T> namer) {
        super(type, instantiator, namer);
        this.convention = new DefaultConvention(instantiator);
        this.dynamicObject = new ExtensibleDynamicObject(this, new ContainerDynamicObject(elementsDynamicObject), convention);
    }

    protected AbstractPolymorphicDomainObjectContainer(Class<T> type, Instantiator instantiator) {
        this(type, instantiator, Named.Namer.forType(type));
    }

    protected AbstractPolymorphicDomainObjectContainer(Class<? extends T> type, Set<T> store, CollectionEventRegister<T> eventRegister, Instantiator instantiator, Namer<? super T> namer) {
        super(type, store, eventRegister, instantiator, namer);
        this.convention = new DefaultConvention(instantiator);
        this.dynamicObject = new ExtensibleDynamicObject(this, new ContainerDynamicObject(elementsDynamicObject), convention);
    }

    protected AbstractPolymorphicDomainObjectContainer(AbstractPolymorphicDomainObjectContainer<? super T> collection, CollectionFilter<T> filter, Instantiator instantiator, Namer<? super T> namer) {
        this(filter.getType(), collection.filteredStore(filter), collection.filteredEvents(filter), instantiator, namer);
    }

    protected abstract <U extends T> U doCreate(String name, Class<U> type);

    public <U extends T> U create(String name, Class<U> type) {
        return create(name, type, null);
    }

    public <U extends T> U maybeCreate(String name, Class<U> type) throws InvalidUserDataException {
        T item = findByName(name);
        if (item != null) {
            return Transformers.cast(type).transform(item);
        }
        return create(name, type);
    }

    public <U extends T> U create(String name, Class<U> type, Action<? super U> configuration) {
        assertCanAdd(name);
        U object = doCreate(name, type);
        add(object);
        if (configuration != null) {
            configuration.execute(object);
        }
        return object;
    }

    @Override
    public Convention getConvention() {
        return convention;
    }

    @Override
    protected DynamicObject getElementsAsDynamicObject() {
        return elementsDynamicObject;
    }

    @Override
    public DynamicObject getAsDynamicObject() {
        return dynamicObject;
    }

    @Override
    protected Object createConfigureDelegate(Closure configureClosure) {
        return new PolymorphicDomainObjectContainerConfigureDelegate(configureClosure.getOwner(), this);
    }

    // TODO(daniel): Implement this
//    protected <S extends T> Set<? extends S> filteredTypes(CollectionFilter<S> filter) {
//        return new FilteredSet<Class<? extends T>, S>(getCreateableTypes(), filter);
//    }

    private class ContainerDynamicObject extends CompositeDynamicObject {
        private ContainerDynamicObject(ContainerElementsDynamicObject elementsDynamicObject) {
            setObjects(new BeanDynamicObject(AbstractPolymorphicDomainObjectContainer.this), elementsDynamicObject, getConvention().getExtensionsAsDynamicObject());
        }

        @Override
        protected String getDisplayName() {
            return AbstractPolymorphicDomainObjectContainer.this.getDisplayName();
        }
    }

    private class ContainerElementsDynamicObject extends AbstractDynamicObject {
        @Override
        protected String getDisplayName() {
            return AbstractPolymorphicDomainObjectContainer.this.getDisplayName();
        }

        @Override
        public boolean hasProperty(String name) {
            return findByName(name) != null;
        }

        @Override
        public Object getProperty(String name) throws MissingPropertyException {
            Object object = findByName(name);
            if (object == null) {
                return super.getProperty(name);
            }
            return object;
        }

        @Override
        public Map<String, T> getProperties() {
            return getAsMap();
        }

        @Override
        public boolean hasMethod(String name, Object... arguments) {
            return isConfigureMethod(name, arguments);
        }

        @Override
        public Object invokeMethod(String name, Object... arguments) throws groovy.lang.MissingMethodException {
            if (isConfigureMethod(name, arguments)) {
                T element = getByName(name);
                Object lastArgument = arguments[arguments.length - 1];
                if (lastArgument instanceof Closure) {
                    ConfigureUtil.configure((Closure) lastArgument, element);
                }
                return element;
            } else {
                return super.invokeMethod(name, arguments);
            }
        }

        private boolean isConfigureMethod(String name, Object... arguments) {
            return (arguments.length == 1 && arguments[0] instanceof Closure
                    || arguments.length == 1 && arguments[0] instanceof Class
                    || arguments.length == 2 && arguments[0] instanceof Class && arguments[1] instanceof Closure)
                    && hasProperty(name);
        }
    }

    public <U extends T> PolymorphicDomainObjectContainer<U> containerWithType(Class<U> type) {
        return new TypedPolymorphicDomainObjectContainerWrapper(this, this.createFilter(type));
    }

    private class TypedPolymorphicDomainObjectContainerWrapper<V> extends AbstractPolymorphicDomainObjectContainer<V> {
        private final AbstractPolymorphicDomainObjectContainer<? super V> parent;
        public TypedPolymorphicDomainObjectContainerWrapper(AbstractPolymorphicDomainObjectContainer<? super V> collection, CollectionFilter<V> filter) {
            super(filter.getType(), collection.filteredStore(filter), new CollectionEventRegister<V>(), collection.getInstantiator(), collection.getNamer());
            this.parent = collection;
        }

        @Override
        protected <U extends V> U doCreate(String name, Class<U> type) {
            return parent.doCreate(name, type);
        }

        @Override
        protected V doCreate(String name) {
            return parent.doCreate(name, getType());
        }

        public Set<? extends Class<? extends V>> getCreateableTypes() {
            // TODO(daniel): Filter this
            return null;//parent.getCreateableTypes();
        }
    }
}
