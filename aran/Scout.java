package aran;

import aran.Constants.InfoEnum;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Scout extends RobotPlayer implements InfoContributor {
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
            	MapLocation rcLoc= rc.getLocation();
            	notMoveGeneric();
            	
            	//Danger, goal, enemy, friend, tree
            	if (!rc.hasMoved()){
                    Vector2D dangerVec= sensor.moveAwayFromBulletsVector(rc, rcLoc, Integer.MAX_VALUE, 10);
                    Vector2D friendVec= sensor.moveTowardsFriendVector(rc, rcLoc, Integer.MAX_VALUE, 2, 0.1f, Constants.ignoreArchonGardener);
                    Vector2D enemyVecStrong= sensor.moveTowardsEnemyVector(rc, rcLoc, Integer.MAX_VALUE, -3, Constants.ignoreNone);    
                    Vector2D enemyVecWeak= sensor.moveTowardsEnemyVector(rc, rcLoc, Integer.MAX_VALUE, 2, Constants.ignoreArchonGardener); 
                    Vector2D treeVec= sensor.moveTowardsTreeVectorDisregardTastiness(rc, rcLoc, 1, 1);
                    Vector2D goalVec= sensor.moveVecTowardsGoal(rc, rcLoc,1, 10);

                    Vector2D tryMoveVec= null;
                    if (dangerVec.length()> Constants.percentageUntilDangerOverride){
                    	System.out.println("Danger vector: " + dangerVec.length());
                    	tryMoveVec= new Vector2D(rcLoc).add(treeVec).add(dangerVec); 
                    }else{
                    	tryMoveVec= new Vector2D(rcLoc).add(goalVec).add(enemyVecStrong).add(enemyVecWeak).add(friendVec).add(treeVec).add(dangerVec);
                    }

                	if (rcLoc.directionTo(tryMoveVec.getMapLoc())!= null){
                		//util.tryMove(rcLoc.directionTo(tryMoveVec.getMapLoc()));
                	}
            		
            	}
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	@Override
	public void updateOwnInfo(RobotController rc) throws GameActionException {
		Info trackedInfo= InformationNetwork.unitInfoMap.get(rc.getType());
		int indexOffset= InformationNetwork.getFirstBehindRoundUpdateRobotIndex(rc); //starting index of an not updated robot type
		
		for (int i = 0; i < trackedInfo.getChannelSize(); i++){
			InfoEnum currentInfo = trackedInfo.getInfoEnum(i);
			
	        switch (currentInfo) {
		        case UPDATE_TIME:
		        	rc.broadcast(indexOffset+ i, rc.getRoundNum());
		            break;

		        case LOCATION:
		        	rc.broadcast(indexOffset+i, InformationNetwork.condenseMapLocation(rc.getLocation()));
		            break;
	        }
		}
	}
}
