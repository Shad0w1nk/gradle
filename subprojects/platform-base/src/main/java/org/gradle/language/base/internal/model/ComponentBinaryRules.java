/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.language.base.internal.model;

import org.gradle.api.Action;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.model.*;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.ComponentSpec;
import org.gradle.platform.base.internal.BinarySpecInternal;

/**
 * Initializes binaries as they are added to components.
 *
 * Note: It's applied to components instead of binaries straight, because binaries are accessible
 * via multiple paths, and thus rules are applied to them multiple times.
 */
@SuppressWarnings("unused")
public class ComponentBinaryRules extends RuleSource {
    @Rules
    void inputRules(AttachInputs attachInputs, ComponentSpec component) {
        attachInputs.setBinaries(component.getBinaries());
        attachInputs.setSources(component.getSources());
    }

    static abstract class AttachInputs extends RuleSource {
        @RuleTarget
        abstract ModelMap<BinarySpec> getBinaries();
        abstract void setBinaries(ModelMap<BinarySpec> binaries);

        @RuleInput
        abstract ModelMap<LanguageSourceSet> getSources();
        abstract void setSources(ModelMap<LanguageSourceSet> sources);

        @Mutate
        void initializeBinarySourceSets(ModelMap<BinarySpec> binaries) {
            binaries.withType(BinarySpecInternal.class).beforeEach(new Action<BinarySpecInternal>() {
                @Override
                public void execute(BinarySpecInternal binary) {
                    binary.getInputs().addAll(getSources().values());
                }
            });
        }
    }
}
