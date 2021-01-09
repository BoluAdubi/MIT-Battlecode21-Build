package bot1;
import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();

        // ArrayList to keep track of robot IDs and flags
        ArrayList<RobotFlagInfo> RobotStorage = new ArrayList<RobotFlagInfo>();

        //Add EC to RobotStorage
        RobotFlagInfo EC = new RobotFlagInfo();
        EC.ID = rc.getID();
        EC.flag = rc.getFlag(EC.ID);
        RobotStorage.add(EC);

        // get IDs of robots on my team near EC and store them
        Team myTeam = rc.getTeam();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for(int i = 0; i < nearbyRobots.length; i++){
            boolean isInStorage = false;
            for(int j = 0; j < RobotStorage.size(); j++) {
                if(nearbyRobots[i].getID() == RobotStorage.get(j).ID){
                    isInStorage = true;
                    break;
                }
            }
            if(!isInStorage && (nearbyRobots[i].getTeam() == myTeam)) {
                // If ID is not in RobotStorage and robot is on my team, add robot to RobotStorage
                RobotFlagInfo newRobot = new RobotFlagInfo();
                newRobot.ID = nearbyRobots[i].getID();
                newRobot.flag = rc.getFlag(newRobot.ID);
                RobotStorage.add(newRobot);
            }
        }

        // print robotStorage
        for (int i = 0; i < RobotStorage.size(); i++) {
            System.out.println(RobotStorage.get(i).ID);
        }

        // flag communication
        for(int i = 0; i < RobotStorage.size(); i++){
            if(rc.canGetFlag(RobotStorage.get(i).ID)){
                int flag = rc.getFlag(RobotStorage.get(i).ID);
                if(flag != 0){
                    rc.setFlag(flag); // If robot flag has changed, set EC flag to robot flag
                }
            }else{
                RobotStorage.remove(i); // if cant get Robot flag, robot is probably dead or converted. Remove
            }
        }

        int influence = 100;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length == 1 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }

        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
        }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * made so Enlightenment Centers can store robot IDs and Flags
     **/
    static public class RobotFlagInfo{
        int ID, flag;
    }

    /**
     * Allows a bot to "send" its current location using a binary flag
     *
     * @throws GameActionException
     **/
    static void sendLocation() throws GameActionException {
        MapLocation location = rc.getLocation();
        int x = location.x, y = location.y;
        int encodedLocation = 128 * (x % 128) + (y % 128); // binary encoding
        if(rc.canSetFlag(encodedLocation)){
            rc.setFlag(encodedLocation);
        }
    }

    /**
     * Gets the exact game map location from a binary flag
     *
     * @param flag The flag with an encoded location
     * @return the MapLocation encoded in the flag
     * @throws GameActionException
     **/
    static MapLocation getLocationFromFlag(int flag) throws GameActionException {
        // binary decoding
        int y = flag % 128;
        int x = (flag / 128) % 128;

        MapLocation currentLocation = rc.getLocation();
        // calculations based on the fact that the game maps upper bound is 64x64
        int offsetx128 = currentLocation.x / 128;
        int offsety128 = currentLocation.y / 128;
        MapLocation actualLocation = new MapLocation(offsetx128 * 128 + x, offsety128 * 128 + y);

        /*
            calculation for actualLocation could be off by +128 or -128 for both coordinates
            So the next block of code checks if any of the translated locations are closer to the robot
            if it is, then it is the actual location
         */
        MapLocation alternative = actualLocation.translate(-128, 0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(128, 0);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, -128);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, 128);
        if(rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }

        return actualLocation;
    }
}
