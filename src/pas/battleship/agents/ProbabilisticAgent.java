package src.pas.battleship.agents;


// SYSTEM IMPORTS


// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.ships.Ship;
import edu.bu.battleship.game.ships.Ship.ShipType;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;


public class ProbabilisticAgent
    extends Agent
{
    public ProbabilisticAgent(String name)
    {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    /*
     * Cache (for remembering stuff)
     */
    Coordinate lastGuessCoordinate = null;
    Coordinate originalHitCoordinate = null;
    Coordinate lastHitCoordinate = null;
    Map<Coordinate, Double> defaultProbabilities;
    int turnCounter = 0;
    int[] currentSquaresConsideration = new int[]{2, 3, 4, 5};

    Map<ShipType, Integer> lastShips;
    boolean lastCardinalMiss = false;

    /*
     * So we consider ships with different weights
     */
    int numOf2Squares = 0;
    int numOf3Squares = 0;
    int numOf4Squares = 0;
    int numOf5Squares = 0;

    private void initShipNums(GameView game) {
        Map<ShipType, Integer> ships = game.getEnemyShipTypeToNumRemaining();

        for (Map.Entry<ShipType, Integer> entry : ships.entrySet()) {
            ShipType shipType = entry.getKey();

            if (shipType.equals(ShipType.AIRCRAFT_CARRIER)) {
                numOf5Squares += entry.getValue();
            } else if (shipType.equals(ShipType.BATTLESHIP)) {
                numOf4Squares += entry.getValue();
            } else if (shipType.equals(ShipType.PATROL_BOAT)) {
                numOf2Squares += entry.getValue();
            } else if (shipType.equals(ShipType.DESTROYER) || shipType.equals(ShipType.SUBMARINE)) {
                numOf3Squares += entry.getValue();
            } else {
                throw new IllegalArgumentException("You have reached the " +
                "else statement of checkNumRemainingShips, which should not " +
                "be possible.");
            }
        }
    }
    /*
     * End of cache
     */

    /*
     * Keep a Set<Coordinate>, the removed coords
     * 
     * when all i-square ships have been SUNK (we can check by 
     * game.getEnemyShipTypeToNumRemaining(parameter)), we will have to
     * recalculate our defaultProbs excluding the i-square probs, and
     * ofc also remove from the new map the coords we have already shot
     * at
     */

    @Override
    public Coordinate makeMove(final GameView game)
    {
        lastShips = game.getEnemyShipTypeToNumRemaining();
        initShipNums(game);

        if (defaultProbabilities == null) {
            /*
            * In Java, if a reference variable has not been assigned any value, 
            * its default value is null
            */
            if (turnCounter == 0) {
                // It is the first turn for the agent
                initSquareCountsAndProbs(game);
            } else {
                throw new IllegalArgumentException("turnCounter=" + turnCounter +
                " is greater than 0 and defaultProbabilities has yet to be " + 
                "initialized. What happened?");
            }
        }

        Coordinate toBeShot = null;
        
        if (turnCounter == 0) {
            /*
             * Base case: yet to make a guess so lastGuessCoord would be null
             */
            toBeShot = shootBasedOnDefaultProbs(game);
            turnCounter += 1;
            return toBeShot;
        }

        // Else, process if the last shot was a miss, hit, or sunk
        Outcome[][] enemyBoard = game.getEnemyBoardView();
        Outcome lastGuessResult = enemyBoard[lastGuessCoordinate.getXCoordinate()][lastGuessCoordinate.getYCoordinate()];

        // Update turnCounter
        turnCounter += 1;

        /*
         * Version relying more on probabilities
         */

        switch (lastGuessResult) {
            case MISS:
                List<Coordinate> adjacents = getAdjacentSquares(lastGuessCoordinate, game);
                for (Coordinate adjacentCoord : adjacents) {
                    if (squareCounts.containsKey(adjacentCoord)) {
                        // Decrease the count to be less dominant
                        squareCounts.put(adjacentCoord, Math.max(0, squareCounts.get(adjacentCoord) - 15.0));
                    } 
                    getNewNormalizedProbs(game);
                }
                toBeShot = shootBasedOnDefaultProbs(game);
                return toBeShot;
            case HIT:
                /*
                 * Increase the prob of the adjacent squares of this HIT
                 * increase squareCounts by 5?
                 * defaultProbs for each square become 1?
                 */
                adjacents = getAdjacentSquares(lastGuessCoordinate, game);
                for (Coordinate adjacentCoord : adjacents) {
                    if (squareCounts.containsKey(adjacentCoord)) {
                        // Increase the count to be dominant
                        squareCounts.put(adjacentCoord, squareCounts.get(adjacentCoord) + 60.0);
                    } else {
                        // Increase the count to be dominant
                        squareCounts.put(adjacentCoord, 60.0);
                    }
                    getNewNormalizedProbs(game);
                }

                toBeShot = shootBasedOnDefaultProbs(game);
                return toBeShot;
            case SUNK:
                /*
                 * Then when we need to take out the consideration of 
                 * i-squares, we could again call checkNumRemainingShips
                 */

                checkNumRemainingShips(game);
                toBeShot = shootBasedOnDefaultProbs(game);
                return toBeShot;
            default:
                throw new IllegalArgumentException("Reached default of switch " + 
                "statement for lastGuessResult, lastGuessResult=" + lastGuessResult);
        }
    }


    private void checkNumRemainingShips(GameView game) {
        Map<ShipType, Integer> currShips = game.getEnemyShipTypeToNumRemaining();
        for (Map.Entry<ShipType, Integer> entry : currShips.entrySet()) {
            ShipType shipType = entry.getKey();

            // Meaning the ship SUNK was of ShipType shipType
            // && entry.getValue() == 0
            if (lastShips.get(shipType) > entry.getValue() ) {
                // Do a check to identify i-squares for what i is
                if (shipType.equals(ShipType.AIRCRAFT_CARRIER)) {
                    // Meaning all 5-square ships have been SUNK
                    // Update defaultProbs to no longer consider 5-squares
                    updateSquareCountsAndProbs(fiveSquares);
                    decrementShipWeight(fiveSquareShipWeights); 
                } else if (shipType.equals(ShipType.BATTLESHIP)) {
                    // Meaning all 4-square ships have been SUNK
                    // Update defaultProbs to no longer consider 4-squares
                    updateSquareCountsAndProbs(fourSquares);
                    decrementShipWeight(fourSquareShipWeights);
                } else if (shipType.equals(ShipType.PATROL_BOAT)) {
                    // Meaning all 2-square ships have been SUNK
                    // Update defaultProbs to no longer consider 2-squares
                    updateSquareCountsAndProbs(twoSquares);
                    decrementShipWeight(twoSquareShipWeights);
                } else if (shipType.equals(ShipType.DESTROYER) || shipType.equals(ShipType.SUBMARINE)) {
                    // Meaning all 3-square ships have been SUNK
                    // Update defaultProbs to no longer consider 3-squares
                    updateSquareCountsAndProbs(threeSquares);
                    decrementShipWeight(threeSquareShipWeights);
                } else {
                    throw new IllegalArgumentException("You have reached the " +
                    "else statement of checkNumRemainingShips, which should not " +
                    "be possible.");
                }
            }
        }
        getNewNormalizedProbs(game);
        lastShips = currShips;
    }

    private void decrementShipWeight(Map<Coordinate, Integer> iSquareShipWeights) {
        for (Map.Entry<Coordinate, Integer> entry : iSquareShipWeights.entrySet()) {
            Coordinate coord = entry.getKey();
            double weight = (double) entry.getValue();

            if (squareCounts.containsKey(coord)) {
                squareCounts.put(coord, squareCounts.get(coord) - weight);
            }
            // Else, this coord has been SHOT at and has been removed
        }
    }

    /*
    * When there is no info regarding enemy ship, specifically when there
    * has not been any HIT, we result to SHOOT based on default probabilities
    */
    private Coordinate shootBasedOnDefaultProbs(GameView game) {


        // Get the max prob value
        double maxValue = Collections.max(defaultProbabilities.values());

        // Create a list to store entries with the max value
        List<Entry<Coordinate, Double>> maxEntries = new ArrayList<Entry<Coordinate, Double>>();

        // Iterate through the entries and add those with the max value to the
        // list for consideration
        for (Entry<Coordinate, Double> entry : defaultProbabilities.entrySet()) {
            if (entry.getValue().equals(maxValue)) {
                maxEntries.add(entry);
            }
        }

        Collections.shuffle(maxEntries);
        Coordinate toBeShot = maxEntries.get(0).getKey();

        // Update our last guess coordinate
        lastGuessCoordinate = toBeShot;
        // Remove this query square
        removeSquare(lastGuessCoordinate);

        return toBeShot;
    }

    List<Coordinate> movesToBeExecuted;
    Set<Coordinate> removedSquares = new HashSet<Coordinate>();

    private void removeSquare(Coordinate coord) {
        // To no longer consider this removed square
        defaultProbabilities.remove(coord);
        squareCounts.remove(coord);
        removedSquares.add(coord);
    }

    /*
     * But this may lead us to consider adjacent squares that may have
     * already been shot
     */
    private List<Coordinate> getAdjacentSquares(Coordinate coord, GameView game) {
        List<Coordinate> adjacentCoords = new ArrayList<Coordinate>();
        /*
         * Only return coords that are w/in bounds
         */
        Coordinate coord1 = new Coordinate(coord.getXCoordinate() + 1, coord.getYCoordinate());
        Coordinate coord2 = new Coordinate(coord.getXCoordinate(), coord.getYCoordinate() + 1);
        Coordinate coord3 = new Coordinate(coord.getXCoordinate() - 1, coord.getYCoordinate());
        Coordinate coord4 = new Coordinate(coord.getXCoordinate(), coord.getYCoordinate() - 1);
        // We will consider only squares that are w/in bounds and we have not
        // shoot at beforehand
        if (game.isInBounds(coord1) && !removedSquares.contains(coord1)) {
            adjacentCoords.add(coord1);
        }
        if (game.isInBounds(coord2) && !removedSquares.contains(coord2)) {
            adjacentCoords.add(coord2);
        }
        if (game.isInBounds(coord3) && !removedSquares.contains(coord3)) {
            adjacentCoords.add(coord3);
        }
        if (game.isInBounds(coord4) && !removedSquares.contains(coord4)) {
            adjacentCoords.add(coord4);
        }
        // System.out.println("adjacent squares of " + coord + ": " + adjacentCoords);
        return adjacentCoords;
    }

    Set<Coordinate> twoSquares;
    Set<Coordinate> threeSquares;
    Set<Coordinate> fourSquares;
    Set<Coordinate> fiveSquares;
    Map<Coordinate, Double> squareCounts = new HashMap<Coordinate, Double>();

    /*
     * For coordinate in squares set, decrement its count in squareCounts,
     * sum it up again and normalize it, and set it to defaultProbs
     */
    private void updateSquareCountsAndProbs(Set<Coordinate> iSquares) {
        // Initialize a temporary map to hold probs
        // Map<Coordinate, Double> squareProbabilities = new HashMap<Coordinate, Double>();
        // Decrement counts of squares
        for (Coordinate square : iSquares) {
            // Consider the case when a square has been SHOT and thus removed
            if (squareCounts.containsKey(square)) {
                squareCounts.put(square, squareCounts.get(square) - 1.0);
            }
        }
    }

    Double avgProb = 0.0;

    private void getNewNormalizedProbs(GameView game) {
        // Initialize a temporary map to hold probs
        Map<Coordinate, Double> squareProbabilities = new HashMap<Coordinate, Double>();

        // Step 1: sum up the values
        double totalSum = 0.0;
        for (double value : squareCounts.values()) {
            totalSum += value;
        }
        // System.out.println("totalSum from getNewNormalizedProbs():" + totalSum);

        // Step 2: normalize each value
        for (Map.Entry<Coordinate, Double> entry : squareCounts.entrySet()) {
            Coordinate square = entry.getKey();
            double count = entry.getValue();
            double normalizedProbability = count / totalSum;
            squareProbabilities.put(square, normalizedProbability);
        }

        int numOfCols = game.getGameConstants().getNumCols();
        int numOfRows = game.getGameConstants().getNumRows();
        for (Map.Entry<Coordinate, Double> entry : squareProbabilities.entrySet()) {
            Coordinate square = entry.getKey();
            double prob = entry.getValue();
            int squareX = square.getXCoordinate();
            int squareY = square.getYCoordinate();
            if (squareX == 0 || squareX == numOfCols - 1 || squareY == 0 || squareY == numOfRows - 1) {
                squareProbabilities.put(square, prob * 1.5);
            }
        }

        // if (avgProb == 0.0) {
        //     avgProb = totalSum / squareCounts.size();
        //     // System.out.println("avgProb: "+avgProb); //68.15016685205784
        // }
        
        // System.out.println("avgProb: " + avgProb);
        // System.out.println("totalSum: "+totalSum+", squareCounts.size(): "+squareCounts.size());

        // System.out.println("Safety check, here is square probs: " + squareProbabilities);
        defaultProbabilities = squareProbabilities;
    }

    // A helper method
    private void updateSquareProbability(Coordinate square, 
                Map<Coordinate, Double> squareCounts, int numOfiSquareShips) {
        // Increment count if square is already in the map
        if (squareCounts.containsKey(square)) {
            squareCounts.put(square, squareCounts.get(square) + (1.0 * numOfiSquareShips));
        } else {
            // Add the square to the map with value of 1 if it's not 
            // already present
            squareCounts.put(square, 1.0 * numOfiSquareShips);
        }
    }

    /*
     * Given our consideration of 2-, 3-, 4-, 5-squares, we will return
     * a Map of the squares that may have a ship along with their corresponding
     * calculated probabilities
     */
    /*
     * Parameters initialized by this method:
     * twoSquares, threeSquares, fourSquares, fiveSquares, squareCounts,
     * defaultProbs
     */
    private void initSquareCountsAndProbs(GameView game) {
        // Initialize data structure to store probs for squares
        Map<Coordinate, Double> squareProbabilities = new HashMap<Coordinate, Double>();

        twoSquares = getAlliSquares(game, 2);
        threeSquares = getAlliSquares(game, 3);
        fourSquares = getAlliSquares(game, 4);
        fiveSquares = getAlliSquares(game, 5);

        for (Coordinate square : twoSquares) {
            updateSquareProbability(square, squareCounts, numOf2Squares);
        }
        for (Coordinate square : threeSquares) {
            updateSquareProbability(square, squareCounts, numOf3Squares);
        }
        for (Coordinate square : fourSquares) {
            updateSquareProbability(square, squareCounts, numOf4Squares);
        }
        for (Coordinate square : fiveSquares) {
            updateSquareProbability(square, squareCounts, numOf5Squares);
        }

        // squareCounts = squareProbabilities;
        // System.out.println("squareCounts: " +squareCounts);

        Map<Coordinate, Integer> shipPlacementWeights = generateHeatMap(game);
        // System.out.println("Here is a printing of shipPlacementWeights/heatmap for debugging purposes: \n" + shipPlacementWeights);

        // adds all the ship placement weights to the square counts
        for (Map.Entry<Coordinate, Integer> entry : shipPlacementWeights.entrySet()) {
            Coordinate coord = entry.getKey();
            double weight = (double) entry.getValue();
            if (squareCounts.containsKey(coord)) {
                squareCounts.put(coord, squareCounts.get(coord) + weight);
            } else {
                squareCounts.put(coord, weight);
            }
        }
        // System.out.println("Here is a printing of squareCounts for debugging purposes: \n" + squareCounts);

        // Step 1: sum up the values
        double totalSum = 0.0;
        for (double value : squareCounts.values()) {
            totalSum += value;
        }
        // System.out.println("totalSum from init: "+totalSum);

        // Step 2: normalize each value
        for (Map.Entry<Coordinate, Double> entry : squareCounts.entrySet()) {
            Coordinate square = entry.getKey();
            double count = entry.getValue();
            double normalizedProbability = count / totalSum;
            squareProbabilities.put(square, normalizedProbability);
        }

        int numOfCols = game.getGameConstants().getNumCols();
        int numOfRows = game.getGameConstants().getNumRows();
        for (Map.Entry<Coordinate, Double> entry : squareProbabilities.entrySet()) {
            Coordinate square = entry.getKey();
            double prob = entry.getValue();
            int squareX = square.getXCoordinate();
            int squareY = square.getYCoordinate();
            if (squareX == 0 || squareX == numOfCols - 1 || squareY == 0 || squareY == numOfRows - 1) {
                squareProbabilities.put(square, prob * 1.5);
            }
        }

        // System.out.println("Safety check, here is square probs: " + squareProbabilities);
        defaultProbabilities = squareProbabilities;
    }


    /*
    * Given a board config, all the squares that need to be check to find
    * all 2-square ships; example config:
    * |**|  |
    * |  |**|
    * all 3-square ships; example config:
    * |**|  |  |
    * |  |**|  |
    * |  |  |**|
    */
    private Set<Coordinate> getAlliSquares(GameView game, int squares){
        Set<Coordinate> result = new HashSet<Coordinate>();
        int counter = 0;

        for (int i = 0; i < game.getGameConstants().getNumRows(); i++) {
            // CHECK THIS INNER LOOP FOR CORRECTNESS!!!
            for (int j = counter; j < game.getGameConstants().getNumCols(); j += squares) {
                result.add(new Coordinate(i, j));
                /*
                 * Make sure the next square isInBounds before we go on to it
                 * To ensure no indexOutOfBounds Error
                 */
                if (!game.isInBounds(i, j + squares) ) {
                    break; // Go on to the next row
                }
            }
            if (counter < squares - 1) {
                counter++;
            } else {
                counter = 0;
            }
        }

        return result;
    }

    Map<Coordinate, Integer> twoSquareShipWeights = new HashMap<Coordinate, Integer>();
    Map<Coordinate, Integer> threeSquareShipWeights = new HashMap<Coordinate, Integer>();
    Map<Coordinate, Integer> fourSquareShipWeights = new HashMap<Coordinate, Integer>();
    Map<Coordinate, Integer> fiveSquareShipWeights = new HashMap<Coordinate, Integer>();


    private Map<Coordinate, Integer> generateHeatMap(GameView game){
        Map<Coordinate, Integer> shipPlacementWeights = new HashMap<>();
        int numRows = game.getGameConstants().getNumRows();
        int numCols = game.getGameConstants().getNumCols();
        
        // Retrieve the current number of each ship type available
        Map<ShipType, Integer> shipsAvailable = game.getEnemyShipTypeToNumRemaining();
        Map<ShipType, Set<Coordinate>> shipPlacements = getShipPlacements();

        for (int i = 0; i < numCols; i++) {
            for (int j = 0; j < numRows; j++) {
                int totalSquareWeight = 0;
                int shipLength = 0;
                int shipCount = 0;
                for (Map.Entry<ShipType, Set<Coordinate>> shipEntry : shipPlacements.entrySet()) {
                    int weight = 0;
                    shipCount = shipsAvailable.get(shipEntry.getKey());
                    shipLength = getShipLength(shipEntry.getKey());
                    Set<Coordinate> placements = shipEntry.getValue();

                    for (Coordinate placement : placements) {
                        int x = placement.getXCoordinate();
                        int y = placement.getYCoordinate();

                        // Currently on index (i, j)
                        if (game.isInBounds((i-x), (j)) && game.isInBounds((i+y), (j))) {
                            weight += 1;
                        }
                        if (game.isInBounds((i), (j-x)) && game.isInBounds((i), (j+y))) {
                            weight += 1;
                        }
                    }

                    if (shipLength == 2) {
                        twoSquareShipWeights.put(new Coordinate(i, j), weight);
                    } else if (shipLength == 3) {
                        threeSquareShipWeights.put(new Coordinate(i, j), weight);
                    } else if (shipLength == 4) {
                        fourSquareShipWeights.put(new Coordinate(i, j), weight);
                    } else if (shipLength == 5) {
                        fiveSquareShipWeights.put(new Coordinate(i, j), weight);
                    }

                    totalSquareWeight += weight;
                }
                shipPlacementWeights.put(new Coordinate(i, j), totalSquareWeight * shipCount);
            }
        }

        return shipPlacementWeights;
    }

    private Map<ShipType, Set<Coordinate>> getShipPlacements() {
        // Create a map to store ship placements
        Map<ShipType, Set<Coordinate>> shipPlacements = new HashMap<>();

        ShipType[] shipTypes = ShipType.values();
        for (ShipType shipType : shipTypes) {
            switch (shipType) {
                case AIRCRAFT_CARRIER:
                    // Add ship placements
                    Set<Coordinate> AIRCRAFT_CARRIER_Coordinates = new HashSet<>();
                    AIRCRAFT_CARRIER_Coordinates.add(new Coordinate(4, 0));
                    AIRCRAFT_CARRIER_Coordinates.add(new Coordinate(3, 1));
                    AIRCRAFT_CARRIER_Coordinates.add(new Coordinate(2, 2));
                    AIRCRAFT_CARRIER_Coordinates.add(new Coordinate(1, 3));
                    AIRCRAFT_CARRIER_Coordinates.add(new Coordinate(0, 4));

                    shipPlacements.put(shipType, AIRCRAFT_CARRIER_Coordinates);
                    break;
                case BATTLESHIP:
                    // Add ship placements
                    Set<Coordinate> BATTLESHIP_Coordinates = new HashSet<>();
                    BATTLESHIP_Coordinates.add(new Coordinate(3, 0));
                    BATTLESHIP_Coordinates.add(new Coordinate(2, 1));
                    BATTLESHIP_Coordinates.add(new Coordinate(1, 2));
                    BATTLESHIP_Coordinates.add(new Coordinate(0, 3));

                    shipPlacements.put(shipType, BATTLESHIP_Coordinates);
                    break;
                case DESTROYER:
                case SUBMARINE:
                    // Add ship placements
                    Set<Coordinate> SUBMARINE_Coordinates = new HashSet<>();
                    SUBMARINE_Coordinates.add(new Coordinate(2, 0));
                    SUBMARINE_Coordinates.add(new Coordinate(1, 1));
                    SUBMARINE_Coordinates.add(new Coordinate(0, 2));

                    shipPlacements.put(shipType, SUBMARINE_Coordinates);
                    break;
                case PATROL_BOAT:
                    // Add ship placements
                    Set<Coordinate> PATROL_BOAT_Coordinates = new HashSet<>();
                    PATROL_BOAT_Coordinates.add(new Coordinate(1, 0));
                    PATROL_BOAT_Coordinates.add(new Coordinate(0, 1));

                    shipPlacements.put(shipType, PATROL_BOAT_Coordinates);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown ship type: " + shipType);
            }
        }

        return shipPlacements;

    }

    // Utility method to return the length of a ship based on its type
    private int getShipLength(ShipType shipType) {  
        switch (shipType) {
            case AIRCRAFT_CARRIER:
                return 5;
            case BATTLESHIP:
                return 4;
            case DESTROYER:
            case SUBMARINE:
                return 3;
            case PATROL_BOAT:
                return 2;
            default:
                throw new IllegalArgumentException("Unknown ship type: " + shipType);
        }
    }

    @Override
    public void afterGameEnds(final GameView game) {}

}