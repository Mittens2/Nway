package core.alg.search;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import core.alg.merge.RandomizedMatchMerger.InvalidSolutionException;
import core.common.AlgoUtil;
import core.common.ElementComparator;
import core.common.N_WAY;
import core.common.ElementComparator.Sort;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.RunResult;

public class Search {
	
	ArrayList<Model> models;
	Set<Element> elements;
	Queue<Map<BigDecimal, Move>> moveCache;
	ArrayList<Tuple> solution;
	MoveStrategy strat;
	int k;
	
	public enum MoveStrategy{
		ELEMENT,
		TUPLE
	}
	
	public class Move{
		public List<Element> toMove;
		public Tuple src;
		public Tuple dest;
		
		public Move(List<Element> toMove, Tuple src, Tuple dest){
			this.toMove = toMove;
			this.src = src;
			this.dest = dest;
		}
	}
	
	public Search(ArrayList<Model> models, ArrayList<Tuple> solution, MoveStrategy strat, int k){
		this.models = models;
		this.strat = strat;
		this.elements = new HashSet<Element>();
		this.solution = solution;
		for (Model m: models){
			elements.addAll(m.getElements());
		}
		for (Tuple t: solution){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		this.k = k;
		moveCache = new ArrayDeque<Map<BigDecimal, Move>>(k);
	}
	
	public Search(ArrayList<Model> models, MoveStrategy strat, int k){
		this.models = models;
		this.strat = strat;
		this.elements = new HashSet<Element>();
		this.solution = new ArrayList<Tuple>();
		for (Model m: models){
			elements.addAll(m.getElements());
			for (Element e: m.getElements()){
				Tuple t = new Tuple().newExpanded(e, models);
				this.solution.add(t);
			}
		}
		this.k = k;
		moveCache = new ArrayDeque<Map<BigDecimal, Move>>(k);
	}
	
	public RunResult execute(){
		long startTime = System.currentTimeMillis();
		runSearch();
		try {
			duplicateElements();
		}catch (InvalidSolutionException e){
			e.printStackTrace();
		}
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		return new RunResult(execTime, weight, avgTupleWeight, solution);
	}
	
	// runs search based on some strategy (probably don't want k, or n as input to this function)
	private void runSearch(){
		Map<BigDecimal, Move> moves = creatNeighbourhood();
		BigDecimal chosen = chooseMove(moves);
		if (chosen.compareTo(BigDecimal.ZERO) > 0){
			//System.out.println("chosen: " + chosen);
			// Removes move so we do not do move again
			if (moveCache.size() == k)
				moveCache.remove();
			moveCache.add(moves);
			executeMove(moves.remove(chosen));
			runSearch();
		}
	}
		
	// n specifies how many elements want to be considered, k specifies how many moves per element want to consider
	private Map<BigDecimal, Move> creatNeighbourhood(){
		List<Element> desired = new ArrayList<Element>();
		// Here can distinguish between elements/tuples that want to generate neighbouring solutions for.
		// Currently not doing any filtering
		if (this.strat == MoveStrategy.ELEMENT)
			desired.addAll(elements);
		else{
			for (Tuple t: solution)
				desired.add(t.getElements().get(0));
		}
	    return this.generateNeighbours(desired);
	}
	
	private Map<BigDecimal, Move> generateNeighbours(List<Element> desired){
		Set<Tuple> solution_copy = new HashSet<Tuple>(solution);
		Map<BigDecimal, Move> neighbours = new HashMap<BigDecimal,Move>();
		// Allows one to consider simply moving the element out of it's current tuple
		solution_copy.add(new Tuple());
		for (Element e: desired){
			solution_copy.remove(e.getContainingTuple());
			List<Element> toMove = new ArrayList<Element>();
			// Can either consider moving elements or combining entire tuples
			if (this.strat == MoveStrategy.ELEMENT)
				toMove.add(e);
			else
				toMove.addAll(e.getContainingTuple().getElements());
			for (Tuple t: solution_copy){
				if (!(this.strat == MoveStrategy.TUPLE) || e.getContainingTuple().haveCommonModelWith(t) == false){
					BigDecimal moveWeight = BigDecimal.ZERO;
					for (Tuple moveTuple: getMove(toMove, e.getContainingTuple(), t))
						moveWeight = moveWeight.add(moveTuple.getWeight());
					moveWeight = moveWeight.subtract(e.getContainingTuple().getWeight().add(t.getWeight()));
					Move move = new Move(toMove, e.getContainingTuple(), t);
					neighbours.put(moveWeight, move);
				}
			}
			solution_copy.add(e.getContainingTuple());
		}
		return neighbours;
	}
	
	// executes move in solution
	private void executeMove(Move move){
		// Remove tuples
		solution.remove(move.src);
		solution.remove(move.dest);
		// Add tuples
		List<Tuple> toAdd = getMove(move.toMove, move.src, move.dest); 
		for (Tuple t: toAdd){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		solution.addAll(toAdd);
	}
	
	
	private List<Tuple> getMove(List<Element> toMove, Tuple src, Tuple dest){
		List<Tuple> toAdd = new ArrayList<Tuple>();
		Tuple newDest = dest;
		for (Element e: toMove){
			int index = AlgoUtil.commonModel(e, dest);
			// If conflict, then move conflicting tuple into new tuple
			// TODO: Move elements back into tuple they came from
			if (index != -1){
				Element conflict = newDest.getElements().get(index);
				newDest = newDest.newExpanded(e, models).lessExpanded(conflict, models);
				toAdd.add(new Tuple().newExpanded(conflict, models));
			}
			else{
				newDest = newDest.newExpanded(e, models);
			}
		}
		toAdd.add(newDest);
		// Add modified original tuple to solution
		if (src.getSize() > toMove.size()){
			Tuple newSrc = src;
			for (Element e: toMove){
				newSrc = newSrc.lessExpanded(e, models);
			}
			toAdd.add(newSrc);
		}
		return toAdd;
	}
	
	// Chooses move, need to make this able to switch strategies in the future
	private BigDecimal chooseMove(Map<BigDecimal, Move> moves){
		BigDecimal max = new BigDecimal(-1);
		for (BigDecimal key: moves.keySet()){
			if (key.compareTo(max) > 0){
				max = key;
			}
		}
		return max;
	}
	
	public ArrayList<Tuple> getTuplesInMatch(){
		return solution;
	}
	
	private void duplicateElements() throws InvalidSolutionException{
		Set<Element> usedElements = new HashSet<Element>();
		for (Tuple t: solution){
			for (Element e: t.getElements()){
				if (usedElements.contains(e)){
					throw new InvalidSolutionException("found duplicate element:" + e);
				}
				usedElements.add(e);
			}
		}
	}
	
	public class InvalidSolutionException extends Exception {
		private static final long serialVersionUID = 1L;

		public InvalidSolutionException(String message) {
	        super(message);
	    }
	}
}
