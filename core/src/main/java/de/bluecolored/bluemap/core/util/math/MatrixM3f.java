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

import com.flowpowered.math.TrigMath;

public class MatrixM3f {

    public float m00 = 1f, m01, m02;
    public float m10, m11 = 1f, m12;
    public float m20, m21, m22 = 1f;

    public MatrixM3f set(Matrix3x3 mat) {
        this.m00 = mat.m00; this.m01 = mat.m01; this.m02 = mat.m02;
        this.m10 = mat.m10; this.m11 = mat.m11; this.m12 = mat.m12;
        this.m20 = mat.m20; this.m21 = mat.m21; this.m22 = mat.m22;
        return this;
    }

    public MatrixM3f invert() {
        float det = determinant();
        Matrix3x3 inverse = new Matrix3x3(new float[]{
                (m11 * m22 - m21 * m12) / det, -(m01 * m22 - m21 * m02) / det, (m01 * m12 - m02 * m11) / det,
                -(m10 * m22 - m20 * m12) / det, (m00 * m22 - m20 * m02) / det, -(m00 * m12 - m10 * m02) / det,
                (m10 * m21 - m20 * m11) / det, -(m00 * m21 - m20 * m01) / det, (m00 * m11 - m01 * m10) / det
        });

        return set(inverse);
    }

    public MatrixM3f identity() {
        return set(Matrix3x3.identity());
    }

    public MatrixM3f scale(float x, float y, float z) {
        return multiplyTo(Matrix3x3.scale(x,y,z));
    }

    public MatrixM3f translate(float x, float y) {
        return multiplyTo(Matrix3x3.translation(x,y));
    }

    public MatrixM3f rotate(float angle, float axisX, float axisY, float axisZ) {

        // create quaternion
        double halfAngle = Math.toRadians(angle) * 0.5;
        double q = TrigMath.sin(halfAngle) / Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        double   //quaternion
                qx = axisX * q,
                qy = axisY * q,
                qz = axisZ * q,
                qw = TrigMath.cos(halfAngle),
                qLength = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);

        // normalize quaternion
        qx /= qLength;
        qy /= qLength;
        qz /= qLength;
        qw /= qLength;

        return rotateByQuaternion((float) qx, (float) qy, (float) qz, (float) qw);
    }

    public MatrixM3f rotate(float pitch, float yaw, float roll) {

        double
                halfYaw = Math.toRadians(yaw) * 0.5,
                qy1 = TrigMath.sin(halfYaw),
                qw1 = TrigMath.cos(halfYaw),

                halfPitch = Math.toRadians(pitch) * 0.5,
                qx2 = TrigMath.sin(halfPitch),
                qw2 = TrigMath.cos(halfPitch),

                halfRoll = Math.toRadians(roll) * 0.5,
                qz3 = TrigMath.sin(halfRoll),
                qw3 = TrigMath.cos(halfRoll);

        // multiply 1 with 2
        double
                qxA =   qw1 * qx2,
                qyA =   qy1 * qw2,
                qzA = - qy1 * qx2,
                qwA =   qw1 * qw2;

        // multiply with 3
        return rotateByQuaternion(
                (float) (qxA * qw3 + qyA * qz3),
                (float) (qyA * qw3 - qxA * qz3),
                (float) (qwA * qz3 + qzA * qw3),
                (float) (qwA * qw3 - qzA * qz3)
        );
    }

    public MatrixM3f rotateByQuaternion(float qx, float qy, float qz, float qw) {
        Matrix3x3 rotationMatrix = new Matrix3x3(new float[]{
                1 - 2 * qy * qy - 2 * qz * qz,
                2 * qx * qy - 2 * qw * qz,
                2 * qx * qz + 2 * qw * qy,
                2 * qx * qy + 2 * qw * qz,
                1 - 2 * qx * qx - 2 * qz * qz,
                2 * qy * qz - 2 * qw * qx,
                2 * qx * qz - 2 * qw * qy,
                2 * qy * qz + 2 * qx * qw,
                1 - 2 * qx * qx - 2 * qy * qy
        });

        return multiplyTo(rotationMatrix);
    }

    public MatrixM3f multiply(Matrix3x3 mat) {
        return set(new Matrix3x3(new float[]{
                this.m00 * mat.m00 + this.m01 * mat.m10 + this.m02 * mat.m20,
                this.m00 * mat.m01 + this.m01 * mat.m11 + this.m02 * mat.m21,
                this.m00 * mat.m02 + this.m01 * mat.m12 + this.m02 * mat.m22,
                this.m10 * mat.m00 + this.m11 * mat.m10 + this.m12 * mat.m20,
                this.m10 * mat.m01 + this.m11 * mat.m11 + this.m12 * mat.m21,
                this.m10 * mat.m02 + this.m11 * mat.m12 + this.m12 * mat.m22,
                this.m20 * mat.m00 + this.m21 * mat.m10 + this.m22 * mat.m20,
                this.m20 * mat.m01 + this.m21 * mat.m11 + this.m22 * mat.m21,
                this.m20 * mat.m02 + this.m21 * mat.m12 + this.m22 * mat.m22
        }));
    }

    public MatrixM3f multiplyTo(Matrix3x3 mat) {
        return set(new Matrix3x3(new float[]{
                mat.m00 * this.m00 + mat.m01 * this.m10 + mat.m02 * this.m20,
                mat.m00 * this.m01 + mat.m01 * this.m11 + mat.m02 * this.m21,
                mat.m00 * this.m02 + mat.m01 * this.m12 + mat.m02 * this.m22,
                mat.m10 * this.m00 + mat.m11 * this.m10 + mat.m12 * this.m20,
                mat.m10 * this.m01 + mat.m11 * this.m11 + mat.m12 * this.m21,
                mat.m10 * this.m02 + mat.m11 * this.m12 + mat.m12 * this.m22,
                mat.m20 * this.m00 + mat.m21 * this.m10 + mat.m22 * this.m20,
                mat.m20 * this.m01 + mat.m21 * this.m11 + mat.m22 * this.m21,
                mat.m20 * this.m02 + mat.m21 * this.m12 + mat.m22 * this.m22
        }));
    }

    public float determinant() {
        return m00 * (m11 * m22 - m12 * m21) - m01 * (m10 * m22 - m12 * m20) + m02 * (m10 * m21 - m11 * m20);
    }

}
