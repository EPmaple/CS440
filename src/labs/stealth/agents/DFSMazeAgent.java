package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;  

import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{
    float EDGE_WEIGHT = 1f;

    public DFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    private Vertex getNeighbor(Vertex vertex, Direction direction) {
        int newX = vertex.getXCoordinate();
        int newY = vertex.getYCoordinate();

        // Update coordinates based on the direction
        switch (direction) {
            case EAST:
                newX += 1;
                break;
            case NORTH:
                newY -= 1;
                break;
            case SOUTH:
                newY += 1;
                break;
            case WEST:
                newX -= 1;
                break;
            case NORTHEAST:
                newX += 1;
                newY -= 1;
                break;
            case NORTHWEST:
                newX -= 1;
                newY -= 1;
                break;
            case SOUTHEAST:
                newX += 1;
                newY += 1;
                break;
            case SOUTHWEST:
                newX -= 1;
                newY += 1;
                break;
            default:
                break;
        }
        return new Vertex(newX, newY);
    }

    private boolean isTownHall(StateView state, Vertex vertex) {
        UnitView unit = state.getUnit(state.unitAt(vertex.getXCoordinate(), vertex.getYCoordinate()));
        String unitTypeName = unit.getTemplateView().getName();
        if (unitTypeName.equals("TownHall")) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Stack<Path> stack = new Stack<>();
        Set<Vertex> visited = new HashSet<>();
        Path srcPath = new Path(src);
        stack.push(srcPath);

        while (!stack.isEmpty()) {
            Path currentPath = stack.pop();
            Vertex currenVertex = currentPath.getDestination();

            // System.out.println("Current path: " + currentPath);
            // System.out.println("Does visited contain neighborVertex: " + currenVertex + ", " + visited.contains(currenVertex));

            if (!visited.contains(currenVertex)) {
                if (currenVertex.equals(goal)) {
                    return currentPath.getParentPath();
                }
                visited.add(currenVertex);

                for (Direction direction : Direction.values()) {
                    Vertex neighborVertex = getNeighbor(currenVertex, direction);
                    int neighborX = neighborVertex.getXCoordinate();
                    int neighborY = neighborVertex.getYCoordinate();

                    if (state.inBounds(neighborX, neighborY) && !visited.contains(neighborVertex)) {
                        if (state.isUnitAt(neighborX, neighborY)) {
                            if (neighborVertex.equals(goal)) {
                                Path newPath = new Path(neighborVertex, EDGE_WEIGHT, currentPath);
                                stack.push(newPath);
                            }
                        } else {
                            if (!state.isResourceAt(neighborX, neighborY)) {
                                Path newPath = new Path(neighborVertex, EDGE_WEIGHT, currentPath);
                                stack.push(newPath);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
    // we are at (1,5), it has not been visited before, and is now marked as visited
    // we will check if its neighbor is not in the visited set

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        Stack<Vertex> currentPlan = this.getCurrentPlan();
        Stack<Vertex> tempStack = new Stack<>();
        tempStack.addAll(currentPlan); // elements in both stacks are the same and have the same order
    
        while (!tempStack.isEmpty()) {
            Vertex vertex = tempStack.pop();
            int vertexX = vertex.getXCoordinate();
            int vertexY = vertex.getYCoordinate();
    
            if (state.isUnitAt(vertexX, vertexY) && !isTownHall(state, vertex)) {
                System.out.println("path replaced");
                return true;
            }
        }
        
        return false;
    }

}
