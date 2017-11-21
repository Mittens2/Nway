package core.alg.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
	List<Map<BigDecimal, Move>> moveCache;
	ArrayList<Tuple> solution;
	ArrayList<Tuple> maxSolution;
	
	public class Move{
		public Element toMove;
		public Tuple dest;
		
		public Move(Element toMove, Tuple dest){
			this.toMove = toMove;
			this.dest = dest;
		}
	}
	
	public Search(ArrayList<Model> models){
		this.models = models;
		this.elements = new HashSet<Element>();
		this.solution = new ArrayList<Tuple>();
		for (Model m: models){
			elements.addAll(m.getElements());
			for (Element e: m.getElements()){
				Tuple t = new Tuple().newExpanded(e, models);
				this.solution.add(t);
			}
		}
		moveCache = new ArrayList<Map<BigDecimal, Move>>();
	}
	
	public RunResult execute(){
		long startTime = System.currentTimeMillis();
		// Might want to play around more with n and k
		// n denotes how many elements to consider moving, k denotes how many moves to consider for each element
		runSearch(1000, 1);
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
	private void runSearch(int n, int k){
		Map<BigDecimal, Move> moves = creatNeighbourhood(solution, n , k);
		BigDecimal chosen = chooseMove(moves);
		if (chosen.compareTo(BigDecimal.ZERO) > 0){
			//System.out.println("chosen: " + chosen);
			// Removes move so we do not do move again
			moveCache.add(moves);
			executeMove(moves.remove(chosen));
			//System.out.println(AlgoUtil.calcGroupWeight(solution));
			runSearch(n, k);
		}
	}
	
	// executes move in solution
	private void executeMove(Move move){
		// Remove tuples
		solution.remove(move.toMove.getContainingTuple());
		solution.remove(move.dest);
		// Add tuples
		List<Tuple> toAdd = getMove(move.toMove, move.toMove.getContainingTuple(), move.dest); 
		for (Tuple t: toAdd){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		solution.addAll(toAdd);
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
	
	// n specifies how many elements want to be considered, k specifies how many moves per element want to consider
	private Map<BigDecimal, Move> creatNeighbourhood(ArrayList<Tuple> solution, int n, int k){
		List<Element> desired = new ArrayList<Element>();
		generateBars();
		PriorityQueue<Element> elemsByBar = new PriorityQueue<Element>(elements.size(), new ElementComparator(true, Sort.BAR, new Tuple(), models));
		elemsByBar.addAll(elements);
		// Get n best candidate elements for moving
		for (int i = 0; i < Math.min(n, elemsByBar.size()); i++)
			desired.add(elemsByBar.remove());
	    return this.generateNeighbours(solution, desired, k);
	}
	
	private Map<BigDecimal, Move> generateNeighbours(ArrayList<Tuple> solution, List<Element> desired, int k){
		ArrayList<Tuple> solution_copy = new ArrayList<Tuple>(solution);
		Map<BigDecimal, Move> neighbours = new HashMap<BigDecimal,Move>();
		// Allows one to consider simply moving the element out of it's current tuple
		solution_copy.add(new Tuple());
		for (Element e: desired){
			Map<BigDecimal, Tuple> kBest = new HashMap<BigDecimal, Tuple>();
			PriorityQueue<BigDecimal> moveValues = new PriorityQueue<BigDecimal>();
			// Try to move element into every tuple, and save the k best moves
			for (Tuple t: solution){
				BigDecimal moveWeight = BigDecimal.ZERO;
				for (Tuple moveTuple: getMove(e, e.getContainingTuple(), t))
					moveWeight = moveWeight.add(moveTuple.getWeight());
				moveWeight = moveWeight.subtract(e.getContainingTuple().getWeight().add(t.getWeight()));
				if (moveValues.size() < k || moveWeight.compareTo(moveValues.peek()) > 0){
					if (kBest.size() == k){
						BigDecimal removed = moveValues.peek();
						kBest.remove(removed);
					}
					moveValues.add(moveWeight);
					kBest.put(moveWeight, t);
				}
			}
			for (BigDecimal key: kBest.keySet()){
				Move move = new Move(e, kBest.get(key));
				neighbours.put(key, move);
			}
		}
		return neighbours;
	}
	
	private List<Tuple> getMove(Element e, Tuple src, Tuple dest){
		List<Tuple> toAdd = new ArrayList<Tuple>();
		int index = AlgoUtil.commonModel(e, dest);
		// If conflict, then move conflicting tuple into new tuple
		// TODO: Move elements back into tuple they came from
		if (index != -1){
			Element conflict = dest.getElements().get(index);
			toAdd.add(dest.newExpanded(e, models).lessExpanded(conflict, models));
			toAdd.add(new Tuple().newExpanded(conflict, models));
		}
		else{
			toAdd.add(dest.newExpanded(e, models));
		}
		// Take element out of original tuple, and put into new tuple
		if (src.getSize() > 1)
			toAdd.add(src.lessExpanded(e, models));
		return toAdd;
	}
	
	private void generateBars(){
		BigDecimal globalScore = AlgoUtil.calcGroupWeight(solution);
		for (Element e: elements){
			Tuple contain = e.getContainingTuple();
			Tuple lessTuple = new Tuple();
			lessTuple.setWeight(BigDecimal.ZERO);
			if (contain.getSize() > 1)
				lessTuple = contain.lessExpanded(e, models);
			BigDecimal bar = globalScore.subtract(contain.getWeight().subtract(lessTuple.getWeight()));
			e.setBar(bar);
		}
	}
	
	public ArrayList<Tuple> getTuplesInMatch(){
		return solution;
	}
	
	private void duplicateElements() throws InvalidSolutionException{
		ArrayList<Element> usedElements = new ArrayList<Element>();
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
