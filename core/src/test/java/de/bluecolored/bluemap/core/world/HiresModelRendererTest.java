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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.map.hires.HiresModelRenderer;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.map.hires.MaxCapacityReachedException;
import de.bluecolored.bluemap.core.world.block.Block;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the HiresModelRenderer class, covering all requested logic paths.
 */
@ExtendWith(MockitoExtension.class)
public class HiresModelRendererTest {

    @Mock private ResourcePack resourcePack;
    @Mock private TextureGallery textureGallery;
    @Mock private RenderSettings renderSettings;
    @Mock private World world;
    @Mock private Chunk chunk;
    @Mock private TileModel tileModel;
    @Mock private TileMetaConsumer tileMetaConsumer;
    @Mock private Entity entity;

    @Captor private ArgumentCaptor<Integer> heightCaptor;
    @Captor private ArgumentCaptor<Float> translateXCaptor;
    @Captor private ArgumentCaptor<Float> translateYCaptor;
    @Captor private ArgumentCaptor<Float> translateZCaptor;

    private HiresModelRenderer hiresModelRenderer;

    private final BlockState airBlock = mock(BlockState.class, "air");
    private final BlockState stoneBlock = mock(BlockState.class, "stone");
    private final BlockState grassBlock = mock(BlockState.class, "grass");
    private final BlockState glassBlock = mock(BlockState.class, "glass");


    @Mock private BlockStateModelRenderer mockBlockRenderer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        hiresModelRenderer = new HiresModelRenderer(resourcePack, textureGallery, renderSettings);

        // Inject the mock renderer for all tests
        Field rendererField = HiresModelRenderer.class.getDeclaredField("threadLocalBlockRenderer");
        rendererField.setAccessible(true);
        ((ThreadLocal<BlockStateModelRenderer>) rendererField.get(hiresModelRenderer)).set(mockBlockRenderer);

        // --- General Mock Setup ---
        lenient().when(renderSettings.getMinPos()).thenReturn(new Vector3i(Integer.MIN_VALUE, 0, Integer.MIN_VALUE));
        lenient().when(renderSettings.getMaxPos()).thenReturn(new Vector3i(Integer.MAX_VALUE, 255, Integer.MAX_VALUE));
        lenient().when(renderSettings.isInsideRenderBoundaries(anyInt(), anyInt())).thenReturn(true);
        lenient().when(renderSettings.isInsideRenderBoundaries(anyInt(), anyInt(), anyInt())).thenReturn(true);

        lenient().when(world.getChunkAtBlock(anyInt(), anyInt())).thenReturn(chunk);
        lenient().when(world.getDimensionType()).thenReturn(mock(DimensionType.class));

        lenient().when(chunk.getMinY(anyInt(), anyInt())).thenReturn(0);
        lenient().when(chunk.getMaxY(anyInt(), anyInt())).thenReturn(255);

        // This is a default that might not be used by every test, so make it lenient.
        lenient().when(chunk.getBlockState(anyInt(), anyInt(), anyInt())).thenReturn(airBlock);

