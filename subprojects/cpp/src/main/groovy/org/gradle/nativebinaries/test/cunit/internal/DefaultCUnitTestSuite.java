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
package org.gradle.nativebinaries.test.cunit.internal;

import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.nativebinaries.ProjectNativeBinary;
import org.gradle.nativebinaries.ProjectNativeComponent;
import org.gradle.nativebinaries.internal.AbstractProjectNativeComponent;
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal;
import org.gradle.nativebinaries.internal.resolve.NativeDependencyResolver;
import org.gradle.nativebinaries.language.internal.DefaultPreprocessingTool;
import org.gradle.nativebinaries.test.cunit.CUnitTestSuite;
import org.gradle.nativebinaries.test.cunit.CUnitTestSuiteBinary;
import org.gradle.runtime.base.NamedProjectComponentIdentifier;
import org.gradle.runtime.base.internal.BinaryNamingScheme;
import org.gradle.runtime.base.internal.DefaultBinaryNamingSchemeBuilder;

public class DefaultCUnitTestSuite extends AbstractProjectNativeComponent implements CUnitTestSuite {
    private ProjectNativeComponent testedComponent;
    private final ConfigureCUnitTestSources configureCUnitTestSources;
    private final CreateCUnitBinaries createCUnitBinaries;

    public DefaultCUnitTestSuite(NamedProjectComponentIdentifier id, ConfigureCUnitTestSources configureCUnitTestSources, CreateCUnitBinaries createCUnitBinaries) {
        super(id);
        this.configureCUnitTestSources = configureCUnitTestSources;
        this.createCUnitBinaries = createCUnitBinaries;
    }

    public String getDisplayName() {
        return String.format("cunit tests '%s'", getName());
    }

    public DefaultCUnitTestSuite testedComponent(ProjectNativeComponent testedComponent) {
        setTestedComponent(testedComponent);
        return this;
    }
    public void setTestedComponent(ProjectNativeComponent testedComponent) {
        if (this.testedComponent != null) {
            throw new IllegalArgumentException("You cannot change the tested component once it's setup");
        } else {
            this.testedComponent = testedComponent;

            configureCUnitTestSources.apply(this);
            createCUnitBinaries.apply(this);
        }
    }
    public ProjectNativeComponent getTestedComponent() {
        return testedComponent;
    }
}
