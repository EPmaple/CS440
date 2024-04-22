package edu.bu.labs.rl.maze.utilities;


// SYSTEM IMPORTS
import edu.cwru.sepia.util.Direction;
import java.util.HashSet;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.utilities.DistanceMetric;


public class Coordinate
    extends Object implements Cloneable
{

	private final int x, y;

	public Coordinate(int x,
                      int y)
	{
		this.x = x;
		this.y = y;
	}

	public int getXCoordinate() { return this.x; }
    public int getYCoordinate() { return this.y; }

    @Override
	public String toString() { return "(" + this.getXCoordinate() + ", " + this.getYCoordinate() + ")"; }

    // taken from https://en.wikipedia.org/wiki/Pairing_function#Cantor_pairing_function
    // & https://stackoverflow.com/questions/682438/hash-function-providing-unique-uint-from-an-integer-coordinate-pair
    @Override
	public int hashCode()
    {
        return ((this.getXCoordinate() + this.getYCoordinate())*
            (this.getXCoordinate() + this.getYCoordinate() + 1)) / 2 + this.getYCoordinate();
    }

    @Override
	public boolean equals(Object other)
	{
		if(other instanceof Coordinate)
		{
			return this.getXCoordinate() == ((Coordinate)other).getXCoordinate() &&
                this.getYCoordinate() == ((Coordinate)other).getYCoordinate();
		}
		return false;
	}

    @Override
    public Coordinate clone() { return new Coordinate(this.getXCoordinate(), this.getYCoordinate()); }

    public Coordinate getAdjacentCoordinate(Direction dir)
    {
        return new Coordinate(this.getXCoordinate() + dir.xComponent(), this.getYCoordinate() + dir.yComponent());
    }

    public Set<Coordinate> getAdjacentCoordinatesInCardinalDirections()
    {
        Set<Coordinate> adjacentCoordinates = new HashSet<Coordinate>();

        adjacentCoordinates.add(this.getAdjacentCoordinate(Direction.NORTH));
        adjacentCoordinates.add(this.getAdjacentCoordinate(Direction.SOUTH));
        adjacentCoordinates.add(this.getAdjacentCoordinate(Direction.EAST));
        adjacentCoordinates.add(this.getAdjacentCoordinate(Direction.WEST));

        return adjacentCoordinates;
    }

	public Set<Coordinate> getAllAdjacentCoordinates()
    {
        return this.getAdjacentCoordinatesInCardinalDirections();
    }

    public Direction getDirectionTo(Coordinate other)
    {
        if(DistanceMetric.chebyshevDistance(this, other) != 1)
        {
            System.err.println("[ERROR] Coordinate.getDirectionTo: cannot get direction to non-adjacent coordinate");
            System.exit(-1);
        }

        int xDiff = this.getXCoordinate() - other.getXCoordinate();
        int yDiff = this.getYCoordinate() - other.getYCoordinate();

        Direction d = null;
        if(xDiff == 1)
        {
            d = Direction.WEST;
        } else if(xDiff == -1)
        {
            d = Direction.EAST;
        } else if(yDiff == 1)
        {
            d = Direction.NORTH;
        } else
        {
            d = Direction.SOUTH;
        }
        return d;
    }

}
