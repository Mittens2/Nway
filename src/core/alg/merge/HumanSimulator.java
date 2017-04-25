package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;

import core.alg.merge.RandomizedMatchMerger.TupleTable;
import core.common.AlgoUtil;
import core.common.N_WAY;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class HumanSimulator {
	
	private enum Strategy{
		LOCAL,
		GLOBAL,
		BAR
	}
	
	private ArrayList<Model> models;
	private boolean switchBuckets;
	private Strategy strategy;
	private RandomizedMatchMerger rmm;
	private int timeout = 3000;
	
	public HumanSimulator(ArrayList<Model> models, int strategy, boolean switchBuckets, RandomizedMatchMerger rmm){
		this.models = models;
		switch (strategy){
		case 0: this.strategy = Strategy.LOCAL;
			break;
		case 1: this.strategy = Strategy.GLOBAL;
			break;
		case 2: this.strategy = Strategy.BAR;
			break;
		}
		this.switchBuckets = switchBuckets;
		this.rmm = rmm;
		
	}
	
	public void play(){
		long startTime = System.currentTimeMillis();
		Element seed = rmm.getSeed(false);
		while (seed != null && System.currentTimeMillis() - startTime < timeout){
			ArrayList<Element> elems = new ArrayList<Element>(rmm.allElements);
			TupleTable currSolution = rmm.new TupleTable(rmm.solutionTable.size());
			for (Tuple t: rmm.solutionTable.getValues()){
				currSolution.add(t);
			}
			BigDecimal maxIncrease = new BigDecimal(0.00002, N_WAY.MATH_CTX);
			BigDecimal currIncrease = BigDecimal.ZERO;
			Tuple containingTup = seed.getContainingTuple();
			currSolution.remove(containingTup);
			if (containingTup.getSize() > 1){
				currIncrease = currIncrease.subtract(containingTup.getWeight());
				containingTup = containingTup.lessExpanded(seed, models);
				for (Element e: containingTup.getElements()){
					e.setContainingTuple(containingTup);
				}
				currSolution.add(containingTup);
				currIncrease = currIncrease.add(containingTup.getWeight());
			}
			Tuple currTuple = new Tuple().newExpanded(seed, models);
			seed.setContainingTuple(currTuple);
			TupleTable bestSolution = null;
			ArrayList<Element> bestTupleElements = new ArrayList<Element>();
			ArrayList<Element> incompatible = new ArrayList<Element>();
			ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
			while(true){
				if (!switchBuckets || currTuple.getSize() == 1){
					elems = AlgoUtil.removeElementsSameModelId(seed, elems);
					incompatible = AlgoUtil.removeElementsSameModelId(seed, incompatible);
				}
				partition = rmm.highlightElements(seed, elems, incompatible, currTuple);
				elems = partition.get(0);
				incompatible = partition.get(1);
				rmm.keepValidElements(elems, incompatible, currTuple);
				seed = chooseElement(elems, currTuple);
				if (seed == null){
					break;
				}
				// Switches element with same model out.
				Tuple oldTuple = seed.getContainingTuple();
				currSolution.remove(oldTuple);
				BigDecimal oldWeight = currTuple.getWeight().add(oldTuple.getWeight());
				if (switchBuckets){
					int commonModel = AlgoUtil.commonModel(seed, currTuple);
					if (commonModel != -1){
						Element replaced = currTuple.getElements().get(commonModel);
						currTuple = currTuple.lessExpanded(replaced, models);
						int priorId = (replaced.resetContainingTupleId());
						if (priorId < 0 || currSolution.getTuple(priorId) == null){
							Tuple self = new Tuple();
							self.addElement(replaced);
							replaced.setContainingTuple(self);
							currSolution.add(self);
						}
						else{
							Tuple prior = currSolution.getTuple(priorId);
							currSolution.remove(prior);
							prior = prior.newExpanded(replaced, models);
							for (Element e: prior.getElements()){
								e.setContainingTuple(prior);
							}
							currSolution.add(prior);
						}
					}
				}
				currTuple = currTuple.newExpanded(seed, models);
				for (Element e: currTuple.getElements()){
					e.setContainingTuple(currTuple);
				}
				// Update tuples of new elements.
				if (oldTuple.getSize() > 1){
					oldTuple = oldTuple.lessExpanded(seed, models);
					for (Element e: oldTuple.getElements()){
						e.setContainingTuple(oldTuple);
					}
					currSolution.add(oldTuple);
				}
				currIncrease = currIncrease.add(currTuple.getWeight().add(oldTuple.getWeight()).
						subtract(oldWeight));
				if (currIncrease.compareTo(maxIncrease) > 0){
					//System.out.println(currIncrease);
					bestTupleElements.addAll(currTuple.getElements());
					bestSolution = rmm.new TupleTable(currSolution.size());
					for (Tuple t: currSolution.getValues()){
						bestSolution.add(t);
					}
					bestSolution.add(currTuple);
					maxIncrease = currIncrease;
				}
			}
			if (bestSolution != null){
				rmm.updateSolution(bestSolution);
			}
			rmm.resetContainingTuples();
			seed = rmm.getSeed(bestSolution != null);
		}
		rmm.end();
	}
	private Element chooseElement(ArrayList<Element> elems, Tuple best){
		if (elems.size() == 0){
			return null;
		}
		if (strategy == Strategy.LOCAL){
			return pickBestLocalElement(elems, best);
		}
		else if (strategy == Strategy.GLOBAL){
			return pickBestGlobalElement(elems, best);
		}
		else{ //strategy == Strategy.BAR
			return pickLowestBarElement(elems);
		}
	}
	
	private Element pickBestLocalElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = null;
		Element maxElement = null;
		for (Element e: elems){
			int index = AlgoUtil.commonModel(e, best);
			Tuple test = best.newExpanded(e, models);
			if (index != -1){
				test = test.lessExpanded(best.getElements().get(index), models);
			}
			if (maxWeight == null || test.getWeight().compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = test.getWeight();
			}
		}
		return maxElement; 
	}
	
	private Element pickBestGlobalElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = null;
		Element maxElement = null;
		for (Element e: elems){
			Tuple test = best.newExpanded(e, models);
			if (switchBuckets){
				int index = AlgoUtil.commonModel(e, best);
				if (index != -1){
					test = test.lessExpanded(best.getElements().get(index), models);
				}
			}
			BigDecimal currWeight;
			Tuple ct = e.getContainingTuple();
			Tuple ctLess = ct.getSize() > 1 ? ct.lessExpanded(e, models) : ct;
			currWeight = (ctLess.getWeight().add(test.getWeight())
					.subtract(ct.getWeight().add(best.getWeight())));
			if (maxWeight == null || currWeight.compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = currWeight;
			}
		}
		return maxElement;
	}
	
	private Element pickLowestBarElement(ArrayList<Element> elems){
		Element lowestBarElement = elems.get(0);
		for (int i = 0; i < elems.size(); i++){
			Element e = elems.get(i);
			if (e.getBar().compareTo(lowestBarElement.getBar()) < 0){
				lowestBarElement = e;
			}
		}
		return lowestBarElement;
	}
}
