package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
		solution = execute(1);
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
	
	private ArrayList<Tuple> execute(){
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		while(unusedElements.size() > 1){
			for (int i = models.size(); i > 0; i--){
				Element picked = unusedElements.get(unusedElements.size());
				unusedElements.remove(unusedElements.size());
				Tuple bestTuple = getBestTuple(new ArrayList<Element>(unusedElements), picked, 1);
				result.add(bestTuple);
			}
		}
		return result;
	}
	
	private ArrayList<Tuple> execute(int threshold){
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		for (int i = threshold; i > 0; i--){
			for (int j = unusedElements.size() - 1; j >= 0; j--){
				Element picked = unusedElements.get(j);
				Tuple bestTuple = getBestTuple(unusedElements, picked, i);
				if (bestTuple.getSize() != 1){
					for (Element e: bestTuple.getElements())
						unusedElements.remove(e);
					result.add(bestTuple);
				}
			}
		}
		return result;
	}
	
	private Tuple getBestTuple(ArrayList<Element> elems, Element picked, int shared){
		Tuple best = new Tuple();
		while(picked != null){
			best = best.newExpanded(picked, models);
			elems = AlgoUtil.removeElementsSameModelId(picked, elems);
			elems = AlgoUtil.getElementsWithSharedProperties(picked, elems, shared);
			picked = getMaxElement(elems, best);
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