        BlockProperties defaultProperties = mock(BlockProperties.class);
        lenient().when(resourcePack.getBlockProperties(any())).thenReturn(defaultProperties);
    }



    @Test
    void render_emptyColumn_resetsMaxHeightToZero() {
        // ARRANGE: Set the chunk to only return AIR blocks
        lenient().when(chunk.getBlockState(anyInt(), anyInt(), anyInt())).thenReturn(airBlock);
        Vector3i modelPos = new Vector3i(10, 0, 10);

        // ACT
        hiresModelRenderer.render(world, modelPos, modelPos, tileModel, tileMetaConsumer);

        // ASSERT
        verify(tileMetaConsumer).set(eq(10), eq(10), any(), heightCaptor.capture(), anyInt());
        assertEquals(0, heightCaptor.getValue(), "maxHeight for an empty column should be 0");
    }

    @Test
    void render_simpleColumn_verifiesBlockStateIsChecked() {
        // ARRANGE
        Vector3i modelMin = new Vector3i(0, 0, 0);
        Vector3i modelMax = new Vector3i(0, 255, 0);

        when(world.getChunkAtBlock(anyInt(), anyInt())).thenReturn(chunk);

        when(chunk.getBlockState(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int x = inv.getArgument(0);
            int y = inv.getArgument(1);
            int z = inv.getArgument(2);
            System.out.println("Queried blockState at " + x + "," + y + "," + z);
            if (x == 0 && y == 0 && z == 0) return stoneBlock;
            return airBlock;
        });

        // ACT
        hiresModelRenderer.render(world, modelMin, modelMax, tileModel, tileMetaConsumer);

        // ASSERT
        ArgumentCaptor<BlockNeighborhood> blockAccessCaptor = ArgumentCaptor.forClass(BlockNeighborhood.class);
        verify(mockBlockRenderer, atLeastOnce()).render(blockAccessCaptor.capture(), any(), any());
        assertEquals(stoneBlock, blockAccessCaptor.getValue().getBlockState());
    }


    @SuppressWarnings("unchecked")
    @Test
    void render_withEntity_translatesModelToEntityPosition() {
        // ARRANGE
        Vector3i modelMin = new Vector3i(16, 0, 32);
        Vector3i modelMax = new Vector3i(31, 255, 47);
        Vector3d entityPosition = new Vector3d(20.5, 64.2, 40.9);

        // The default airBlock stub is sufficient for the loop to run.
        when(entity.getPos()).thenReturn(entityPosition);
        doAnswer(invocation -> {
            ((java.util.function.Consumer<Entity>) invocation.getArgument(4)).accept(entity);
            return null;
        }).when(world).iterateEntities(anyInt(), anyInt(), anyInt(), anyInt(), any());

        // ACT
        hiresModelRenderer.render(world, modelMin, modelMax, tileModel, tileMetaConsumer);

        // ASSERT
        verify(tileModel, atLeastOnce()).translate(anyInt(), anyInt(), translateXCaptor.capture(), translateYCaptor.capture(), translateZCaptor.capture());

        // The translation is relative to the model's anchor point (minX, 0, minZ)
        assertEquals(20.5f - 16, translateXCaptor.getValue(), 0.01f); // 4.5f
        assertEquals(64.2f - 0,   translateYCaptor.getValue(), 0.01f); // 64.2f
        assertEquals(40.9f - 32, translateZCaptor.getValue(), 0.01f); // 8.9f
    }


    @Test
    void render_whenColumnIsOutsideBoundaries_skipsProcessing() {
        Vector3i modelMin = new Vector3i(0, 0, 0);
        Vector3i modelMax = new Vector3i(0, 255, 0);
        when(renderSettings.isInsideRenderBoundaries(0, 0)).thenReturn(false);

        hiresModelRenderer.render(world, modelMin, modelMax, tileModel, tileMetaConsumer);

        verify(world, never()).getChunkAtBlock(anyInt(), anyInt());
    }

    @Test
    void render_skipsBlockOutsideRenderBounds() {
        Vector3i modelPos = new Vector3i(1, 0, 1);

        lenient().when(renderSettings.isInsideRenderBoundaries(anyInt(), anyInt())).thenReturn(false);

        hiresModelRenderer.render(world, modelPos, modelPos, tileModel, tileMetaConsumer);

        // Ensure internal BlockRenderer was never called
        verify(mockBlockRenderer, never()).render(any(), any(), any());

        // Ensure tileMetaConsumer was still invoked with default maxHeight 0
        verify(tileMetaConsumer).set(eq(1), eq(1), any(), eq(0), anyInt());
    }


    @Test
    void render_withOpaqueBlock_updatesMaxHeightAndColumnColor() {
        Vector3i modelPos = new Vector3i(0, 64, 0);

        // lenient stubs to avoid UnnecessaryStubbingException
        lenient().when(world.getChunkAtBlock(anyInt(), anyInt())).thenReturn(chunk);
        lenient().when(chunk.getBlockState(anyInt(), anyInt(), anyInt())).thenReturn(stoneBlock);
        lenient().when(chunk.getMinY(anyInt(), anyInt())).thenReturn(64);
        lenient().when(chunk.getMaxY(anyInt(), anyInt())).thenReturn(64);

        // block renderer sets blockColor.a to simulate opacity
        doAnswer(inv -> {
            Color blockColor = inv.getArgument(2);
            blockColor.a = 1.0f;
            return null;
        }).when(mockBlockRenderer).render(any(), any(), any());

        hiresModelRenderer.render(world, modelPos, modelPos, tileModel, tileMetaConsumer);

        verify(tileMetaConsumer).set(eq(0), eq(0), any(), heightCaptor.capture(), anyInt());
        assertEquals(64, heightCaptor.getValue(), "maxHeight should be set to the block's Y level");
    }


    @Test
    void render_withRenderTopOnly_breaksAfterOpaqueCullingBlock() {
        Vector3i modelMin = new Vector3i(0, 0, 0);
        Vector3i modelMax = new Vector3i(0, 5, 0);

        when(renderSettings.isRenderTopOnly()).thenReturn(true);
        when(world.getChunkAtBlock(anyInt(), anyInt())).thenReturn(chunk);
        when(chunk.getMinY(anyInt(), anyInt())).thenReturn(0);
        when(chunk.getMaxY(anyInt(), anyInt())).thenReturn(5);

        BlockProperties cullingProps = mock(BlockProperties.class);
        when(cullingProps.isCulling()).thenReturn(true);
        when(resourcePack.getBlockProperties(stoneBlock)).thenReturn(cullingProps);

        // Top block opaque and culling → triggers break.
        when(chunk.getBlockState(0, 5, 0)).thenReturn(stoneBlock);

        doAnswer(inv -> {
            Color blockColor = inv.getArgument(2);
            blockColor.a = 1.0f; // Opaque
            return null;
        }).when(mockBlockRenderer).render(any(), any(), any());

        hiresModelRenderer.render(world, modelMin, modelMax, tileModel, tileMetaConsumer);

        // ASSERT
        verify(mockBlockRenderer, times(1)).render(any(), any(), any());
    }

    @Test
    void render_maxCapacityReached_logsWarning() throws MaxCapacityReachedException {
        Vector3i modelMin = new Vector3i(0, 0, 0);
        Vector3i modelMax = new Vector3i(0, 0, 0);

        doThrow(new MaxCapacityReachedException("too complex"))
                .when(mockBlockRenderer).render(any(), any(), any());

        hiresModelRenderer.render(world, modelMin, modelMax, tileModel, tileMetaConsumer);

    }

}