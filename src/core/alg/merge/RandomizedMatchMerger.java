package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
	protected Hashtable<Integer, Tuple> solutionTable;
	protected ArrayList<Element> unusedElements;
	protected ArrayList<Element> allElements;
	private RunResult res;
	private MergeDescriptor md;
	private GeneticAlgorithm geneticAlgorithm;

	public RandomizedMatchMerger(ArrayList<Model> models, MergeDescriptor md){
		super(models);
		solution = new ArrayList<Tuple>();
		allElements = new ArrayList<Element>();
		unusedElements = new ArrayList<Element>();
		this.md = md;
		//geneticAlgorithm = new GeneticAlgorithm(uniRate, mutRate, 2000);
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
				/*if (propFreq.containsKey(p)){
					int count = propFreq.get(p);
					propFreq.put(p, count + 1);
				}
				else{
					//propFreq.put(p, -1);
					propFreq.put(p, 1);
				}*/
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
		for (Tuple t: solution){
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
			//Tuple bestTuple = new Tuple();
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
		/*if (md.randomize){
			result = runGeneticAlgorithm(result);
		}*/
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
		//solutionTable = new Hashtable<Integer, Tuple>(prevSolution.size() * 7);
		/*for (Tuple t: prevSolution){
			solutionTable.put(t.getId() % solutionTable.size(), t);
		}*/
		solution.addAll(prevSolution);
		for (Element e: allElements){
			if (e.getContaingTuple().getSize() == 1){
				//solution.add(e.getContaingTuple());
				solutionTable.put(e.getContaingTuple().getId() % solutionTable.size(), e.getContaingTuple());
			}
		}
		int iterations = 0;
	    boolean changed = true;
		while(changed){
			changed = false;
			unusedElements = joinAllModels2();
			while (System.currentTimeMillis() - startTime < (1000 * 60 * 5) && unusedElements.size() > 0){
				Element picked = unusedElements.remove(0);
				ArrayList<Element> allElemsCopy = new ArrayList<Element>(allElements);
				if (buildNewTuple(allElemsCopy, picked)){
					changed = true;
					//System.out.println(AlgoUtil.calcGroupWeight(solution));
					if (md.reshuffle){
						unusedElements = joinAllModels2();
					}
				}
			}
			//System.out.println(AlgoUtil.calcGroupWeight(solution));
			iterations++;
		}
		System.out.println("iterations: " + iterations);
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.addIterations(iterations);
		res.setTitle("Randomized");
		clear();
	}
	/**
	 * Does randomized merge algorithm described in tex file.
	 * New function more modular.
	 *
	 * @deprecated use {@link #buildTuple()} instead.  
	 */
	@Deprecated
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
			partition = AlgoUtil.partitionShared(picked, elems, 1);
			elems = partition.get(0);
			// Consider all elements that also have tuple.size properties in common with any one of elements in tuple.
			if (md.highlight == 1){
				incompatible.addAll(partition.get(1));
				for (Element e: best.getElements()){
					partition = AlgoUtil.partitionShared(e, incompatible, best.getSize());
					elems.addAll(partition.get(0));
					incompatible = partition.get(1);
				}
			}
			// Consider all elements that have tuple.size elements in common in total with elements in tuple.
			else if (md.highlight == 2){
				incompatible.addAll(partition.get(1));
					partition = AlgoUtil.getElementsWithSharedProperties(best, incompatible, 
							best.getSize());//(int) Math.pow(best.getSize(), 2));
					elems.addAll(partition.get(0));
					incompatible = partition.get(1);
			}
			picked = chooseElement(elems, best);
			unusedElements.remove(picked);
		}
		return best;
	}
	
	private Tuple buildTuple(ArrayList<Element> elems, Tuple current){
		/**
		 * Variation of the randomized merge algorithm. Instead of only considering elements
		 * that have one shared property with the current element, the algorithm reconsiders elements
		 * to append to the tuple if they have at least bestTuple.size() shared properties with any of
		 * elements in the current tuple.
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
		BigDecimal weight = tup.calcWeight(models);
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
		//Hashtable<Integer, Element> elemTable = new Hashtable<Integer, Element>(models.size() * Element.maxModelElems);
		//Hashtable<Integer, Element> incompTable = new Hashtable<Integer, Element>(models.size() * Element.maxModelElems);
		/*for (Element e: elems){
			elemTable.put(e.hashCode(), e);
		}*/
		ArrayList<Tuple> currSolution = new ArrayList<Tuple>();
		currSolution.addAll(solution);
		BigDecimal maxScore = AlgoUtil.calcGroupWeight(solution).add(new BigDecimal(0.00001, N_WAY.MATH_CTX));
		
		Tuple containingTup = current.getContaingTuple();
		currSolution.remove(containingTup);
		if (containingTup.getSize() > 1){
			Tuple newTuple = containingTup.lessExpanded(current, models);
			for (Element e: newTuple.getElements()){
				e.setContaintingTuple(newTuple);
			}
			currSolution.add(newTuple);
		}
		
		Tuple currTuple = new Tuple().newExpanded(current, models);
		current.setContaintingTuple(currTuple);
		
		ArrayList<Tuple> bestSolution = null;
		Tuple bestTuple = null;
		ArrayList<Element> incompatible = new ArrayList<Element>();
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		while(true){
			//System.out.println(currTuple);
			elems.remove(current);
			if (!md.switchBuckets || currTuple.getSize() == 1){
				elems = AlgoUtil.removeElementsSameModelId(current, elems);
				incompatible = AlgoUtil.removeElementsSameModelId(current, incompatible);
			}
			partition = highlightElements(current, elems, incompatible, currTuple);
			elems = partition.get(0);
			incompatible = partition.get(1);
			keepValidElements(elems, incompatible, currTuple);
			current= chooseElement(elems, currTuple);
			if (current == null){
				break;
			}
			if (md.switchBuckets){
				int commonModel = AlgoUtil.commonModel(current, currTuple);
				if (commonModel != -1){
					Element replaced = currTuple.getElements().get(commonModel);
					if (currTuple.getSize() > 1)
						currTuple = currTuple.lessExpanded(replaced, models);
					else
						currTuple = new Tuple();
					unusedElements.add(replaced);
					//elems.add(replaced);
					Tuple self = new Tuple();
					self.addElement(replaced);
					replaced.setContaintingTuple(self);
					currSolution.add(self);
					//Tuple prior = replaced.resetContainingTuple();
					//prior = prior.newExpanded(replaced, models);
					//System.out.println(prior);
					/*for (Element e: prior.getElements()){
						e.setContaintingTuple(prior);
					}*/
				}
			}
			currTuple = currTuple.newExpanded(current, models);
			if (md.switchTuples){
				Tuple oldTuple = current.getContaingTuple();
				currSolution.remove(oldTuple);
				for (Element e: currTuple.getElements()){
					e.setContaintingTuple(currTuple);
				}
				if (oldTuple.getSize() > 1){
					oldTuple = oldTuple.lessExpanded(current, models);
					for (Element e: oldTuple.getElements()){
						e.setContaintingTuple(oldTuple);
					}
					currSolution.add(oldTuple);
				}
			}
			currSolution.add(currTuple);
			if (AlgoUtil.calcGroupWeight(currSolution).compareTo(maxScore) > 0){
				//System.out.println(maxScore + ", " + AlgoUtil.calcGroupWeight(currSolution));
				bestTuple = currTuple;
				bestSolution = new ArrayList<Tuple>();
				bestSolution.addAll(currSolution);
				maxScore = AlgoUtil.calcGroupWeight(bestSolution);
			}
			//System.out.println(currTuple);
			currSolution.remove(currTuple);
		}
		if (bestSolution != null){
			//System.out.println("here");
			solution = bestSolution;
			resetContainingTuples();
			for (Element e: bestTuple.getElements()){
				unusedElements.remove(e);
			}
			/*ArrayList<Element> usedElements = new ArrayList<Element>();
			for (Tuple t: solution){
				for (Element e: t.getElements()){
					if (usedElements.contains(e))
						System.out.println(e);
					usedElements.add(e);
				}
			}*/
			return true;
		}
		else{
			//System.out.println("hare");
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
		/*else if (md.highlight == 1){
			ArrayList<ArrayList<Element>> partition = AlgoUtil.partitionShared(picked, elems, 1);
			elems = partition.get(0);
			incompatible.addAll(partition.get(1));
			for (Element e: current.getElements()){
				partition = AlgoUtil.partitionShared(e, incompatible, current.getSize());
				elems.addAll(partition.get(0));
				incompatible = partition.get(1);
			}
			partition.set(0, elems);
			partition.set(1, incompatible);
			return partition;
		}*/
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
		}
		else{
			return pickElementAboveThreshold(elems, best);
		}*/
	}
	
	private Element pickBestLocalElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = best.calcWeight(models);
		Element maxElement = null;
		for (Element e: elems){
			int index = AlgoUtil.commonModel(e, best);
			Tuple test = best.newExpanded(e, models);
			if (index != -1){
				test = test.lessExpanded(best.getElements().get(index), models);
			}
			if (test.calcWeight(models).compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = test.calcWeight(models);
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
			currWeight = (ctLess.calcWeight(models).add(test.calcWeight(models)))
					.subtract(ct.calcWeight(models).add(best.calcWeight(models)));
			if (maxWeight == null || currWeight.compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = currWeight;
			}
		}
		return maxElement;
	}
	
	private Element pickLowestBarElement(ArrayList<Element> elems){
		Element lowestBarElement = elems.get(0);
		//for (Element e: elems){
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
				currWeight = (ctLess.calcWeight(models).add(test.calcWeight(models)))
						.subtract(ct.calcWeight(models).add(best.calcWeight(models)));
				
			}
			else currWeight = test.calcWeight(models);
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
	
	private Element pickElementAboveThreshold(ArrayList<Element> elems, Tuple best){
		for (Element e: elems){
			Tuple test = best.newExpanded(e, models);
			if (!best.calcWeight(models).equals(BigDecimal.ZERO)){
				if (test.calcWeight(models).divide(best.calcWeight(models), N_WAY.MATH_CTX).compareTo(new BigDecimal(1.1)) > 0)
					return e;
			}
			else if (test.calcWeight(models).compareTo(best.calcWeight(models)) > 0){
				return e;
			}
		}
		return null;
	}
	private ArrayList<Tuple> runGeneticAlgorithm(ArrayList<Tuple> result){
		Population pop = geneticAlgorithm.evolvePopulation(new Population(result), 0);
		return pop.convertToTuples();
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
	public class Individual {
	    private final static int defaultGeneLength = 100;
	    private Element[] genes = new Element[defaultGeneLength];
	    private double fitness = -1;

	    // Create a random individual
	    public Individual(Tuple t) {
	        for (Element e: t.getElements()) {
	        	genes[Integer.parseInt(e.getModelId()) - 1] = e;
	        }
	    }
	    
	    public Element getGene(int index) {
	        return genes[index];
	    }

	    public void setGene(int index, Element value) {
	        genes[index] = value;
	        fitness = -1;
	    }

	    /* Public methods */
	    public int size() {
	        return genes.length;
	    }

	    public double getFitness() {
        	Tuple t = new Tuple();
	    	for (int i = 0; i < size(); i++){
	    		if (genes[i] != null){
	    			t = t.newExpanded(genes[i], models);
	    		}
	    	}
	    	//System.out.println(t.getWeight().doubleValue());
	    	fitness = t.getWeight().doubleValue();
	        return fitness;
	    }


	    @Override
	    public String toString() {
	        String geneString = "";
	        for (int i = 0; i < size(); i++) {
	            geneString += getGene(i);
	        }
	        return geneString;
	    }
	}
	public class IndividualComparator implements Comparator<Individual>{
		@Override
		public int compare(Individual i1, Individual i2) {
			if (i1.getFitness() > i2.getFitness()){
				return -1;
			}
			else if (i1.getFitness() == i2.getFitness()){
				return 0;
			}
			else{
				return 1;
			}
		}
		
	}
	public class Population {
	    private ArrayList<Individual> individuals;
	    private ArrayList<Individual> sortedIndividuals;
	    private ArrayList<Element> mutations;
	    private IndividualComparator cmp = new IndividualComparator();
	    private double fitness = -1;
	    /*
	     * Constructors
	     */
	    // Create a population
	    public Population(ArrayList<Tuple> pop) {
	        // Initialise population
	    	individuals = new ArrayList<Individual>();
	    	mutations = new ArrayList<Element>();
	        for (int i = 0; i < pop.size(); i++) {
	        	Tuple t = pop.get(i);
	        	if (pop.get(i).getSize() == 1){
	        		mutations.add(t.getElements().get(0));
	        	}
	        	else{
	        		addIndividual(t);
	        	}
	        }
	    }

	    /* Getters */
	    public Individual getIndividual(int index) {
	        return individuals.get(index);
	    }

	    /* Public methods */
	    // Get population size
	    public int size() {
	        return individuals.size();
	    }

	    // Save individual
	    public void addIndividual(Tuple t) {
	        individuals.add(new Individual(t));
	    }
	    
	    public void addIndividual(Individual ind){
	    	individuals.add(ind);
	    }
	    
	    public ArrayList<Individual> getSortedIndividuals(){
	    	if (sortedIndividuals == null){
	    		sortedIndividuals = new ArrayList<Individual>(individuals);
	    		Collections.sort(sortedIndividuals, cmp);
	    	}
	    	return new ArrayList<Individual>(sortedIndividuals);
	    }
	    
	    public ArrayList<Individual> getIndividuals(){
	    	return new ArrayList<Individual>(individuals);
	    }
	    
	    public ArrayList<Element> getMutations(){
	    	return mutations;
	    }
	    
	    public void setMutations(ArrayList<Element> mutations){
	    	this.mutations = mutations;
	    }
	    
	    public double getFitness(){
    		fitness = 0;
    		for (Individual i: individuals){
    			fitness += i.getFitness();
    		}
	    	return fitness;
	    }
	    
	    public ArrayList<Tuple> convertToTuples(){
	    	ArrayList<Tuple> tuples = new ArrayList<Tuple>();
	    	for (Individual i: individuals){
	    		Tuple t = new Tuple();
	    		for (int j = 0; j < Individual.defaultGeneLength; j++){
	    			if (i.getGene(j) != null)
	    				t = t.newExpanded(i.getGene(j), models);
	    		}
	    		if (t.getSize() > 0){
	    			tuples.add(t);
	    		}
	    	}
	    	for (Element e: mutations){
	    		Tuple t = new Tuple();
	    		t = t.newExpanded(e, models);
	    		tuples.add(t);
	    	}
	    	return tuples;
	    }

	}
	public class GeneticAlgorithm {
	    /* GA parameters */
	    private double uniformRate = 0.005;
	    private double mutationRate = 0.005;
	    private Random rand = new Random();
	    private int stableCycles = 2000;

	    /* Public methods */
	    public GeneticAlgorithm (double uniformRate, double mutationRate, int cycles){
	    	this.uniformRate = uniformRate;
	    	this.mutationRate = mutationRate;
	    	this.stableCycles = cycles;
	    }
	    // Evolve a population
	    public Population evolvePopulation(Population pop, int stablePops) {
	    	if (stablePops == stableCycles){
	    		return pop;
	    	}
	    	ArrayList<Individual> oldIndividuals = pop.getIndividuals();
	        ArrayList<Individual> newIndividuals = new ArrayList<Individual>();
	        Population newPop = new Population(new ArrayList<Tuple>());
	        newPop.setMutations(new ArrayList<Element>(pop.getMutations()));
	        // Loop over the population size and create new individuals with crossover.
        	int size = oldIndividuals.size() - (oldIndividuals.size() % 2);
	        for (int i = 0; i < size; i+=2) {
	        	int random = rand.nextInt(oldIndividuals.size() - 1);
	            Individual indiv1 = oldIndividuals.get(0);
	            oldIndividuals.remove(0);
	            Individual indiv2 = oldIndividuals.get(random);
	            oldIndividuals.remove(random);
	            oldIndividuals.addAll(crossover(indiv1, indiv2));
	            //newIndividuals.addAll(crossover(indiv1, indiv2));
	        }
	       
	        for (int i = 0; i < newIndividuals.size(); i++) {
	            mutate(newIndividuals.get(i), newPop);
	        }
	        for (Individual i: newIndividuals){
	    	   newPop.addIndividual(i);
	        }
	        for (Individual i: oldIndividuals){
	        	newPop.addIndividual(i);
	        }
	        
	        if (newPop.getFitness() > pop.getFitness()){
	        	return evolvePopulation(newPop, stablePops);
	        }
	        else{
	        	return evolvePopulation(pop, stablePops + 1);
	        }
	    }

	    // Crossover individuals
	    private ArrayList<Individual> crossover(Individual indiv1, Individual indiv2) {
	    	ArrayList<Individual> individuals = new ArrayList<Individual>();
	        Individual newSol = new Individual(new Tuple());
	        Individual newSol2 = new Individual(new Tuple());
	        // Loop through genes
	        for (int i = 0; i < indiv1.size(); i++) {
	            // Crossover
	            if (Math.random() <= uniformRate) {
	                newSol.setGene(i, indiv1.getGene(i));
	                newSol2.setGene(i, indiv2.getGene(i));
	            } else {
	                newSol.setGene(i, indiv2.getGene(i));
	                newSol2.setGene(i, indiv1.getGene(i));
	            }
	        }
	        individuals.add(newSol);
	        individuals.add(newSol2);
	        return individuals;
	    }
	    
	    private void mutate(Individual indiv, Population pop) {
	    	ArrayList<Element> mutations = pop.getMutations();
	    	if (mutations.size() > 0){
		    	int random = rand.nextInt(mutations.size());
		        if (Math.random() <= mutationRate) {
		            // Substitute random gene
		        	Element mutant = mutations.get(random);
		        	mutations.remove(random);
		        	int geneNum = Integer.parseInt(mutant.getModelId()) - 1;
		        	if (indiv.getGene(geneNum) != null){
		        		mutations.add(indiv.getGene(geneNum));
		        	}
		            indiv.setGene(geneNum, mutant);
		        }
	    	}
	    }
	    
	    public void setUniformRate(double rate){
	    	this.uniformRate = rate;
	    }
	    
	    public void setMutationRate(double rate){
	    	this.mutationRate = rate;
	    }
	}

}
