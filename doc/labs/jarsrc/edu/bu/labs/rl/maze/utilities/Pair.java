package edu.bu.labs.rl.maze.utilities;

public class Pair<K, V> extends Object
{

	private final K first;
	private final V second;

	public Pair(K first, V second)
	{
		this.first = first;
		this.second = second;
	}

	public K getFirst() { return this.first; }
	public V getSecond() { return this.second; }

	@Override
	public int hashCode()
	{
		return (int)(Math.pow(this.getFirst().hashCode(), 2) +
				Math.pow(this.getSecond().hashCode(), 3));
	}

}
