package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for <code>Maze</code> objects
 * using a fork/join multi-thread depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */

public class ForkJoinSolver extends SequentialSolver {
	private ForkJoinSolver parent;
	private ConcurrentSkipListSet<Integer> visited;
	public AtomicBoolean stop;
	private ForkJoinSolver root;
	private int current;

	// HashSet<Integer> forkVisited;
	/**
	 * Creates a solver that searches in <code>maze</code> from the start node to a
	 * goal.
	 *
	 * @param maze the maze to be searched
	 */
	public ForkJoinSolver(Maze maze) {
		super(maze);
	}

	/*
	 * public ForkJoinSolver(Maze maze, Set<Integer> forkVisited) { this(maze);
	 * visited.addAll(forkVisited); }
	 */

	/**
	 * Creates a solver that searches in <code>maze</code> from the start node to a
	 * goal, forking after a given number of visited nodes.
	 *
	 * @param maze      the maze to be searched
	 * @param forkAfter the number of steps (visited nodes) after which a parallel
	 *                  task is forked; if <code>forkAfter &lt;= 0</code> the solver
	 *                  never forks new tasks
	 */
	public ForkJoinSolver(Maze maze, int forkAfter) {
		this(maze);
		this.forkAfter = forkAfter;

	}

	//Creates a solver that takes a maze, atomicbool, start and root
	public ForkJoinSolver(Maze maze, AtomicBoolean stop, Integer start, ForkJoinSolver root) {
		this(maze);
		this.stop = stop;
		this.start = start;
		this.root = root;

	}
	
	/**
	 * initialize root 
	 */
	private void initRoot() {
		root = this;
		this.visited = new ConcurrentSkipListSet<Integer>();
		this.stop = new AtomicBoolean();
	}

	/**
	 * Searches for and returns the path, as a list of node identifiers, that goes
	 * from the start node to a goal node in the maze. If such a path cannot be
	 * found (because there are no goals, or all goals are unreacheable), the method
	 * returns <code>null</code>.
	 *
	 * @return the list of node identifiers from the start node to a goal node in
	 *         the maze; <code>null</code> if such a path cannot be found.
	 */
	@Override
	public List<Integer> compute() {

		return parallelSearch();
	}

	private List<Integer> parallelSearch() {
		//if root, initialize root
		if(root == null)
			initRoot();
		//initiate player
		// one player active on the maze at start
		
		int player = maze.newPlayer(start);
		//pushes start node to stack
		frontier.push(start);		

		//Stores possible ForkJoinSollers
		HashSet<ForkJoinSolver> kids = new HashSet<ForkJoinSolver>();
		
	
		// as long as not all nodes have been processed and stop is false
		while (!frontier.empty() && !stop.get()) {

			// get the new node to process
			current = frontier.pop();
			// if current node has a goal
			if (maze.hasGoal(current)) {

				// move player to goal
				maze.move(player, current);
				//set stop bool to true
				stop.set(true);
				//return a path from current solvers start to current pos which is a goal
				return pathFromTo(start, current);
			}
				// checks if current node has been visited nad if it hasnt adds it to visited
			if (root.visited.add(current)) {
				//mvoes player to current pos
				maze.move(player, current);
			}
				// for every node nb adjacent to current
				for (int nb : maze.neighbors(current)) {
					// if nb has not been already visited,
					if (!root.visited.contains(nb)) {
						// nb can be reached from current (i.e., current is nb's predecessor)
						frontier.push(nb);
						predecessor.put(nb, current);
					}
				}
				//checks if there are more than 1 unvisited nodes in stack
				if (frontier.size() > 1) {
					//checks stakc if there are unvisited nodes and creates new solver for each
					while (frontier.size() > 0) {
						//get a node to start from
						int newCurr = frontier.pop();
						//add the node we are going to start from to visited
						if (root.visited.add(newCurr)) {
							//creates a new solver 
							ForkJoinSolver anotherOne = new ForkJoinSolver(this.maze, this.stop, newCurr, this.root);
							//add the new solver to the current solvers list of solvers
							kids.add(anotherOne);
							//start the new fork process
							anotherOne.fork();
						}
					}
			}
		}
		//check the solvers solvers 
		for (ForkJoinSolver child : kids) {
			//check if they found the goal
			if (child.join() != null) {
				List<Integer> result = pathFromTo(start, current);
				result.addAll(child.join());
				return result;
			}
		}
		return null;
	}

}
