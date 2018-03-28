// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      Abdullah Younis
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================

import java.util.*;

public class MyAI extends Agent
{
    final int maxDimension = 7;

    // Tile Structure
    private class Tile
    {
        int visitedNum = 0;
        boolean breeze = false; //used to infer pit
        boolean stench = false; //used to infer wumpus
        double pitProb = 0.2;
        double wumpusProb = 1.0/((double)maxDimension*(double)maxDimension);
        boolean queue = false; //has been in DFS queue?
    }

    //each option represents an adjacent square
    private class Options
    {
        int xOff = 0;
        int yOff = 0;
        boolean valid = false;
    }

    //agent's Location
    private class Node{

        int x;
        int y;
        int parentX;
        int parentY;

        Node(int x, int y){

            this.x = x;
            this.y = y;
            this.parentX = x;
            this.parentY = y;
        }
    }

    Node initialState;
    Node nextGoal;
    Node wumpus;
    Stack<Node> DFSqueue;
    Stack<Node> pathToOrigin;

    private enum AgentDirection {
        RIGHT, DOWN, LEFT, UP;
    }

    //Need to create a Linked List to save subsequent Actions
    LinkedList<Agent.Action> actionSequence; //FIFO

    // Agent Variables => record what has happened in the world
    private boolean	goldLooted;		// True if gold was successfuly looted
    private boolean goldImpossible; // gold is in a pit or surrounded by pits
    private boolean	hasArrow;		// True if the agent can shoot
    private boolean	wumpusAlive;	// Wumpus alive flag
    private boolean wumpusCertain; // know where is wumpus
    private boolean killWumpus;    // only when it is very likely that wumpus holds the gold
    private AgentDirection		agentDir;		// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
    private boolean moving;         // agent is moving to the next goal
    private int		agentX;			// The column where the agent is located ( x-coord = col-coord )
    private int		agentY;			// The row where the agent is located ( y-coord = row-coord )
    private AgentDirection     lastAgentDir;
    private int		lastAgentX;			// The column where the agent was located ( x-coord = col-coord )
    private int		lastAgentY;			// The row where the agent was located ( y-coord = row-coord )
    private Agent.Action	lastAction;	// The last action the agent made
    private Tile[][]	board;			// The game board, max = 7*7
    private Options[] options;          // each option represents an adjacent square
    private int     visitedTiles;       // number of tiles which have been visited
    private int actionCount;
    //private boolean debug;
    private boolean originalPath;

    //because probability(pit) = 0.2, if we have visited 80% of tiles but still do not find gold
    //it means that gold is off a pit => give up! (but the rule proves to be useless)
    private int visitedTilesLimit(){

        return (int)((colDimensionLogic*rowDimensionLogic - 1)*0.8);
    }

    //assume wumpus and pit are independent, so P(safe) = P(no wumpus)*P(no pit)
    private double safeProb(int x, int y){

        if(x >= colDimensionLogic || y >= rowDimensionLogic) return 0.0;
        return (1 - board[x][y].wumpusProb)*(1 - board[x][y].pitProb);
    }

    private void assignPitProb(int x, int y, double p){

        if(board[x][y].pitProb != 0){ //if it is zero, means we are sure there is no pit, so don't need to change

            board[x][y].pitProb = p;
        }
    }

    private void assignWumpusProb(int x, int y, double p){

        if(board[x][y].wumpusProb != 0){ //if it is zero, means we are sure there is no wumpus, so don't need to change

            board[x][y].wumpusProb = p;
        }
    }

    // Board Variables
    private int			colDimensionLogic;	// The number of columns the game board has, inferred by agent
    private int			rowDimensionLogic;	// The number of rows the game board has, inferred by agent

