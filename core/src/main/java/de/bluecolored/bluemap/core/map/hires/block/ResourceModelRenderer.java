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
package de.bluecolored.bluemap.core.map.hires.block;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM2f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.block.ExtendedBlock;

/**
 * This model builder creates a BlockStateModel using the information from parsed resource-pack json files.
 */
@SuppressWarnings("DuplicatedCode")
public class ResourceModelRenderer implements BlockRenderer {
    private static final float BLOCK_SCALE = 1f / 16f;

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;
    private final RenderSettings renderSettings;
    private final BlockColorCalculatorFactory.BlockColorCalculator blockColorCalculator;

    private final VectorM3f[] corners = new VectorM3f[8];
    private final VectorM2f[] rawUvs = new VectorM2f[4];
    private final VectorM2f[] uvs = new VectorM2f[4];
    private final Color tintColor = new Color();
    private final Color mapColor = new Color();

    private BlockNeighborhood block;
    private Variant variant;
    private Model modelResource;
    private TileModelView blockModel;
    private Color blockColor;
    private float blockColorOpacity;

    public ResourceModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;
        this.blockColorCalculator = resourcePack.getColorCalculatorFactory().createCalculator();

        for (int i = 0; i < corners.length; i++) corners[i] = new VectorM3f(0, 0, 0);
        for (int i = 0; i < rawUvs.length; i++) rawUvs[i] = new VectorM2f(0, 0);
    }

    public void render(BlockNeighborhood block, Variant variant, TileModelView blockModel, Color color) {
        this.block = block;
        this.blockModel = blockModel;
        this.blockColor = color;
        this.blockColorOpacity = 0f;
        this.variant = variant;
        this.modelResource = variant.getModel().getResource(resourcePack::getModel);

        if (this.modelResource == null) return;

        this.tintColor.set(0, 0, 0, -1, true);

        // render model
        int modelStart = blockModel.getStart();

        Element[] elements = modelResource.getElements();
        if (elements != null) {
            for (Element element : elements) {
                buildModelElementResource(element, blockModel.initialize());
            }
        }

        if (color.a > 0) {
            color.flatten().straight();
            color.a = blockColorOpacity;
        }

        blockModel.initialize(modelStart);

        // apply model-transform
        if (variant.isTransformed())
            blockModel.transform(variant.getTransformMatrix());

        //random offset
        if (block.getProperties().isRandomOffset()){
            float dx = (hashToFloat(block.getX(), block.getZ(), 123984) - 0.5f) * 0.75f;
            float dz = (hashToFloat(block.getX(), block.getZ(), 345542) - 0.5f) * 0.75f;
            blockModel.translate(dx, 0, dz);
        }

    }

    private final MatrixM4f modelElementTransform = new MatrixM4f();
    private void buildModelElementResource(Element element, TileModelView blockModel) {

        //create faces
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();

        float
                minX = Math.min(from.getX(), to.getX()),
                minY = Math.min(from.getY(), to.getY()),
                minZ = Math.min(from.getZ(), to.getZ()),
                maxX = Math.max(from.getX(), to.getX()),
                maxY = Math.max(from.getY(), to.getY()),
                maxZ = Math.max(from.getZ(), to.getZ());

        VectorM3f[] c = corners;
        c[0].x = minX; c[0].y = minY; c[0].z = minZ;
        c[1].x = minX; c[1].y = minY; c[1].z = maxZ;
        c[2].x = maxX; c[2].y = minY; c[2].z = minZ;
        c[3].x = maxX; c[3].y = minY; c[3].z = maxZ;
        c[4].x = minX; c[4].y = maxY; c[4].z = minZ;
        c[5].x = minX; c[5].y = maxY; c[5].z = maxZ;
        c[6].x = maxX; c[6].y = maxY; c[6].z = minZ;
        c[7].x = maxX; c[7].y = maxY; c[7].z = maxZ;

        int modelStart = blockModel.getStart();
        createElementFace(element, Direction.DOWN, c[0], c[2], c[3], c[1]);
        createElementFace(element, Direction.UP, c[5], c[7], c[6], c[4]);
        createElementFace(element, Direction.NORTH, c[2], c[0], c[4], c[6]);
        createElementFace(element, Direction.SOUTH, c[1], c[3], c[7], c[5]);
        createElementFace(element, Direction.WEST, c[0], c[1], c[5], c[4]);
        createElementFace(element, Direction.EAST, c[3], c[2], c[6], c[7]);
        blockModel.initialize(modelStart);

        //rotate and scale down
        blockModel.transform(modelElementTransform
                .copy(element.getRotation().getMatrix())
                .scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE)
        );
    }

    private final VectorM3f faceRotationVector = new VectorM3f(0, 0, 0);

    // =================================================================================================================
    // region ## REFACTORED createElementFace AND HELPERS ##
    //
    // Code Smell Code [java:S6541] Brain Method
    // single-responsibility methods to reduce complexity and improve readability.
    // =================================================================================================================

    /**
     * A simple data class to hold sky and block light values together.
     */
    private static class FaceLight {
        final int skyLight;
        final int blockLight;

        public FaceLight(int skyLight, int blockLight) {
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }
    }

    /**
     * Main method to create a face of a model element. It now orchestrates calls to helper methods.
     */
    private void createElementFace(Element element, Direction faceDir, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3) {
        Face face = element.getFaces().get(faceDir);
        if (face == null) return;

        // --- Step 1: Lighting & Culling ---
        FaceLight faceLight = calculateLight(faceDir);
        if (shouldRemoveInCave(faceLight)) return;

        calculateFaceRotationVector(element, faceDir); // Sets the field faceRotationVector
        if (shouldCullFace(face, faceRotationVector)) return;

        // --- Step 2: Initialize Model and Set Positions ---
        blockModel.initialize();
        blockModel.add(2); // 2 triangles per face
        int face1 = blockModel.getStart();
        int face2 = face1 + 1;
        setFacePositions(face1, face2, c0, c1, c2, c3);

        // --- Step 3: Material and Texture ---
        ResourcePath<Texture> texturePath = face.getTexture().getTexturePath(modelResource.getTextures()::get);
        setTextureForFaces(face1, face2, texturePath);

        // --- Step 4: UVs and Tinting ---
        VectorM2f[] finalUvs = calculateUvCoordinates(face, faceDir);
        setUvsForFaces(face1, face2, finalUvs);
        applyFaceTint(face, face1, face2);

        // --- Step 5: Light and Ambient Occlusion ---
        applyLightToFaces(face1, face2, faceLight, element.getLightEmission());
        applyAmbientOcclusion(face1, face2, c0, c1, c2, c3, faceDir);

        // --- Step 6: Update Final Map Color ---
        updateMapColor(texturePath, faceRotationVector, faceLight);
    }

    /**
     * Calculates the effective sunlight and blocklight for a face.
     */
    private FaceLight calculateLight(Direction faceDir) {
        ExtendedBlock facedBlockNeighbor = getRotationRelativeBlock(faceDir);
        LightData blockLightData = block.getLightData();
        LightData facedLightData = facedBlockNeighbor.getLightData();

        int sunLight = Math.max(blockLightData.getSkyLight(), facedLightData.getSkyLight());
        int blockLight = Math.max(blockLightData.getBlockLight(), facedLightData.getBlockLight());

        return new FaceLight(sunLight, blockLight);
    }

    /**
     * Checks if the face should be removed based on cave-rendering settings.
     */
    private boolean shouldRemoveInCave(FaceLight faceLight) {
        if (!block.isRemoveIfCave()) return false;

        int lightLevel = renderSettings.isCaveDetectionUsesBlockLight()
                ? Math.max(faceLight.blockLight, faceLight.skyLight)
                : faceLight.skyLight;

        return lightLevel == 0;
    }

    /**
     * Calculates the rotated direction vector for a face and stores it in the class field `faceRotationVector`.
     */
    private void calculateFaceRotationVector(Element element, Direction faceDir) {
        Vector3i faceDirVector = faceDir.toVector();
        faceRotationVector.set(
                faceDirVector.getX(),
                faceDirVector.getY(),
                faceDirVector.getZ()
        );
        faceRotationVector.rotateAndScale(element.getRotation().getMatrix());
        makeRotationRelative(faceRotationVector);
    }

    /**
     * Determines if a face should be culled based on its direction or cullface properties.
     */
    private boolean shouldCullFace(Face face, VectorM3f faceRotationVec) {
        if (renderSettings.isRenderTopOnly() && faceRotationVec.y < 0.01) return true;

        if (face.getCullface() != null) {
            ExtendedBlock b = getRotationRelativeBlock(face.getCullface());
            BlockProperties p = b.getProperties();
            if (p.isCulling()) return true;
            return p.getCullingIdentical() && b.getBlockState().equals(block.getBlockState());
        }

        return false;
    }

    /**
     * Sets the vertex positions for the two triangles that make up the face.
     */
    private void setFacePositions(int face1, int face2, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3) {
        TileModel tileModel = blockModel.getTileModel();
        tileModel.setPositions(face1, c0.x, c0.y, c0.z, c1.x, c1.y, c1.z, c2.x, c2.y, c2.z);
        tileModel.setPositions(face2, c0.x, c0.y, c0.z, c2.x, c2.y, c2.z, c3.x, c3.y, c3.z);
    }

    /**
     * Sets the material (texture) for the face's triangles.
     */
    private void setTextureForFaces(int face1, int face2, ResourcePath<Texture> texturePath) {
        int textureId = textureGallery.get(texturePath);
        blockModel.getTileModel().setMaterialIndex(face1, textureId);
        blockModel.getTileModel().setMaterialIndex(face2, textureId);
    }

    /**
     * Calculates the final UV coordinates, accounting for face and UV-lock rotations.
     */
    private VectorM2f[] calculateUvCoordinates(Face face, Direction faceDir) {
        Vector4f uvRaw = face.getUv();
        rawUvs[0].set(uvRaw.getX() / 16f, uvRaw.getW() / 16f);
        rawUvs[1].set(uvRaw.getZ() / 16f, uvRaw.getW() / 16f);
        rawUvs[2].set(uvRaw.getZ() / 16f, uvRaw.getY() / 16f);
        rawUvs[3].set(uvRaw.getX() / 16f, uvRaw.getY() / 16f);

        int rotationSteps = Math.floorDiv(face.getRotation(), 90) % 4;
        if (rotationSteps < 0) rotationSteps += 4;
        for (int i = 0; i < 4; i++) {
            uvs[i] = rawUvs[(rotationSteps + i) % 4];
        }

        if (variant.isUvlock() && variant.isTransformed()) {
            Vector3i faceDirVector = faceDir.toVector();
            float xRotSin = TrigMath.sin(variant.getX() * TrigMath.DEG_TO_RAD);
            float xRotCos = TrigMath.cos(variant.getX() * TrigMath.DEG_TO_RAD);

            float uvRotation = variant.getY() * (faceDirVector.getY() * xRotCos + faceDirVector.getZ() * xRotSin) +
                    variant.getX() * (1 - faceDirVector.getY());

            if (uvRotation != 0) {
                uvRotation = (float)(uvRotation * TrigMath.DEG_TO_RAD);
                float cx = TrigMath.cos(uvRotation), cy = TrigMath.sin(uvRotation);
                for (VectorM2f uv : uvs) {
                    uv.translate(-0.5f, -0.5f).rotate(cx, cy).translate(0.5f, 0.5f);
                }
            }
        }

        return uvs;
    }

    /**
     * Sets the UV coordinates for the face's triangles.
     */
    private void setUvsForFaces(int face1, int face2, VectorM2f[] finalUvs) {
        TileModel tileModel = blockModel.getTileModel();
        tileModel.setUvs(face1, finalUvs[0].x, finalUvs[0].y, finalUvs[1].x, finalUvs[1].y, finalUvs[2].x, finalUvs[2].y);
        tileModel.setUvs(face2, finalUvs[0].x, finalUvs[0].y, finalUvs[2].x, finalUvs[2].y, finalUvs[3].x, finalUvs[3].y);
    }

    /**
     * Applies tint color to the model faces if required.
     */
    private void applyFaceTint(Face face, int face1, int face2) {
        TileModel tileModel = blockModel.getTileModel();
        if (face.getTintindex() >= 0) {
            if (tintColor.a < 0) { // Lazy load tint color
                blockColorCalculator.getBlockColor(block, tintColor);
            }
            tileModel.setColor(face1, tintColor.r, tintColor.g, tintColor.b);
            tileModel.setColor(face2, tintColor.r, tintColor.g, tintColor.b);
        } else {
            tileModel.setColor(face1, 1f, 1f, 1f);
            tileModel.setColor(face2, 1f, 1f, 1f);
        }
    }

    /**
     * Applies light data to the model faces.
     */
    private void applyLightToFaces(int face1, int face2, FaceLight faceLight, int lightEmission) {
        TileModel tileModel = blockModel.getTileModel();
        int emissiveBlockLight = Math.max(faceLight.blockLight, lightEmission);
        tileModel.setBlocklight(face1, emissiveBlockLight);
        tileModel.setBlocklight(face2, emissiveBlockLight);
        tileModel.setSunlight(face1, faceLight.skyLight);
        tileModel.setSunlight(face2, faceLight.skyLight);
    }

    /**
     * Calculates and applies ambient occlusion to the model faces.
     */
    private void applyAmbientOcclusion(int face1, int face2, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3, Direction dir) {
        float ao0 = 1f, ao1 = 1f, ao2 = 1f, ao3 = 1f;
        if (modelResource.isAmbientocclusion()) {
            ao0 = testAo(c0, dir);
            ao1 = testAo(c1, dir);
            ao2 = testAo(c2, dir);
            ao3 = testAo(c3, dir);
        }
        blockModel.getTileModel().setAOs(face1, ao0, ao1, ao2);
        blockModel.getTileModel().setAOs(face2, ao0, ao2, ao3);
    }

    /**
     * Updates the aggregate map color if the face is pointing upwards.
     */
    private void updateMapColor(ResourcePath<Texture> texturePath, VectorM3f faceRotationVec, FaceLight faceLight) {
        if (faceRotationVec.y <= 0.01 || texturePath == null) return;

        Texture texture = texturePath.getResource(resourcePack::getTexture);
        if (texture == null) return;

        mapColor.set(texture.getColorPremultiplied());
        if (tintColor.a >= 0) {
            mapColor.multiply(tintColor);
        }

        // Apply light
        float combinedLight = Math.max(faceLight.skyLight / 15f, faceLight.blockLight / 15f);
        combinedLight = (1 - renderSettings.getAmbientLight()) * combinedLight + renderSettings.getAmbientLight();
        mapColor.r *= combinedLight;
        mapColor.g *= combinedLight;
        mapColor.b *= combinedLight;

        if (mapColor.a > blockColorOpacity) {
            blockColorOpacity = mapColor.a;
        }

        blockColor.add(mapColor);
    }

    // endregion
    // =================================================================================================================


    private ExtendedBlock getRotationRelativeBlock(Direction direction){
        return getRotationRelativeBlock(direction.toVector());
    }

    private ExtendedBlock getRotationRelativeBlock(Vector3i direction){
        return getRotationRelativeBlock(
                direction.getX(),
                direction.getY(),
                direction.getZ()
        );
    }

    private final VectorM3f rotationRelativeBlockDirection = new VectorM3f(0, 0, 0);
    private ExtendedBlock getRotationRelativeBlock(int dx, int dy, int dz){
        rotationRelativeBlockDirection.set(dx, dy, dz);
        makeRotationRelative(rotationRelativeBlockDirection);

        return block.getNeighborBlock(
                Math.round(rotationRelativeBlockDirection.x),
                Math.round(rotationRelativeBlockDirection.y),
                Math.round(rotationRelativeBlockDirection.z)
        );
    }

    private void makeRotationRelative(VectorM3f direction){
        if (variant.isTransformed())
            direction.rotateAndScale(variant.getTransformMatrix());
    }

    private float testAo(VectorM3f vertex, Direction dir){
        Vector3i dirVec = dir.toVector();
        int occluding = 0;

        int x = 0;
        if (vertex.x == 16){
            x = 1;
        } else if (vertex.x == 0){
            x = -1;
        }

        int y = 0;
        if (vertex.y == 16){
            y = 1;
        } else if (vertex.y == 0){
            y = -1;
        }

        int z = 0;
        if (vertex.z == 16){
            z = 1;
        } else if (vertex.z == 0){
            z = -1;
        }


        if (x * dirVec.getX() + y * dirVec.getY() > 0){
            if (getRotationRelativeBlock(x, y, 0).getProperties().isOccluding()) occluding++;
        }

        if (x * dirVec.getX() + z * dirVec.getZ() > 0){
            if (getRotationRelativeBlock(x, 0, z).getProperties().isOccluding()) occluding++;
        }

        if (y * dirVec.getY() + z * dirVec.getZ() > 0){
            if (getRotationRelativeBlock(0, y, z).getProperties().isOccluding()) occluding++;
        }

        if (x * dirVec.getX() + y * dirVec.getY() + z * dirVec.getZ() > 0){
            if (getRotationRelativeBlock(x, y, z).getProperties().isOccluding()) occluding++;
        }

        if (occluding > 3) occluding = 3;
        return  Math.max(0f, Math.min(1f - occluding * 0.25f, 1f));
    }

    private static float hashToFloat(int x, int z, long seed) {
        final long hash = x * 73428767L ^ z * 4382893L ^ seed * 457;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

}
