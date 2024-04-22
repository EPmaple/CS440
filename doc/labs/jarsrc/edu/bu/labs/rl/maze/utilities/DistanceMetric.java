package edu.bu.labs.rl.maze.utilities;


// SYSTEM IMPORTS



// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.utilities.Coordinate;


public class DistanceMetric
    extends Object
{
    public static float chebyshevDistance(Coordinate a,
                                          Coordinate b)
    {
        return (float)Math.max(Math.abs(a.getXCoordinate() - b.getXCoordinate()),
                               Math.abs(a.getYCoordinate() - b.getYCoordinate()));
    }

    public static float manhattanDistance(Coordinate a,
                                          Coordinate b)
    {
        return (float)(Math.abs(a.getXCoordinate() - b.getXCoordinate()) +
            Math.abs(a.getYCoordinate() - b.getYCoordinate()));
    }

    public static float euclideanDistance(Coordinate a,
                                          Coordinate b)
    {
        return (float)(Math.pow(a.getXCoordinate() - b.getXCoordinate(), 2) +
            Math.pow(a.getYCoordinate() - b.getYCoordinate(), 2));
    }

    public static float pDistance(Coordinate a,
                                  Coordinate b,
                                  int p)
    {
        return (float)(Math.pow(Math.abs(a.getXCoordinate() - b.getXCoordinate()), p) +
            Math.pow(Math.abs(a.getYCoordinate() - b.getYCoordinate()), p));
    }
}