    public MyAI ( )
    {
        // ======================================================================
        // YOUR CODE BEGINS
        // ======================================================================

        // Agent Initialization
        goldLooted   = false;
        goldImpossible = false;
        hasArrow     = true;
        wumpusAlive = true;
        wumpusCertain = false;
        //our initial goal is finding the gold, not killing wumpus
        //if we can grab gold while keeping wumpus alive, it would be awesome!
        killWumpus   = false;
        agentDir     = AgentDirection.RIGHT; // The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
        moving       = false;
        agentX       = 0;
        agentY       = 0;
        lastAgentDir = AgentDirection.RIGHT;
        lastAgentX  = -1;
        lastAgentY  = -1;
        lastAction   = Action.CLIMB;
        board = new Tile[maxDimension][maxDimension];			// The game board, max = 7*7
        for ( int r = 0; r < maxDimension; ++r )
            for ( int c = 0; c < maxDimension; ++c )
                board[c][r] = new Tile();

        // Initialize options
        options = new Options[4]; // Index represents relative direction, e.g. options[3] means upper square
        for ( int i = 0; i < 4; i++)
            options[i] = new Options();
        options[0].xOff = 1;
        options[1].yOff = -1;
        options[2].xOff = -1;
        options[3].yOff = 1;

        colDimensionLogic = maxDimension;
        rowDimensionLogic = maxDimension;

        actionSequence = new LinkedList<Agent.Action>();
        visitedTiles = 0;
        actionCount = 0;

        initialState = new Node(0,0);
        nextGoal = new Node(0,0);
        wumpus = new Node(colDimensionLogic - 1, rowDimensionLogic - 1);
        DFSqueue = new Stack<Node>();
        pathToOrigin = new Stack<Node>();

        //debug = false;
        originalPath = false;
        // ======================================================================
        // YOUR CODE ENDS
        // ======================================================================
    }

