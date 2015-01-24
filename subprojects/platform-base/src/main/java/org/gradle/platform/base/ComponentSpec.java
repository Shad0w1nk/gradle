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

package org.gradle.platform.base;

import org.gradle.api.*;
import org.gradle.internal.HasInternalProtocol;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.platform.base.test.TestSuiteSpec;

/**
 * A software component that is built by a Gradle project.
 */
@Incubating
@HasInternalProtocol
public interface ComponentSpec extends Named {
    /**
     * The path the the project containing this component.
     */
    String getProjectPath();

    /**
     * Returns a human-consumable display name for this component.
     */
    String getDisplayName();

    /**
     * The source sets that are used to build this component.
     */
    DomainObjectSet<LanguageSourceSet> getSource();

    /**
     * Configure the source sets used to build this component.
     */
    void sources(Action<? super PolymorphicDomainObjectContainer<LanguageSourceSet>> action);

    /**
     * The test suites that are used to test this component.
     */
    NamedDomainObjectSet<TestSuiteSpec> getTestSuites();

    /**
     * Configure the test suites used to test this component.
     */
    void testSuites(Action<? super PolymorphicDomainObjectContainer<TestSuiteSpec>> action);

    /**
     * The binaries that are built for this component. You can use this to configure the binaries for this component.
     */
    DomainObjectSet<BinarySpec> getBinaries();
}
