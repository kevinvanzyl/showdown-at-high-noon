package codes.kevinvanzyl.showdownathighnoon.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by kevin on 2018/07/23.
 */

public class TiltSensor implements SensorEventListener {

    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_LEFT = 2;
    public static final int DIRECTION_RIGHT = 3;
    public static final int DIRECTION_NONE = -1;

    private final float[] mMagnet = new float[3];               // magnetic field vector
    private final float[] mAcceleration = new float[3];         // accelerometer vector
    private final float[] mAccMagOrientation = new float[3];    // orientation angles from mAcceleration and mMagnet
    private float[] mRotationMatrix = new float[9];

    SensorManager sensorManager;
    Sensor accSensor;
    Sensor magSensor;

    private int currentDirection = DIRECTION_NONE;

    public TiltSensor(Activity activity) {

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void resume() {
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void pause() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAcceleration, 0, 3);
                calculateAccMagOrientation(event.values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagnet, 0, 3);
                break;
            default: break;
        }
    }

    public void calculateAccMagOrientation(float[] values) {

        if (SensorManager.getRotationMatrix(mRotationMatrix, null, mAcceleration, mMagnet))
            SensorManager.getOrientation(mRotationMatrix, mAccMagOrientation);
        else {
            double gx, gy, gz;
            gx = mAcceleration[0] / 9.81f;
            gy = mAcceleration[1] / 9.81f;
            gz = mAcceleration[2] / 9.81f;
            // http://theccontinuum.com/2012/09/24/arduino-imu-pitch-roll-from-accelerometer/
            float pitch = (float) -Math.atan(gy / Math.sqrt(gx * gx + gz * gz));
            float roll = (float) -Math.atan(gx / Math.sqrt(gy * gy + gz * gz));
            float azimuth = 0; // Impossible to guess

            mAccMagOrientation[0] = azimuth;
            mAccMagOrientation[1] = pitch;
            mAccMagOrientation[2] = roll;
            mRotationMatrix = getRotationMatrixFromOrientation(mAccMagOrientation);
        }

        float x = values[0];
        float y = values[1];
        if (Math.abs(x) > Math.abs(y)) {
            if (x < 0) {

                if (Math.toDegrees(mAccMagOrientation[2]) >= 50) {
                    currentDirection = DIRECTION_UP;
                }
            }
            if (x > 0) {

                if (Math.toDegrees(mAccMagOrientation[2]) <= -50) {
                    currentDirection = DIRECTION_DOWN;
                }
            }
        } else {
            if (y < 0) {

                if (Math.toDegrees(mAccMagOrientation[1]) >= 50) {
                    currentDirection = DIRECTION_LEFT;
                }
            }
            if (y > 0) {

                if (Math.toDegrees(mAccMagOrientation[1]) <= -50) {
                    currentDirection = DIRECTION_RIGHT;
                }
            }
        }
        if (x > (-2) && x < (2) && y > (-2) && y < (2)) {

            currentDirection = DIRECTION_NONE;
        }
    }

    public static float[] getRotationMatrixFromOrientation(float[] o) {

        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;xM[1] = 0.0f;xM[2] = 0.0f;
        xM[3] = 0.0f;xM[4] = cosX;xM[5] = sinX;
        xM[6] = 0.0f;xM[7] =-sinX;xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;yM[1] = 0.0f;yM[2] = sinY;
        yM[3] = 0.0f;yM[4] = 1.0f;yM[5] = 0.0f;
        yM[6] =-sinY;yM[7] = 0.0f;yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;zM[1] = sinZ;zM[2] = 0.0f;
        zM[3] =-sinZ;zM[4] = cosZ;zM[5] = 0.0f;
        zM[6] = 0.0f;zM[7] = 0.0f;zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    public static float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public int getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(int currentDirection) {
        this.currentDirection = currentDirection;
    }
}