    @Override
    public Action getAction
            (
                    boolean stench,
                    boolean breeze,
                    boolean glitter,
                    boolean bump,
                    boolean scream
            )
    {
        // ======================================================================
        // YOUR CODE BEGINS
        // ======================================================================
        //

        // Note:
        // 1. ONLY Forward moves the agent => change agentX, agentY, lastAgentX, lastAgentY manually before Forward
        // because agentX, agentY is NOT part of input
        // 2. Back up LastAction before any action
        // 3. If # of actions >=2 (e.g. TURN_LEFT -> FORWARD), need to save Actions in a Linked List
        // and execute the Linked List until next = NULL

        //STEP1: finish the previous action sequence

        actionCount++;

        if (!actionSequence.isEmpty()) {

            lastAction = actionSequence.poll();
            return lastAction;
        }

        //STEP2: update internal model based on new percepts: bump, scream, breeze & stench, glitter

        if(bump && lastAction == Action.FORWARD){

            switch(agentDir){

                case RIGHT:
                    agentX = agentX - 1; //need to revise back
                    colDimensionLogic = agentX + 1;
                    break;

                case DOWN:
                    //System.out.println("Bug: Should not bump into the bottom wall. Please check the logic.");
                    break;

                case LEFT:
                    //System.out.println("Bug: Should not bump into the left wall. Please check the logic.");
                    break;

                case UP:
                    agentY = agentY - 1; //need to revise back
                    rowDimensionLogic = agentY + 1;
                    break;
            }

            //because the current goal is invalid (over the boundary), need to re-set goal
            setNextGoal();
            moving = true;
            return moveToGoal();
        }

        if(lastAction == Action.SHOOT){

            if(scream){

                wumpusAlive = false;
                setWumpusProb(agentX, agentY); //when wumpus dies, all squares have Prob(wumpus) = 0
            }
            else{ //at least learned that no wumpus forward => set forward squares Prob(wumpus) = 0

                wumpusAlive = true;

                switch(agentDir){

                    case RIGHT:
                        for(int i = agentX; i < colDimensionLogic; i++)
                            board[i][agentY].wumpusProb = 0;
                        break;

                    case LEFT:
                        for(int i = agentX; i >=0; i--)
                            board[i][agentY].wumpusProb = 0;
                        break;

                    case UP:
                        for(int i = agentY; i < rowDimensionLogic; i++)
                            board[agentX][i].wumpusProb = 0;
                        break;

                    case DOWN:
                        for(int i = agentY; i >= 0; i--)
                            board[agentX][i].wumpusProb = 0;
                        break;
                }

                updateWumpusProb(agentX, agentY); //try to infer the location of wumpus
            }
        }

        if(lastAction == Action.CLIMB || (lastAction == Action.FORWARD && !bump)){

            //keep track of the original path in case the agent needs to follow it to go back
            if(!originalPath){

                if(!pathToOrigin.isEmpty()){

                    Node node_1 = pathToOrigin.peek();

                    if(agentX == node_1.parentX && agentY == node_1.parentY){

                        pathToOrigin.pop();
                    }
                    else{

                        Node node = new Node(agentX, agentY);
                        node.parentX = lastAgentX;
                        node.parentY = lastAgentY;
                        pathToOrigin.push(node);
                    }
                }
                else{

                    Node node = new Node(agentX, agentY);
                    node.parentX = lastAgentX;
                    node.parentY = lastAgentY;
                    pathToOrigin.push(node);
                }
            }

            //if not bump => store new information
            if(board[agentX][agentY].visitedNum == 0){

                visitedTiles++;
            }

            //The following rule proves to be useless
            //if(visitedTiles >= visitedTilesLimit()){

            //goldImpossible = true; //give up
            //}

            board[agentX][agentY].visitedNum++;

            //if this is the first time to visit here...
            if(board[agentX][agentY].visitedNum == 1){

                //Step 1: add new percepts
                board[agentX][agentY].breeze = breeze; //used to infer pit
                board[agentX][agentY].stench = stench; //used to infer wumpus

                //Step 2: add new inferences

                //agent still alive means there is neither pit nor wumpus
                board[agentX][agentY].pitProb = 0;
                board[agentX][agentY].wumpusProb = 0;

                generateOptions(agentX, agentY);

                if(!breeze){ //means no pit around

                    for(int i = 0; i < 4; i++){

                        if(options[i].valid){

                            int x_1, y_1;
                            x_1 = agentX + options[i].xOff;
                            y_1 = agentY + options[i].yOff;

                            board[x_1][y_1].pitProb = 0;
                        }
                    }
                }
                else{ //means pit may be around

                    for(int i = 0; i < 4; i++){

                        if(options[i].valid){

                            int x_1, y_1;
                            x_1 = agentX + options[i].xOff;
                            y_1 = agentY + options[i].yOff;

                            assignPitProb(x_1, y_1, 1.0);
                        }
                    }
                }

                //if wumpus alive and uncertain => need to infer wumpus and stench matters
                if(wumpusAlive && !wumpusCertain){

                    if(!stench){ //means no wumpus around

                        for(int i = 0; i < 4; i++){

                            if(options[i].valid){

                                int x_1, y_1;
                                x_1 = agentX + options[i].xOff;
                                y_1 = agentY + options[i].yOff;

                                board[x_1][y_1].wumpusProb = 0;
                            }
                        }
                    }
                } //if(wumpusAlive && !wumpusCertain)
            } //if visitedNum == 1
        }

        //after receiving new information, try to infer the location of wumpus
        if(stench && wumpusAlive && !wumpusCertain) updateWumpusProb(agentX, agentY);

        //expand the current node to generate children and put them into the stack
        expandNode(agentX, agentY);

        //if find gold => GRAB it immediately!
        if(!goldLooted && glitter){

            //stop DFS search
            moving = false;
            goldLooted   = true;
            goldImpossible = false;

            lastAction   = Action.GRAB;
            return Action.GRAB;
        }

        if(agentX == 0 && agentY == 0){

            //immediately climb out if the initial node has breeze because Prob(pit forward) = 50%!
            if(breeze || goldLooted || goldImpossible) {

                lastAction = Action.CLIMB;
                return Action.CLIMB; //immediately climb out
            }
        }

        //if uncertain of wumpus location, it's worth shooting to test where it is
        if(stench && wumpusAlive && !wumpusCertain && hasArrow && validShoot(agentDir.ordinal())) {

            hasArrow = false;
            lastAction = Action.SHOOT;
            return Action.SHOOT;
        }

        if(stench && wumpusAlive && killWumpus && hasArrow){

            generateOptions(agentX, agentY);

            int i;

            for(i = 0; i < 4; i++){

                if(options[i].valid){

                    int x_1, y_1;
                    x_1 = agentX + options[i].xOff;
                    y_1 = agentY + options[i].yOff;

                    //got the wumpus
                    if(x_1 == wumpus.x && y_1 == wumpus.y){

                        moving = false;
                        break;
                    }
                }
            }

            if(i < 4){

                //kill the wumpus
                return startKillWumpus(agentDir, i);
            }
        }

        //STEP3: decide what to do next

        //give up DFS search if stuck in the same place too long
        //setting the stop-loss threshold to be 6 times number of squares proves to be the optimal
        if(!goldLooted && !goldImpossible && actionCount > 2*3*colDimensionLogic*rowDimensionLogic){

            moving = false;
            goldImpossible = true;
        }

        if(moving){

            if(agentX == nextGoal.x && agentY == nextGoal.y){ //have reached the goal

                moving = false;
            }
            else{

                return moveToGoal();
            }
        }

        //retreat to the origin if find gold or give up
        if(goldLooted || goldImpossible){

            nextGoal = initialState;
        }
        else{

            //set the next goal
            setNextGoal();
        }

        moving = true;
        return moveToGoal();
        // ======================================================================
        // YOUR CODE ENDS
        // ======================================================================
    }

