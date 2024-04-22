package src.pas.tetris.agents;


// SYSTEM IMPORTS
import java.util.*;
import java.util.function.Function; 


// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.game.minos.Mino.Orientation;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;


public class TetrisQAgent
    extends QAgent
{
    boolean debug = false;

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction()
    {
        // if (numOfFeatures == null) {
        //     throw new IllegalArgumentException("numOfFeatures is null, and "+
        //     "thus getQFunctionInput() is not called before initQFunction().");
        // }
        final int totalFeatures = calculateTotalFeatures();
        // For the first hidden layer to have more neurons allows the network
        // to create a broad range of features combinations and interactions
        // from the input data  
        final int hiddenDim1 = totalFeatures * 2;
        // The second hidden layer then takes the broad interpretations from
        // the first hidden layer and hone them together
        final int hiddenDim2 = totalFeatures;
        final int outDim = 1;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(totalFeatures, hiddenDim1));
        // Using ReLU to ensure good gradient flow, and to take care of the problem
        // of vanishing gradient
        qFunction.add(new ReLU()); 
        qFunction.add(new Dense(hiddenDim1, hiddenDim2));
        qFunction.add(new ReLU()); // Additional ReLU activation for deeper network
        qFunction.add(new Dense(hiddenDim2, outDim));


        // build a single-hidden-layer feedforward network
        // this example will create a 3-layer neural network (1 hidden layer)
        // in this example, the input to the neural network is the
        // image of the board unrolled into a giant vector
        // final int numPixelsInImage = Board.NUM_ROWS * Board.NUM_COLS;
        // final int hiddenDim = 2 * numPixelsInImage;


        // Sequential qFunction = new Sequential();
        // qFunction.add(new Dense(numPixelsInImage, hiddenDim));
        // qFunction.add(new Tanh());
        // qFunction.add(new Dense(hiddenDim, outDim));

        return qFunction;
    }

    // Calculate total number of input features based on the feature extraction logic
    private int calculateTotalFeatures() {
        int numPixels = Board.NUM_ROWS * Board.NUM_COLS; // Pixels from the image
        int colHeights = Board.NUM_COLS;                // Heights of each column
        int lineCompletions = 1;                        // Single feature for line completion potential
        int holeScores = 3;                             // Scores for three types of holes

        return numPixels + colHeights + lineCompletions + holeScores;
    }

    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */

    // System.out.println("Here is a printout of the grayscaleImage of the"+
    // " board: ");
    // System.out.println(game.getGrayscaleImage(potentialAction).toString());
    /*
    * boardCol, = 16
    * start from row = 0 to 16, to get topMostFilledCell
    * say it is at row = 4, then the height of the column is 16-4
    */
