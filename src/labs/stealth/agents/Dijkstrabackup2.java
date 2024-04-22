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


// JAVA PROJECT IMPORTS


public class DijkstraMazeAgent
    extends MazeAgent
{
    double epsilon = 1e-4;

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

    // when we retrace a path, for example, from the goal vertex to a neighboring vertex to the NORTHWEST of it, in the actual path for our unit to be traveled, it should be actually going from this neighboring vertex to the goal vertex in the SOUTHEAST direction
    private float getRetraceEdgeWeight(Direction direction) {
        float edgeWeight;
        switch (direction) {
            case EAST:
            case WEST:
                edgeWeight = getEdgeWeight(direction);
                break;
            case SOUTH:
                edgeWeight = getEdgeWeight(Direction.NORTH);
                break;
            case NORTH:
                edgeWeight = getEdgeWeight(Direction.SOUTH);
                break;
            case NORTHEAST:
                edgeWeight = getEdgeWeight(Direction.SOUTHWEST);
                break;
            case NORTHWEST:
                edgeWeight = getEdgeWeight(Direction.SOUTHEAST);
                break;
            case SOUTHEAST:
                edgeWeight = getEdgeWeight(Direction.NORTHWEST);
                break;
            case SOUTHWEST:
                edgeWeight = getEdgeWeight(Direction.NORTHEAST);
                break;
            default:
                edgeWeight = 0f;
                break;
        }
        return edgeWeight;
    }

    // if neighbor is NW of current, then it means to go from neighbor to current, we will have to go in the SE direction
    
    private Path reconstructPath(Vertex src, Vertex goal, Map<Vertex, Float> dist, StateView state) {
        Map<Vertex, Float> vertexToEdgeWeight = new HashMap<>();
        Path path = new Path(goal); 
        Vertex current = goal;

        while (!current.equals(src)) {
          for (Direction direction : Direction.values()) {
            Vertex neighbor = getNeighbor(current, direction);

            if (isVertexValid(state, neighbor) && dist.containsKey(neighbor)) {
                float retraceEdgeWeight = getRetraceEdgeWeight(direction);
                if (Math.abs(dist.get(neighbor) - (dist.get(current) - retraceEdgeWeight)) < epsilon) {
                    path = new Path(neighbor, retraceEdgeWeight, path);
                    vertexToEdgeWeight.put(neighbor, retraceEdgeWeight);
                    current = neighbor;
                    break;
                }
            }
          }
        }
        System.out.println(path);

        Path reversedPath = new Path(path.getDestination());
        Path tempPath = path.getParentPath();

        while (tempPath != null) {
            Vertex nextVertex = tempPath.getDestination();

            if (nextVertex.equals(goal)){
                break;
            }

            reversedPath = new Path(nextVertex, vertexToEdgeWeight.get(nextVertex), reversedPath);
            tempPath = tempPath.getParentPath();
        }
        // System.out.println("Reconstructed Path Reversed: " + reversedPath);
        return reversedPath;
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
        Map<Vertex, Float> dist = new HashMap<>();
        // Set<Vertex> visited = new HashSet<>();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        dist.put(src, 0f);
        pq.add(src);

        while (!pq.isEmpty()) {
            Vertex currentVertex = pq.poll();

            //visited.add(currentVertex);

            for (Direction direction : Direction.values()) {
              Vertex neighborVertex = getNeighbor(currentVertex, direction);
              int neighborX = neighborVertex.getXCoordinate();
              int neighborY = neighborVertex.getYCoordinate();

              if (isVertexValid(state, neighborVertex)) {
                float alt = dist.get(currentVertex) + getEdgeWeight(direction);
                if (!dist.containsKey(neighborVertex) || alt < dist.get(neighborVertex)) {
                    dist.put(neighborVertex, alt);
                    pq.add(neighborVertex);
                }
              }
          }
        }
        // No valid path found
        // System.out.println("No valid path found");
        Path reconstructedPath = reconstructPath(src, goal, dist, state);
        return reconstructedPath;

        return null;
    }

    // private boolean isMyUnit(StateView state, int x, int y) {
    //     return state.unitAt(x, y) == this.getMyUnitID();
    // }

    //  || isMyUnit(state, vertexX, vertexY)
    private boolean isVertexValid(StateView state, Vertex vertex) {
        int vertexX = vertex.getXCoordinate();
        int vertexY = vertex.getYCoordinate();

        if (state.inBounds(vertexX, vertexY)) {
          if (state.isUnitAt(vertexX, vertexY)) {
            if (isTownHall(state, vertex)) {
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