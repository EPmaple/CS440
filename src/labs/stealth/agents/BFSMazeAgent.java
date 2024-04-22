package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;  

import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;           // will need for bfs
import java.util.Stack;


// JAVA PROJECT IMPORTS

public class BFSMazeAgent
    extends MazeAgent
{
    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    float EDGE_WEIGHT = 1f;

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

    private void exploreNeighborsBFS(Path currentPath, Queue<Path> queue, StateView state, Set<Vertex> visited, Vertex goal) {
        Vertex currentVertex = currentPath.getDestination();

        for (Direction direction : Direction.values()) {
            Vertex neighborVertex = getNeighbor(currentVertex, direction);
            int neighborX = neighborVertex.getXCoordinate();
            int neighborY = neighborVertex.getYCoordinate();

            if (state.inBounds(neighborX, neighborY) && visited.add(neighborVertex)) { // vertex is w/in bounds and has not been visited
                if (state.isUnitAt(neighborX, neighborY)) {  // It is a unit 
                    if (neighborVertex.equals(goal)) { // and it is the TH
                        Path newPath = new Path(neighborVertex, EDGE_WEIGHT, currentPath);
                        queue.offer(newPath);
                    }
                }
                else { // Else it is not a unit
                    if (!state.isResourceAt(neighborX, neighborY)) { // if it is also not a resource
                        Path newPath = new Path(neighborVertex, EDGE_WEIGHT, currentPath);
                        queue.offer(newPath);
                    }
                }
            }
        }
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Queue<Path> queue = new LinkedList<>();
        Set<Vertex> visited = new HashSet<>();
        Path srcPath = new Path(src); // src as the tail of Path
        visited.add(src); // have visited src
        queue.offer(srcPath);

        while (!queue.isEmpty()) {
            Path currentPath = queue.poll();
            Vertex currentVertex = currentPath.getDestination();

            if (currentVertex.equals(goal)) { // current vertex is goal, and the parentPath.getDestination() is a vertex next to the goal all the way to src of (0,0)
                return currentPath.getParentPath(); // this parentPath is "parentPath.getDestination() is a vertex next to the goal all the way to src of (0,0)"
            }
            exploreNeighborsBFS(currentPath, queue, state, visited, goal);
        }

        return null;
    }

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        Stack<Vertex> currentPlan = this.getCurrentPlan();
        // make a copy of the currentPlan stack
        Stack<Vertex> tempStack = new Stack<>();
        tempStack.addAll(currentPlan); // elements in both stacks are the same and have the same order

        while (!tempStack.isEmpty()) {
            Vertex vertex = tempStack.pop();
            int vertexX = vertex.getXCoordinate();
            int vertexY = vertex.getYCoordinate();

            if (state.isUnitAt(vertexX, vertexY) && !isTownHall(state, vertex)) {
                return true;
            }
        }

        return false;
    }

}
