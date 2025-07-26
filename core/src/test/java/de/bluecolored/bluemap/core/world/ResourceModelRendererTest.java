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

import java.util.Dictionary;

import static org.mockito.Mockito.*;

class ResourceModelRendererTest {

    private ResourceModelRenderer renderer;
    private TileModelView tileModelView;
    private TileModel tileModel;
    private BlockNeighborhood block;
    private Variant variant;
    private Model model;
    private Color color;

    @BeforeEach
    void setup() {
        // Mocks
        ResourcePack resourcePack = mock(ResourcePack.class);
        TextureGallery textureGallery = mock(TextureGallery.class);
        RenderSettings renderSettings = mock(RenderSettings.class);
        tileModelView = mock(TileModelView.class);
        tileModel = mock(TileModel.class);
        block = mock(BlockNeighborhood.class);
        variant = mock(Variant.class);
        model = mock(Model.class);
        Element element = mock(Element.class);
        Face face = mock(Face.class);
        color = new Color();

        LightData lightData = mock(LightData.class);
        BlockProperties blockProperties = mock(BlockProperties.class);
        ExtendedBlock neighborBlock = mock(ExtendedBlock.class);
        Texture texture = mock(Texture.class);

        // Setup renderer
        BlockColorCalculatorFactory calculatorFactory = mock(BlockColorCalculatorFactory.class);
        when(resourcePack.getColorCalculatorFactory()).thenReturn(calculatorFactory);

        ResourcePath<Model> mockModelPath = mock(ResourcePath.class);
        when(mockModelPath.getResource(any())).thenReturn(model);

        when(calculatorFactory.createCalculator()).thenAnswer(invocation -> {
            // Create a mock BlockColorCalculator
            BlockColorCalculatorFactory.BlockColorCalculator calculator = mock(BlockColorCalculatorFactory.BlockColorCalculator.class);

            // Mock the getBlockColor method to set the color and return it
            when(calculator.getBlockColor(any(BlockNeighborhood.class), any(Color.class)))
                    .thenAnswer(colorInvocation -> {
                        Color color = colorInvocation.getArgument(1);
                        return color.set(0.5f, 0.5f, 0.5f, 1f, false);
                    });

            return calculator;
        });

        renderer = new ResourceModelRenderer(resourcePack, textureGallery, renderSettings);

        // Setup mocks
        when(block.getX()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getProperties()).thenReturn(blockProperties);
        when(block.getLightData()).thenReturn(lightData);
        when(block.getNeighborBlock(anyInt(), anyInt(), anyInt())).thenReturn(neighborBlock);
        when(neighborBlock.getLightData()).thenReturn(lightData);
        when(neighborBlock.getProperties()).thenReturn(mock(BlockProperties.class));
        when(blockProperties.isRandomOffset()).thenReturn(false);
        when(tileModelView.getStart()).thenReturn(0);
        when(tileModelView.getTileModel()).thenReturn(tileModel);
        when(tileModelView.initialize()).thenReturn(tileModelView);
        when(tileModelView.getTileModel()).thenReturn(tileModel);

        when(variant.getModel()).thenReturn(mockModelPath);
        when(variant.getTransformMatrix()).thenReturn(new MatrixM4f());
        when(variant.isTransformed()).thenReturn(false);
        when(variant.isUvlock()).thenReturn(false);

        when(model.getElements()).thenReturn(new Element[]{element});
        when(model.isAmbientocclusion()).thenReturn(true);

        // Simulasikan 1 face (misal UP)
        when(element.getFrom()).thenReturn(new com.flowpowered.math.vector.Vector3f(0, 0, 0));
        when(element.getTo()).thenReturn(new com.flowpowered.math.vector.Vector3f(16, 16, 16));

        Rotation rotation = mock(Rotation.class);
        when(rotation.getMatrix()).thenReturn(new MatrixM4f()); // Return identity matrix
        when(element.getRotation()).thenReturn(rotation);

        Dictionary mockFaceDict = mock(Dictionary.class);
        when(mockFaceDict.get(Direction.UP)).thenReturn(face);
        doReturn(mockFaceDict).when(element).getFaces();

        when(face.getUv()).thenReturn(new com.flowpowered.math.vector.Vector4f(0, 0, 16, 16));

        TextureVariable mockTextureVariable = mock(TextureVariable.class);
        when(face.getTexture()).thenReturn(mockTextureVariable);

        when(face.getRotation()).thenReturn(0);
        when(face.getTintindex()).thenReturn(-1);
        when(face.getCullface()).thenReturn(null);

        when(renderSettings.getAmbientLight()).thenReturn(0f);
        when(textureGallery.get(any())).thenReturn(1);

        when(resourcePack.getTexture(any())).thenReturn(texture);
        when(texture.getColorPremultiplied()).thenReturn(new Color().set(1f, 1f, 1f, 1f, true));
    }

    @Test
    void testRender_addsFaceToTileModel() {
        // Act
        renderer.render(block, variant, tileModelView, color);

        // Assert
        // Verifies that 2 triangles (1 face) are added
        verify(tileModelView, atLeastOnce()).add(2);

        // Verifies that tile model has positions set
        verify(tileModel, atLeastOnce()).setPositions(anyInt(),
                anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat(), anyFloat());

        // Verifies that UVs are set
        verify(tileModel, atLeastOnce()).setUvs(anyInt(),
                anyFloat(), anyFloat(),
                anyFloat(), anyFloat(),
                anyFloat(), anyFloat());

        // Verifies that material is set
        verify(tileModel, atLeastOnce()).setMaterialIndex(anyInt(), anyInt());

        // Verifies that AO, light, and color are set
        verify(tileModel, atLeastOnce()).setAOs(anyInt(), anyFloat(), anyFloat(), anyFloat());
        verify(tileModel, atLeastOnce()).setBlocklight(anyInt(), anyInt());
        verify(tileModel, atLeastOnce()).setSunlight(anyInt(), anyInt());
        verify(tileModel, atLeastOnce()).setColor(anyInt(), anyFloat(), anyFloat(), anyFloat());
    }
}
