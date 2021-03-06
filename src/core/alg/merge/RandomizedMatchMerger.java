package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import core.alg.Matchable;
import core.alg.local.BestFoundLocalSearch;
import core.alg.local.FirstFoundLocalSearch;
import core.common.AlgoUtil;
import core.common.ElementComparator;
import core.common.ElementComparator.Sort;
import core.common.ModelComparator;
import core.common.N_WAY;
import core.common.TupleComparator;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.RunResult;

public class RandomizedMatchMerger extends Merger implements Matchable {
	protected ArrayList<Tuple> solution;
	protected TupleTable solutionTable;
	protected ArrayList<Element> seeds;
	protected ArrayList<Element> allElements;
	protected ArrayList<Element> usedSeeds;
	private RunResult res;
	private MergeDescriptor md;
	private long startTime;
	private HumanSimulator hsim;
	boolean noImprove;

	public RandomizedMatchMerger(ArrayList<Model> models, MergeDescriptor md){
		super(models);
		solution = new ArrayList<Tuple>();
		allElements = new ArrayList<Element>();
		seeds = new ArrayList<Element>();
		usedSeeds = new ArrayList<Element>();
		this.md = md;
		hsim = new HumanSimulator(models, md.choose, this);
		noImprove = true;
	}
	
	public void improveSolution(ArrayList<Tuple> prevSolution){
		startTime = System.currentTimeMillis();
		for (Model m: models){
			allElements.addAll(m.getElements());
		}
		solutionTable = new TupleTable(allElements.size());
		for (Tuple t: prevSolution){
			solutionTable.add(t);
		}
		for (Element e: allElements){
			if (e.getContainingTuple().getSize() == 1){
				solutionTable.add(e.getContainingTuple());
			}
		}
		seeds = joinAllModels();
		hsim.play();
	}
	
	public Element getSeed(boolean improved){
		//double gapSeeds = 0;
		//int iterations = 0;
		//int seedsUsed = 0;
		//double firstChangeSum = 0;
		//int numGaps = 0;
		if (md.reshuffle > 0 && improved){
			noImprove = false;
			seeds = joinAllModels();
			if (md.reshuffle == 2){
				for (Element e: usedSeeds)
					seeds.remove(e);
			}
		}
		if (seeds.size() == 0){
			if (noImprove)
				return null;
			noImprove = true;
			usedSeeds = new ArrayList<Element>();
			seeds = joinAllModels();
		}
		Element picked = seeds.remove(0);
		usedSeeds.add(picked);
		return picked;
	}
	
	public void end(){
		solution = solutionTable.getValues();
		try {
			duplicateElements();
		}catch (InvalidSolutionException e){
			e.printStackTrace();
		}
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		/*res.addIterations(iterations);
		if (iterations == 1){
			res.addFirstChangeAvg(0);
			res.addGapAvg(0);
		}
		else{
			res.addFirstChangeAvg(firstChangeSum / (iterations - 1));
			res.addGapAvg(gapSeeds / numGaps);
		}*/
		//res.addSeedsUsed(seedsUsed);
		res.setTitle("Randomized");
		clear();
	}
	
	private void clear(){
		for (Element e: allElements){
			Tuple t = new Tuple();
			t.addElement(e);
			e.setContainingTuple(t);
		}
	}
	
