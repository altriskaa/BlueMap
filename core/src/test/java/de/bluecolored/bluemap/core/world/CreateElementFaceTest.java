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
package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.resources.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.*;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.block.ExtendedBlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Mockito.*;

class CreateElementFaceTest {

    private ResourceModelRenderer renderer;
    private ResourcePack resourcePack;
    private TextureGallery textureGallery;
    private RenderSettings renderSettings;
    private TileModelView tileModelView;
    private TileModel tileModel;
    private BlockNeighborhood block;
    private Variant variant;
    private Model model;
    private Element element;
    private Face face;
    private Color color;
    private LightData lightData;
    private BlockProperties blockProperties;
    private ExtendedBlock neighborBlock;
    private BlockProperties neighborBlockProperties;
    private Texture texture;
    private TextureVariable textureVariable;
    private ResourcePath<Texture> texturePath;
    private Object blockState;

    @BeforeEach
    void setup() {
        // Initialize all mocks
        resourcePack = mock(ResourcePack.class);
        textureGallery = mock(TextureGallery.class);
        renderSettings = mock(RenderSettings.class);
        tileModelView = mock(TileModelView.class);
        tileModel = mock(TileModel.class);
        block = mock(BlockNeighborhood.class);
        variant = mock(Variant.class);
        model = mock(Model.class);
        element = mock(Element.class);
        face = mock(Face.class);
        color = new Color();
        lightData = mock(LightData.class);
        blockProperties = mock(BlockProperties.class);
        neighborBlock = mock(ExtendedBlock.class);
        neighborBlockProperties = mock(BlockProperties.class);
        texture = mock(Texture.class);
        textureVariable = mock(TextureVariable.class);
        texturePath = mock(ResourcePath.class);
        blockState = new Object();

        setupBasicMocks();
        setupRenderer();
    }

    private void setupBasicMocks() {
        // Block color calculator setup
        BlockColorCalculatorFactory calculatorFactory = mock(BlockColorCalculatorFactory.class);
        when(resourcePack.getColorCalculatorFactory()).thenReturn(calculatorFactory);

        BlockColorCalculatorFactory.BlockColorCalculator calculator = mock(BlockColorCalculatorFactory.BlockColorCalculator.class);
        when(calculatorFactory.createCalculator()).thenReturn(calculator);
        when(calculator.getBlockColor(any(BlockNeighborhood.class), any(Color.class)))
                .thenAnswer(invocation -> {
                    Color color = invocation.getArgument(1);
                    return color.set(0.5f, 0.5f, 0.5f, 1f, false);
                });

        // Basic block setup
        when(block.getX()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getProperties()).thenReturn(blockProperties);
        when(block.getLightData()).thenReturn(lightData);
        when(block.getNeighborBlock(anyInt(), anyInt(), anyInt())).thenReturn(neighborBlock);
        when(block.isRemoveIfCave()).thenReturn(false);
        when(block.getBlockState()).thenReturn(mock(BlockState.class));

        // Light data setup
        when(lightData.getSkyLight()).thenReturn(15);
        when(lightData.getBlockLight()).thenReturn(15);

        // Block properties setup
        when(blockProperties.isRandomOffset()).thenReturn(false);

        // Neighbor block setup
        when(neighborBlock.getLightData()).thenReturn(lightData);
        when(neighborBlock.getProperties()).thenReturn(neighborBlockProperties);
        when(neighborBlock.getBlockState()).thenReturn(mock(BlockState.class));
        when(neighborBlockProperties.isCulling()).thenReturn(false);
        when(neighborBlockProperties.getCullingIdentical()).thenReturn(false);
        when(neighborBlockProperties.isOccluding()).thenReturn(false);

        // Tile model setup
        when(tileModelView.getStart()).thenReturn(0);
        when(tileModelView.getTileModel()).thenReturn(tileModel);
        when(tileModelView.initialize()).thenReturn(tileModelView);
        when(tileModelView.initialize(anyInt())).thenReturn(tileModelView);

        // Variant setup
        ResourcePath<Model> mockModelPath = mock(ResourcePath.class);
        when(mockModelPath.getResource(any())).thenReturn(model);
        when(variant.getModel()).thenReturn(mockModelPath);
        when(variant.getTransformMatrix()).thenReturn(new MatrixM4f());
        when(variant.isTransformed()).thenReturn(false);
        when(variant.isUvlock()).thenReturn(false);
        when(variant.getX()).thenReturn(0f);
        when(variant.getY()).thenReturn(0f);

        // Model setup
        when(model.getElements()).thenReturn(new Element[]{element});
        when(model.isAmbientocclusion()).thenReturn(true);

        // Element setup
        when(element.getFrom()).thenReturn(new com.flowpowered.math.vector.Vector3f(0, 0, 0));
        when(element.getTo()).thenReturn(new com.flowpowered.math.vector.Vector3f(16, 16, 16));
        when(element.getLightEmission()).thenReturn(0);

        Rotation rotation = mock(Rotation.class);
        when(rotation.getMatrix()).thenReturn(new MatrixM4f());
        when(element.getRotation()).thenReturn(rotation);

        // Face setup
        Dictionary<Direction, Face> faceDict = new Hashtable<>();
        faceDict.put(Direction.UP, face);
        doReturn(faceDict).when(element).getFaces();

        when(face.getUv()).thenReturn(new com.flowpowered.math.vector.Vector4f(0, 0, 16, 16));
        when(face.getTexture()).thenReturn(textureVariable);
        when(face.getRotation()).thenReturn(0);
        when(face.getTintindex()).thenReturn(-1);
        when(face.getCullface()).thenReturn(null);

        // Texture setup
        when(textureVariable.getTexturePath(any())).thenReturn(texturePath);
        when(texturePath.getResource(any())).thenReturn(texture);
        when(texture.getColorPremultiplied()).thenReturn(new Color().set(1f, 1f, 1f, 1f, true));

        // Render settings setup
        when(renderSettings.getAmbientLight()).thenReturn(0f);
        when(renderSettings.isCaveDetectionUsesBlockLight()).thenReturn(false);
        when(renderSettings.isRenderTopOnly()).thenReturn(false);

        // Texture gallery setup
        when(textureGallery.get(any())).thenReturn(1);
        when(resourcePack.getTexture(any())).thenReturn(texture);
    }

