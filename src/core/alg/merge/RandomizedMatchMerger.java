package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import core.alg.Matchable;
import core.common.AlgoUtil;
import core.common.ElementComparator;
import core.common.ModelComparator;
import core.common.N_WAY;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.RunResult;

public class RandomizedMatchMerger extends Merger implements Matchable {
	protected ArrayList<Tuple> solution;
	protected ArrayList<Element> unusedElements;
	private RunResult res;

	public RandomizedMatchMerger(ArrayList<Model> models){
		super(models);
	}
	
	public void run(){
		long startTime = System.currentTimeMillis();
		unusedElements = joinAllModels();
		solution = execute();
		//solution = execute(2);
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		if(AlgoUtil.areThereDuplicates(solution)){
			weight = new BigDecimal(-500);
		}
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		if(solution.size() == 0)
			return;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.setTitle("Randomized");
		
	}
	
	private ArrayList<Element> joinAllModels() {
		/**
		 * Joins all of the merger's models into a single Elements list.
		 * 
		 * @return The list of all of the models' Elements.
		 */
		ArrayList<Element> elems = new ArrayList<Element>();
		//Collections.sort(models, new ModelComparator(true));
		for(Model m:models){
			ArrayList<Element> modelElems = m.getElements();
			//Collections.sort(modelElems, new ElementComparator(true));
			elems.addAll(modelElems);
		}
		
		//Collections.sort(elems, new ElementComparator(false));
		return elems;
	}
	
	private HashMap<String, Integer> getSortedProperties(ArrayList<Element> elems){
		HashMap<String, Integer> propFreq = new HashMap<String, Integer>();
		for (Element e: elems){
			for (String p: e.getProperties()){
				int count = propFreq.containsKey(p) ? propFreq.get(p) : 0;
				propFreq.put(p, count + 1);
			}
		}
		return propFreq;
	}
	
	private ArrayList<Tuple> execute(){
		/**
		 * Executes an instance of the randomized merge algorithm (DumbHuman).
		 * 
		 * @return The list of Tuples derived from performing randomized merge on given models.
		 */
		Random rand = new Random();
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		while(unusedElements.size() > 1){
			//Element picked = unusedElements.get(rand.nextInt(unusedElements.size()));
			//unusedElements.remove(rand.nextInt(unusedElements.size()));
			//Element picked = unusedElements.get((int) (unusedElements.size() / 2));
			//unusedElements.remove((int) (unusedElements.size() / 2));
			Element picked = unusedElements.get(0);
			unusedElements.remove(0);
			//Tuple bestTuple = getBestTuple(new ArrayList<Element>(unusedElements), picked, 1);
			Tuple bestTuple = getBestTuple(new ArrayList<Element>(unusedElements), picked);
			result.add(bestTuple);
		}
		return result;
	}
	
	private ArrayList<Tuple> execute(int threshold){
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		// TODO Algorithm that starts with looking for threshold number of matches
		// threshold is decremented by 1 every iteration.
		return result;
	}
	
	private Tuple getBestTuple(ArrayList<Element> elems, Element picked, int shared){
		/**
		 * Returns the best tuple that can be created in a single run of randomized merge.
		 * The function consider elements from different models that have at least one
		 * shared property with the element picked. The function then finds the element within
		 * this set that maximizes the current tuple's score, and appends to current the current tuple.
		 */
		Tuple best = new Tuple();
		while(picked != null){
			best = best.newExpanded(picked, models);
			elems = AlgoUtil.removeElementsSameModelId(picked, elems);
			elems = AlgoUtil.getElementsWithSharedProperties(picked, elems, shared);
			picked = getMaxElement(elems, best);
			unusedElements.remove(picked);
		}
		return best;
	}
	
	private Tuple getBestTuple(ArrayList<Element> elems, Element picked){
		/**
		 * Variation of the randomized merge algorithm. Instead of only considering elements
		 * that have one shared property with the current element, the algorithm reconsiders elements
		 * to append to the tuple if they have at least bestTuple.size() shared properties with any of
		 * elements in the current tuple.
		 */
		Tuple best = new Tuple();
		ArrayList<Element> incompatible = new ArrayList<Element>();
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		while(picked != null){
			best = best.newExpanded(picked, models);
			elems = AlgoUtil.removeElementsSameModelId(picked, elems);
			incompatible = AlgoUtil.removeElementsSameModelId(picked, incompatible);
			
			/*partition = AlgoUtil.partitionShared(picked, incompatible, 1);
			elems.addAll(partition.get(0));
			incompatible = partition.get(1);*/
			//
			partition = AlgoUtil.partitionShared(picked, elems, 1);
			elems = partition.get(0);
			incompatible.addAll(partition.get(1));
			for (Element e: best.getElements()){
				partition = AlgoUtil.partitionShared(e, incompatible, best.getSize());
				elems.addAll(partition.get(0));
				incompatible = partition.get(1);
			}
			picked = getMaxElement(elems, best);
			unusedElements.remove(picked);
		}
		return best;
	}
	
	private Element getMaxElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = best.calcWeight(models);
		Element maxElement = null;
		for (Element e: elems){
			Tuple test = best.newExpanded(e, models);
			if (test.calcWeight(models).compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = test.calcWeight(models);
			}
		}
		return maxElement;
	}

	@Override
	public ArrayList<Tuple> getTuplesInMatch() {
		// TODO Auto-generated method stub
		return solution;
	}

	@Override
	public ArrayList<Model> getModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Matchable getMatch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RunResult getRunResult(int numOfModels) {
		return res;
	}

}
