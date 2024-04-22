package edu.bu.labs.rl.maze.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.utilities.Coordinate;
import edu.bu.labs.rl.maze.utilities.Pair;


public abstract class StochasticAgent
    extends Agent
{

    public static class TransitionModel
        extends Object
    {
        public static final List<Direction> CARDINAL_DIRECTIONS = Arrays.asList(new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
        });

        public static boolean areDirectionsPerpendicular(Direction a,
                                                         Direction b)
        {
            boolean perp = false;

            if(a.equals(Direction.NORTH) || a.equals(Direction.SOUTH))
            {
                perp = b.equals(Direction.EAST) || b.equals(Direction.WEST);
            } else if(a.equals(Direction.EAST) || a.equals(Direction.WEST))
            {
                perp = b.equals(Direction.NORTH) || b.equals(Direction.SOUTH);
            }

            return perp;
        }

        public static Set<Pair<Coordinate, Double> > getTransitionProbs(StateView state,
                                                                 Coordinate src,
                                                                 Direction dirToGo)
        {
            Set<Pair<Coordinate, Double> > transitionsAndProbs = new HashSet<Pair<Coordinate, Double> >();

            // can only go in cardinal directions
            if(CARDINAL_DIRECTIONS.contains(dirToGo))
            {
                for(Direction d : CARDINAL_DIRECTIONS)
                {
                    Coordinate nextCoord = src.getAdjacentCoordinate(d);
                    if(!state.inBounds(nextCoord.getXCoordinate(), nextCoord.getYCoordinate()) ||
                       state.isResourceAt(nextCoord.getXCoordinate(), nextCoord.getYCoordinate()))
                    {
                        nextCoord = src;
                    }

                    if(d.equals(dirToGo))
                    {
                        transitionsAndProbs.add(new Pair<Coordinate, Double>(nextCoord, 0.8));
                    } else if(TransitionModel.areDirectionsPerpendicular(d, dirToGo))
                    {
                        transitionsAndProbs.add(new Pair<Coordinate, Double>(nextCoord, 0.1));
                    }
                }
            } else
            {
                System.err.println("[ERROR] TransitionModel.getTransitionProb: unsupported non-cardinal direction " + dirToGo);
                System.exit(-1);
            }
            return transitionsAndProbs;
        }
    }

    public static class RewardFunction
        extends Object
    {

        public static double NONTERMINAL_REWARD = -0.04;

        public static double getReward(Coordinate s)
        {
            double reward = NONTERMINAL_REWARD;
            if(s.equals(POSITIVE_TERMINAL_STATE))
            {
                reward = +1.0;
            } else if(s.equals(NEGATIVE_TERMINAL_STATE))
            {
                reward = -1.0;
            }
            return reward;
        }
    }

    public static final Coordinate POSITIVE_TERMINAL_STATE = new Coordinate(3, 0);
    public static final Coordinate NEGATIVE_TERMINAL_STATE = new Coordinate(3, 1);

    private int myUnitID;
    private Map<Coordinate, Direction> policy;

	public StochasticAgent(int playerNum)
	{
		super(playerNum);

		this.myUnitID = -1;
        this.policy = new HashMap<Coordinate, Direction>();
	}

	public int getMyUnitID() { return this.myUnitID; }
    public Map<Coordinate, Direction> getPolicy() { return this.policy; }

    private void setMyUnitID(int i) { this.myUnitID = i; }
    protected void setPolicy(Map<Coordinate, Direction> p) { this.policy = p; }
    

    public abstract void computePolicy(StateView state,
                                       HistoryView history);

    public Direction pickRandomDirection(List<Direction> directions,
                                         final List<Double> probs)
    {
    	Direction d = null;
    	if(directions.size() == 1)
    	{
    		d = directions.get(0);
    	} else if (directions.size() > 1)
    	{
	    	// need to argsort probs...this is ok since it is O(4) worst case
	    	List<Integer> idxs = new ArrayList<Integer>(directions.size());
	    	for(int i = 0; i < directions.size(); ++i)
	    	{
	    		idxs.add(i);
	    	}
	
	    	idxs.sort(new Comparator<Integer>()
	    		{
	    			public int compare(final Integer idx1, final Integer idx2)
	    			{
	    				return -1 * Double.compare(probs.get(idx1), probs.get(idx2)); // sort descendingly
	    			}
	    		});
	    	
	    	// convert sorted probs into cdf so we can make the random choice
	    	List<Double> cdf = new ArrayList<Double>(probs.size());
	    	cdf.add(probs.get(idxs.get(0)));
	    	for(int i = 1; i < probs.size(); ++i)
	    	{
	    		cdf.add(cdf.get(i-1) + probs.get(idxs.get(i)));
	    	}
	    	
	    	// get random double and find the largest index j such that randValue <= cdf[j+1]
	    	double randValue = Math.random();
	    	int j = 0;
	    	while(j < cdf.size() && cdf.get(j) <= randValue)
	    	{
	    		j++;
	    	}
	    	
	    	if(j == cdf.size()-1 || j == 0)
	    	{
	    		; // do nothing if we reached the end of the cdf...j is the proper position (edge case)
	    	}
	    	else
	    	{
	    		j -= 1; // go back one to get the proper idx
	    	}
	    	
	    	d = directions.get(idxs.get(j));
    	}
    	return d;
    }

    private Direction getNextDirection(StateView state,
                                       Direction chosenDirection)
    {
        List<Direction> directions = new ArrayList<Direction>(3);
        List<Double> probs = new ArrayList<Double>(3);

        for(Direction d : TransitionModel.CARDINAL_DIRECTIONS)
        {
            if(d.equals(chosenDirection))
            {
                directions.add(d);
                probs.add(0.8);
            } else if(TransitionModel.areDirectionsPerpendicular(d, chosenDirection))
            {
                directions.add(d);
                probs.add(0.1);
            }
        }
        return this.pickRandomDirection(directions, probs);
    }

	@Override
	public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
	{

		// locate enemy and friendly units
        Set<Integer> myUnitIDs = new HashSet<Integer>();
		for(Integer unitID : state.getUnitIds(this.getPlayerNumber()))
        {
            myUnitIDs.add(unitID);
        }
        

		// check that all units are HiddenSquare units
        for(Integer unitID : myUnitIDs)
        {
		    if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman"))
		    {
			    System.err.println("[ERROR] StochasticAgent.initialStep: StochasticAgent should control only Footman units");
			    System.exit(1);
		    }
        }
        if(myUnitIDs.size() != 1)
        {
            System.err.println("[ERROR] StochasticAgent.initialStep: should only control one unit but found " + myUnitIDs.size());
            System.exit(-1);
        }
        this.setMyUnitID(myUnitIDs.iterator().next());

        this.computePolicy(state, history);

		return this.middleStep(state, history);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history) {
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

        Coordinate myUnitCoordinate = new Coordinate(state.getUnit(this.getMyUnitID()).getXPosition(),
            state.getUnit(this.getMyUnitID()).getYPosition());

        if(myUnitCoordinate.equals(POSITIVE_TERMINAL_STATE)
           || myUnitCoordinate.equals(NEGATIVE_TERMINAL_STATE))
        {
            System.out.println("[INFO] StochasticAgent.middleStep: reached terminal state!");

            // end game!
            System.exit(0);

        } else
        {
            if(!this.getPolicy().containsKey(myUnitCoordinate))
            {
                System.err.println("[ERROR] StochasticAgent.middleStep: policy does not have an entry for coordinate " +
                    myUnitCoordinate);
                System.exit(-1);
            }

            // follow the policy
            Direction chosenDirection = this.getPolicy().get(myUnitCoordinate);

            // transition model gets to mess with it
            actions.put(this.getMyUnitID(),
                Action.createPrimitiveMove(this.getMyUnitID(),
                    this.getNextDirection(state, chosenDirection)));
        }

        // System.out.println(actions);

		return actions;
	}

    @Override
	public void terminalStep(StateView state, HistoryView history) {}

    @Override
	public void loadPlayerData(InputStream arg0) {}

	@Override
	public void savePlayerData(OutputStream arg0) {}

}