	private ArrayList<Element> joinAllModels(){
		ArrayList<Element> elems = new ArrayList<Element>();
		ArrayList<Tuple> currTuples = solutionTable.getValues();
		// Random
		if (md.seed == 0){
			Collections.shuffle(currTuples, new Random(System.nanoTime()));
			for (Tuple t: currTuples){
				elems.addAll(t.getElements());
			}
		}
		// Size/Weight
		else if (md.seed < 3){
			Collections.sort(currTuples, new TupleComparator(md.asc, md.seed == 1));
			for (Tuple t: currTuples){
				elems.addAll(t.getElements());
			}
		}
		// By Bar
		else{
			generateBars(new HashSet<Element>(allElements));
			elems.addAll(allElements);
			Collections.sort(elems, new ElementComparator(md.asc, Sort.BAR, new Tuple(), models));
		}
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
	private ArrayList<HashMap> getPropFreqs(ArrayList<Element> elems){
		HashMap<String, Integer> propFreq = new HashMap<String, Integer>();
		HashMap<String, Set<String>> propModels = new HashMap<String, Set<String>>();
		for (Element e: elems){
			for (String p: e.getProperties()){
				int count = propFreq.containsKey(p) ? propFreq.get(p) : 0;
				Set<String> pModels = propModels.containsKey(p)? propModels.get(p) : new HashSet<String>();
				pModels.add(e.getModelId());
				propModels.put(p, pModels);
				propFreq.put(p, count + 1);
			}
		}
		ArrayList<HashMap> propProps = new ArrayList<HashMap>();
		propProps.add(propFreq);
		propProps.add(propModels);
		return propProps;
	}

	public void generateBars(Set<Element> elems){
		BigDecimal globalScore = AlgoUtil.calcGroupWeight(solutionTable.getValues());
		for (Element e: elems){
			Tuple contain = e.getContainingTuple();
			Tuple lessTuple = new Tuple();
			lessTuple.setWeight(BigDecimal.ZERO);
			if (contain.getSize() > 1)
				lessTuple = contain.lessExpanded(e, models);
			BigDecimal bar = globalScore.subtract(contain.getWeight().subtract(lessTuple.getWeight()));
			e.setBar(bar);
		}
	}
	
	public void resetContainingTuples(){
		for (Tuple t: solutionTable.getValues()){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
	}
	
	public void duplicateElements() throws InvalidSolutionException{
		ArrayList<Element> usedElements = new ArrayList<Element>();
		for (Tuple t: solutionTable.getValues()){
			for (Element e: t.getElements()){
				if (usedElements.contains(e)){
					//System.out.println("detected duplicate element:" + e);
					throw new InvalidSolutionException("found duplicate element:" + e);
				}
				usedElements.add(e);
			}
		}
	}
	
	
	public void keepValidElements(Set<Element> elems, Set<Element> incompatible, Tuple tup){
		BigDecimal weight = tup.getWeight();
		Tuple compare;
		Set<Element> elemsCopy = new HashSet<Element>(elems);
		for (Element e: elemsCopy){
			int index = AlgoUtil.commonModel(e, tup);
			compare = tup.newExpanded(e, models);
			if (index != -1){
				 compare = compare.lessExpanded(tup.getElements().get(index), models);
			}
			if (compare.calcWeight(models).compareTo(weight) <= 0){
				elems.remove(e);
				incompatible.add(e);
			}
		}
	}
	
//	private boolean buildNewTuple(ArrayList<Element> elems, Element current){
//		/**
//		 * Variation of the randomized merge algorithm. Instead of only considering elements
//		 * that have one shared property with the current element, the algorithm reconsiders elements
//		 * to append to the tuple if they have at least bestTuple.size() shared properties with any of
//		 * elements in the current tuple.
//		 */
//		//System.out.println(AlgoUtil.calcGroupWeight(solutionTable.getValues()));
//		TupleTable currSolution = new TupleTable(solutionTable.size());
//		for (Tuple t: solutionTable.getValues()){
//			currSolution.add(t);
//		}
//		BigDecimal maxIncrease = new BigDecimal(0.00002, N_WAY.MATH_CTX);
//		BigDecimal currIncrease = BigDecimal.ZERO;
//		Tuple containingTup = current.getContainingTuple();
//		currSolution.remove(containingTup);
//		if (containingTup.getSize() > 1){
//			currIncrease = currIncrease.subtract(containingTup.getWeight());
//			containingTup = containingTup.lessExpanded(current, models);
//			for (Element e: containingTup.getElements()){
//				e.setContainingTuple(containingTup);
//			}
//			currSolution.add(containingTup);
//			currIncrease = currIncrease.add(containingTup.getWeight());
//		}
//		Tuple currTuple = new Tuple().newExpanded(current, models);
//		current.setContainingTuple(currTuple);
//		TupleTable bestSolution = null;
//		ArrayList<Element> bestTupleElements = new ArrayList<Element>();
//		ArrayList<Element> incompatible = new ArrayList<Element>();
//		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
//		while(true){
//			if (!md.switchBuckets || currTuple.getSize() == 1){
//				elems = AlgoUtil.removeElementsSameModelId(current, elems);
//				incompatible = AlgoUtil.removeElementsSameModelId(current, incompatible);
//			}
//			partition = highlightElements(current, elems, incompatible, currTuple);
//			elems = partition.get(0);
//			incompatible = partition.get(1);
//			keepValidElements(elems, incompatible, currTuple);
//			current = chooseElement(elems, currTuple);
//			if (current == null){
//				break;
//			}
//			// Switches element with same model out.
//			Tuple oldTuple = current.getContainingTuple();
//			currSolution.remove(oldTuple);
//			BigDecimal oldWeight = currTuple.getWeight().add(oldTuple.getWeight());
//			if (md.switchBuckets){
//				int commonModel = AlgoUtil.commonModel(current, currTuple);
//				if (commonModel != -1){
//					Element replaced = currTuple.getElements().get(commonModel);
//					currTuple = currTuple.lessExpanded(replaced, models);
//					int priorId = (replaced.resetContainingTupleId());
//					if (priorId < 0 || currSolution.getTuple(priorId) == null){
//						Tuple self = new Tuple();
//						self.addElement(replaced);
//						replaced.setContainingTuple(self);
//						currSolution.add(self);
//					}
//					else{
//						Tuple prior = currSolution.getTuple(priorId);
//						currSolution.remove(prior);
//						prior = prior.newExpanded(replaced, models);
//						for (Element e: prior.getElements()){
//							e.setContainingTuple(prior);
//						}
//						currSolution.add(prior);
//					}
//				}
//			}
//			currTuple = currTuple.newExpanded(current, models);
//			for (Element e: currTuple.getElements()){
//				e.setContainingTuple(currTuple);
//			}
//			// Update tuples of new elements.
//			if (oldTuple.getSize() > 1){
//				oldTuple = oldTuple.lessExpanded(current, models);
//				for (Element e: oldTuple.getElements()){
//					e.setContainingTuple(oldTuple);
//				}
//				currSolution.add(oldTuple);
//			}
//			currIncrease = currIncrease.add(currTuple.getWeight().add(oldTuple.getWeight()).
//					subtract(oldWeight));
//			if (currIncrease.compareTo(maxIncrease) > 0){
//				//System.out.println(currIncrease);
//				bestTupleElements.addAll(currTuple.getElements());
//				bestSolution = new TupleTable(currSolution.size());
//				for (Tuple t: currSolution.getValues()){
//					bestSolution.add(t);
//				}
//				bestSolution.add(currTuple);
//				maxIncrease = currIncrease;
//			}
//		}
//		if (bestSolution != null){
//			solutionTable = bestSolution;
//			resetContainingTuples();
//			return true;
//		}
//		else{
//			resetContainingTuples();
//			return false;
//		}
//	}
	
	public ArrayList<Set<Element>> highlightElements(Element picked, Set<Element> elems, 
			Set<Element> incompatible, Tuple current){
		// One with all
		if (md.highlight == 0){
			return AlgoUtil.partitionShared(picked, elems, 1);
		}
		// One with one
		else if (md.highlight == 1){
			elems.addAll(incompatible);
			return AlgoUtil.getElementsWithSharedProperties(current, elems, 1);
		}
		// Tuple.size with tuple
		else if (md.highlight == 2){
			elems.addAll(incompatible);
			return AlgoUtil.getElementsWithSharedProperties(current, elems, current.getSize());
		}
		// No highlight
		else if (md.highlight == 3){
			ArrayList<Set<Element>> partition = new ArrayList<Set<Element>>();
			partition.add(elems);
			partition.add(incompatible);
			return partition;
		}
		// Highlight by pairwise
		else{
			Set<Tuple> compatible = new HashSet<Tuple>();
			incompatible.addAll(elems);
			elems = new HashSet<Element>();
			for (Element e: incompatible){
				if (!compatible.contains(e.getContainingTuple())){
					BigDecimal pw = BigDecimal.ZERO;
					for (Element e2: e.getContainingTuple().getElements()){
						Tuple rel = current.newExpanded(e2, models);
						pw = pw.add(rel.getWeight());
					}
					if (pw.compareTo(BigDecimal.ZERO) > 0)
						compatible.add(e.getContainingTuple());
				}
			}
			//System.out.println(compatible.size());
			for (Tuple t: compatible){
				for (Element e2: t.getElements()){
					if (incompatible.contains(e2)){
						incompatible.remove(e2);
						elems.add(e2);
					}
				}
			}
			ArrayList<Set<Element>> partition = new ArrayList<Set<Element>>();
			partition.add(elems);
			partition.add(incompatible);
			return partition;
		}
		
	}
	
	public void updateSolution(TupleTable newSolution){
		solutionTable = (TupleTable) newSolution;
	}
	
	@Override
	public ArrayList<Tuple> getTuplesInMatch() {
		// TODO Auto-generated method stub
		return solution;
	}

	@Override
	public ArrayList<Model> getModels() {
		// TODO Auto-generated method stub
		return models;
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
	
	public class TupleTable extends Hashtable<Integer, LinkedList<Tuple>>{
		
		private static final long serialVersionUID = 1L;
		public TupleTable (int loadFactor){
			super(loadFactor);
		}
		
		public Tuple getTuple(Integer key){
			Tuple desired = null;
			LinkedList<Tuple> bucket = this.get(key);
			for (int i = bucket.size() - 1; i >= 0; i--){
				if (bucket.get(i).getId() == key){
					desired = bucket.get(i);
				}
			}
			return desired;
		}
		
		public LinkedList<Tuple> add(Tuple t){
			Integer key = t.getId();
			LinkedList<Tuple> bucket = null;
			if (this.containsKey(key)){
				bucket = this.get(key);
				bucket.add(t);
				this.put(key, bucket);
			}
			else{
				bucket = new LinkedList<Tuple>();
				bucket.add(t);
				this.put(key, bucket);
			}
			return bucket;
		}
		
		public ArrayList<Tuple> getValues(){
			ArrayList<Tuple> valueSet = new ArrayList<Tuple>();
			for (LinkedList<Tuple> llt: this.values()){
				for (Tuple t: llt){
					valueSet.add(t);
				}
			}
			return valueSet;
		}
		public Tuple remove(Tuple t){
			Integer key = t.getId();
			Tuple removed = null;
			LinkedList<Tuple> bucket = this.get(key);
			for (int i = bucket.size() - 1; i >= 0; i--){
			//	System.out.println(bucket.get(i));
				if (bucket.get(i).getId() == key){
					removed = bucket.remove(i);
					break;
				}
			}
			return removed;
		}
	}
	
	public class InvalidSolutionException extends Exception {
		private static final long serialVersionUID = 1L;

		public InvalidSolutionException(String message) {
	        super(message);
	    }
	}

}