    // ======================================================================
    // YOUR CODE BEGINS
    // ======================================================================

    //update probability based on new percepts
    private void updateWumpusProb(int x, int y){

        generateOptions(x, y);

        int w_count = 0;

        for(int i = 0; i < 4; i++){

            if(options[i].valid){

                int x_1, y_1;

                x_1 = x + options[i].xOff;
                y_1 = y + options[i].yOff;

                if(board[x_1][y_1].wumpusProb != 0){

                    assignWumpusProb(x_1, y_1, 1.0);
                    wumpus.x = x_1;
                    wumpus.y = y_1;
                    w_count++;
                }
            }
        }

        if(w_count == 1){ //only one square is uncertain => that's it!

            setWumpusProb(wumpus.x, wumpus.y);
        }
    }

    //update wumpus probability when wumpus location is determined
    private void setWumpusProb(int x, int y){

        wumpusCertain = true;

        //because there is only one wumpus
        for(int i = 0; i < colDimensionLogic; i++){

            for(int j = 0; j < rowDimensionLogic; j++){

                board[i][j].wumpusProb = 0;
            }
        }

        if(wumpusAlive){

            board[x][y].wumpusProb = 1;
        }
    }

    //classification of agent's location
    private enum Location
    {
        LEFT_UP,
        UP,
        RIGHT_UP,
        LEFT,
        MIDDLE,
        RIGHT,
        LEFT_DOWN,
        DOWN,
        RIGHT_DOWN
    }

    //classify agent's location
    private Location locationCase(int x,int y){

        if(x == 0){

            if(y == 0) return Location.LEFT_DOWN;
            if(y == rowDimensionLogic - 1) return Location.LEFT_UP;
            return Location.LEFT;
        }

        if(x == colDimensionLogic - 1){

            if(y == 0) return Location.RIGHT_DOWN;
            if(y == rowDimensionLogic - 1) return Location.RIGHT_UP;
            return Location.RIGHT;
        }

        if(y == 0) return Location.DOWN;
        if(y == rowDimensionLogic - 1) return Location.UP;
        return Location.MIDDLE;
    }

    //generate Options based on Agent Location
    private void generateOptions(int x,int y){

        for(int i = 0; i < 4; i++) options[i].valid = false;

        Location location = locationCase(x,y);

        switch(location){

            case LEFT_DOWN:
                options[0].valid = true;
                options[3].valid = true;
                break;

            case LEFT:
                options[0].valid = true;
                options[1].valid = true;
                options[3].valid = true;
                break;

            case LEFT_UP:
                options[0].valid = true;
                options[1].valid = true;
                break;

            case DOWN:
                options[0].valid = true;
                options[2].valid = true;
                options[3].valid = true;
                break;

            case MIDDLE:
                options[0].valid = true;
                options[1].valid = true;
                options[2].valid = true;
                options[3].valid = true;
                break;

            case UP:
                options[0].valid = true;
                options[1].valid = true;
                options[2].valid = true;
                break;

            case RIGHT_DOWN:
                options[2].valid = true;
                options[3].valid = true;
                break;

            case RIGHT:
                options[1].valid = true;
                options[2].valid = true;
                options[3].valid = true;
                break;

            case RIGHT_UP:
                options[1].valid = true;
                options[2].valid = true;
                break;
        }
    }

