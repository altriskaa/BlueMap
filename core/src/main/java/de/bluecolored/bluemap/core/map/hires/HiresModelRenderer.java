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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.map.hires.entity.EntityModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.block.Block;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class HiresModelRenderer {

    private final ResourcePack resourcePack;
    private final RenderSettings renderSettings;

    private final ThreadLocal<BlockStateModelRenderer> threadLocalBlockRenderer;
    private final ThreadLocal<EntityModelRenderer> threadLocalEntityRenderer;

    public HiresModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.renderSettings = renderSettings;

        this.threadLocalBlockRenderer = ThreadLocal.withInitial(() -> new BlockStateModelRenderer(resourcePack, textureGallery, renderSettings));
        this.threadLocalEntityRenderer = ThreadLocal.withInitial(() -> new EntityModelRenderer(resourcePack, textureGallery, renderSettings));
    }

    public void render(World world, Vector3i modelMin, Vector3i modelMax, TileModel model) {
        render(world, modelMin, modelMax, model, (x, z, c, h, l) -> {});
    }

    /**
     * =================================================================================================================
     * region ## REFACTORED render AND HELPERS ##
     * Code Smell Code [java:S3776] High Cognitive complexity (36 CogC)
     * Refactor method: Extract method
     * =================================================================================================================
     */

    public void render(World world, Vector3i modelMin, Vector3i modelMax, TileModel tileModel, TileMetaConsumer tileMetaConsumer) {
        try {
            Vector3i min = modelMin.max(renderSettings.getMinPos());
            Vector3i max = modelMax.min(renderSettings.getMaxPos());
            Vector3i modelAnchor = new Vector3i(modelMin.getX(), 0, modelMin.getZ());

            BlockStateModelRenderer blockRenderer = threadLocalBlockRenderer.get();
            EntityModelRenderer entityRenderer = threadLocalEntityRenderer.get();
            BlockNeighborhood block = new BlockNeighborhood(new Block(world, 0, 0, 0), resourcePack, renderSettings, world.getDimensionType());
            TileModelView tileModelView = new TileModelView(tileModel);

            RenderContext renderCtx = new RenderContext(tileModelView, blockRenderer, block, renderSettings);
            renderBlockColumns(world, min, max, modelAnchor, renderCtx, tileMetaConsumer);
            renderEntities(world, min, max, modelAnchor, tileModelView, entityRenderer, block);

        } catch (MaxCapacityReachedException ex) {
            Logger.global.noFloodWarning("max-capacity-reached",
                    "One or more map-tiles are too complex to be completed (@~ %s to %s): %s".formatted(modelMin, modelMax, ex));
        }
    }

    /* ---------- Helpers and Extracted Methods ---------- */

    private record ColumnContext(int x, int z, Vector3i min, Vector3i max, Vector3i modelAnchor) {}

    private record RenderContext(TileModelView tileModelView, BlockStateModelRenderer blockRenderer,
                                 BlockNeighborhood block, RenderSettings renderSettings) {}

    private void renderBlockColumns(World world, Vector3i min, Vector3i max, Vector3i modelAnchor,
                                    RenderContext ctx, TileMetaConsumer tileMetaConsumer) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                ColumnContext colCtx = new ColumnContext(x, z, min, max, modelAnchor);
                renderBlocksInColumn(world, colCtx, ctx, tileMetaConsumer);
            }
        }
    }

    private void renderBlocksInColumn(World world, ColumnContext colCtx, RenderContext ctx, TileMetaConsumer tileMetaConsumer) {
        int maxHeight = Integer.MIN_VALUE;
        double topBlockLight = 0;
        Color columnColor = new Color();
        Color blockColor = new Color();
        columnColor.set(0, 0, 0, 0, true);

        int x = colCtx.x();
        int z = colCtx.z();

        if (!ctx.renderSettings().isInsideRenderBoundaries(x, z)) {
            tileMetaConsumer.set(x, z, columnColor, 0, 0);
            return;
        }

        Chunk chunk = world.getChunkAtBlock(x, z);
        int minY = Math.max(colCtx.min().getY(), chunk.getMinY(x, z));
        int maxY = Math.min(colCtx.max().getY(), chunk.getMaxY(x, z));

        for (int y = maxY; y >= minY; y--) {
            boolean stop = renderBlockAtY(x, y, z, colCtx.modelAnchor(), ctx, columnColor, blockColor);
            maxHeight = Math.max(maxHeight, y);
            topBlockLight = Math.max(topBlockLight, ctx.block().getBlockLightLevel() * (1 - columnColor.a));
            if (stop) break; // stop rendering below opaque block if "render top only" mode
        }

        if (maxHeight == Integer.MIN_VALUE) maxHeight = 0;
        tileMetaConsumer.set(x, z, columnColor, maxHeight, (int) topBlockLight);
    }

    private boolean renderBlockAtY(int x, int y, int z, Vector3i modelAnchor, RenderContext ctx, Color columnColor, Color blockColor) {
        ctx.block().set(x, y, z);
        if (!ctx.block().isInsideRenderBounds()) return false;

        ctx.tileModelView().initialize();
        ctx.blockRenderer().render(ctx.block(), ctx.tileModelView(), blockColor);

        ctx.tileModelView().translate(x - modelAnchor.getX(), y - modelAnchor.getY(), z - modelAnchor.getZ());

        if (blockColor.a > 0) {
            columnColor.underlay(blockColor.premultiplied());
        }

        return ctx.renderSettings().isRenderTopOnly() && blockColor.a > 0.999 && ctx.block().getProperties().isCulling(); // signal to stop rendering further down
    }

    private void renderEntities(World world, Vector3i min, Vector3i max, Vector3i modelAnchor,
                                TileModelView tileModelView, EntityModelRenderer entityRenderer, BlockNeighborhood block) {
        world.iterateEntities(min.getX(), min.getZ(), max.getX(), max.getZ(), entity -> {
            Vector3d pos = entity.getPos();
            block.set(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
            entityRenderer.render(entity, block, tileModelView.initialize());
            tileModelView.translate(
                    (float) pos.getX() - modelAnchor.getX(),
                    (float) pos.getY() - modelAnchor.getY(),
                    (float) pos.getZ() - modelAnchor.getZ()
            );
        });
    }

}
