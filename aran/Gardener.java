package aran;
import aran.Constants.AddInfo;
import aran.Constants.InfoEnum;
import aran.Constants.SixAngle;
import battlecode.common.*;
public class Gardener extends RobotPlayer
{
	public static Team enemy;
	static int queue = 0;//first plants a tree, then a scout, then 3 soldiers, and repeats
    static int treesPlanted = 0;
    static double chance = 0.20;
    static Team myTeam;
    static int dirNum = 6; // number of directions this gardener can plant/build robots
    static int currentlyPlanted = 0;
    static int earlyGameIndex = 0;
    static int[] earlyGameQueue = {};
    
    static enum status {earlygame, looking, gardenCheck, midgame, ratiogame};
    //static String status = "looking";
    static status stat= status.looking;
    
    static int soldierCount= 0;
    static int tankCount= 0;
    static int scoutCount= 0;
    static int lumberjackCount= 0;
    static int gardenerCount = 0;
    static float myHealth = 0;
    static int lookingCountTotal = 0;
    static int lookingCountTotalLimit = 150;
    
	public static void run(RobotController rc) throws GameActionException {
		enemy = rc.getTeam().opponent();
        myTeam = rc.getTeam();
        
        int lookingCount = 0;
        int lookingCountLimit = 7;
        Direction lookingDir = Util.randomDirection();
        myHealth = rc.getHealth();
        incrementCountOnSpawn();
        earlyGameInit();
        while (true) {
            try {
            	//infoUpdate();
            	
            	//move away from other gardeners
            	if(stat== status.earlygame)//(status == "earlygame")
            	{
            		earlyGame();
            	}
            	if(stat== status.looking)
            	{
            		
            		RobotInfo[] robots = rc.senseNearbyRobots(-1, myTeam);
            		int gardenerCount = 0;
            		for(int i=0;i<robots.length;i++){
            			if(robots[i].type == RobotType.GARDENER || robots[i].type == RobotType.ARCHON)
            			{
            				gardenerCount+=1;
            				if(robots[i].type == RobotType.ARCHON)
            				{
            					Info unitCountInfo= InfoNet.addInfoMap.get(AddInfo.UNITCOUNT);
            					if(rc.readBroadcast(unitCountInfo.getStartIndex() + unitCountInfo.getIndex(InfoEnum.GARDENER_COUNT)) == 1)
            					{
            						gardenerCount -= 1;
            					}
            				}
            				break;
            			}
            		}
            		if(gardenerCount == 0 || lookingCountTotal > lookingCountTotalLimit)
            		{
            			stat= status.gardenCheck;
            		}
            		else
            		{
            			if(rc.getHealth() < myHealth)
            			{
            				myHealth = rc.getHealth();
            				Util.tryBuildRobot(RobotType.SOLDIER);
            			}
            			else
            			{
		            		boolean moved = Util.tryMove(lookingDir, 30.0f, 3);
		            		if(moved)
		            		{
		            			lookingCountTotal += 1;
		            			lookingCount += 1;
		            		}
		            		else
		            		{
		            			//cannot move, use perpendicular direction
		            			lookingDir = lookingDir.rotateLeftDegrees(90.0f);
		            			lookingCount = 0;
		            		}
		            		
		            		if(lookingCount > lookingCountLimit)
		            		{
		            			//find new direction
		            			lookingDir = Util.randomDirection();
		            			lookingCount = 0;
		            		}
            			}
            		}
            		
            	}
            	if(stat==status.gardenCheck)//status == "gardenCheck")
            	{
            		dirNum = 0;
            		for (SixAngle ra : Constants.SixAngle.values()) {
                        Direction d = new Direction(ra.getRadians());
                        if(rc.getTeamBullets() >= 50.0f){
                        	if (rc.canPlantTree(d)) {
                        		dirNum+=1;
                        	}
                        }
                        else
                        {
                        	if (rc.canMove(d)) {
                        		dirNum+=1;
                        	}
                        }
                    }
                    //status = "gardening";	
            		stat= status.midgame;
            	}
            	if(stat== status.midgame)//status == "gardening")
            	{
            		Util.waterLowestHealthTreeWithoutMoving(rc, myTeam);
            		//check for enemyRobots
            		if(!checkForEnemyRobots())
            		{
            			ratioGame();
            		}
            		if(rc.getRoundNum() %100 == 50)
            		{
            			//update plantable regions
            			int tdirNum = 0;
                		for (SixAngle ra : Constants.SixAngle.values()) {
                            Direction d = new Direction(ra.getRadians());
                            if(rc.getTeamBullets() >= 50.0f){
                            	if (rc.canPlantTree(d)) {
                            		tdirNum+=1;
                            	}
                            }
                            else
                            {
                            	if (rc.canMove(d)) {
                            		tdirNum+=1;
                            	}
                            }
                        }
                		dirNum = tdirNum + currentlyPlanted;
            		}
            	}
            	
            	decrementCountOnLowHealth(Constants.LOW_HEALTH_DECREMENT_VALUE);
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
	private static boolean checkForEnemyRobots() throws GameActionException
	{
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		if (robots.length > 0) {
            // And we have enough bullets, and haven't attacked yet this turn...
			
        	RobotInfo bestRobot = robots[0];
        	//broadcast enemy location
        	
    		int lowestRoundCount = rc.getRoundLimit() + 10;
    		int channelToUse = 119;
    		int currentRound = rc.getRoundNum();
    		for(int i=100;i<120;i++){
    			int readRound = rc.readBroadcast(i);
    			if(readRound == -1 || currentRound - readRound > Constants.MessageValidTime)
    			{
    				channelToUse = i;
    				break;
    			}
    			if(readRound < lowestRoundCount)
    			{
    				lowestRoundCount = readRound;
    				channelToUse = i;
    			}
    		}
    		rc.broadcast(channelToUse, currentRound);
    		rc.broadcast(channelToUse+20, (int)bestRobot.location.x);
    		rc.broadcast(channelToUse+40, (int)bestRobot.location.y);
    		return true;
    	}
		return false;
	}
	private static void ratioGame() throws GameActionException { 
		//try to spawn soldier if i am losing health
		if(rc.getHealth() < myHealth)
		{
			myHealth = rc.getHealth();
			Util.tryBuildRobot(RobotType.SOLDIER);
			return;
		}
		
		
		//spawn units based on the corresponding ratio
		//3 soldier to 1 lumberjack to 1 tank to 1 scout
		//Gardener first checks whether or not the unit count is accurate, if it is not then presume 0
		
		Info unitCountInfo= InfoNet.addInfoMap.get(AddInfo.UNITCOUNT);
		
		soldierCount= rc.readBroadcast(unitCountInfo.getStartIndex()+ unitCountInfo.getIndex(InfoEnum.SOLDIER_COUNT));
		tankCount= rc.readBroadcast(unitCountInfo.getStartIndex()+ unitCountInfo.getIndex(InfoEnum.TANK_COUNT));
		scoutCount= rc.readBroadcast(unitCountInfo.getStartIndex()+ unitCountInfo.getIndex(InfoEnum.SCOUT_COUNT));
		lumberjackCount= rc.readBroadcast(unitCountInfo.getStartIndex()+ unitCountInfo.getIndex(InfoEnum.LUMBERJACK_COUNT));
		gardenerCount = rc.readBroadcast(unitCountInfo.getStartIndex() + unitCountInfo.getIndex(InfoEnum.GARDENER_COUNT));
		
		int totalUnitCount = soldierCount + tankCount + scoutCount;
		float farmingBulletCount = (float)(gardenerCount * RobotType.GARDENER.bulletCost +rc.getTreeCount() *50); 
		float combatBulletCount = (float)(soldierCount * RobotType.SOLDIER.bulletCost + tankCount * RobotType.TANK.bulletCost
				+ scoutCount * RobotType.SCOUT.bulletCost + lumberjackCount * RobotType.LUMBERJACK.bulletCost);
		float farmingToCombatRatio = ((float)rc.readBroadcast(501)) / 10000.0f;
		float safeBulletBank =(float)( totalUnitCount * 5 + Constants.SAFEMINIMUMBANK);
		if(rc.getRoundNum() < 200){
			safeBulletBank = Constants.SAFEMINIMUMBANK;
		}
		
		
		if(farmingBulletCount < 0.01f){
			//build tree
			buildTree(safeBulletBank, totalUnitCount);
		}
		else if(combatBulletCount < 0.01f)
		{
			buildRobot(safeBulletBank, totalUnitCount);
		}
		else if(farmingBulletCount/combatBulletCount < farmingToCombatRatio && rc.getTreeCount() < 35)
		{
			//build tree
			if(totalUnitCount > 24){
				if(rc.getTeamBullets() - 150.0f>0.0f)
				{
					rc.donate(rc.getTeamBullets() - 150.0f);
				}
				broadcastPrint(rc,970,1,"trytoDonate");
				buildTree(99999.0f, totalUnitCount);
				
			}
			else
			{
				//build combat unit
				buildTree(safeBulletBank, totalUnitCount);
			}
			
		}
		else
		{
			if(totalUnitCount > 24){
				if(rc.getTeamBullets() - 150.0f>0.0f)
				{
					rc.donate(rc.getTeamBullets() - 150.0f);
				}
				broadcastPrint(rc,970,1,"trytoDonate");
				buildTree(99999.0f, totalUnitCount);
				
			}
			else
			{
				//build combat unit
				buildRobot(safeBulletBank, totalUnitCount);
			}
		}
		
		//Util.tryBuildRobot(rtToBuild);
	}
	
	public static void buildTree(float safeBulletBank, int totalUnitCount) throws GameActionException{
		if(currentlyPlanted < dirNum - 1)
		{
		
			boolean hasPlanted = plantCircleTrees();
			if(hasPlanted){
				currentlyPlanted+=1;
				//Clock.yield();
				return;
			}
			
		}
		
		if(rc.getTeamBullets() > safeBulletBank)
		{
			buildRobot(safeBulletBank, totalUnitCount);
		}
		
	}
	public static void buildRobot(float safeBulletBank, int totalUnitCount) throws GameActionException{
		if(rc.getTeamBullets() > safeBulletBank)
		{
			/*if(scoutCount < 2)
			{
				Util.tryBuildRobot(RobotType.SCOUT);
				return;
			}*/
			/*if(totalUnitCount > Constants.MINTANKUNITCOUNT && tankCount < 2)
			{
				Util.tryBuildRobot(RobotType.TANK);
				return;
			}*/
			float[] unitRatio = new float[4];
			float totalRatioN = 0.0f;
			for(int i=0;i<4;i++){
				unitRatio[i] = (float)rc.readBroadcast(503+i);
				totalRatioN += unitRatio[i];
			}
			for(int i=0;i<4;i++){
				unitRatio[i]/=totalRatioN;
			}
			float unitTotalCount = (float)(soldierCount + tankCount + scoutCount + lumberjackCount);
			//broadcastPrint(rc, 960,lumberjackCount,"lumberjackCount");
			if(lumberjackCount < 1.01f){
				Util.tryBuildRobot(RobotType.LUMBERJACK);
				return;
			}
			unitRatio[0] = unitRatio[0] - ((float)soldierCount)/unitTotalCount ;
			unitRatio[1] = unitRatio[1] - ((float)scoutCount)/unitTotalCount ;
			unitRatio[2] = unitRatio[2] - ((float)tankCount)/unitTotalCount ;
			unitRatio[3] = unitRatio[3] - ((float)lumberjackCount)/unitTotalCount ;
			
			float maxIndex = 0;
			float max = unitRatio[0];
			for(int i=1;i<4;i++){
				if(unitRatio[i] > max)
				{
					max = unitRatio[i];
					maxIndex = i;
				}
			}
			
			boolean flag = false;
			if(maxIndex==0){
				flag = Util.tryBuildRobot(RobotType.SOLDIER);
				//broadcastPrint(rc, 930, 0, "soldier");
			}
			if(maxIndex==1){
				flag = Util.tryBuildRobot(RobotType.SCOUT);
				//broadcastPrint(rc, 930, 0, "scout");
			}
			if(maxIndex==2){
				flag = Util.tryBuildRobot(RobotType.TANK);
				//broadcastPrint(rc, 930, 0, "tank");
			}
			if(maxIndex==3)
			{
				if(lumberjackCount > 2)
				{
					rc.broadcast(506, 1);
					return;
				}
				flag = Util.tryBuildRobot(RobotType.LUMBERJACK);
				//broadcastPrint(rc, 930, 0, "lumberjack");
			}
			/*if(flag){
				Clock.yield();
			}*/
		}
	}


	private static void earlyGame() throws GameActionException {
		if (rc.getRoundNum()< 500) {
			boolean flag = false;
			Direction dir = new Direction(0.0f);
			for (int i = 0; i < 12; i++) {
				if (i >= 0 && i < earlyGameQueue.length) { //added check because I found Array Index out of bound error
					if (earlyGameQueue[earlyGameIndex] == 0) {						
						flag= Util.tryBuildRobot(RobotType.SCOUT);
						if (flag)
							break;
						
					} else if (earlyGameQueue[earlyGameIndex] == 1) {
						if (rc.canPlantTree(dir)) {
							rc.plantTree(dir);
							flag = true;
							break;
						}
					} else if (earlyGameQueue[earlyGameIndex] == 2) {
						flag= Util.tryBuildRobot(RobotType.SCOUT);
						if (flag)
							break;
					}
					dir = dir.rotateLeftDegrees(30.0f);
				}
			}
			
			if (flag) {
				earlyGameIndex += 1;
				if (earlyGameIndex >= earlyGameQueue.length) {
					rc.broadcast(500, 1);
					// status = "gardenCheck";
					stat = status.looking;
				}
			}
		}else{
			stat= status.looking;
		}
	}
	private static void earlyGameInit() throws GameActionException{
		int isEarlyGame = rc.readBroadcast(500);
        
        if(isEarlyGame == 0){
        	//status = "earlygame";
        	stat=stat.earlygame;
        	int earlyGameType = rc.readBroadcast(507);
        	if(earlyGameType == 0)
        	{
        		earlyGameQueue = Constants.EARLYGAME_SCOUTFIRST_SPAWNORDER;
        	}
        	else if(earlyGameType == 1)
        	{
        		earlyGameQueue = Constants.EARLYGAME_TREEFIRST_SPAWNORDER;
        	}
        	else if(earlyGameType == 2)
        	{
        		earlyGameQueue = Constants.EARLYGAME_SOLDIERFIRST_SPAWNORDER;
        	}
        }
        else if(isEarlyGame == 1){
        	stat = stat.looking;
        }
	}
	
	
	private static boolean plantCircleTrees() throws GameActionException {
        for (SixAngle ra : Constants.SixAngle.values()) {
            Direction d = new Direction(ra.getRadians());
            if (rc.canPlantTree(d)) {
                rc.plantTree(d);
                return true;
            }
        }
        return false;
    }
}