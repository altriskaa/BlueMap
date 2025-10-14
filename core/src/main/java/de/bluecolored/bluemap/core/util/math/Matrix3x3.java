package de.bluecolored.bluemap.core.util.math;

/**
 * =========================================== REFACTORED SECTION =======================================================
 * This section is affected by the code smell in the MatrixM3f.java
 *
 * The code smell is LONG PARAMETER LIST.
 * CS description from SonarQube:
 * - Method has 9 parameters, which is greater than 7 authorized.
 *
 * Refactor method used: Introduce Parameter Object
 * =================================================================================================================
 */


public class Matrix3x3 {

    // public final fields for direct access
    public final float m00, m01, m02;
    public final float m10, m11, m12;
    public final float m20, m21, m22;

    // constructor using array to avoid >7 parameters
    public Matrix3x3(float[] values) {
        if (values.length != 9) {
            throw new IllegalArgumentException("Array must contain exactly 9 values");
        }
        this.m00 = values[0]; this.m01 = values[1]; this.m02 = values[2];
        this.m10 = values[3]; this.m11 = values[4]; this.m12 = values[5];
        this.m20 = values[6]; this.m21 = values[7]; this.m22 = values[8];
    }

    // alternative constructor with 2D array
    public Matrix3x3(float[][] values) {
        if (values.length != 3 || values[0].length != 3) {
            throw new IllegalArgumentException("Array must be 3x3");
        }
        this.m00 = values[0][0]; this.m01 = values[0][1]; this.m02 = values[0][2];
        this.m10 = values[1][0]; this.m11 = values[1][1]; this.m12 = values[1][2];
        this.m20 = values[2][0]; this.m21 = values[2][1]; this.m22 = values[2][2];
    }

    // convenience constructor for identity matrix
    public Matrix3x3() {
        this(new float[]{1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f});
    }

    // factory method for identity matrix
    public static Matrix3x3 identity() {
        return new Matrix3x3();
    }

    // factory method for scale matrix
    public static Matrix3x3 scale(float x, float y, float z) {
        return new Matrix3x3(new float[]{
                x,  0f, 0f,
                0f, y,  0f,
                0f, 0f, z
        });
    }

    // factory method for translation matrix
    public static Matrix3x3 translation(float x, float y) {
        return new Matrix3x3(new float[]{
                1f, 0f, x,
                0f, 1f, y,
                0f, 0f, 1f
        });
    }

    // convert to array for easy parameter passing
    public float[] toArray() {
        return new float[]{m00, m01, m02, m10, m11, m12, m20, m21, m22};
    }
}
