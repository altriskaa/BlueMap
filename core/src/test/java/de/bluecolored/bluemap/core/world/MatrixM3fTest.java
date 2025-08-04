package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixM3fTest {
    private MatrixM3f matrix;
    private static final float EPSILON = 1e-6f;

    @BeforeEach
    void setup(){
        matrix = new MatrixM3f();

    }

    @Test
    void testDefaultConstructor() {
        // Default constructor should create identity matrix
        assertEquals(1f, matrix.m00);
        assertEquals(0f, matrix.m01);
        assertEquals(0f, matrix.m02);
        assertEquals(0f, matrix.m10);
        assertEquals(1f, matrix.m11);
        assertEquals(0f, matrix.m12);
        assertEquals(0f, matrix.m20);
        assertEquals(0f, matrix.m21);
        assertEquals(1f, matrix.m22);
    }

    @Test
    void testSet() {
        MatrixM3f result = matrix.set(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f
        );

        // Should return the same instance
        assertSame(matrix, result);

        // Check all values are set correctly
        assertEquals(1f, matrix.m00);
        assertEquals(2f, matrix.m01);
        assertEquals(3f, matrix.m02);
        assertEquals(4f, matrix.m10);
        assertEquals(5f, matrix.m11);
        assertEquals(6f, matrix.m12);
        assertEquals(7f, matrix.m20);
        assertEquals(8f, matrix.m21);
        assertEquals(9f, matrix.m22);
    }

    @Test
    void testIdentity() {
        // Set to non-identity first
        matrix.set(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f);

        MatrixM3f result = matrix.identity();

        // Should return the same instance
        assertSame(matrix, result);

        // Should be identity matrix
        assertEquals(1f, matrix.m00);
        assertEquals(0f, matrix.m01);
        assertEquals(0f, matrix.m02);
        assertEquals(0f, matrix.m10);
        assertEquals(1f, matrix.m11);
        assertEquals(0f, matrix.m12);
        assertEquals(0f, matrix.m20);
        assertEquals(0f, matrix.m21);
        assertEquals(1f, matrix.m22);
    }

    @Test
    void testDeterminant() {
        // Test identity matrix determinant
        assertEquals(1f, matrix.determinant(), EPSILON);

        // Test known matrix determinant
        matrix.set(
                1f, 2f, 3f,
                1f, 2f, 3f,
                4f, 5f, 6f
        );
        assertEquals(0f, matrix.determinant(), EPSILON); // This matrix is singular

        // Test another matrix
        matrix.set(
                2f, 1f, 3f,
                1f, 0f, 1f,
                1f, 2f, 1f
        );
        assertEquals(2f, matrix.determinant(), EPSILON);
    }

    @Test
    void testInvert() {
        // Test inverting identity matrix
        MatrixM3f result = matrix.invert();
        assertSame(matrix, result);

        // Should still be identity
        assertMatrixEquals(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f);

        // Test inverting a known invertible matrix
        matrix.set(
                2f, 1f, 0f,
                1f, 2f, 1f,
                0f, 1f, 2f
        );

        matrix.invert();

        // Expected inverse values
        assertMatrixEquals(
                3f/4f, -2f/4f, 1f/4f,
                -2f/4f, 4f/4f, -2f/4f,
                1f/4f, -2f/4f, 3f/4f
        );
    }

    @Test
    void testInvertThrowsOnSingularMatrix() {
        // Set to a singular matrix (determinant = 0)
        matrix.set(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f
        );

        // Invert the singular matrix
        matrix.invert();

        // Should result in infinity or NaN values since det = 0
        assertTrue(Float.isInfinite(matrix.m00) || Float.isNaN(matrix.m00) ||
                        Float.isInfinite(matrix.m01) || Float.isNaN(matrix.m01) ||
                        Float.isInfinite(matrix.m02) || Float.isNaN(matrix.m02),
                "Inverting singular matrix should produce infinite or NaN values");
    }

    @Test
    void testScale() {
        MatrixM3f result = matrix.scale(2f, 3f, 4f);
        assertSame(matrix, result);

        assertMatrixEquals(
                2f, 0f, 0f,
                0f, 3f, 0f,
                0f, 0f, 4f
        );
    }

    @Test
    void testTranslate() {
        MatrixM3f result = matrix.translate(5f, 10f);
        assertSame(matrix, result);

        assertMatrixEquals(
                1f, 0f, 5f,
                0f, 1f, 10f,
                0f, 0f, 1f
        );
    }

    @Test
    void testRotateAroundAxis() {
        // Test 90-degree rotation around Z-axis
        matrix.rotate(90f, 0f, 0f, 1f);

        // Expected result for 90-degree Z rotation
        assertMatrixEquals(
                0f, -1f, 0f,
                1f, 0f, 0f,
                0f, 0f, 1f
        );
    }

    @Test
    void testRotateEulerAngles() {
        // Test with simple rotations
        matrix.rotate(0f, 90f, 0f); // 90-degree yaw rotation

        // This should create a Y-axis rotation matrix
        assertMatrixEquals(
                0f, 0f, 1f,
                0f, 1f, 0f,
                -1f, 0f, 0f
        );
    }

    @Test
    void testRotateByQuaternion() {
        // Test with identity quaternion (no rotation)
        MatrixM3f result = matrix.rotateByQuaternion(0f, 0f, 0f, 1f);
        assertSame(matrix, result);

        // Should remain identity
        assertMatrixEquals(
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f
        );

        // Test 90-degree rotation around Z-axis
        // Quaternion for 90-degree Z rotation: (0, 0, sin(45°), cos(45°))
        float sin45 = (float) Math.sin(Math.PI / 4);
        float cos45 = (float) Math.cos(Math.PI / 4);

        matrix.identity().rotateByQuaternion(0f, 0f, sin45, cos45);

        assertMatrixEquals(
                0f, -1f, 0f,
                1f, 0f, 0f,
                0f, 0f, 1f
        );
    }

    @Test
    void testMultiply() {
        matrix.set(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f
        );

        MatrixM3f result = matrix.multiply(
                2f, 0f, 0f,
                0f, 2f, 0f,
                0f, 0f, 2f
        );

        assertSame(matrix, result);

        // Result should be original matrix * 2
        assertMatrixEquals(
                2f, 4f, 6f,
                8f, 10f, 12f,
                14f, 16f, 18f
        );
    }

    @Test
    void testMultiplyTo() {
        matrix.set(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f
        );

        MatrixM3f result = matrix.multiplyTo(
                2f, 0f, 0f,
                0f, 2f, 0f,
                0f, 0f, 2f
        );

        assertSame(matrix, result);

        // Result should be 2 * original matrix
        assertMatrixEquals(
                2f, 4f, 6f,
                8f, 10f, 12f,
                14f, 16f, 18f
        );
    }

    @Test
    void testMultiplyVsMultiplyTo() {
        MatrixM3f matrix1 = new MatrixM3f().set(
                1f, 2f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f
        );

        MatrixM3f matrix2 = new MatrixM3f().set(
                1f, 0f, 3f,
                0f, 1f, 4f,
                0f, 0f, 1f
        );

        // Test multiply (this * other)
        MatrixM3f result1 = new MatrixM3f().set(
                matrix1.m00, matrix1.m01, matrix1.m02,
                matrix1.m10, matrix1.m11, matrix1.m12,
                matrix1.m20, matrix1.m21, matrix1.m22
        ).multiply(
                matrix2.m00, matrix2.m01, matrix2.m02,
                matrix2.m10, matrix2.m11, matrix2.m12,
                matrix2.m20, matrix2.m21, matrix2.m22
        );

        // Test multiplyTo (other * this)
        MatrixM3f result2 = new MatrixM3f().set(
                matrix1.m00, matrix1.m01, matrix1.m02,
                matrix1.m10, matrix1.m11, matrix1.m12,
                matrix1.m20, matrix1.m21, matrix1.m22
        ).multiplyTo(
                matrix2.m00, matrix2.m01, matrix2.m02,
                matrix2.m10, matrix2.m11, matrix2.m12,
                matrix2.m20, matrix2.m21, matrix2.m22
        );

        // Results should be different due to non-commutativity of matrix multiplication
        // result1 should have m02 = 3, result2 should have m02 = 3 + 2*4 = 11
        assertNotEquals(result1.m02, result2.m02, EPSILON, "multiply() and multiplyTo() should produce different results");
    }

    @Test
    void testChainedOperations() {
        // Test chaining multiple operations
        matrix.identity()
                .scale(2f, 2f, 1f)
                .translate(1f, 1f)
                .rotate(0f, 0f, 45f);

        // Verify the matrix is not identity anymore
        assertNotEquals(1f, matrix.m00, EPSILON);
    }

    @Test
    void testInverseProperty() {
        // Create a non-singular matrix
        matrix.set(
                2f, 1f, 0f,
                1f, 2f, 1f,
                0f, 1f, 2f
        );

        // Store original
        MatrixM3f original = new MatrixM3f().set(
                matrix.m00, matrix.m01, matrix.m02,
                matrix.m10, matrix.m11, matrix.m12,
                matrix.m20, matrix.m21, matrix.m22
        );

        // Invert and then invert again
        matrix.invert().invert();

        // Should be back to original (within floating point precision)
        assertMatrixEquals(
                original.m00, original.m01, original.m02,
                original.m10, original.m11, original.m12,
                original.m20, original.m21, original.m22
        );
    }

    private void assertMatrixEquals(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {
        assertEquals(m00, matrix.m00, EPSILON, "m00 mismatch");
        assertEquals(m01, matrix.m01, EPSILON, "m01 mismatch");
        assertEquals(m02, matrix.m02, EPSILON, "m02 mismatch");
        assertEquals(m10, matrix.m10, EPSILON, "m10 mismatch");
        assertEquals(m11, matrix.m11, EPSILON, "m11 mismatch");
        assertEquals(m12, matrix.m12, EPSILON, "m12 mismatch");
        assertEquals(m20, matrix.m20, EPSILON, "m20 mismatch");
        assertEquals(m21, matrix.m21, EPSILON, "m21 mismatch");
        assertEquals(m22, matrix.m22, EPSILON, "m22 mismatch");
    }
}
