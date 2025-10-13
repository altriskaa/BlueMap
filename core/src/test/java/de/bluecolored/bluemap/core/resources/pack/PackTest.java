/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.resources.pack;

import de.bluecolored.bluemap.core.util.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the abstract Pack class, focusing on the loadResourcePath method.
 * This version uses JUnit 5's @TempDir for file operations, requiring no extra dependencies.
 */
@ExtendWith(MockitoExtension.class)
class PackTest {

    @TempDir
    Path tempDir;

    @Mock
    private Pack.PathLoader mockLoader;

    @Spy
    private de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack pack = new de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack(15, null);

    @Test
    void loadResourcePath_givenSimpleDirectory_thenLoadsRoot() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("pack");
        Files.createDirectory(root);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenValidZipFile_thenRecursivelyLoadsContent() throws IOException, InterruptedException {
        // Arrange
        Path zipPath = tempDir.resolve("test.zip");
        URI zipUri = URI.create("jar:" + zipPath.toUri());

        try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, Map.of("create", "true"))) {
            Path internalDir = zipfs.getPath("/assets/minecraft");
            Files.createDirectories(internalDir);
        }

        // Act
        pack.loadResourcePath(zipPath, mockLoader);

        // Assert
        verify(pack, times(2)).loadResourcePath(any(), any());

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);

        verify(mockLoader, times(1)).load(pathCaptor.capture());

        Path capturedPath = pathCaptor.getValue();
        assertEquals("/", capturedPath.toString(), "The loaded path inside the zip should be the root.");
    }

    @Test
    void loadResourcePath_givenCorruptedZipFile_thenDoesNotCrash() throws IOException, InterruptedException {
        // Arrange
        Path corruptedZip = tempDir.resolve("corrupted.zip");
        Files.writeString(corruptedZip, "This is not a zip file");

        // Act
        pack.loadResourcePath(corruptedZip, mockLoader);

        // Assert
        verify(mockLoader, never()).load(any());
    }

    @Test
    void loadResourcePath_givenFabricJsonWithNestedJar_thenRecursivelyLoadsJar() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("fabric-mod");
        Files.createDirectory(root);

        Path nestedJar = root.resolve("nested.jar");
        Files.createFile(nestedJar);

        Path fabricJson = root.resolve("fabric.mod.json");
        String jsonContent = """
        {
          "schemaVersion": 1,
          "id": "my-mod",
          "jars": [
            { "file": "nested.jar" }
          ]
        }
        """;
        Files.writeString(fabricJson, jsonContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        verify(pack, times(1)).loadResourcePath(eq(nestedJar), eq(mockLoader));
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenFabricJsonWithMissingJar_thenSkipsAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("fabric-mod");
        Files.createDirectory(root);

        Path fabricJson = root.resolve("fabric.mod.json");
        String jsonContent = """
        { "jars": [{ "file": "missing.jar" }] }
        """;
        Files.writeString(fabricJson, jsonContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        verify(pack, times(1)).loadResourcePath(root, mockLoader); // Only called on root
        verify(mockLoader, times(1)).load(root); // Still loads the root
    }

    @Test
    void loadResourcePath_whenRequiredFeaturesNotMet_thenSkipsLoading() throws IOException, InterruptedException {
        // Arrange
        Set<Key> enabledFeatures = Collections.emptySet();
        de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack featurePack = spy(new de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack(15, enabledFeatures));

        Path root = tempDir.resolve("feature-pack");
        Files.createDirectory(root);
        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
        {
          "features": { "enabled": ["bluemap:hires"] }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        featurePack.loadResourcePath(root, mockLoader);

        // Assert
        verify(mockLoader, never()).load(any());
    }

    @Test
    void loadResourcePath_whenRequiredFeaturesMet_thenLoadsNormally() throws IOException, InterruptedException {
        // Arrange
        Set<Key> enabledFeatures = Set.of(new Key("bluemap:hires"));
        de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack featurePack = spy(new de.bluecolored.bluemap.core.resources.pack.PackTest.TestPack(15, enabledFeatures));

        Path root = tempDir.resolve("feature-pack");
        Files.createDirectory(root);
        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
        {
          "features": { "enabled": ["bluemap:hires"] }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        featurePack.loadResourcePath(root, mockLoader);

        // Assert
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenMultipleOverlays_thenLoadsInReverseOrder() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("overlay-pack");
        Path overlay1 = root.resolve("overlay1");
        Path overlay2 = root.resolve("overlay2");
        Files.createDirectories(overlay1);
        Files.createDirectories(overlay2);

        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
        {
          "overlays": {
            "entries": [
              { "directory": "overlay1", "formats": [10, 20] },
              { "directory": "overlay2", "formats": [10, 20] }
            ]
          }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        InOrder inOrder = inOrder(mockLoader, pack);
        // Overlays are loaded first, in reverse order of the mcmeta file
        inOrder.verify(pack).loadResourcePath(eq(overlay2), any());
        inOrder.verify(mockLoader).load(overlay2);
        inOrder.verify(pack).loadResourcePath(eq(overlay1), any());
        inOrder.verify(mockLoader).load(overlay1);
        // Root is loaded last
        inOrder.verify(mockLoader).load(root);
    }

    @Test
    void loadResourcePath_givenOverlayWithMismatchedVersion_thenSkipsOverlay() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("overlay-pack");
        Path overlay1 = root.resolve("good-overlay");
        Path overlay2 = root.resolve("bad-overlay");
        Files.createDirectories(overlay1);
        Files.createDirectories(overlay2);

        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
        {
          "overlays": {
            "entries": [
              { "directory": "good-overlay", "formats": [10, 20] },
              { "directory": "bad-overlay", "formats": [90, 100] }
            ]
          }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        // pack has version 15, so "good-overlay" should match but "bad-overlay" should not.
        pack.loadResourcePath(root, mockLoader);

        // Assert
        verify(pack, times(1)).loadResourcePath(eq(overlay1), any());
        verify(pack, never()).loadResourcePath(eq(overlay2), any());
        verify(mockLoader, times(1)).load(overlay1);
        verify(mockLoader, times(1)).load(root);
        verify(mockLoader, never()).load(overlay2);
    }

    @Test
    void loadResourcePath_givenNestedDatapacks_thenLoadsRecursively() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("datapack-test");
        Path datapacksDir = root.resolve("data/minecraft/datapacks");
        Path pack1 = datapacksDir.resolve("pack1");
        Path pack2 = datapacksDir.resolve("pack2");
        Files.createDirectories(pack1);
        Files.createDirectories(pack2);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        InOrder inOrder = inOrder(mockLoader, pack);
        inOrder.verify(pack).loadResourcePath(eq(pack1), any());
        inOrder.verify(mockLoader).load(pack1);
        inOrder.verify(pack).loadResourcePath(eq(pack2), any());
        inOrder.verify(mockLoader).load(pack2);
        inOrder.verify(mockLoader).load(root);
    }

    @Test
    void loadResourcePath_whenThreadIsInterrupted_thenThrowsInterruptedException() {
        // Arrange
        Path root = tempDir.resolve("any-pack");
        Thread.currentThread().interrupt(); // Set the interrupted flag

        // Act & Assert
        assertThrows(InterruptedException.class, () -> {
            pack.loadResourcePath(root, mockLoader);
        });

        // Clear the flag for subsequent tests
        Thread.interrupted();
    }

    /**
     * A concrete implementation of the abstract Pack class for testing purposes.
     */
    private static class TestPack extends Pack {
        public TestPack(int packVersion, Set<Key> enabledFeatures) {
            super(packVersion, enabledFeatures);
        }

        @Override
        public void loadResources(Iterable<Path> roots) {
        }
    }

    @Test
    void loadResourcePath_givenZipWithFileAtRoot_thenSkipsFileAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path zipPath = tempDir.resolve("root-file.zip");
        URI zipUri = URI.create("jar:" + zipPath.toUri());

        try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, Map.of("create", "true"))) {
            // Create a file and a directory at the root of the archive
            Files.createFile(zipfs.getPath("/info.txt"));
            Files.createDirectory(zipfs.getPath("/assets"));
        }

        // Act
        pack.loadResourcePath(zipPath, mockLoader);

        // Assert
        // The loader should be called on the directory, but the file should be skipped by the `continue` statement.
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(mockLoader, times(1)).load(pathCaptor.capture());
        assertEquals("/", pathCaptor.getValue().toString());
    }

    @Test
    void loadResourcePath_givenFabricJsonWithoutJarsKey_thenSkipsAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("fabric-mod-no-jars");
        Files.createDirectory(root);

        Path fabricJson = root.resolve("fabric.mod.json");
        // This JSON is valid but is missing the "jars" key.
        String jsonContent = """
        {
          "schemaVersion": 1,
          "id": "my-mod"
        }
        """;
        Files.writeString(fabricJson, jsonContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // Verify that the main pack still loads and no recursive calls were attempted for nested jars.
        verify(mockLoader, times(1)).load(root);
        verify(pack, times(1)).loadResourcePath(any(), any()); // Should only be called once on the root
    }

    @Test
    void loadResourcePath_whenNestedJarLoadFails_thenCatchesExceptionAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("fabric-mod-bad-jar");
        Files.createDirectory(root);
        Path badJar = root.resolve("bad.jar");
        Files.createFile(badJar);

        Path fabricJson = root.resolve("fabric.mod.json");
        String jsonContent = """
        { "jars": [{ "file": "bad.jar" }] }
        """;
        Files.writeString(fabricJson, jsonContent);

        // Tell the spy to throw an error when it tries to load the bad JAR
        doThrow(new IOException("Test IO Exception"))
                .when(pack)
                .loadResourcePath(eq(badJar), any());

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The exception for the nested JAR should be caught, and the root should still be loaded.
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenMalformedFabricJson_thenCatchesExceptionAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("fabric-mod-malformed");
        Files.createDirectory(root);

        Path fabricJson = root.resolve("fabric.mod.json");
        // This JSON is invalid because of the trailing comma.
        String jsonContent = """
        {
          "id": "my-mod",
        }
        """;
        Files.writeString(fabricJson, jsonContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The exception from parsing the JSON should be caught, and loading should proceed for the root pack.
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenMalformedPackMeta_thenCatchesExceptionAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("malformed-pack");
        Files.createDirectory(root);

        Path packMeta = root.resolve("pack.mcmeta");
        // This JSON is invalid due to the trailing comma
        String metaContent = """
        { "pack": { "pack_format": 15, } }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The loader should still be called, proving the error was handled and a default PackMeta was created.
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_whenNestedDatapackLoadFails_thenCatchesExceptionAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("datapack-fail-test");
        Path badDatapack = root.resolve("data/minecraft/datapacks/bad_pack");
        Files.createDirectories(badDatapack);

        // Tell our spy to throw an exception when it tries to load this specific datapack
        doThrow(new IOException("Test IO Exception"))
                .when(pack)
                .loadResourcePath(eq(badDatapack), any());

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The root pack should still load, proving the exception for the bad datapack was caught.
        verify(mockLoader, times(1)).load(root);
    }

    @Test
    void loadResourcePath_givenOverlayWithNullDirectory_thenSkipsAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("overlay-null-dir");
        Files.createDirectory(root);

        Path packMeta = root.resolve("pack.mcmeta");
        // This overlay entry is missing the "directory" key
        String metaContent = """
        {
          "overlays": {
            "entries": [
              { "formats": [10, 20] }
            ]
          }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The root pack should load, and no recursive calls for overlays should be made.
        verify(mockLoader, times(1)).load(root);
        verify(pack, times(1)).loadResourcePath(any(), any()); // Only the initial call on root
    }

    @Test
    void loadResourcePath_givenMissingOverlayDirectory_thenSkipsAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("overlay-missing-dir");
        Files.createDirectory(root);

        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
        {
          "overlays": {
            "entries": [
              { "directory": "this-folder-does-not-exist", "formats": [10, 20] }
            ]
          }
        }
        """;
        Files.writeString(packMeta, metaContent);

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The root pack should load, and no attempt to load the missing overlay should occur.
        verify(mockLoader, times(1)).load(root);
        verify(pack, never()).loadResourcePath(eq(root.resolve("this-folder-does-not-exist")), any());
    }

    @Test
    void loadResourcePath_whenOverlayLoadFails_thenCatchesExceptionAndContinues() throws IOException, InterruptedException {
        // Arrange
        Path root = tempDir.resolve("overlay-fail-test");
        Path badOverlay = root.resolve("bad_overlay");
        Files.createDirectories(badOverlay);

        Path packMeta = root.resolve("pack.mcmeta");
        String metaContent = """
    {
      "overlays": {
        "entries": [
          { "directory": "bad_overlay", "formats": [10, 20] }
        ]
      }
    }
    """;
        Files.writeString(packMeta, metaContent);

        // Tell our spy to throw an exception when it tries to load this specific overlay
        doThrow(new IOException("Test IO Exception on overlay"))
                .when(pack)
                .loadResourcePath(eq(badOverlay), any());

        // Act
        pack.loadResourcePath(root, mockLoader);

        // Assert
        // The root pack should still load, proving the exception for the bad overlay was caught.
        verify(mockLoader, times(1)).load(root);
    }

}
