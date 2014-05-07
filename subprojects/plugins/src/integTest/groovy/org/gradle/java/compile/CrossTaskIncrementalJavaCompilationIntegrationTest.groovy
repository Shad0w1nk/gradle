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



package org.gradle.java.compile

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.CompilationOutputsFixture

public class CrossTaskIncrementalJavaCompilationIntegrationTest extends AbstractIntegrationSpec {

    CompilationOutputsFixture impl

    def setup() {
        impl = new CompilationOutputsFixture(file("impl/build/classes/main"))

        buildFile << """
            subprojects {
                apply plugin: 'java'
                compileJava.options.incremental = true
                compileJava.options.fork = true
            }
            project(':impl') {
                dependencies { compile project(':api') }
            }
        """
        settingsFile << "include 'api', 'impl'"
    }

    private File java(Map projectToClassBodies) {
        File out
        projectToClassBodies.each { project, bodies ->
            bodies.each { body ->
                def className = (body =~ /(?s).*?class (\w+) .*/)[0][1]
                assert className: "unable to find class name"
                def f = file("$project/src/main/java/${className}.java")
                f.createFile()
                f.text = body
                out = f
            }
        }
        out
    }

    def "detects changed class in an upstream project"() {
        java api: ["class A {}", "class B {}"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class A { String change; }"]
        run "impl:compileJava"

        then:
        impl.recompiledClasses("ImplA")
    }

    def "detects change to transitive dependency in an upstream project"() {
        java api: ["class A {}", "class B extends A {}"]
        java impl: ["class SomeImpl {}", "class ImplB extends B {}", "class ImplB2 extends ImplB {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class A { String change; }"]
        run "impl:compileJava"

        then:
        impl.recompiledClasses("ImplB", "ImplB2")
    }

    def "deletion of jar without dependents does not recompile any classes"() {
        java api: ["class A {}"], impl: ["class SomeImpl {}"]
        impl.snapshot { run "compileJava" }

        when:
        buildFile << """
            project(':impl') {
                configurations.compile.dependencies.clear() //so that api jar is no longer on classpath
            }
        """
        run "impl:compileJava"

        then:
        impl.noneRecompiled()
    }

    def "deletion of jar with dependents causes compilation failure"() {
        java api: ["class A {}"], impl: ["class ImplA extends A {}"]
        impl.snapshot { run "compileJava" }

        when:
        buildFile << """
            project(':impl') {
                configurations.compile.dependencies.clear() //so that api jar is no longer on classpath
            }
        """
        fails "impl:compileJava"

        then:
        impl.noneRecompiled()
    }

    def "detects change to dependency and ensures class dependency info refreshed"() {
        java api: ["class A {}", "class B extends A {}"]
        java impl: ["class SomeImpl {}", "class ImplB extends B {}", "class ImplB2 extends ImplB {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class B { /* remove extends */ }"]
        run "impl:compileJava"

        then: impl.recompiledClasses("ImplB", "ImplB2")

        when:
        impl.snapshot()
        java api: ["class A { /* change */ }"]
        run "impl:compileJava"

        then: impl.noneRecompiled() //because after earlier change to B, class A is no longer a dependency
    }

    def "detects deleted class in an upstream project and fails compilation"() {
        def b = java(api: ["class A {}", "class B {}"])
        java impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        assert b.delete()
        fails "impl:compileJava"

        then:
        impl.noneRecompiled()
    }

    def "recompilation not necessary when upstream does not change any of the actual dependencies"() {
        java api: ["class A {}", "class B {}"], impl: ["class ImplA extends A {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class B { String change; }"]
        run "impl:compileJava"

        then:
        impl.noneRecompiled()
    }

    def "deletion of jar with non-private constant causes full rebuild"() {
        java api: ["class A { final static int x = 1; }"], impl: ["class X {}", "class Y {}"]
        impl.snapshot { run "compileJava" }

        when:
        buildFile << """
            project(':impl') {
                configurations.compile.dependencies.clear() //so that api jar is no longer on classpath
            }
        """
        run "impl:compileJava"

        then:
        impl.recompiledClasses("X", "Y")
    }

    def "change in an upstream class with non-private constant causes full rebuild"() {
        java api: ["class A {}", "class B { final static int x = 1; }"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class B { /* change */ }"]
        run "impl:compileJava"

        then:
        impl.recompiledClasses('ImplA', 'ImplB')
    }

    def "change in an upstream transitive class with non-private constant does not cause full rebuild"() {
        java api: ["class A { final static int x = 1; }", "class B extends A {}"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class B { /* change */ }"]
        run "impl:compileJava"

        then:
        impl.recompiledClasses('ImplB')
    }

    def "private constant in upstream project does not trigger full rebuild"() {
        java api: ["class A {}", "class B { private final static int x = 1; }"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class B { /* change */ }"]
        run "impl:compileJava"

        then:
        impl.recompiledClasses('ImplB')
    }

    def "detects changed classes when upstream project was built in isolation"() {
        java api: ["class A {}", "class B {}"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class A { String change; }"]
        run "api:compileJava"
        run "impl:compileJava"

        then:
        impl.recompiledClasses("ImplA")
    }

    def "detects class changes in subsequent runs ensuring the jar snapshots are refreshed"() {
        java api: ["class A {}", "class B {}"], impl: ["class ImplA extends A {}", "class ImplB extends B {}"]
        impl.snapshot { run "compileJava" }

        when:
        java api: ["class A { String change; }"]
        run "api:compileJava"
        run "impl:compileJava"

        then: impl.recompiledClasses("ImplA")

        when:
        impl.snapshot()
        java api: ["class B { String change; }"]
        run "compileJava"

        then: impl.recompiledClasses("ImplB")
    }

    def "changes to resources in jar do not incur recompilation"() {
        //TODO
    }

    def "each compile task uses separate local cache"() {
        //TODO
        //ensure that local jar snapshots cache does not 'leak' to other compile tasks in this project
    }

    def "class in source dir wins over a duplicate found in some jar on classpath"() {
        //TODO
    }

    def "the order of classpath items is unchanged"() {
        //TODO
    }
}