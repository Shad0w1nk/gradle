/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.nativeplatform.internal;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.specs.Spec;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativeplatform.NativeLibraryRequirement;
import org.gradle.nativeplatform.NativeLibrarySpec;
import org.gradle.nativeplatform.test.NativeTestSuiteSpec;

public class DefaultNativeLibrarySpec extends AbstractTargetedNativeComponentSpec implements NativeLibrarySpec {
    private PolymorphicDomainObjectContainer<NativeTestSuiteSpec> testSuites = null;
    private DefaultNamedDomainObjectSet<NativeTestSuiteSpec> delegate = null;

    public String getDisplayName() {
        return String.format("native library '%s'", getName());
    }

    public NativeLibraryRequirement getShared() {
        return new ProjectNativeLibraryRequirement(getProjectPath(), this.getName(), "shared");
    }

    public NativeLibraryRequirement getStatic() {
        return new ProjectNativeLibraryRequirement(getProjectPath(), this.getName(), "static");
    }

    public NativeLibraryRequirement getApi() {
        return new ProjectNativeLibraryRequirement(getProjectPath(), this.getName(), "api");
    }

    public void setTestSuites(PolymorphicDomainObjectContainer<NativeTestSuiteSpec> testSuites) {
        this.testSuites = testSuites;
        testSuites.whenObjectAdded(new Action<NativeTestSuiteSpec>() {
            public void execute(NativeTestSuiteSpec testSuite) {
                testSuite.setTestedComponent(DefaultNativeLibrarySpec.this);
            }
        });
    }

    public NamedDomainObjectSet<NativeTestSuiteSpec> getTestSuite() {
        return testSuites.withType(NativeTestSuiteSpec.class).matching(new Spec<NativeTestSuiteSpec>() {
            public boolean isSatisfiedBy(NativeTestSuiteSpec library) {
                return library.getTestedComponent() == DefaultNativeLibrarySpec.this;
            }
        });
    }

    public void testSuites(Action<? super PolymorphicDomainObjectContainer<NativeTestSuiteSpec>> action) {
        if (testSuites == null) {
            throw new RuntimeException("Apply a test plugin first.");
        }
        action.execute(testSuites);
    }

    public void testSuites(Closure<Void> configClosure) {
        if (testSuites == null) {
            throw new RuntimeException("Apply a test plugin first.");
        }
        testSuites.configure(configClosure);
    }
}