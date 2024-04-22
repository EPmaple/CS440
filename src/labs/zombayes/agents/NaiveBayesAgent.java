package src.labs.zombayes.agents;


// SYSTEM IMPORTS
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;


// JAVA PROJECT IMPORTS
import edu.bu.labs.zombayes.agents.SurvivalAgent;
import edu.bu.labs.zombayes.features.Features.FeatureType;
import edu.bu.labs.zombayes.linalg.Matrix;
import edu.bu.labs.zombayes.utils.Pair;



public class NaiveBayesAgent
    extends SurvivalAgent
{

    public static class NaiveBayes
        extends Object
    {

        public static final FeatureType[] FEATURE_HEADER = {FeatureType.CONTINUOUS,
                                                            FeatureType.CONTINUOUS,
                                                            FeatureType.DISCRETE,
                                                            FeatureType.DISCRETE};

        Map<Double, Double> discrete3TotalProbs;
        Map<Double, Double> discrete4TotalProbs;
        // TODO: complete me!
        public NaiveBayes()
        {
            discrete3TotalProbs = new HashMap<Double, Double>();
            discrete4TotalProbs = new HashMap<Double, Double>();
            groundTruths = new HashMap<Double, Double>();
            continuous1 = new HashMap<Double, Double[]>();
            continuous2 = new HashMap<Double, Double[]>();
            discrete3 = new HashMap<Double, Map<Double, Double>>();
            discrete4 = new HashMap<Double, Map<Double, Double>>();
            counter = 0;
            discrete3FeatureGivenClass =
                    new HashMap<Double, Map<Double, Double>>();
            discrete4FeatureGivenClass =
                    new HashMap<Double, Map<Double, Double>>();
        }
        int counter;

        Map<Double, Map<Double, Double>> discrete3FeatureGivenClass;
        Map<Double, Map<Double, Double>> discrete4FeatureGivenClass;

        // 0.0 == human, 1.0 == zombie
        // 0.0 : 34     1.0 : 32
        Map<Double, Double> groundTruths;

        // continuous1[0] = mean, continuous1[1] = variance, continuous1[2] = memberCount
//        Double[] continuous1;
        Map<Double, Double[]> continuous1;
//        Double[] continuous2;
        Map<Double, Double[]> continuous2;
        Map<Double, Map<Double, Double>> discrete3;
        Map<Double, Map<Double, Double>> discrete4;

        // row = 531, col = 1
        private void calculateGroundTruths(Matrix y_gt) {
//            System.out.println("row for y_gt: " + y_gt.getShape().getNumRows() +
//                    "col for y_gt: " + y_gt.getShape().getNumCols());
            for (int i = 0; i < y_gt.getShape().getNumRows(); i++) {
                double truth = y_gt.get(i, 0);
                if (groundTruths.containsKey(truth)) {
                    groundTruths.put(truth, groundTruths.get(truth) + 1.0);
                } else {
                    groundTruths.put(truth, 1.0);
                }
            }
        }

        Matrix humanClassSubset;
        Matrix zombieClassSubset;

        private void countClassAndFeatureProbs(Matrix X, Matrix y_gt) {
            // there is only one col in y_gt, and 0.0 is human class
            Matrix humanClassRowMask = y_gt.getRowMaskEq(0.0, 0);
            try {
                humanClassSubset = X.filterRows(humanClassRowMask);
            } catch (Exception err) {
                throw new IllegalArgumentException("An error occurred while filtering " +
                        "rows using the mask: " + humanClassRowMask);
            }


            Matrix zombieClassRowMask = y_gt.getRowMaskEq(1.0, 0);
            try {
                zombieClassSubset = X.filterRows(zombieClassRowMask);
            } catch (Exception err) {
                throw new IllegalArgumentException("An error occurred while filtering " +
                        "rows using the mask: " + zombieClassRowMask);
            }

            double humanLabel = 0.0;
            double zombieLabel = 1.0;
            // classLabel = 0.0
            for (int i = 0; i < humanClassSubset.getShape().getNumRows(); i++) {
                for (int j = 0; j < humanClassSubset.getShape().getNumCols(); j++) {
                    // j=0, j=1 are continuous, j=2, j=3 are discrete
                    double element = X.get(i, j);
                    if (j == 0) {
                        updateContinuousMap(humanLabel, element, continuous1);
                    } else if (j == 1) {
                        updateContinuousMap(humanLabel, element, continuous2);
                    } else if (j == 2) {
                        updateNestedDiscreteMap(humanLabel, element, discrete3);
                    } else if (j == 3) {
                        updateNestedDiscreteMap(humanLabel, element, discrete4);
                    }
                }
            }

            for (int i = 0; i < zombieClassSubset.getShape().getNumRows(); i++) {
                for (int j = 0; j < zombieClassSubset.getShape().getNumCols(); j++) {
                    // j=0, j=1 are continuous, j=2, j=3 are discrete
                    double element = X.get(i, j);
                    if (j == 0) {
                        updateContinuousMap(zombieLabel, element, continuous1);
                    } else if (j == 1) {
                        updateContinuousMap(zombieLabel, element, continuous2);
                    } else if (j == 2) {
                        updateNestedDiscreteMap(zombieLabel, element, discrete3);
                    } else if (j == 3) {
                        updateNestedDiscreteMap(zombieLabel, element, discrete4);
                    }
                }
            }
        }




        // TODO: complete me!
        public void fit(Matrix X, Matrix y_gt)
        {

            calculateGroundTruths(y_gt);
//            System.out.println("groundTruths: " + groundTruths.toString());
//            groundTruths: {0.0=267.0, 1.0=264.0}

            // this calculates counts [class AND feature], both discrete and continuous
            countClassAndFeatureProbs(X, y_gt);
//            System.out.println("discrete3: " + discrete3.toString());
//            discrete3: {0.0={2.0=97.0, 1.0=143.0, 0.0=27.0}, 1.0={0.0=212.0, 2.0=51.0, 1.0=1.0}}
//            System.out.println("discrete4: " + discrete4.toString());
//            discrete4: {0.0={1.0=84.0, 0.0=36.0, 2.0=58.0, 3.0=89.0}, 1.0={1.0=211.0, 0.0=24.0, 2.0=15.0, 3.0=14.0}}

            // to produce means and variance of gaussian
            parameterizePdf(continuous1, 0);
//            System.out.println("continuous1: (0.0) " + Arrays.toString(continuous1.get(0.0)) +
//                    "(1.0) " + Arrays.toString(continuous1.get(1.0)));
//            continuous1: (0.0) [1.995848699219806, 1.02549961629314, 267.0, 532.8916026916881]
//            (1.0) [2.0017878778185314, 64.96160900928258, 264.0, 528.4719997440923]
            parameterizePdf(continuous2, 1);
//            System.out.println("continuous2: (0.0) " + Arrays.toString(continuous2.get(0.0)) +
//                    "(1.0) " + Arrays.toString(continuous2.get(1.0)));
//            continuous2: (0.0) [-9.97865377372738, 1.1318218136592884, 267.0, -2664.3005575852108]
//            (1.0) [-9.97786835671197, 395.5673337032527, 264.0, -2634.1572461719597]

            // to produce Pr[feature|class]
            calculateProbOfFeatureGivenClass(discrete3, discrete3FeatureGivenClass);
//            System.out.println("discrete3FeatureGivenClass: " + discrete3FeatureGivenClass);
//            discrete3FeatureGivenClass: {0.0={2.0=0.36329588014981273, 1.0=0.5355805243445693,
//                    0.0=0.10112359550561797}, 1.0={0.0=0.803030303030303, 2.0=0.19318181818181818, 1.0=0.003787878787878788}}
            calculateProbOfFeatureGivenClass(discrete4, discrete4FeatureGivenClass);
//            System.out.println("discrete4FeatureGivenClass: " + discrete4FeatureGivenClass);
//            discrete4FeatureGivenClass: {0.0={1.0=0.3146067415730337, 0.0=0.1348314606741573, 2.0=0.21722846441947566,
//                    3.0=0.3333333333333333}, 1.0={1.0=0.7992424242424242, 0.0=0.09090909090909091, 2.0=0.056818181818181816, 3.0=0.05303030303030303}}

            //  DON'T know why we need to normalize discrete
            // this calculates pr[class AND feature]
//            normalizeDiscrete(discrete3);
//            System.out.println("discrete3 normalized: " + discrete3);
//            discrete3 normalized: {0.0={2.0=0.36329588014981273, 1.0=0.5355805243445693,
//                0.0=0.10112359550561797}, 1.0={0.0=0.803030303030303, 2.0=0.19318181818181818, 1.0=0.003787878787878788}}
//            normalizeDiscrete(discrete4);
//            System.out.println("discrete4 normalized: " + discrete4);
//            discrete4 normalized: {0.0={1.0=0.3146067415730337, 0.0=0.1348314606741573, 2.0=0.21722846441947566
//                , 3.0=0.3333333333333333}, 1.0={1.0=0.7992424242424242, 0.0=0.09090909090909091, 2.0=0.056818181818181816, 3.0=0.05303030303030303}}

            normalizeGroundTruths();
//            System.out.println("groundTruths normalized: " + groundTruths.toString());
//            groundTruths normalized: {0.0=0.5028248587570622, 1.0=0.4971751412429379}


            // DON'T think we need this
            // using law of total prob: pr[smelly] =
            // pr[smelly|zombie]pr[zombie] + pr[smelly|human]pr[human]
//            sumUpDiscreteFeatureProbs(discrete3FeatureGivenClass, discrete3TotalProbs);
//            System.out.println("discrete3TotalProbs: " + discrete3TotalProbs);
//            discrete3TotalProbs: {2.0=0.2787193973634652, 1.0=0.2711864406779661, 0.0=0.4500941619585688}
//            sumUpDiscreteFeatureProbs(discrete4FeatureGivenClass, discrete4TotalProbs);
//            System.out.println("discrete4TotalProbs: " + discrete4TotalProbs);
//            discrete4TotalProbs: {1.0=0.5555555555555556, 0.0=0.11299435028248589, 2.0=0.1374764595103578, 3.0=0.19397363465160075}
        }


        private void normalizeGroundTruths() {
            double gtSum = 0.0;
            for (Map.Entry<Double, Double> entry : groundTruths.entrySet()) {
                gtSum += entry.getValue();
            }
            for (Map.Entry<Double, Double> entry : groundTruths.entrySet()) {
                double key = entry.getKey();
                double prevValue = entry.getValue();
                groundTruths.put(key, prevValue/gtSum);
            }
        }


        private void calculateProbOfFeatureGivenClass(Map<Double, Map<Double, Double>> discrete,
                                                      Map<Double, Map<Double, Double>> discreteFeatureGivenClass) {
            // iterate through the discrete, go over each class AND feature
            // have another map that takes in these probs
            for (Map.Entry<Double, Map<Double, Double>> entry : discrete.entrySet()) {
                double classLabel = entry.getKey();
                double countOfClass = groundTruths.get(classLabel);
                Map<Double, Double> innerMap = entry.getValue();

                for (Map.Entry<Double, Double> innerEntry : innerMap.entrySet()) {
                    double innerKey = innerEntry.getKey();
                    double classANDFeatureCount = innerEntry.getValue();

                    double classGivenFeatureProb = classANDFeatureCount / countOfClass;

                    // Get the inner map, or create a new one if it doesn't exist
                    Map<Double, Double> featureGivenClassInnerMap = discreteFeatureGivenClass.getOrDefault(classLabel,
                            new HashMap<Double, Double>());
                    // Put this key-value pair into the innerMap
                    featureGivenClassInnerMap.put(innerKey, classGivenFeatureProb);
                    // This overwrite should be fine as we grabbed the original innerMap
                    // from discreteFeatureGivenClass
                    discreteFeatureGivenClass.put(classLabel, featureGivenClassInnerMap);
                }
            }
        }


        // using law of total prob: pr[smelly] =
        // pr[smelly|zombie]pr[zombie] + pr[smelly|human]pr[human]
//        private void sumUpDiscreteFeatureProbs(Map<Double, Map<Double, Double>> discrete,
//                                               Map<Double, Double> discreteTotalProbs) {
//            Map<Double, Double> discreteHumanInnerMap = discrete.get(0.0);
//            Map<Double, Double> discreteZombieInnerMap = discrete.get(1.0);
//            for (Map.Entry<Double, Double> entry : discreteHumanInnerMap.entrySet()) {
//                double feature = entry.getKey();
//                double humanComponent =
//                        entry.getValue() * groundTruths.get(0.0);
//                double zombieComponent =
//                        discreteZombieInnerMap.get(feature) * groundTruths.get(1.0);
//                discreteTotalProbs.put(feature, humanComponent + zombieComponent);
//            }
//        }

        // array [mean, variance, memberCount, sum]
        // continuous1 has featureCol of 0, continuous2 has featureCol of 1
        private void parameterizePdf(Map<Double, Double[]> continuous, int featureCol) {
            // continuousFeature {classLabel: {mean, variance, count, sum}}
            for (Map.Entry<Double, Double> entry : groundTruths.entrySet()) {
                double classLabel = entry.getKey();

                Double[] continuousArray = continuous.get(classLabel);

                // mean = sum / count
                continuousArray[0] = continuousArray[3] / continuousArray[2];

                // if human, loop through humanClassSubset, get the corresponding feature
                // values and
                Matrix currentClassSubset;
                if (classLabel == 0.0) { // 0.0 = human
                    currentClassSubset = humanClassSubset;
                } else { // 1.0 = zombie
                    currentClassSubset = zombieClassSubset;
                }

                double squareSum = 0.0;
                for (int i = 0; i < currentClassSubset.getShape().getNumRows(); i++) {
                    double element = currentClassSubset.get(i, featureCol);
                    double squareOfDiff = Math.pow(element - continuousArray[0], 2);
                    squareSum += squareOfDiff;
                }
                // variance = squareSum / count
                continuousArray[1] = squareSum / continuousArray[2];
            }
        }


//        private void normalizeDiscrete(Map<Double, Map<Double, Double>> discrete) {
//            for (Map.Entry<Double, Map<Double, Double>> outerEntry : discrete.entrySet()) {
//                double givenGT = outerEntry.getKey();
//                Map<Double, Double> innerMap = outerEntry.getValue();
//
//                double groundTruthCounts = groundTruths.get(givenGT);
//
//                for (Map.Entry<Double, Double> innerEntry : innerMap.entrySet()) {
//                    double innerKey = innerEntry.getKey();
//                    double count = innerEntry.getValue();
//
//                    innerMap.put(innerKey, count / groundTruthCounts);
//                }
//            }
//        }


        private void updateContinuousMap(double outerkey, double value,
                                         Map<Double, Double[]> continuous) {
            // Get the innerArray or create a new one
            Double[] meanVarianceCountSum = continuous.getOrDefault(outerkey,
                    new Double[]{0.0, 0.0, 0.0, 0.0});

            meanVarianceCountSum[2] += 1.0; // update member count
            meanVarianceCountSum[3] += value; // update sum

            // Put the updated inner array back into the outerMap
            // This overwrite is fine because we based our new value off old value
            continuous.put(outerkey, meanVarianceCountSum);
        }

        private void updateNestedDiscreteMap(double outerKey, double innerKey,
                                     Map<Double, Map<Double, Double>> discrete) {
            double value = 1.0;

            // Get the inner map for the outerKey, or create a new one if it doesn't exist
            Map<Double, Double> innerMap = discrete.getOrDefault(outerKey, new HashMap<Double, Double>());

            // Get the current value for the innerKey, or initialize it to 0.0 if it doesn't exist
            double prevValue = innerMap.getOrDefault(innerKey, 0.0);

            // Update the value for the inner key
            innerMap.put(innerKey, prevValue + value);

            // Put the updated inner map back into the outer map
            // This overwrite is fine as we based the new value off the old value
            discrete.put(outerKey, innerMap);
        }

        private double calculateGaussianProb(double value, double mean,
                                             double variance) {
            double stdDev = Math.sqrt(variance);
            double firstPart = 1 / (stdDev * Math.sqrt(2 * Math.PI));
            double exponent = -(1.0/2.0) * Math.pow((value - mean)/stdDev, 2);
            double secondPart = Math.pow(Math.E, exponent);
            double prediction = firstPart * secondPart;

            return prediction;
        }

        // TODO: complete me!
        public int predict(Matrix x)
        {
            counter += 1;
//            System.out.println("iteration " + counter + ": " + x.toString());
//            [2.141964675020079, -9.697671216394538, 2.0, 1.0]
//[2.1575438997135117, -10.878372616523823, 0.0, 3.0]
//[9.338116942436903, 10.332171769010618, 1.0, 2.0]
//[1.699470127015307, -11.413721820026016, 1.0, 1.0]
//[-0.5522089380783108, -8.838603829507296, 2.0, 3.0]
//[1.0233349695042318, -11.109138408391708, 2.0, 3.0]
//[10.12992284722915, 11.427435838417658, 0.0, 1.0]
            double bestProb = Double.NEGATIVE_INFINITY;
            int bestClass = -1;

            // For every classLabel, two in this case: 0.0 human, 1.0 zombie
            // Prob[human|input] = pr[human] * the product of pr[feature|human]
            // if feature is discrete3, then discrete3.get(human).get(feature)
            // if feature is cont, calculateGaussianProb for its prob

            for (Map.Entry<Double, Double> entry : groundTruths.entrySet()) {
                double classLabel = entry.getKey(); // 0.0 = human or 1.0 = zombie

//                System.out.println("discrete3FeatureGivenClass: "+ discrete3FeatureGivenClass);
//                System.out.println("discrete4FeatureGivenClass: "+ discrete4FeatureGivenClass);

                // Still a distribution conditioned on a specific class
                // 10.12992284722915
                Double[] continuous1InnerArray = continuous1.get(classLabel);
                double continuous1Prob = calculateGaussianProb(x.get(0, 0) ,
                        continuous1InnerArray[0], continuous1InnerArray[1]);
//                System.out.println("continuous1Prob: "+ continuous1Prob);
                // 11.427435838417658
                Double[] continuous2InnerArray = continuous2.get(classLabel);
                double continuous2Prob = calculateGaussianProb(x.get(0, 1) ,
                        continuous2InnerArray[0], continuous2InnerArray[1]);
//                System.out.println("continuous2Prob: "+ continuous2Prob);

                Map<Double, Double> discrete3InnerMap =
                        discrete3FeatureGivenClass.get(classLabel);
                // 0.0
                double discrete3Prob = discrete3InnerMap.getOrDefault(x.get(0, 2),
                        0.00001);
//                System.out.println("discrete3Prob: "+ discrete3Prob);

                Map<Double, Double> discrete4InnerMap =
                        discrete4FeatureGivenClass.get(classLabel);
                // 0.0
                double discrete4Prob = discrete4InnerMap.getOrDefault(x.get(0, 3),
                        0.00001);
//                System.out.println("discrete4Prob: "+ discrete4Prob);


                double posterior =
                        Math.log(entry.getValue()) +
                                Math.log(continuous1Prob) +
                                Math.log(continuous2Prob) +
                                Math.log(discrete3Prob) +
                                Math.log(discrete4Prob);

                String msg = "iteration " + counter + ": ";
                if (classLabel == 0.0) {
                    msg += "human=" + posterior;
                } else {
                    msg += "zombie=" + posterior;
                }
//                System.out.println(msg);

                if (posterior > bestProb) {
                    bestProb = posterior;
                    bestClass = (int)classLabel;
                }
            }
//            System.out.println("bestClass: " + bestClass);
            return bestClass;

            // TODO: use Bayes' rule for this?
//            return -1;
        }
    }
    
    private NaiveBayes model;

    public NaiveBayesAgent(int playerNum, String[] args)
    {
        super(playerNum, args);
        this.model = new NaiveBayes();
    }

    public NaiveBayes getModel() { return this.model; }

    @Override
    public void train(Matrix X, Matrix y_gt)
    {
        System.out.println(X.getShape() + " " + y_gt.getShape());
        this.getModel().fit(X, y_gt);
    }

    @Override
    public int predict(Matrix featureRowVector)
    {
        return this.getModel().predict(featureRowVector);
    }

}
