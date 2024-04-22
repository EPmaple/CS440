package src.labs.infexf.agents;

// SYSTEM IMPORTS
import edu.bu.labs.infexf.agents.SpecOpsAgent;
import edu.bu.labs.infexf.distance.DistanceMetric;
import edu.bu.labs.infexf.graph.Vertex;
import edu.bu.labs.infexf.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.Set;
import java.util.Stack;
import java.util.Iterator;



// JAVA PROJECT IMPORTS


public class InfilExfilAgent
    extends SpecOpsAgent
{

    public InfilExfilAgent(int playerNum)
    {
        super(playerNum);
    }

    private float smallestDistToEnemy(StateView state, Vertex vertex) {
        float smallestDist = 99f;
        Set<Integer> enemyIds = getOtherEnemyUnitIDs();

        for (Integer enemyId : enemyIds) {
            UnitView enemyUnit = state.getUnit(enemyId);
            if (enemyUnit == null) {
                break;
            }

            Vertex enemyVertex = new Vertex(enemyUnit.getXPosition(), enemyUnit.getYPosition());
            float chebyshevDist = DistanceMetric.chebyshevDistance(enemyVertex, vertex);

            if (chebyshevDist < smallestDist) {
                smallestDist = chebyshevDist;
            }
        }

        return smallestDist;
    }

    // if two of the  
    // private boolean isCornered(StateView state, Vertex vertex) {
    //     // for the four cardinal distances, check if two of them are null
    //     state.inBounds(vertex.getXCoordinate(), vertex.getYCoordinate())
    // }

    private float getDangerValue(StateView state, Vertex vertex) {
        float edgeWeight = 1000f;
        float totalDangerValue = 0f;
        Set<Integer> enemyIds = getOtherEnemyUnitIDs();
        UnitView myUnit = state.getUnit(getMyUnitID());
        Vertex myUnitVertex = new Vertex(myUnit.getXPosition(), myUnit.getYPosition());

        for (Integer enemyId : enemyIds) {
            // float thisEnemyDangerValue = 0f;

            UnitView enemyUnit = state.getUnit(enemyId);
            if (enemyUnit == null) {
                break;
            }

            Vertex enemyVertex = new Vertex(enemyUnit.getXPosition(), enemyUnit.getYPosition());
            float enemyToVertexDist = DistanceMetric.chebyshevDistance(enemyVertex, vertex);
            float enemyToMyUnitDist = DistanceMetric.chebyshevDistance(enemyVertex, myUnitVertex);


            float dangerValueToVertex =  edgeWeight/ (float) Math.pow(enemyToVertexDist,2);
            float dangerValueToMe = edgeWeight/ (float) Math.pow(enemyToMyUnitDist,3);
            totalDangerValue += dangerValueToMe + dangerValueToVertex;

            // if (enemyToVertexDist <= 2) { //  this means we will be within the attack radius of an enemy unit, means it will get us killed； 5
            //     dangerValue = (float) Math.pow(edgeWeight/enemyToMyUnitDist, 3);
            //     totalDangerValue += dangerValue;
            // } else if (enemyToVertexDist == 3) { // 3
            //     dangerValue = (float) Math.pow(edgeWeight/enemyToMyUnitDist, 2.5);
            //     totalDangerValue += dangerValue;
            // } else if (enemyToVertexDist == 4) { // 2
            //     dangerValue = (float) Math.pow(edgeWeight/enemyToMyUnitDist, 2);
            //     totalDangerValue += dangerValue;
            // } else if (enemyToVertexDist == 5) { // 1
            //     dangerValue = (float) Math.pow(edgeWeight/enemyToMyUnitDist, 1.5);
            //     totalDangerValue += dangerValue;
            // } else if (enemyToVertexDist == 6) {
            //     dangerValue = (float) Math.pow(edgeWeight/enemyToMyUnitDist, 1);
            //     totalDangerValue += dangerValue;
            // } else { // very safe; 1/enemyToVertexDist 
            //     dangerValue = (1 + 1/enemyToMyUnitDist);
            //     totalDangerValue += dangerValue;
            // }
            // dangerValue += 7/chebyshevDist;
            // System.out.println("Enemy vertex is at: " + enemyVertex + " and is: " + enemyToVertexDist + " away from our path vertex at: " + vertex + "; the enemy vertex is: " + enemyToMyUnitDist + " away from our unit at: " + myUnitVertex + ". Total danger value is: " + totalDangerValue + ", with danger value to vertex being: " + dangerValueToVertex + ", and danger value to me being: " + dangerValueToMe);
            // System.out.println("enemy vertex at: " + enemyVertex + ", is : " + enemyToVertexDist + "away from us. "+ "We are at" ", enemyToMyUnitDist: " + enemyToMyUnitDist + ", dangerValue: " + dangerValue);
            // dangerValue += thisEnemyDangerValue;
        }

        return totalDangerValue;
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
    public float getEdgeWeight(Vertex src,
                               Vertex dst,
                               StateView state)
    {
        float dangerValue = getDangerValue(state, dst);

        // System.out.println("      edgeWeight from " + src + " to " + dst + ": " + edgeWeight);
        return dangerValue;
    }

    // True if enemy unit is in our path
    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        // Stack<Vertex> currentPlan = this.getCurrentPlan();
        // // make a copy of the currentPlan stack
        // Stack<Vertex> tempStack = new Stack<>();
        // tempStack.addAll(currentPlan); // elements in both stacks are the same and have the same order


        // while (!tempStack.isEmpty()) {
        //     Vertex vertex = tempStack.pop();
        //     int vertexX = vertex.getXCoordinate();
        //     int vertexY = vertex.getYCoordinate();

        //     // System.out.println("printing vertex to see if it is valid: " + vertex);
        //     if ((state.isUnitAt(vertexX, vertexY) && !isTownHall(state, vertex)) || smallestDistToEnemy(state, vertex) <= 4) {
        //         System.out.println("Changing the plan as it leads into the attack radius of an enemy unit.");
        //         return true;
        //     }
        // }

        // return false;

        int numVerticesLookedAt = 0;
        Iterator<Vertex> it = this.getCurrentPlan().iterator();

        // && numVerticesLookedAt < 15
        while(it.hasNext() ) {
            Vertex vertex = it.next();
            numVerticesLookedAt += 1;
            int vertexX = vertex.getXCoordinate();
            int vertexY = vertex.getYCoordinate();

            // && !isTownHall(state, vertex)
            if ((state.isUnitAt(vertexX, vertexY) ) || smallestDistToEnemy(state, vertex) <= 4) {
                System.out.println("Changing the plan as it leads into the attack radius of an enemy unit.");
                return true;
            }
        }
        return false;
    }

}
