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

package org.gradle.api.internal.artifacts.component;

import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ComponentReplacements {

    private final List<DependencyConflictResolver> replacements = new LinkedList<DependencyConflictResolver>();

    public ComponentReplacementTarget from(String moduleIdentifier) {
        return new ComponentReplacementTarget(moduleIdentifier, replacements);
    }

    public Collection<DependencyConflictResolver> toConflictResolvers() {
        return replacements;
    }
}