package com.example.alan.a0811whacamole;

import static java.lang.Double.NaN;

/**
 * Created by Alan on 2017/9/22.
 */

public class UtilForTrajectoryProcessing {

    private double[] lastTransitionData = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
    DigitalSmoother digitalSmootherForVelocityX = new DigitalSmoother();
    DigitalSmoother digitalSmootherForVelocityY = new DigitalSmoother();
    DigitalSmoother digitalSmootherForVelocityZ = new DigitalSmoother();

    private double[] lastVelocityData = {0.0, 0.0, 0.0, 0.0};
    DigitalSmoother digitalSmootherForAccelerationX = new DigitalSmoother();
    DigitalSmoother digitalSmootherForAccelerationY = new DigitalSmoother();
    DigitalSmoother digitalSmootherForAccelerationZ = new DigitalSmoother();

    public void setLastTransitionData(double[] lastTransitionData) {
        this.lastTransitionData = lastTransitionData;
        this.lastTransitionData[0] = this.lastTransitionData[0] - 0.01;
        this.lastTransitionData[2] = -this.lastTransitionData[2];
    }

    //the translationData should be in the order of timeStamp + translation + quaternion (scale first)
    public double[] getVelocity(double[] transitionData){
        double[] velocity = new double[3];
        transitionData[2] = - transitionData[2];//inverse the direction because image coordinator's Y axis is downward

//        if ((transitionData[0] - lastTransitionData[0]) == 0) return lastVelocityData;

        velocity[0] = (transitionData[1] - lastTransitionData[1]) / (transitionData[0] - lastTransitionData[0]);
        velocity[0] = digitalSmootherForVelocityX.digitalSmooth(velocity[0]);

        velocity[1] = (transitionData[2] - lastTransitionData[2]) / (transitionData[0] - lastTransitionData[0]);
        velocity[1] = digitalSmootherForVelocityY.digitalSmooth(velocity[1]);

        velocity[2] = (transitionData[3] - lastTransitionData[3]) / (transitionData[0] - lastTransitionData[0]);
        velocity[2] = digitalSmootherForVelocityZ.digitalSmooth(velocity[2]);


        System.arraycopy(transitionData, 0, lastTransitionData, 0, transitionData.length);
        return velocity;
    }

    public double[] getAcceleration(double[] VelocityData){
        double[] acceleration = new double[3];

        acceleration[0] = (VelocityData[1] - lastVelocityData[1]) / (VelocityData[0] - lastVelocityData[0]);
        acceleration[0] = digitalSmootherForAccelerationX.digitalSmooth(acceleration[0]);

        acceleration[1] = (VelocityData[2] - lastVelocityData[2]) / (VelocityData[0] - lastVelocityData[0]);
        acceleration[1] = digitalSmootherForAccelerationY.digitalSmooth(acceleration[1]);

        acceleration[2] = (VelocityData[3] - lastVelocityData[3]) / (VelocityData[0] - lastVelocityData[0]);
        acceleration[2] = digitalSmootherForAccelerationZ.digitalSmooth(acceleration[2]);

        System.arraycopy(VelocityData, 0, lastVelocityData, 0, VelocityData.length );
        return acceleration;
    }

    public double[] getAccelerationFromTransition(double[] transitionData){
        double[] velocity = getVelocity(transitionData);        //get the current velocity
        double[] VelocityData = new double[4];
        VelocityData[0] = (transitionData[0] + lastTransitionData[0]) / 2.0;//timeStamp
        VelocityData[1] = velocity[0];
        VelocityData[2] = velocity[1];
        VelocityData[3] = velocity[2];
        return getAcceleration(VelocityData);
    }

    private static class DigitalSmoother{

        private static final int NUMREADINGS = 8;
        double[] readings = new double[NUMREADINGS];
        private int ndx=0;
        private int count=0;
        private double total=0;

        // remove signal noise
        private double digital_smooth(double value, double[] data_array) {
            total -= data_array[ndx];
            data_array[ndx] = value;
            total += data_array[ndx];
            ndx = (ndx + 1) % NUMREADINGS;
            if (count < NUMREADINGS)
                count++;
            return total / count;
        }

        public double digitalSmooth(double value){
            return digital_smooth(value, readings);
        }
    }

    double accelThreshold = 0.1; //threshold to decide whether device is moving or not 端末が静止しているかどうか判定するための，加速度の変化量のしきい値
    double[] a1 = {0.0, 0.0, 0.0}; //acceleration of time t-1  t-1の加速度
    double[] a2 = {0.0, 0.0, 0.0}; //acceleration of time t-2  t-2の加速度
    double[] a3 = {0.0, 0.0, 0.0}; //acceleration of time t-3  t-3の加速度
    double[] a4 = {0.0, 0.0, 0.0}; //acceleration of time t-4  t-4の加速度
    double[] a5 = { 0.0, 0.0, 0.0}; // acceleration of time t-5 t-5の加速度
    // Judge if the device is moving or not 端末が静止しているかどうか判定
    private boolean isAxisStop(double a, int axis) {
        /// Substitution of acceleration 加速度の代入
        a5[axis] = a4[axis];
        a4[axis] = a3[axis];
        a3[axis] = a2[axis];
        a2[axis] = a1[axis];
        a1[axis] = a;
        /// If a4 is 0, the device is stop a4が0ならまだ静止しているとみなす
        if (a4[axis] == 0.0f) {
            return true;
        }
        /// If acceleration is above threshold two times continuity, the device
        /// is moving 2回連続でしきい値以上なら，動いているとみなす
        // if(Math.abs(a1[axis]-a2[axis]) > accelThreshold &&
        // Math.abs(a2[axis]-a3[axis]) > accelThreshold &&
        // Math.abs(a3[axis]-a4[axis]) > accelThreshold){
        /// If al-a5 is above threshold, the device is moving
        /// 5期前と比較した差がしきい値以上なら，動いているとみなす
        if (Math.abs(a1[axis] - a5[axis]) > accelThreshold) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isDeviceStop(double[] accData) {
        boolean isXStop = isAxisStop(accData[0], 0);
        boolean isYStop = isAxisStop(accData[1], 1);
        boolean isZStop = isAxisStop(accData[2], 2);
        return isXStop && isYStop && isZStop;
    }

}








