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
package de.bluecolored.bluemap.core.util.math;

public class Matrix3x3 {
    // Public final fields for direct access (like your original MatrixM3f)
    public final float m00, m01, m02;
    public final float m10, m11, m12;
    public final float m20, m21, m22;

    // Constructor using array to avoid >7 parameters
    public Matrix3x3(float[] values) {
        if (values.length != 9) {
            throw new IllegalArgumentException("Array must contain exactly 9 values");
        }
        this.m00 = values[0]; this.m01 = values[1]; this.m02 = values[2];
        this.m10 = values[3]; this.m11 = values[4]; this.m12 = values[5];
        this.m20 = values[6]; this.m21 = values[7]; this.m22 = values[8];
    }

    // Alternative constructor with 2D array
    public Matrix3x3(float[][] values) {
        if (values.length != 3 || values[0].length != 3) {
            throw new IllegalArgumentException("Array must be 3x3");
        }
        this.m00 = values[0][0]; this.m01 = values[0][1]; this.m02 = values[0][2];
        this.m10 = values[1][0]; this.m11 = values[1][1]; this.m12 = values[1][2];
        this.m20 = values[2][0]; this.m21 = values[2][1]; this.m22 = values[2][2];
    }

    // Convenience constructor for identity matrix
    public Matrix3x3() {
        this(new float[]{1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f});
    }

    // Factory method for identity matrix
    public static Matrix3x3 identity() {
        return new Matrix3x3();
    }

    // Factory method for scale matrix
    public static Matrix3x3 scale(float x, float y, float z) {
        return new Matrix3x3(new float[]{
                x,  0f, 0f,
                0f, y,  0f,
                0f, 0f, z
        });
    }

    // Factory method for translation matrix
    public static Matrix3x3 translation(float x, float y) {
        return new Matrix3x3(new float[]{
                1f, 0f, x,
                0f, 1f, y,
                0f, 0f, 1f
        });
    }

    // Convert to array for easy parameter passing
    public float[] toArray() {
        return new float[]{m00, m01, m02, m10, m11, m12, m20, m21, m22};
    }
}