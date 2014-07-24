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
package org.gradle.nativebinaries.test.cunit

import org.gradle.nativebinaries.language.c.plugins.CPlugin
import org.gradle.nativebinaries.test.cunit.plugins.CUnitPlugin;
import org.gradle.nativebinaries.test.TestSuiteContainer
import org.gradle.util.TestUtil
import spock.lang.Specification

class CUnitTest extends Specification {
    final def project = TestUtil.createRootProject();

    def "check the correct binary type are created for the test suite"() {
        given:
        project.plugins.apply(CPlugin)
        project.libraries.create("main")
        project.plugins.apply(CUnitPlugin)

        when:
        project.testSuites.create("mainTest", CUnitTestSuite) {
            testedComponent project.libraries.main
        }
        project.evaluate()

        then:
        def binaries = project.getExtensions().getByType(TestSuiteContainer).getByName("mainTest").binaries
        binaries.collect({it instanceof CUnitTestSuiteBinary}) == [true]*binaries.size()
    }

    def "check the tested component cannot be changed once it is set"() {
        given:
        project.plugins.apply(CPlugin)
        project.libraries.create("main")
        project.libraries.create("anotherMain")
        project.plugins.apply(CUnitPlugin)
        project.testSuites.create("mainTest", CUnitTestSuite) {
            testedComponent project.libraries.main
        }

        when:
        project.testSuites.mainTest.testedComponent project.libraries.anotherMain

        then:
        thrown IllegalArgumentException
    }
}
