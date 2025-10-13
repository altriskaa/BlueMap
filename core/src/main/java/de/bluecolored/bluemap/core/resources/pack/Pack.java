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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public abstract class Pack {

    private final int packVersion;
    private final @Nullable Set<Key> enabledFeatures;

    public Pack(int packVersion) {
        this(packVersion, null);
    }

    public abstract void loadResources(Iterable<Path> roots) throws IOException, InterruptedException;

    /**
     * =================================================================================================================
     * region ## REFACTORED render AND HELPERS ##
     * Code Smell Code:
     * [java:S3776] High Cognitive complexity (45 CogC)
     * [java:S6541] Brain Method: 82 LOC, 16 Complexity, 6 Nesting, 25 Num of Variables
     * Refactor method: Extract method
     * =================================================================================================================
     */

    /**
     * The main entry point for loading resources from a given path.
     * It coordinates the loading process by delegating to specialized helper methods.
     */
    protected void loadResourcePath(Path root, Pack.PathLoader resourceLoader) throws IOException, InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        if (!Files.isDirectory(root)) {
            loadFromArchive(root, resourceLoader);
            return;
        }

        loadNestedFabricJars(root, resourceLoader);

        PackMeta packMeta = loadPackMeta(root);

        if (featuresAreMissing(packMeta)) {
            logSkippedPack(root, packMeta);
            return;
        }

        loadNestedDatapacks(root, resourceLoader);
        loadOverlays(root, packMeta, resourceLoader);

        resourceLoader.load(root);
    }

    /**
     * Handles loading from archive files like .zip or .jar.
     */
    private void loadFromArchive(Path archivePath, Pack.PathLoader resourceLoader) throws InterruptedException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(archivePath, (ClassLoader) null)) {
            for (Path fsRoot : fileSystem.getRootDirectories()) {
                if (Files.isDirectory(fsRoot)) {
                    loadResourcePath(fsRoot, resourceLoader);
                }
            }
        } catch (IOException ex) {
            // This is the corrected block
            Logger.global.logDebug("Failed to read archive '" + archivePath + "': " + ex);
        }
    }

    /**
     * Loads nested .jar files specified in a fabric.mod.json file.
     */
    private void loadNestedFabricJars(Path root, Pack.PathLoader resourceLoader) throws InterruptedException {
        Path fabricModJson = root.resolve("fabric.mod.json");
        if (!Files.isRegularFile(fabricModJson)) return;

        try (BufferedReader reader = Files.newBufferedReader(fabricModJson)) {
            JsonObject rootElement = ResourcesGson.INSTANCE.fromJson(reader, JsonObject.class);
            if (rootElement.has("jars")) {
                for (JsonElement element : rootElement.getAsJsonArray("jars")) {
                    Path file = root.resolve(element.getAsJsonObject().get("file").getAsString());
                    if (Files.exists(file)) {
                        loadNestedJar(file, resourceLoader);
                    }
                }
            }
        } catch (IOException | JsonSyntaxException ex) {
            Logger.global.logDebug("Failed to read fabric.mod.json: " + ex);
        }
    }

    /**
     * Tries to load a single nested jar file, handling potential exceptions.
     */
    private void loadNestedJar(Path jarPath, Pack.PathLoader resourceLoader) throws InterruptedException {
        try {
            loadResourcePath(jarPath, resourceLoader);
        } catch (IOException ex) {
            Logger.global.logDebug("Failed to read nested jar '" + jarPath + "': " + ex);
        }
    }

    /**
     * Loads and parses the pack.mcmeta file. Returns a default PackMeta if not found or on error.
     */
    private PackMeta loadPackMeta(Path root) {
        Path packMetaFile = root.resolve("pack.mcmeta");
        if (Files.isRegularFile(packMetaFile)) {
            try (BufferedReader reader = Files.newBufferedReader(packMetaFile)) {
                return ResourcesGson.INSTANCE.fromJson(reader, PackMeta.class);
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read pack.mcmeta: " + ex);
            }
        }
        return new PackMeta();
    }

    /**
     * Checks if the pack requires features that are not enabled.
     */
    private boolean featuresAreMissing(PackMeta packMeta) {
        return enabledFeatures != null && !enabledFeatures.containsAll(packMeta.getFeatures().getEnabled());
    }

    /**
     * Logs a debug message for a pack that was skipped due to missing features.
     */
    private void logSkippedPack(Path root, PackMeta packMeta) {
        Logger.global.logDebug("Skipping resources from '%s' because not all required features (%s) are enabled (%s)"
                .formatted(
                        root,
                        Arrays.toString(packMeta.getFeatures().getEnabled().toArray()),
                        Arrays.toString(enabledFeatures.toArray())
                ));
    }

    /**
     * Scans for and loads nested datapacks.
     */
    private void loadNestedDatapacks(Path root, Pack.PathLoader resourceLoader) {
        list(root.resolve("data"))
                .map(namespaceRoot -> namespaceRoot.resolve("datapacks"))
                .filter(Files::isDirectory)
                .flatMap(Pack::list)
                .forEach(nestedPack -> {
                    try {
                        loadResourcePath(nestedPack, resourceLoader);
                    } catch (InterruptedException ex) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                        Logger.global.logDebug("Interrupted while loading nested datapack '" + nestedPack + "'");
                    } catch (IOException ex) {
                        // Log other IO exceptions normally
                        Logger.global.logDebug("Failed to load nested datapack '" + nestedPack + "': " + ex);
                    }
                });
    }

    /**
     * Loads overlays specified in the pack metadata, in reverse order.
     */
    private void loadOverlays(Path root, PackMeta packMeta, Pack.PathLoader resourceLoader) throws InterruptedException {
        PackMeta.Overlay[] overlays = packMeta.getOverlays().getEntries();
        for (int i = overlays.length - 1; i >= 0; i--) {
            PackMeta.Overlay overlay = overlays[i];
            String dir = overlay.getDirectory();
            if (dir != null && overlay.getFormats().includes(this.packVersion)) {
                Path overlayRoot = root.resolve(dir);
                if (Files.exists(overlayRoot)) {
                    try {
                        loadResourcePath(overlayRoot, resourceLoader);
                    } catch (IOException ex) {
                        Logger.global.logDebug("Failed to load overlay '" + overlayRoot + "': " + ex);
                    }
                }
            }
        }
    }

    protected <T> void loadResource(Path root, Path file, int namespacePos, int valuePos, Loader<T> loader, Map<? super ResourcePath<T>, T> resultMap) {
        try {
            ResourcePath<T> resourcePath = new ResourcePath<>(root.relativize(file), namespacePos, valuePos);
            if (resultMap.containsKey(resourcePath)) return; // don't load already present resources

            T resource = loader.load(resourcePath);
            if (resource == null) return; // don't load missing resources

            resourcePath.setResource(resource);
            resultMap.put(resourcePath, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
        }
    }

    protected static Stream<Path> list(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        try {
            return Files.list(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    protected static Stream<Path> walk(Path root) {
        if (!Files.exists(root)) return Stream.empty();
        if (Files.isRegularFile(root)) return Stream.of(root);
        try {
            return FileHelper.walk(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    protected interface Loader<T> {
        T load(ResourcePath<T> resourcePath) throws IOException;
    }

    protected interface PathLoader {
        void load(Path root) throws IOException;
    }

}
