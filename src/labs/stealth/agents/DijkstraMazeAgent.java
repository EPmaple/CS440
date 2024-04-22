package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;                           // Directions in Sepia


import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue; // heap in java
import java.util.Set;
import java.util.Stack;


// Worked with student Aaron Lu on Dijkstra's search


public class DijkstraMazeAgent
    extends MazeAgent
{
    double EPSILON = 1e-4;

    private boolean isTownHall(StateView state, Vertex vertex) {
        UnitView unit = state.getUnit(state.unitAt(vertex.getXCoordinate(), vertex.getYCoordinate()));
        String unitTypeName = unit.getTemplateView().getName();
        if (unitTypeName.equals("TownHall")) {
            return true;
        } else {
            return false;
        }
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

    private float getEdgeWeight(Direction direction) {
        float edgeWeight;
        switch (direction) {
            case EAST:
            case WEST:
                edgeWeight = 5f;
                break;
            case SOUTH:
                edgeWeight = 1f;
                break;
            case NORTH:
                edgeWeight = 10f;
                break;
            case NORTHEAST:
                edgeWeight = (float) Math.sqrt(Math.pow(getEdgeWeight(Direction.NORTH), 2) + Math.pow(getEdgeWeight(Direction.EAST), 2));
                break;
            case NORTHWEST:
                edgeWeight = (float) Math.sqrt(Math.pow(getEdgeWeight(Direction.NORTH), 2) + Math.pow(getEdgeWeight(Direction.WEST), 2));
                break;
            case SOUTHEAST:
                edgeWeight = (float) Math.sqrt(Math.pow(getEdgeWeight(Direction.SOUTH), 2) + Math.pow(getEdgeWeight(Direction.EAST), 2));
                break;
            case SOUTHWEST:
                edgeWeight = (float) Math.sqrt(Math.pow(getEdgeWeight(Direction.SOUTH), 2) + Math.pow(getEdgeWeight(Direction.WEST), 2));
                break;
            default:
                edgeWeight = 0f;
                break;
        }
        return edgeWeight;
    }

    private boolean isMyUnit(StateView state, Vertex vertex) {
        int vertexX = vertex.getXCoordinate();
        int vertexY = vertex.getYCoordinate();

        return state.unitAt(vertexX, vertexY) == this.getMyUnitID();
    }

    private boolean isVertexValid(StateView state, Vertex vertex) {
        int vertexX = vertex.getXCoordinate();
        int vertexY = vertex.getYCoordinate();

        if (state.inBounds(vertexX, vertexY)) {
            if (state.isUnitAt(vertexX, vertexY)) {
                if (isTownHall(state, vertex) || isMyUnit(state, vertex)) {
                    return true;
                }
            }
            else {
                if (!state.isResourceAt(vertexX, vertexY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public DijkstraMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Map<Vertex, Float> dist = new HashMap<>(); // to store vertex:dist pairs that represents the shortest path cost discovered so far for that vertex by Dijkstra's
        
        // Storing Path's instead of Vertices so we build the path as we go through the search, and at the end, straight up return this shortest path to the goal, allowing us not to write another function to reconstruct the path
        PriorityQueue<Path> pq = new PriorityQueue<>(Comparator.comparingDouble(Path::getTrueCost)); // using the getTrueCost function of the Path class, to get the total cost of the path for comparison in the heap

        dist.put(src, 0f);
        pq.add(new Path(src));

        while (!pq.isEmpty()) {
            Path currentPath = pq.poll();
            Vertex currentVertex = currentPath.getDestination();
            
            if (currentVertex.equals(goal)) {
                return currentPath.getParentPath();
            }

            for (Direction direction : Direction.values()) {
                Vertex neighborVertex = getNeighbor(currentVertex, direction);

                if (isVertexValid(state, neighborVertex)) {
                    float edgeCost = getEdgeWeight(direction);
                    // newDist consist of the values: dist of the current path, and the edgeCost to travel from the current destination to this neighborVertex => a valid path
                    float newDist = currentPath.getTrueCost() + edgeCost;

                    if (!dist.containsKey(neighborVertex) || newDist < dist.get(neighborVertex)) {
                        dist.put(neighborVertex, newDist);
                        Path newPath = new Path(neighborVertex, edgeCost, currentPath);
                        pq.add(newPath);
                    }
                }
            }
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