    //generate children and put them into the stack
    private void expandNode(int x, int y){

        generateOptions(x,y);

        //add back children first so that they will be expanded last
        int backChild;
        backChild = agentDir.ordinal() + 2;
        if(backChild >= 4) backChild = backChild - 4;

        expandNode_1(x, y, backChild);

        for(int i = 0; i < 4; i++){

            if(i != agentDir.ordinal() && i != backChild){

                expandNode_1(x, y, i);
            }
        }

        //add front children last so that they will be expanded first
        expandNode_1(x, y, agentDir.ordinal());
    }

    private void expandNode_1(int x, int y, int i){

        if(options[i].valid){

            int x_1, y_1;

            x_1 = x + options[i].xOff;
            y_1 = y + options[i].yOff;

            //only add 100% safe and unexplored nodes; do not add the node if it has been in the queue
            if(safeProb(x_1, y_1) == 1 && board[x_1][y_1].visitedNum == 0 && !board[x_1][y_1].queue){

                Node node = new Node(x_1, y_1);
                node.parentX = x;
                node.parentY = y;

                if(DFSqueue.search(node) == -1){ //not yet been added

                    DFSqueue.push(node);
                    board[x_1][y_1].queue = true;
                }
            }
        }
    }

    //verify that the shooting is worthwhile
    //if wumpus is in a pit, then it's a waste to shoot wumpus (even if wumpus dies, we cannot enter the square)
    private boolean validShoot(int dir){

        boolean valid = false;

        switch(dir){

            case 0:
                if(colDimensionLogic - 1 > agentX){

                    if(board[agentX+1][agentY].pitProb == 0) valid = true;
                }
                break;

            case 1:
                if(0 < agentY){

                    if(board[agentX][agentY-1].pitProb == 0) valid = true;
                }
                break;

            case 2:
                if(0 < agentX){

                    if(board[agentX-1][agentY].pitProb == 0) valid = true;
                }
                break;

            case 3:
                if(rowDimensionLogic - 1 > agentY){

                    if(board[agentX][agentY+1].pitProb == 0) valid = true;
                }
                break;
        }

        return valid;
    }

    //set the top of the stack to be the next goal
    private void setNextGoal(){

        do{
            //if the queue becomes empty, it means that there is no 100% safe and unexplored node
            //it is very likely that gold is in a pit or surrounded by pits => give up!
            if(DFSqueue.empty()){

                //maybe wumpus holds the gold => kill wumpus!
                if(!killWumpus && wumpusAlive && wumpusCertain && hasArrow && board[wumpus.x][wumpus.y].pitProb == 0){

                    killWumpus = true;
                    nextGoal = wumpus;
                }
                else{

                    goldImpossible = true;
                    nextGoal = initialState;
                }
            }
            else{

                nextGoal = DFSqueue.pop();
            }

        }while(!(nextGoal.x < colDimensionLogic && nextGoal.y < rowDimensionLogic));
        //It is possible that when adding the node to the queue, we still do not know the boundary
        //so need to check if it is within the boundary before expanding it
    }