@Override
    public Matrix getQFunctionInput(final GameView game,
                                    final Mino potentialAction)
    {
        List<Double> features = new ArrayList<Double>();
        Matrix resultingOneVectorMatrix = null;

        try
        {
            Matrix currentMatrix = game.getGrayscaleImage(potentialAction);
            if (debug) System.out.println(currentMatrix.toString());

            // Provide the NN with the grayScaleImage of the Game
            Matrix flattenedImage = currentMatrix.flatten();
            for (int col = 0; col < flattenedImage.getShape().getNumCols(); col++) {
                for (int row = 0; row < flattenedImage.getShape().getNumRows(); row++) {
                    features.add(flattenedImage.get(row, col));
                }
            }

            // Explicitly provide the NN with the height of each column of the Game
            for (int col = 0; col < currentMatrix.getShape().getNumCols(); col++) {
                double colHeight = 0.0;
                for (int row = 0; row < currentMatrix.getShape().getNumRows(); row++) {
                    if (currentMatrix.get(row, col) != 0) { // meaning not empty
                        colHeight = currentMatrix.getShape().getNumRows() - row;
                        break;
                    }
                }
                features.add(colHeight);
            }

            // Evalute potential for line completions and add to features
            features.add(calculatePotentialLineCompletion(currentMatrix));

            // Evaluate holes and add their corresponding score as a feature
            Map<String, List<Integer>> holes = classifyHoles(game.getBoard());
            double[] scores = calculateScoreForEachHole(holes);
            // adding the scores for "rule1", "rule2", "rule3"
            for (double score : scores) {
                features.add(score);
            }

            // turn the feature List into a Vector
            Matrix zeroMatrix = Matrix.zeros(1, features.size());

            for (int row = 0; row < 1; row++) {
                for (int col = 0; col < features.size(); col++) {
                    zeroMatrix.set(row, col, features.get(col));
                }
            }

            resultingOneVectorMatrix = zeroMatrix;


        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        return resultingOneVectorMatrix;
    }

    private double calculatePotentialLineCompletion(Matrix matrix) {
        double completeLines = 0.0;
        for (int row = 0; row < matrix.getShape().getNumRows(); row++) {
            boolean complete = true;
            for (int col = 0; col < matrix.getShape().getNumCols(); col++) {
                if (matrix.get(row, col) == 0) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                completeLines++;
            }
        }
        return completeLines;
    }

    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    @Override
    public boolean shouldExplore(final GameView game,
                                 final GameCounter gameCounter)
    {
        long totalGamesPlayed = gameCounter.getTotalGamesPlayed(); // give it the variable x
        // System.out.println("Current total games played: " + totalGamesPlayed);
        // Math.max (0.05, 1.0 * (0.995)^x), as the number of games we play
        // increases, the latter will decay
        double currentExplorationProb = Math.max(MIN_EXPLORATION_PROB, 
            INITIAL_EXPLORATION_PROB * Math.pow(EXPLORATION_DECAY_RATE, totalGamesPlayed));
        return this.getRandom().nextDouble() <= currentExplorationProb;
        // return this.getRandom().nextDouble() <= EXPLORATION_PROB;
    }

    private static final double INITIAL_EXPLORATION_PROB = 1.0;
    private static final double MIN_EXPLORATION_PROB = 0.05;
    private static final double EXPLORATION_DECAY_RATE = 0.995;  


    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    HashMap<Mino, Double> minoToReward = new HashMap<Mino, Double>();
    HashMap<Mino, Integer> minoToCount = new HashMap<Mino, Integer>();
    int totalMinoCount = 0;

    @Override
    public Mino getExplorationMove(final GameView game)
    {
        // int randIdx = this.getRandom().nextInt(game.getFinalMinoPositions().size());
        // return game.getFinalMinoPositions().get(randIdx);

        double bestUCBScoreSoFar = 0.0;
        Mino bestMinoPositionSoFar = null;

        /*
         * reward, initially, we have 0.0, so we have to getReward,
         * 
         * but then afterwards, we may have some <double> reward, then can use the value
         * of the get
         * 
         * if minoToReward.get(mino) is 0.0, then we add reward to it
         * else minoToReward(mino), then we still add reward to it
         */
        for (Mino mino : game.getFinalMinoPositions()) {
            /*
             * the defaults below and the if-condition is to handle possible 
             * initialization edge cases
             */
            double currentMinoTotalReward = minoToReward.getOrDefault(mino,
            getRewardForMinoInMatrix(game, mino));
            int currentMinoCount = minoToCount.getOrDefault(mino, 1);
            int localMinoCount = totalMinoCount;
            if (totalMinoCount == 0) {
                localMinoCount = 1;
            }

            double UCBScore = getUCBScore(currentMinoTotalReward, currentMinoCount, localMinoCount);

            if (UCBScore > bestUCBScoreSoFar) {
                bestUCBScoreSoFar = UCBScore;
                bestMinoPositionSoFar = mino;
            }
        }

        // sanity check
        if (bestUCBScoreSoFar == 0.0 || bestMinoPositionSoFar == null) {
            throw new IllegalArgumentException("Either bestUCBScoreSoFar == 0.0 " +
            "or bestMinoPositionSoFar == null, meaning a valid exploration move " +
            "was not selected.");
        }

        // having arrived here, we've found the move we should execute according to
        // UCB

        // update minoToReward
        double minoTotalReward = minoToReward.getOrDefault(bestMinoPositionSoFar, 0.0);
        minoToReward.put(bestMinoPositionSoFar, minoTotalReward + getRewardForMinoInMatrix(game, bestMinoPositionSoFar));
        // update minoToCount
        int minoTotalCount = minoToCount.getOrDefault(bestMinoPositionSoFar, 0);
        minoToCount.put(bestMinoPositionSoFar, minoTotalCount + 1);
        // update totalMinoCount
        totalMinoCount += 1;

        return bestMinoPositionSoFar;
    }
    // we could for one use getReward to examine the current board, 
    // or i can keep a hashmap of each mino with its count
    private static final double UCBTunabilityFactor = Math.sqrt(2);

    private double getUCBScore(double minoReward, int minoCount, int totalMinoCount) {
        double avgMinoReward = minoReward / minoCount;
        return avgMinoReward + UCBTunabilityFactor * Math.sqrt(Math.log(totalMinoCount) / minoCount);
    }

    private double getRewardForMinoInMatrix(final GameView game, Mino mino) {
        double inversedScore = 0.0;
        try {
            Matrix grayScaleMatrix = game.getGrayscaleImage(mino);
            Map<String, List<Integer>> holes = classifyHoles(grayScaleMatrix);
            double score = calculateScore(holes);
            // System.out.println("Print of score: " + score);
            inversedScore = aOverSqrtX(score);
            // System.out.println("Print of inversedScore: " + inversedScore);

        } catch (Exception err) {
            System.err.println("Failed to generate grayscale image: " + err.getMessage());
            err.printStackTrace();

        }

        if (inversedScore == 0.0) {
            throw new IllegalArgumentException("The method getRewardForMinoInMatrix()" + 
            "returned a score of 0.0, which should not be allowed.");
        }

        return inversedScore;
    }

    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game)
    {
        Board board = game.getBoard();

        Map<String, List<Integer>> holes = classifyHoles(board);

        // calculateScore returns a high number for a bad state
        // returns a small number for a good state
        double score = calculateScore(holes);
        if (debug) System.out.println("Print of a badness score: " + score);

        // we need to inverse it, s.t. the high number of a bad state => low number
        // s.t. the low number of a good state => high number
        double inversedScore = aOverSqrtX(score);
        if (debug) System.out.println("Print of boardStateScore: " + inversedScore);
        
        double heightScore = calculateHeightScore(board);
        if (debug) System.out.println("Print of heightScore: " + heightScore);

        // need to make game.getScoreThisTurn() much more significant so that
        // we can highly incentivize the agent to try to complete lines

        /*
         * we will either have to change the score function, or inflate the score
         * acquired
         * 
         * for example, if a score of 6 is acquired etc, then this should dominate
         * the score of calculating the board, no?
         */
        double lineCompleteScore = getLineCompleteScore(game.getScoreThisTurn());
        if (debug) System.out.println("Print of lineClearedScore: " + lineCompleteScore);

        double totalScore = inversedScore + lineCompleteScore + heightScore;
        if (debug) System.out.println("Print of totalScore: " + totalScore);

        return totalScore;
    }

    private double getLineCompleteScore(double scoreThisTurn) {
        if (scoreThisTurn != 0) {
            if (debug) System.out.println("Check score here. " + scoreThisTurn);
        }
        return Math.exp(3.0 / 4.0 * scoreThisTurn) - 1;
    }

    // we are using a height-based weighing function for the holes
    private Map<String, List<Integer>> classifyHoles(Board board) {
        // System.out.println("Here is a print out of the board: " + board.toString());
        // System.out.println("Above is a print out of the current board.");
        Map<String, List<Integer>> holes = new HashMap<>();
        holes.put("rule1", new ArrayList<>());
        holes.put("rule2", new ArrayList<>());
        holes.put("rule3", new ArrayList<>());

        int boardCol = board.NUM_COLS;
        int boardRow = board.NUM_ROWS;

        // rule 1
        for (int col = 1; col < boardCol; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < boardRow; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < boardRow; tempRow++) {
                if (!board.isCoordinateOccupied(col - 1, tempRow)) {
                    // System.out.println(
                    //     "rule1 hole in the board: (" + (col-1) + "," + tempRow + ")");
                    holes.get("rule1").add(tempRow);
                }
            }
        }

        // rule 3
        for (int col = 0; col < boardCol - 1; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < boardRow; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < boardRow; tempRow++) {
                if (!board.isCoordinateOccupied(col + 1, tempRow)) {
                    // System.out.println(
                    //     "rule3 hole in the board: (" + (col+1) + "," + tempRow + ")");
                    holes.get("rule3").add(tempRow);
                }
            }
        }

        // rule 2
        for (int col = 0; col < boardCol; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < boardRow; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < boardRow; tempRow++) {
                if (!board.isCoordinateOccupied(col, tempRow)) {
                    // System.out.println(
                    //     "rule2 hole in the board: (" + col + "," + tempRow + ")");
                    holes.get("rule2").add(tempRow);
                }
            }
        }

        // for (int col = 0; col < boardCol; col++) {
        //     int topMostFilledRow = -1;
        //     for (int row = 0; row < boardRow; row++) {
        //         if (board.isCoordinateOccupied(col, row)) {
        //             topMostFilledRow = row;
        //             break;
        //         }
        //     }
    
        //     for (int row = topMostFilledRow + 1; row < boardRow; row++) {
        //         if (!board.isCoordinateOccupied(col, row)) { // Empty cell under the topmost filled cell
        //             System.out.println(
        //                 "rule2 hole in the board: (" + col + "," + row + ")");
        //             holes.get("rule2").add(row);
        //         }
        //     }
    
        //     // Check neighboring columns for rule 1 and rule 3
        //     if (col > 0) { // Check left neighbor
        //         int leftTopMostFilledRow = findTopMostFilled(board, col - 1, boardRow);
        //         for (int row = 0; row <= leftTopMostFilledRow; row++) {
        //             if (!board.isCoordinateOccupied(col, row)) {
        //                 System.out.println(
        //                     "rule1 hole in the board: (" + col + "," + row + ")");
        //                 holes.get("rule1").add(row);
        //             }
        //         }
        //     }
        //     if (col < boardCol - 1) { // Check right neighbor
        //         int rightTopMostFilledRow = findTopMostFilled(board, col + 1, boardRow);
        //         for (int row = 0; row <= rightTopMostFilledRow; row++) {
        //             if (!board.isCoordinateOccupied(col, row)) {
        //                 System.out.println(
        //                     "rule3 hole in the board: (" + col + "," + row + ")");
        //                 holes.get("rule3").add(row);
        //             }
        //         }
        //     }
        // }
        // System.out.println("");
        return holes;
    } 

    // assuming a grayscaleimage matrix is passed in
    // this function would not work correctly with any other types of matrix
    private Map<String, List<Integer>> classifyHoles(Matrix matrix) {
        Map<String, List<Integer>> holes = new HashMap<>();
        holes.put("rule1", new ArrayList<>());
        holes.put("rule2", new ArrayList<>());
        holes.put("rule3", new ArrayList<>());

        int matrixCol = matrix.getShape().getNumCols();
        int matrixRow = matrix.getShape().getNumRows();

        // rule 1
        for (int col = 1; col < matrixCol; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < matrixRow; row++) {
                if (matrix.get(row, col) != 0) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < matrixRow; tempRow++) {
                if (matrix.get(tempRow, col - 1) == 0) {
                    holes.get("rule1").add(tempRow);
                }
            }
        }

        // rule 3
        for (int col = 0; col < matrixCol - 1; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < matrixRow; row++) {
                if (matrix.get(row, col) != 0) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < matrixRow; tempRow++) {
                if (matrix.get(tempRow, col + 1) == 0) {
                    holes.get("rule3").add(tempRow);
                }
            }
        }

        // rule 2
        for (int col = 0; col < matrixCol; col++) {
            Integer topMostRow = null;
            for (int row = 0; row < matrixRow; row++) {
                if (matrix.get(row, col) != 0) {
                    topMostRow = row;
                    break;
                }
            }
            if (topMostRow == null) {
                continue;
            }

            for (int tempRow = topMostRow; tempRow < matrixRow; tempRow++) {
                if (matrix.get(tempRow, col) == 0) {
                    holes.get("rule2").add(tempRow);
                }
            }
        }
    
        // for (int col = 0; col < matrixWidth; col++) {
        //     int topMostFilledRow = -1;
        //     for (int row = 0; row < matrixHeight; row++) {
        //         if (matrix.get(row, col) == 1) { // Assuming '1' represents a filled cell
        //             topMostFilledRow = row;
        //             break;
        //         }
        //     }
    
        //     for (int row = topMostFilledRow + 1; row < matrixHeight; row++) {
        //         if (matrix.get(row, col) == 0) { // Empty cell under the topmost filled cell
        //             holes.get("rule2").add(row);
        //         }
        //     }
    
        //     // Check neighboring columns for rule 1 and rule 3
        //     if (col > 0) { // Check left neighbor
        //         int leftTopMostFilledRow = findTopMostFilled(matrix, col - 1, matrixHeight);
        //         for (int row = 0; row <= leftTopMostFilledRow; row++) {
        //             if (matrix.get(row, col) == 0) {
        //                 holes.get("rule1").add(row);
        //             }
        //         }
        //     }
        //     if (col < matrixWidth - 1) { // Check right neighbor
        //         int rightTopMostFilledRow = findTopMostFilled(matrix, col + 1, matrixHeight);
        //         for (int row = 0; row <= rightTopMostFilledRow; row++) {
        //             if (matrix.get(row, col) == 0) {
        //                 holes.get("rule3").add(row);
        //             }
        //         }
        //     }
        // }
        return holes;
    }

    /*
     * row
     * 0   0
     * 1   0
     * 2   0
     * 3   1
     * 4   0.5
     * 
     * so row = 3 is returned
     */

    private double calculateScore(Map<String, List<Integer>> holes) {
        double score = 0;
        Function<Integer, Double> f = y -> Math.pow(y, 3);
        Function<Integer, Double> g = y -> Math.pow(y, 2);
    
        score += holes.get("rule1").stream().mapToDouble(row -> g.apply(row)).sum();
        score += holes.get("rule2").stream().mapToDouble(row -> f.apply(row)).sum();
        score += holes.get("rule3").stream().mapToDouble(row -> g.apply(row)).sum();
    
        return score;
    }

    private double[] calculateScoreForEachHole(Map<String, List<Integer>> holes) {
        double[] scores = new double[3];

        Function<Integer, Double> f = y -> Math.pow(y, 3);
        Function<Integer, Double> g = y -> Math.pow(y, 2);
    
        scores[0] = holes.get("rule1").stream().mapToDouble(row -> g.apply(row)).sum();
        scores[1] = holes.get("rule2").stream().mapToDouble(row -> f.apply(row)).sum();
        scores[2] = holes.get("rule3").stream().mapToDouble(row -> g.apply(row)).sum();

        return scores;
    }

    private static final int a = 100;

    private double aOverSqrtX(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("x cannot be negative");
        }
        if (x == 0.0) {
            return 0.0;
        }
        return a / (Math.sqrt(x));
    }

    private double calculateHeightScore(Board board) {
        final int maxRows = Board.NUM_ROWS;
        double totalHeight = 0;
        double[] heights = new double[board.NUM_COLS];  // Store individual column heights
        double maxColumnHeight = 0;

        // Calculate total filled height and find max height
        for (int col = 0; col < board.NUM_COLS; col++) {
            for (int row = 0; row < board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    heights[col] = maxRows - row;
                    if (heights[col] > maxColumnHeight) {
                        maxColumnHeight = heights[col];
                    }
                    break;
                }
            }
            totalHeight += heights[col];
        }

        // Apply exponential decay formula with variance
        return Math.exp(1 - (0.15 * maxColumnHeight));
    }

    // private double calculateHeightScore(Board board) {
    //     final int maxRows = Board.NUM_ROWS;
    //     double totalHeight = 0;
    //     double[] heights = new double[board.NUM_COLS];  // Store individual column heights
    //     double maxColumnHeight = 0;

    //     // Calculate total filled height and find max height
    //     for (int col = 0; col < board.NUM_COLS; col++) {
    //         for (int row = 0; row < board.NUM_ROWS; row++) {
    //             if (board.isCoordinateOccupied(col, row)) {
    //                 heights[col] = maxRows - row;
    //                 if (heights[col] > maxColumnHeight) {
    //                     maxColumnHeight = heights[col];
    //                 }
    //                 break;
    //             }
    //         }
    //         totalHeight += heights[col];
    //     }

    //     // Calculate average filled height
    //     double averageHeight = totalHeight / board.NUM_COLS;

    //     // Calculate variance
    //     double variance = 0;
    //     for (double height : heights) {
    //         variance += Math.pow(height - averageHeight, 2);
    //     }
    //     variance /= board.NUM_COLS;

    //     // Constants for decay calculation
    //     double decayConstant = 0.1;  // Adjust decay rate
    //     double varianceImpact = 0.5;  // Adjust impact of variance

    //     // Apply exponential decay formula with variance
    //     return Math.exp(-decayConstant * (averageHeight / maxRows + varianceImpact * Math.sqrt(variance) / maxRows));
    // }
}