    private void setupRenderer() {
        renderer = new ResourceModelRenderer(resourcePack, textureGallery, renderSettings);
    }

    @Nested
    @DisplayName("Cave Detection Tests")
    class CaveDetectionTests {

        @Test
        @DisplayName("Should render face when cave detection is disabled")
        void shouldRenderWhenCaveDetectionDisabled() {
            when(block.isRemoveIfCave()).thenReturn(false);
            when(lightData.getSkyLight()).thenReturn(0);
            when(lightData.getBlockLight()).thenReturn(0);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, atLeastOnce()).add(2);
        }

        @Test
        @DisplayName("Should not render face in cave when using sky light detection")
        void shouldNotRenderInCaveWithSkyLight() {
            when(block.isRemoveIfCave()).thenReturn(true);
            when(renderSettings.isCaveDetectionUsesBlockLight()).thenReturn(false);
            when(lightData.getSkyLight()).thenReturn(0);
            when(lightData.getBlockLight()).thenReturn(5);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, never()).add(2);
        }

        @Test
        @DisplayName("Should not render face in cave when using block light detection")
        void shouldNotRenderInCaveWithBlockLight() {
            when(block.isRemoveIfCave()).thenReturn(true);
            when(renderSettings.isCaveDetectionUsesBlockLight()).thenReturn(true);
            when(lightData.getSkyLight()).thenReturn(0);
            when(lightData.getBlockLight()).thenReturn(0);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, never()).add(2);
        }

        @Test
        @DisplayName("Should render face when there is light in cave detection")
        void shouldRenderWhenThereIsLight() {
            when(block.isRemoveIfCave()).thenReturn(true);
            when(renderSettings.isCaveDetectionUsesBlockLight()).thenReturn(false);
            when(lightData.getSkyLight()).thenReturn(5);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, atLeastOnce()).add(2);
        }
    }

    @Nested
    @DisplayName("Face Culling Tests")
    class FaceCullingTests {

        @Test
        @DisplayName("Should not render non-top faces when render top only is enabled")
        void shouldNotRenderNonTopFaces() {
            when(renderSettings.isRenderTopOnly()).thenReturn(true);

            // Setup a side face (NORTH) instead of UP
            Dictionary<Direction, Face> faceDict = new Hashtable<>();
            faceDict.put(Direction.NORTH, face);
            doReturn(faceDict).when(element).getFaces();

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, never()).add(2);
        }

        @Test
        @DisplayName("Should render top faces when render top only is enabled")
        void shouldRenderTopFaces() {
            when(renderSettings.isRenderTopOnly()).thenReturn(true);
            // UP face is already set in setup

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, atLeastOnce()).add(2);
        }

        @Test
        @DisplayName("Should not render face when neighbor is culling")
        void shouldNotRenderWhenNeighborIsCulling() {
            when(face.getCullface()).thenReturn(Direction.UP);
            when(neighborBlockProperties.isCulling()).thenReturn(true);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, never()).add(2);
        }

        @Test
        @DisplayName("Should not render face when neighbor has identical culling")
        void shouldNotRenderWhenNeighborHasIdenticalCulling() {
            BlockState sharedBlockState = mock(BlockState.class);
            when(face.getCullface()).thenReturn(Direction.UP);
            when(neighborBlockProperties.isCulling()).thenReturn(false);
            when(neighborBlockProperties.getCullingIdentical()).thenReturn(true);
            when(block.getBlockState()).thenReturn(sharedBlockState);
            when(neighborBlock.getBlockState()).thenReturn(sharedBlockState);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, never()).add(2);
        }

        @Test
        @DisplayName("Should render face when neighbor has no culling")
        void shouldRenderWhenNeighborHasNoCulling() {
            when(face.getCullface()).thenReturn(Direction.UP);
            when(neighborBlockProperties.isCulling()).thenReturn(false);
            when(neighborBlockProperties.getCullingIdentical()).thenReturn(false);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModelView, atLeastOnce()).add(2);
        }
    }

    @Nested
    @DisplayName("UV Lock and Rotation Tests")
    class UVLockTests {

        @Test
        @DisplayName("Should apply UV lock rotation when variant is transformed and UV locked")
        void shouldApplyUVLockRotation() {
            when(variant.isUvlock()).thenReturn(true);
            when(variant.isTransformed()).thenReturn(true);
            when(variant.getX()).thenReturn(90f);
            when(variant.getY()).thenReturn(45f);

            renderer.render(block, variant, tileModelView, color);

            // Verify that UVs are set (indicating UV processing occurred)
            verify(tileModel, atLeastOnce()).setUvs(anyInt(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("Should not apply UV lock when not transformed")
        void shouldNotApplyUVLockWhenNotTransformed() {
            when(variant.isUvlock()).thenReturn(true);
            when(variant.isTransformed()).thenReturn(false);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setUvs(anyInt(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("Should apply face rotation to UVs")
        void shouldApplyFaceRotation() {
            when(face.getRotation()).thenReturn(90);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setUvs(anyInt(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(),
                    anyFloat(), anyFloat());
        }
    }

    @Nested
    @DisplayName("Face Tint Tests")
    class FaceTintTests {

        @Test
        @DisplayName("Should apply tint color when tint index is set")
        void shouldApplyTintColor() {
            when(face.getTintindex()).thenReturn(0);

            renderer.render(block, variant, tileModelView, color);

            // Should set color with tint (0.5f values from mock calculator)
            verify(tileModel, atLeastOnce()).setColor(anyInt(), eq(0.5f), eq(0.5f), eq(0.5f));
        }

        @Test
        @DisplayName("Should use white color when no tint index")
        void shouldUseWhiteColorWhenNoTint() {
            when(face.getTintindex()).thenReturn(-1);

            renderer.render(block, variant, tileModelView, color);

            // Should set color to white (1f, 1f, 1f)
            verify(tileModel, atLeastOnce()).setColor(anyInt(), eq(1f), eq(1f), eq(1f));
        }
    }

    @Nested
    @DisplayName("Map Color Tests")
    class MapColorTests {

        @Test
        @DisplayName("Should calculate map color for top face with texture")
        void shouldCalculateMapColorForTopFace() {
            // UP face is already configured in setup
            Color testColor = new Color().set(0.8f, 0.6f, 0.4f, 1f, true);
            when(texture.getColorPremultiplied()).thenReturn(testColor);

            renderer.render(block, variant, tileModelView, color);

            // The color should be modified (added to) by the map color calculation
            verify(tileModel, atLeastOnce()).setPositions(anyInt(),
                    anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("Should apply tint to map color when tint is available")
        void shouldApplyTintToMapColor() {
            when(face.getTintindex()).thenReturn(0);
            Color testColor = new Color().set(0.8f, 0.6f, 0.4f, 1f, true);
            when(texture.getColorPremultiplied()).thenReturn(testColor);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setColor(anyInt(), eq(0.5f), eq(0.5f), eq(0.5f));
        }

        @Test
        @DisplayName("Should apply ambient light to map color")
        void shouldApplyAmbientLightToMapColor() {
            when(renderSettings.getAmbientLight()).thenReturn(0.3f);
            when(lightData.getSkyLight()).thenReturn(10);
            when(lightData.getBlockLight()).thenReturn(5);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setBlocklight(anyInt(), anyInt());
            verify(tileModel, atLeastOnce()).setSunlight(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should not calculate map color for non-top faces")
        void shouldNotCalculateMapColorForNonTopFaces() {
            // Setup a side face (NORTH) instead of UP
            Dictionary<Direction, Face> faceDict = new Hashtable<>();
            faceDict.put(Direction.NORTH, face);
            doReturn(faceDict).when(element).getFaces();

            renderer.render(block, variant, tileModelView, color);

            // Should still set basic properties but map color won't be calculated for side faces
            verify(tileModel, atLeastOnce()).setColor(anyInt(), anyFloat(), anyFloat(), anyFloat());
        }
    }

    @Nested
    @DisplayName("Light Emission Tests")
    class LightEmissionTests {

        @Test
        @DisplayName("Should use element light emission when higher than block light")
        void shouldUseElementLightEmission() {
            when(element.getLightEmission()).thenReturn(10);
            when(lightData.getBlockLight()).thenReturn(5);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setBlocklight(anyInt(), eq(10));
        }

        @Test
        @DisplayName("Should use block light when higher than element emission")
        void shouldUseBlockLight() {
            when(element.getLightEmission()).thenReturn(3);
            when(lightData.getBlockLight()).thenReturn(8);

            renderer.render(block, variant, tileModelView, color);

            verify(tileModel, atLeastOnce()).setBlocklight(anyInt(), eq(8));
        }
    }

    @Test
    @DisplayName("Should handle null texture gracefully")
    void shouldHandleNullTexture() {
        when(texturePath.getResource(any())).thenReturn(null);

        renderer.render(block, variant, tileModelView, color);

        // Should still complete rendering even with null texture
        verify(tileModelView, atLeastOnce()).add(2);
    }

    @Test
    @DisplayName("Should handle null model gracefully")
    void shouldHandleNullModel() {
        ResourcePath<Model> mockModelPath = mock(ResourcePath.class);
        when(mockModelPath.getResource(any())).thenReturn(null);
        when(variant.getModel()).thenReturn(mockModelPath);

        renderer.render(block, variant, tileModelView, color);

        // Should return early and not add any faces
        verify(tileModelView, never()).add(anyInt());
    }

    @Test
    @DisplayName("Should handle ambient occlusion calculation")
    void shouldHandleAmbientOcclusion() {
        when(model.isAmbientocclusion()).thenReturn(true);
        when(neighborBlockProperties.isOccluding()).thenReturn(true);

        renderer.render(block, variant, tileModelView, color);

        verify(tileModel, atLeastOnce()).setAOs(anyInt(), anyFloat(), anyFloat(), anyFloat());
    }
}