    //decide how to move to the next goal
    //randomize methods to avoid sticking around
    private Action moveToGoal(){

        generateOptions(agentX, agentY);

        if(originalPath){

            return followOriginalPath();
        }

        int validCount = 0;
        LinkedList<Integer> moveMethods = new LinkedList<Integer>();
        LinkedList<Integer> moveMethods_1 = new LinkedList<Integer>();

        for(int i = 0; i < 4; i++){

            if(options[i].valid) {

                validCount++;
                moveMethods.addLast(i);
                moveMethods_1.addLast(i);
            }
        }

        double randomSelector;
        int selector, method, counter;

        counter = validCount;

        //first, select the possibly shortest path to goal
        while(!moveMethods.isEmpty() && counter!=0){

            randomSelector = Math.random(); //between 0 and 1
            selector = (int)(randomSelector/(1.0/counter));

            method = moveMethods.get(selector);
            moveMethods.remove(selector);
            counter--;

            switch(method){

                case 0:
                    if(nextGoal.x > agentX && safeProb(agentX+1, agentY) == 1)
                        return addActionSequence(agentDir, AgentDirection.RIGHT);
                    break;

                case 1:
                    if(nextGoal.y < agentY && safeProb(agentX, agentY-1) == 1)
                        return addActionSequence(agentDir, AgentDirection.DOWN);
                    break;

                case 2:
                    if(nextGoal.x < agentX && safeProb(agentX-1, agentY) == 1)
                        return addActionSequence(agentDir, AgentDirection.LEFT);
                    break;

                case 3:
                    if(nextGoal.y > agentY && safeProb(agentX, agentY+1) == 1)
                        return addActionSequence(agentDir, AgentDirection.UP);
                    break;
            }
        }

        //if the path to the origin is roundabout, better follow the original path to avoid getting stuck
        if((goldLooted || goldImpossible) && !originalPath){

            originalPath = true;
            pathToOrigin.pop(); //pop the current location, move to the previous location
            return followOriginalPath();
        }

        //otherwise, randomly select a safe path to goal
        counter = validCount;

        while(!moveMethods_1.isEmpty() && counter!=0){

            randomSelector = Math.random(); //between 0 and 1
            selector = (int)(randomSelector/(1.0/counter));

            method = moveMethods_1.get(selector);
            moveMethods_1.remove(selector);
            counter--;

            switch(method){

                case 0:
                    if(safeProb(agentX+1, agentY) == 1)
                        return addActionSequence(agentDir, AgentDirection.RIGHT);
                    break;

                case 1:
                    if(safeProb(agentX, agentY-1) == 1)
                        return addActionSequence(agentDir, AgentDirection.DOWN);
                    break;

                case 2:
                    if(safeProb(agentX-1, agentY) == 1)
                        return addActionSequence(agentDir, AgentDirection.LEFT);
                    break;

                case 3:
                    if(safeProb(agentX, agentY+1) == 1)
                        return addActionSequence(agentDir, AgentDirection.UP);
                    break;
            }
        }

        //GRAB won't affect any future decision, so set it as default action
        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    //if the path to the origin is not straightforward, better follow the original path to avoid getting stuck
    private Action followOriginalPath(){

        Node node = pathToOrigin.pop();

        for(int i = 0; i < 4; i++){

            if(options[i].valid){

                int x_1, y_1;

                x_1 = agentX + options[i].xOff;
                y_1 = agentY + options[i].yOff;

                if(x_1 == node.x && y_1 == node.y){

                    switch(i) {

                        case 0:
                            return addActionSequence(agentDir, AgentDirection.RIGHT);

                        case 1:
                            return addActionSequence(agentDir, AgentDirection.DOWN);

                        case 2:
                            return addActionSequence(agentDir, AgentDirection.LEFT);

                        case 3:
                            return addActionSequence(agentDir, AgentDirection.UP);
                    }
                }
            }
        }

        //GRAB won't affect any future decision, so set it as default action
        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    // Based on current direction and the desired direction, add action sequence to the queue
    private Agent.Action addActionSequence(AgentDirection currentDirection, AgentDirection desiredDirection) {

        //back up old location
        lastAgentX = agentX;
        lastAgentY = agentY;
        lastAgentDir = agentDir;

        //update location
        agentX = agentX + options[desiredDirection.ordinal()].xOff;
        agentY = agentY + options[desiredDirection.ordinal()].yOff;
        agentDir = desiredDirection;

        lastAction = Action.FORWARD;

        switch(currentDirection) {

            case RIGHT:
                switch(desiredDirection) {
                    case RIGHT: // Facing R, go R: {F}
                        return Action.FORWARD;

                    case DOWN: // Facing R, go D: {R -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case LEFT: // Facing R, go L: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing R, go U: {L -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case DOWN:
                switch(desiredDirection) {
                    case RIGHT: // Facing D, go R: {L -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing D, go D: {F}
                        return Action.FORWARD;

                    case LEFT: // Facing D, go L: {R -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case UP: // Facing D, go U: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case LEFT:
                switch(desiredDirection) {
                    case RIGHT: // Facing L, go R: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing L, go D: {L -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing L, go L: {F}
                        return Action.FORWARD;

                    case UP: // Facing L, go U: {R -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;
                }

            case UP:
                switch(desiredDirection) {
                    case RIGHT: // Facing U, go R: {R -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case DOWN: // Facing U, go D: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing U, go L: {L -> F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing U, go U: {F}
                        return Action.FORWARD;
                }
        }

        //GRAB won't affect any future decision, so set it as default action
        agentX = lastAgentX;
        agentY = lastAgentY;
        agentDir = lastAgentDir;

        lastAction = Action.GRAB;
        return Action.GRAB;
    }

    private Agent.Action startKillWumpus(AgentDirection currentDirection, int desiredDirection) {

        //back up old location
        lastAgentX = agentX;
        lastAgentY = agentY;
        lastAgentDir = agentDir;

        //update location
        agentX = agentX + options[desiredDirection].xOff;
        agentY = agentY + options[desiredDirection].yOff;

        switch(desiredDirection) {

            case 0:
                agentDir = AgentDirection.RIGHT;
                break;

            case 1:
                agentDir = AgentDirection.DOWN;
                break;

            case 2:
                agentDir = AgentDirection.LEFT;
                break;

            case 3:
                agentDir = AgentDirection.UP;
                break;
        }

        hasArrow = false;
        wumpusAlive = false;
        setWumpusProb(agentX, agentY); //when wumpus dies, all squares have Prob(wumpus) = 0
        lastAction = Action.FORWARD;

        switch(currentDirection) {

            case RIGHT:
                switch(agentDir) {
                    case RIGHT: // Facing R, go R: {F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case DOWN: // Facing R, go D: {R -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case LEFT: // Facing R, go L: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing R, go U: {L -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case DOWN:
                switch(agentDir) {
                    case RIGHT: // Facing D, go R: {L -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing D, go D: {F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case LEFT: // Facing D, go L: {R -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case UP: // Facing D, go U: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;
                }

            case LEFT:
                switch(agentDir) {
                    case RIGHT: // Facing L, go R: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case DOWN: // Facing L, go D: {L -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing L, go L: {F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.SHOOT;

                    case UP: // Facing L, go U: {R -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;
                }

            case UP:
                switch(agentDir) {
                    case RIGHT: // Facing U, go R: {R -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_RIGHT;

                    case DOWN: // Facing U, go D: {L -> L -> F}
                        actionSequence.addLast(Action.TURN_LEFT);
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case LEFT: // Facing U, go L: {L -> F}
                        actionSequence.addLast(Action.SHOOT);
                        actionSequence.addLast(Action.FORWARD);
                        return Action.TURN_LEFT;

                    case UP: // Facing U, go U: {F}
                        actionSequence.addLast(Action.FORWARD);
                        return Action.SHOOT;
                }
        }

        agentX = lastAgentX;
        agentY = lastAgentY;
        agentDir = lastAgentDir;

        lastAction = Action.SHOOT;
        return Action.SHOOT;
    }

    //The following two methods are for debugging

    private void printDFSqueue(){

        System.out.print("DFS queue (size " + DFSqueue.size() + "): ");

        for(int i = DFSqueue.size()-1; i >= 0; i--){

            Node node = new Node(0, 0);
            node = DFSqueue.get(i);
            System.out.print("(" + node.x + ", " + node.y + ") ");
        }

        System.out.print("\n");
    }

    private void printTile(int x, int y) {

        System.out.print("(" + x + ", " + y + "):");
        System.out.print(" visitedNum " + board[x][y].visitedNum);
        System.out.print(", pitProb " + board[x][y].pitProb);
        System.out.print(", wumpusProb " + board[x][y].wumpusProb);
        System.out.print("\n");
    }
    // ======================================================================
    // YOUR CODE ENDS
    // ======================================================================
}