/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.tasks.cache

import spock.lang.Subject

import static org.gradle.api.internal.tasks.CacheableTaskOutputFilePropertySpec.OutputType.DIRECTORY
import static org.gradle.api.internal.tasks.CacheableTaskOutputFilePropertySpec.OutputType.FILE

@Subject(OutputPreparingTaskOutputPacker)
class OutputPreparingTaskOutputPackerTest extends AbstractTaskOutputPackerSpec {
    def delegate = Mock(TarTaskOutputPacker)
    def packer = new OutputPreparingTaskOutputPacker(delegate)
    def input = Mock(InputStream)

    def "cleans up leftover files"() {
        def targetOutputFile = tempDir.file("output.txt")
        def targetOutputDir = tempDir.file("output").createDir()
        targetOutputFile << "Some data"
        targetOutputDir.file("sub-dir/data.txt") << "Some data"

        when:
        packer.unpack(taskOutputs, input)

        then:
        1 * taskOutputs.getFileProperties() >> ([
            new TestProperty(propertyName: "testFile", outputFile: targetOutputFile),
            new TestProperty(propertyName: "testDir", outputFile: targetOutputDir)
        ] as SortedSet)
        1 * delegate.unpack(taskOutputs, input)

        then:
        !targetOutputFile.exists()
        targetOutputDir.exists()
        targetOutputDir.assertIsEmptyDir()

        then:
        0 * _
    }

    def "creates necessary directories"() {
        def targetOutputFile = tempDir.file("build/some-dir/output.txt")
        def targetOutputDir = tempDir.file("output")

        when:
        packer.unpack(taskOutputs, input)

        then:
        1 * taskOutputs.getFileProperties() >> ([
            new TestProperty(propertyName: "testFile", outputFile: targetOutputFile, outputType: FILE),
            new TestProperty(propertyName: "testDir", outputFile: targetOutputDir, outputType: DIRECTORY)
        ] as SortedSet)
        1 * delegate.unpack(taskOutputs, input)

        then:
        !targetOutputFile.exists()
        targetOutputFile.parentFile.directory
        targetOutputDir.directory

        then:
        0 * _
    }

    def "creates directories even if there is a pre-existing file in its place"() {
        def targetOutputFile = tempDir.file("build/some-dir/output.txt").createDir()
        def targetOutputDir = tempDir.file("output") << "This should become a directory"

        when:
        packer.unpack(taskOutputs, input)

        then:
        1 * taskOutputs.getFileProperties() >> ([
            new TestProperty(propertyName: "testFile", outputFile: targetOutputFile, outputType: FILE),
            new TestProperty(propertyName: "testDir", outputFile: targetOutputDir, outputType: DIRECTORY)
        ] as SortedSet)
        1 * delegate.unpack(taskOutputs, input)

        then:
        !targetOutputFile.exists()
        targetOutputFile.parentFile.directory
        targetOutputDir.directory

        then:
        0 * _
    }
}
