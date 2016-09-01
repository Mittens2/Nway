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
	protected ArrayList<Element> unusedElements;
	protected ArrayList<Element> allElements;
	private RunResult res;
	private MergeDescriptor md;

	public RandomizedMatchMerger(ArrayList<Model> models, MergeDescriptor md){
		super(models);
		solution = new ArrayList<Tuple>();
		allElements = new ArrayList<Element>();
		unusedElements = new ArrayList<Element>();
		this.md = md;
	}
	
	public void run(){
		long startTime = System.currentTimeMillis();
		joinAllModels();
		allElements.addAll(unusedElements);
		execute();
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.setTitle("Randomized");
		clear();
	}
	
	private void clear(){
		for (Element e: allElements){
			Tuple t = new Tuple();
			t.addElement(e);
			e.setContaintingTuple(t);
		}
	}
	private void joinAllModels(){
		/**
		 * Function to implement seeding strategy.
		 */
		ArrayList<Element> elems = new ArrayList<Element>();
		if (md.seed == 4){
			Collections.shuffle(models, new Random(System.nanoTime()));
			Collections.sort(models, new ModelComparator(md.asc));
		}
		for (Model m: models){
			ArrayList<Element> modelElems = new ArrayList<Element>(m.getElements());
			if (md.seed == 3 || md.seed == 4){
				Collections.shuffle(modelElems, new Random(System.nanoTime()));
				Collections.sort(modelElems, new ElementComparator(md.elementAsc, false));
			}
			elems.addAll(modelElems);
		}
		if (md.seed == 0){
			Collections.shuffle(elems, new Random(System.nanoTime()));
		}
		else if (md.seed == 1){
			Collections.shuffle(elems, new Random(System.nanoTime()));
			Collections.sort(elems, new ElementComparator(md.elementAsc, false));
		}
		else if (md.seed == 5){
			ArrayList<HashMap> propProps = getPropFreqs(elems);
			HashMap<String, Integer> propFreq = (HashMap<String, Integer>) propProps.get(0);
			HashMap<String, Set<String>> propModels = (HashMap<String, Set<String>>) propProps.get(1);
			for (Element e: elems){
				int sum = 0;
				Set<String> eModels = new HashSet<String>();
				for (String p: e.getProperties()){
					sum += propFreq.get(p);
					eModels.addAll(propModels.get(p));
				}
				double score = sum / e.getProperties().size() * Math.log(1 + eModels.size() / models.size());
				e.setPropScore(score);
			}
			Collections.shuffle(elems, new Random(System.nanoTime()));
			Collections.sort(elems, new ElementComparator(md.elementAsc, true));
		}
		allElements.addAll(elems);
		unusedElements.addAll(elems);
		Collections.shuffle(allElements, new Random(System.nanoTime()));
		//return elems;
	}
	
	private ArrayList<Element> joinAllModels2(){
		ArrayList<Element> elems = new ArrayList<Element>();
		if (md.seed == 0){
			Collections.shuffle(solution, new Random(System.nanoTime()));
			for (Tuple t: solution){
				elems.addAll(t.getElements());
			}
		}
		else if (md.seed < 3){
			Collections.sort(solution, new TupleComparator(md.asc, md.seed == 1));
			for (Tuple t: solution){
				elems.addAll(t.getElements());
			}
		}
		else{
			generateBars(solution);
			elems.addAll(allElements);
			Collections.sort(elems, new ElementComparator(md.asc, true));
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

	private void generateBars(ArrayList<Tuple> tuples){
		BigDecimal globalScore = AlgoUtil.calcGroupWeight(tuples);
		for (Tuple t: tuples){
			ArrayList<Element> tupleElems = t.getElements();
			if (t.getSize() > 1){
				for (int i = tupleElems.size() - 1; i >= 0; i--){
					Element curr = tupleElems.get(i);
					Tuple lessTuple = t.lessExpanded(curr, models);
					BigDecimal bar = globalScore.subtract(t.calcWeight(models).subtract(lessTuple.calcWeight(models)));
					curr.setBar(bar);
				}
			}
		}
	}
	
	private void resetContainingTuples(){
		for (Tuple t: solutionTable.getValues()){
			for (Element e: t.getElements()){
				e.setContaintingTuple(t);
			}
		}
	}
	
	private void execute(){
		/**
		 * Executes an instance of the randomized merge algorithm (DumbHuman).
		 * 
		 * @return The list of Tuples derived from performing randomized merge on given models.
		 */
		while(unusedElements.size() > 0){
			Element picked = unusedElements.get(0);
			unusedElements.remove(0);
			Tuple bestTuple = new Tuple().newExpanded(picked, models);
			if (md.switchTuples){
				picked.setContaintingTuple(bestTuple);
				bestTuple = buildTuple(new ArrayList<Element>(allElements), bestTuple);
			}
			else{
				bestTuple = buildTuple(new ArrayList<Element>(unusedElements), bestTuple);
			}
			solution.add(bestTuple);
		}
	}
	
	public void improveSolution(ArrayList<Tuple> prevSolution){
		long startTime = System.currentTimeMillis();
		// Puts all elements in allElements so can add elements not belonging to a tuple in their own tuple.
		joinAllModels();
		solution.addAll(prevSolution);
		for (Element e: allElements){
			if (e.getContaingTuple().getSize() == 1){
				solution.add(e.getContaingTuple());
			}
		}
		unusedElements = joinAllModels2();
		int iterations = 0;
		int count = 0;
		BigDecimal currentScore = AlgoUtil.calcGroupWeight(solution).subtract(new BigDecimal(0.01));
		while(count != 1){
			currentScore = AlgoUtil.calcGroupWeight(solution);
			while (System.currentTimeMillis() - startTime < (1000 * 60 * 5) && unusedElements.size() > 0){
				Element picked = unusedElements.get(0);
				Tuple currTuple = picked.getContaingTuple();
				unusedElements.removeAll(currTuple.getElements());
				ArrayList<Element> allElemsCopy = new ArrayList<Element>(allElements);
				allElemsCopy.removeAll(currTuple.getElements());
				solution.remove(currTuple);
				solution.add(buildTuple(new ArrayList<Element>(allElemsCopy), currTuple));
			}
			if (currentScore.compareTo(AlgoUtil.calcGroupWeight(solution)) >= 0){
				count++;
			}
			else{
				count = 0;
			}
			unusedElements.addAll(allElements);
			iterations++;
		}
		System.out.println("iterations: " + iterations);
		ArrayList<Element> usedElements = new ArrayList<Element>();
		for (Tuple t: solution){
			for (Element e: t.getElements()){
				if (usedElements.contains(e))
					System.out.println(e);
				usedElements.add(e);
			}
		}
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.addIterations(iterations);
		res.setTitle("Randomized");
		clear();
	}
	
	public void improveSolution2(ArrayList<Tuple> prevSolution){
		long startTime = System.currentTimeMillis();
		// Puts all elements in allElements so can add elements not belonging to a tuple in their own tuple.
		for (Model m: models){
			allElements.addAll(m.getElements());
		}
		solutionTable = new TupleTable(prevSolution.size() * 7);
		for (Tuple t: prevSolution){
			solutionTable.add(t);
		}
		solution.addAll(prevSolution);
		for (Element e: allElements){
			if (e.getContaingTuple().getSize() == 1){
				solutionTable.add(e.getContaingTuple());
				solution.add(e.getContaingTuple());
			}
		}
		int gapSeeds = 0;
		int iterations = 0;
		double seedsUsed = 0;
		double firstChangeSum = 0;
		int numGaps = 0;
	    boolean changed = true;
	    ArrayList<Element> toRemoveElems = new ArrayList<Element>();
		while(changed){
			iterations++;
			changed = false;
			unusedElements = joinAllModels2();
			while (System.currentTimeMillis() - startTime < (1000 * 60 * 5) && unusedElements.size() > 0){
				Element picked = unusedElements.remove(0);
				toRemoveElems.add(picked);
				seedsUsed++;
				ArrayList<Element> allElemsCopy = new ArrayList<Element>(allElements);
				if (buildNewTuple(allElemsCopy, picked)){
					if (!changed){
						changed = true;
						firstChangeSum += toRemoveElems.size();
					}
					gapSeeds += seedsUsed;
					seedsUsed = 0;
					numGaps++;
					if (md.reshuffle > 0){
						solution = new ArrayList<Tuple>();
						solution.addAll(solutionTable.getValues());
						unusedElements = joinAllModels2();
						if (md.reshuffle == 2){
							for (Element e: toRemoveElems)
								unusedElements.remove(e);
						}
						else{
							changed = false;
							iterations++;
						}
					}
					toRemoveElems = new ArrayList<Element>();
				}
			}
			solution = new ArrayList<Tuple>();
			solution.addAll(solutionTable.getValues());
		}
		System.out.println("iterations: " + iterations);
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.addIterations(iterations);
		if (iterations == 1){
			res.addFirstChangeAvg(0);
			res.addGapAvg(0);
		}
		else{
			res.addFirstChangeAvg(firstChangeSum / (iterations - 1));
			res.addGapAvg(gapSeeds / numGaps);
		}
		res.addSeedsUsed(seedsUsed);
		res.setTitle("Randomized");
		clear();
	}
	
	private Tuple buildTuple(ArrayList<Element> elems, Tuple current){
		/**
		 * HSim as initally implele
		 */
		ArrayList<Element> incompatible = new ArrayList<Element>();
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		for (Element e: current.getElements()){
			if (!md.switchBuckets){
				elems = AlgoUtil.removeElementsSameModelId(e, elems);
				incompatible = AlgoUtil.removeElementsSameModelId(e, incompatible);
			}
			partition = highlightElements(e, elems, incompatible, current);
			elems = partition.get(0);
			incompatible = partition.get(1);
		}
		
		while(true){
			// Consider all elements that also have tuple.size properties in common with any one of elements in tuple.
			Element picked = chooseElement(elems, current);
			if (picked == null){
				break;
			}
			Element replaced = null;
			if (md.switchBuckets){
				int commonModel = AlgoUtil.commonModel(picked, current);
				if (commonModel != -1){
					replaced = current.getElements().get(commonModel);
					if (current.getSize() > 1)
						current = current.lessExpanded(replaced, models);
					else
						current = new Tuple();
					unusedElements.add(replaced);
					elems.add(replaced);
					Tuple self = new Tuple();
					self.addElement(replaced);
					replaced.setContaintingTuple(self);
				}
			}
			unusedElements.remove(picked);
			elems.remove(picked);
			current = current.newExpanded(picked, models);
			if (md.switchTuples){
				Tuple oldTuple = picked.getContaingTuple();
				solution.remove(oldTuple);
				for (Element e: current.getElements()){
					e.setContaintingTuple(current);
				}
				if (oldTuple.getSize() > 1){
					oldTuple = oldTuple.lessExpanded(picked, models);
					for (Element e: oldTuple.getElements()){
						e.setContaintingTuple(oldTuple);
					}
					solution.add(oldTuple);
				}
			}
			if (!md.switchBuckets){
				elems = AlgoUtil.removeElementsSameModelId(picked, elems);
				incompatible = AlgoUtil.removeElementsSameModelId(picked, incompatible);
			}
			// System.out.println(elems.size());
			partition = highlightElements(picked, elems, incompatible, current);
			elems = partition.get(0);
			incompatible = partition.get(1);
			//System.out.println(current);
		}
		return current;
	}
	
	private void keepValidElements(ArrayList<Element> elems, ArrayList<Element> incompatible, Tuple tup){
		BigDecimal weight = tup.getWeight();
		Tuple compare;
		for (int i = elems.size() - 1; i >= 0; i--){
			int index = AlgoUtil.commonModel(elems.get(i), tup);
			compare = tup.newExpanded(elems.get(i), models);
			if (index != -1){
				 compare = compare.lessExpanded(tup.getElements().get(index), models);
			}
			if (compare.calcWeight(models).compareTo(weight) <= 0){
				incompatible.add(elems.remove(i));
			}
		}
	}
	
	private boolean buildNewTuple(ArrayList<Element> elems, Element current){
		/**
		 * Variation of the randomized merge algorithm. Instead of only considering elements
		 * that have one shared property with the current element, the algorithm reconsiders elements
		 * to append to the tuple if they have at least bestTuple.size() shared properties with any of
		 * elements in the current tuple.
		 */
		//System.out.println(AlgoUtil.calcGroupWeight(solutionTable.getValues()));
		TupleTable currSolution = new TupleTable(solutionTable.size());
		for (Tuple t: solutionTable.getValues()){
			currSolution.add(t);
		}
		BigDecimal maxIncrease = new BigDecimal(0.00002, N_WAY.MATH_CTX);
		BigDecimal currIncrease = BigDecimal.ZERO;
		Tuple containingTup = current.getContaingTuple();
		currSolution.remove(containingTup);
		if (containingTup.getSize() > 1){
			currIncrease = currIncrease.subtract(containingTup.getWeight());
			containingTup = containingTup.lessExpanded(current, models);
			for (Element e: containingTup.getElements()){
				e.setContaintingTuple(containingTup);
			}
			currSolution.add(containingTup);
			currIncrease = currIncrease.add(containingTup.getWeight());
		}
		Tuple currTuple = new Tuple().newExpanded(current, models);
		current.setContaintingTuple(currTuple);
		TupleTable bestSolution = null;
		ArrayList<Element> bestTupleElements = new ArrayList<Element>();
		ArrayList<Element> incompatible = new ArrayList<Element>();
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		while(true){
			if (!md.switchBuckets || currTuple.getSize() == 1){
				elems = AlgoUtil.removeElementsSameModelId(current, elems);
				incompatible = AlgoUtil.removeElementsSameModelId(current, incompatible);
			}
			partition = highlightElements(current, elems, incompatible, currTuple);
			elems = partition.get(0);
			incompatible = partition.get(1);
			keepValidElements(elems, incompatible, currTuple);
			current = chooseElement(elems, currTuple);
			if (current == null){
				break;
			}
			// Switches element with same model out.
			Tuple oldTuple = current.getContaingTuple();
			currSolution.remove(oldTuple);
			BigDecimal oldWeight = currTuple.getWeight().add(oldTuple.getWeight());
			if (md.switchBuckets){
				int commonModel = AlgoUtil.commonModel(current, currTuple);
				if (commonModel != -1){
					Element replaced = currTuple.getElements().get(commonModel);
					currTuple = currTuple.lessExpanded(replaced, models);
					int priorId = (replaced.resetContainingTupleId());
					if (priorId < 0 || currSolution.getTuple(priorId) == null){
						Tuple self = new Tuple();
						self.addElement(replaced);
						replaced.setContaintingTuple(self);
						currSolution.add(self);
					}
					else{
						Tuple prior = currSolution.getTuple(priorId);
						currSolution.remove(prior);
						prior = prior.newExpanded(replaced, models);
						for (Element e: prior.getElements()){
							e.setContaintingTuple(prior);
						}
						currSolution.add(prior);
					}
				}
			}
			currTuple = currTuple.newExpanded(current, models);
			for (Element e: currTuple.getElements()){
				e.setContaintingTuple(currTuple);
			}
			// Update tuples of new elements.
			if (oldTuple.getSize() > 1){
				oldTuple = oldTuple.lessExpanded(current, models);
				for (Element e: oldTuple.getElements()){
					e.setContaintingTuple(oldTuple);
				}
				currSolution.add(oldTuple);
			}
			currIncrease = currIncrease.add(currTuple.getWeight().add(oldTuple.getWeight()).
					subtract(oldWeight));
			if (currIncrease.compareTo(maxIncrease) > 0){
				//System.out.println(currIncrease);
				bestTupleElements.addAll(currTuple.getElements());
				bestSolution = new TupleTable(currSolution.size());
				for (Tuple t: currSolution.getValues()){
					bestSolution.add(t);
				}
				bestSolution.add(currTuple);
				maxIncrease = currIncrease;
			}
		}
		if (bestSolution != null){
			solutionTable = bestSolution;
			resetContainingTuples();
			/*for (Element e: bestTupleElements){
				unusedElements.remove(e);
			}*/
			/*ArrayList<Element> usedElements = new ArrayList<Element>();
			for (Tuple t: solutionTable.getValues()){
				for (Element e: t.getElements()){
					if (usedElements.contains(e))
						System.out.println(e);
					usedElements.add(e);
				}
			}*/
			return true;
		}
		else{
			resetContainingTuples();
			return false;
		}
	}
	
	private ArrayList<ArrayList<Element>> highlightElements(Element picked, ArrayList<Element> elems, 
			ArrayList<Element> incompatible, Tuple current){
		if (md.highlight == 0){
			return AlgoUtil.partitionShared(picked, elems, 1);
		}
		else if (md.highlight == 1){
			elems.addAll(incompatible);
			return AlgoUtil.getElementsWithSharedProperties(current, elems, 1);
		}
		else if (md.highlight == 2){
			elems.addAll(incompatible);
			return AlgoUtil.getElementsWithSharedProperties(current, elems, current.getSize());
		}
		else{
			ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
			partition.add(elems);
			partition.add(incompatible);
			return partition;
		}
	}
	
	private Element chooseElement(ArrayList<Element> elems, Tuple best){
		if (elems.size() == 0){
			return null;
		}
		/*if (md.choose == 0){ //Choose element that increases the score the most.
			return pickMaxElement(elems, best);
		}
		else if (md.choose == 1){
			return pickRandomElement(elems, best);
		}
		else{
			return pickWorstElement(elems, best);
		}*/
		if (md.choose == 0){
			return pickBestLocalElement(elems, best);
		}
		else if (md.choose == 1){
			return pickBestGlobalElement(elems, best);
		}
		else{
			return pickLowestBarElement(elems);
		}
		/*else if (md.choose == 1){
			return pickElementShareProps(elems, best);
			//return pickElementLastShareProps(elems, best);
		}*/
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
			if (md.switchBuckets){
				int index = AlgoUtil.commonModel(e, best);
				if (index != -1){
					test = test.lessExpanded(best.getElements().get(index), models);
				}
			}
			BigDecimal currWeight;
			Tuple ct = e.getContaingTuple();
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
	
	
	private Element pickMaxElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = md.switchTuples? BigDecimal.ZERO : best.calcWeight(models);
		Element maxElement = null;
		//int count = 0;
		for (Element e: elems){
			int index = AlgoUtil.commonModel(e, best);
			Tuple test;
			test = best.newExpanded(e, models);
			if (index != -1){
				test = test.lessExpanded(best.getElements().get(index), models);
			}
			BigDecimal currWeight;
			if (md.switchTuples){
				Tuple ct = e.getContaingTuple();
				Tuple ctLess = ct.getSize() > 1 ? ct.lessExpanded(e, models) : ct;
				currWeight = (ctLess.getWeight().add(test.getWeight()))
						.subtract(ct.getWeight().add(best.getWeight()));
				
			}
			else currWeight = test.getWeight();
			//if (currWeight.compareTo(BigDecimal.ZERO) > 0)
				//count++;
			if (currWeight.compareTo(maxWeight) > 0){
				//System.out.println(currWeight + "," + maxWeight);
				maxElement = e;
				maxWeight = currWeight;
			}
		}
		//System.out.println(count);
		return maxElement;
	}
	
	private Element pickRandomElement(ArrayList<Element> elems, Tuple best){
		ArrayList<Integer> randList = new ArrayList<Integer>();
		for (int i = 0; i < elems.size(); i++){
			randList.add(i);
		}
		Collections.shuffle(randList);
		while (randList.size() > 0){
			Element random = elems.get(randList.get(randList.size() - 1));
			randList.remove(randList.size() - 1);
			Tuple test = best.newExpanded(random, models);
			BigDecimal currWeight;
			if (md.switchTuples){
				Tuple ct = random.getContaingTuple();
				Tuple ctLess = ct.getSize() > 1 ? ct.lessExpanded(random, models) : ct;
				currWeight = (ctLess.calcWeight(models).add(test.calcWeight(models)))
						.subtract(ct.calcWeight(models).add(best.calcWeight(models)));
			}
			else currWeight = test.calcWeight(models).subtract(best.calcWeight(models));
			if (currWeight.compareTo(BigDecimal.ZERO) > 0)
				return random;
		}
		return null;
	}
	
	private Element pickWorstElement(ArrayList<Element> elems, Tuple best){
		BigDecimal minWeight = BigDecimal.TEN;
		Element minElement = null;
		for (Element e: elems){
			Tuple test = best.newExpanded(e, models);
			BigDecimal currWeight;
			if (md.switchTuples){
				Tuple ct = e.getContaingTuple();
				Tuple ctLess = ct.getSize() > 1 ? ct.lessExpanded(e, models) : ct;
				currWeight = (ctLess.calcWeight(models).add(test.calcWeight(models)))
						.subtract(ct.calcWeight(models).add(best.calcWeight(models)));
				
			}
			else currWeight = test.calcWeight(models).subtract(best.calcWeight(models));
			if (currWeight.compareTo(BigDecimal.ZERO) > 0 && currWeight.compareTo(minWeight) < 0){
				minElement = e;
				minWeight = currWeight;
			}
		}
		return minElement;
	}
	
	private Element pickElementShareProps(ArrayList<Element> elems, Tuple best){
		Element maxElement = null;
		int maxElementSharedProps = Integer.MIN_VALUE;
		for (Element e: elems){
			ArrayList<Element> tupleElems = new ArrayList<Element>(best.getElements());
			tupleElems.add(e);
			HashMap<String, Integer> propFreqs = getSortedProperties(tupleElems);
			int sharedProps = 0;
			for (String p: e.getProperties()){
				sharedProps += propFreqs.get(p);
			}
			if (sharedProps > maxElementSharedProps){
				Tuple test = best.newExpanded(e, models);
				BigDecimal currWeight;
				if (md.switchTuples){
					Tuple ct = e.getContaingTuple();
					Tuple ctLess = ct.getSize() > 1 ? ct.lessExpanded(e, models) : ct;
					currWeight = (ctLess.calcWeight(models).add(test.calcWeight(models)))
							.subtract(ct.calcWeight(models).add(best.calcWeight(models)));
					
				}
				else currWeight = test.calcWeight(models).subtract(best.calcWeight(models));
				if (currWeight.compareTo(BigDecimal.ZERO) > 0){
				//if (test.calcWeight(models).compareTo(best.calcWeight(models)) > 0){
					maxElementSharedProps = sharedProps;
					maxElement = e;
				}
			}
		}
		return maxElement; 
	}
	
	private Element pickElementLastShareProps(ArrayList<Element> elems, Tuple best){
		int maxProps = 0;
		Element maxElement = null;
		for (Element tupleE: best.getElements()){
			for (Element e: elems){
				int commonProps = AlgoUtil.getCommonProperties(tupleE, e);
				if (commonProps > maxProps){
					maxProps = commonProps;
					maxElement = e;
				}
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

}
