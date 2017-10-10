package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/10/9.
 */

public class IMUMovementUtil {

    private static final float POS_MOVE_THRESHOLD = 0.005f;
    private static final float POS_NOT_MOVE_THRESHOLD = 0.006f;

    private float[] previousPosData = new float[4];

    boolean stillFirstTime = false;
    boolean stillSecondTime = false;
    boolean stillThirdTime = false;
    boolean stillForthTime = false;

    boolean movingFirstTime = false;
    boolean movingSecondTime = false;
    boolean movingThirdTime = false;
    boolean movingForthTime = false;

    boolean isFirstTime = true;

//    public IMUMovementUtil(float[] posData) {
//        previousPosData[1] = posData[1];
//        previousPosData[2] = posData[2];
//        previousPosData[3] = posData[3];
//    }


    private boolean isPosMoving(float[] posData){
        boolean flag;
        if( Math.sqrt( Math.pow( Math.abs(posData[1] - previousPosData[1]), 2 ) +
                Math.pow( Math.abs(posData[2] - previousPosData[2]), 2 ) +
                Math.pow( Math.abs(posData[3] - previousPosData[3]), 2 ) ) >  POS_MOVE_THRESHOLD ){
            flag = true;
        }else{
            flag = false;
        }
        System.arraycopy(posData, 0, previousPosData, 0, previousPosData.length);
        return flag;
    }



    public boolean isMoving(float[] posData){
        if (isFirstTime) {
            previousPosData[1] = posData[1];
            previousPosData[2] = posData[2];
            previousPosData[3] = posData[3];
            isFirstTime = false;
            return false;
        }
        if( isPosMoving(posData) ){
            if(movingFirstTime){
                if(movingSecondTime){
                    if(movingThirdTime){
                        if(movingForthTime){
                            //reset flag
                            movingFirstTime = false;
                            movingSecondTime = false;
                            movingThirdTime = false;
                            movingForthTime = false;
                            //movingFiveTime = false;
                            return true;
                        }else{
                            movingForthTime = true;
                            return false;
                        }
                    }else{
                        movingThirdTime = true;
                        movingForthTime = false;
                        return false;
                    }
                }else{
                    movingSecondTime = true;
                    movingThirdTime = false;
                    movingForthTime = false;
                    return false;
                }
            }
            else{
                movingFirstTime = true;
                movingSecondTime = false;
                movingThirdTime = false;
                movingForthTime = false;
                return false;
            }
        }else{
            //reset flag
            movingFirstTime = false;
            movingSecondTime = false;
            movingThirdTime = false;
            movingForthTime = false;
            return false;
        }
    }



    private boolean isPosStill(float[] posData){
        boolean flag;
        if( Math.sqrt( Math.pow( Math.abs(posData[1] - previousPosData[1]), 2 ) +
                Math.pow( Math.abs(posData[2] - previousPosData[2]), 2 ) +
                Math.pow( Math.abs(posData[3] - previousPosData[3]), 2 ) ) <  POS_NOT_MOVE_THRESHOLD ){
            flag = true;
        }else{
            flag = false;
        }
        System.arraycopy(posData, 0, previousPosData, 0, previousPosData.length);
        return flag;
    }

    public boolean isStill(float[] posData){
        if( isPosStill(posData) ){
            if(stillFirstTime){
                if(stillSecondTime){
                    if(stillThirdTime){
                        if(stillForthTime){
                            //reset flag
                            stillFirstTime = false;
                            stillSecondTime = false;
                            stillThirdTime = false;
                            stillForthTime = false;
                            //stillFiveTime = false;
                            return true;
                        }else{
                            stillForthTime = true;
                            return false;
                        }
                    }else{
                        stillThirdTime = true;
                        stillForthTime = false;
                        return false;
                    }
                }else{
                    stillSecondTime = true;
                    stillThirdTime = false;
                    stillForthTime = false;
                    return false;
                }
            }
            else{
                stillFirstTime = true;
                stillSecondTime = false;
                stillThirdTime = false;
                stillForthTime = false;
                return false;
            }
        }else{
            //reset flag
            stillFirstTime = false;
            stillSecondTime = false;
            stillThirdTime = false;
            stillForthTime = false;
            return false;
        }
    }

	/*public boolean isMoving(float[] posData){
		if(isStill(posData))
			return false;
		else
			return true;
	}*/

}
