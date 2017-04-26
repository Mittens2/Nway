package core.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import core.alg.TupleExaminer;
import core.alg.merge.HungarianMerger;
import core.alg.merge.MergeDescriptor;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class AlgoUtil {
	
	public static boolean TRACE = false;
	
	public static boolean COMPUTE_RESULTS_CLASSICALLY = false;
	
	public static final double PROPERTIES_QUALITY_THRESHOLD = 0;
	
	private static final BigDecimal tresholdRatioBase = new BigDecimal(4.0/3.0, N_WAY.MATH_CTX);
	public static BigDecimal tresholdRatio =  new BigDecimal ("" + 4.0/3.0, N_WAY.MATH_CTX); // to be later divided by k
	
	private static final BigDecimal pairWisePairingTresholdBase =  new BigDecimal(0.5, N_WAY.MATH_CTX);//.0.5;// 2.0 / 3.0;
	public static BigDecimal pairWisePairingTreshold =  new BigDecimal(""+2.0/3.0, N_WAY.MATH_CTX);//0.5;// 2.0 / 3.0;
	
	public static boolean filterGeneration = true;
	public static String NO_MODEL_ID ="-1";
	
	private static final TupleExaminer EMPTY_EXAMINER = new TupleExaminer() {
		@Override
		public boolean proceedWithTupleExpansion(Tuple t) { return true;}
		@Override
		public boolean examine(Tuple t) { return true;}
		@Override
		public void doneWithTupleCreation() {}
	};

	public static BigDecimal ratio(String up, String down){
		return bigDec(up).divide(bigDec(down), N_WAY.MATH_CTX);
	}
	
	public static BigDecimal  bigDec(String num){
		return new BigDecimal(num, N_WAY.MATH_CTX);
	}
	
	public static void useTreshold(boolean use){
		if(use){
			tresholdRatio = tresholdRatioBase;
			pairWisePairingTreshold = pairWisePairingTresholdBase; 
		}
		else{
			tresholdRatio =  BigDecimal.ZERO; 
			pairWisePairingTreshold = BigDecimal.ZERO; 
		}
	}
	
	public static boolean areThereDuplicates(ArrayList<Tuple> tpls){
			HashSet<Element> elems = new HashSet<Element>();
			for(Tuple t:tpls){
				for(Element e:t.getRealElements()){
					if(elems.contains(e)){
						return true;
					}
					elems.add(e);
				}
			}
			return false;
		}

	public static ArrayList<Tuple> generateTuplePartially(ArrayList<Model> models, TupleExaminer examiner){
		ArrayList<Tuple> retVal = new ArrayList<Tuple>();
		generateTuplePartially(models, 0, examiner,retVal  , new Tuple());
		return retVal;
	}
	
	private static boolean generateTuplePartially(ArrayList<Model> models,int currModelIndex,  TupleExaminer examiner, ArrayList<Tuple> accum, Tuple t){
		int numOfModels = models.size();
		if(currModelIndex == numOfModels)
			return true;
		Model m = models.get(currModelIndex);
		ArrayList<Element> elems = m.getElementsSortedByLabel();
		Element e;
		for(int i=0;i<=elems.size();i++){
			if(i==elems.size())
				e = null;
			else
				e = elems.get(i);
			Tuple created = (e==null)?t:t.newExpanded(e, models);
			if(e!=null && examiner.examine(created))
				accum.add(created);
			if(!examiner.proceedWithTupleExpansion(created))
				continue;
			if(!generateTuplePartially(models, currModelIndex+1, examiner, accum, created))
				return false;
		}
		return true;
	}

	public static ArrayList<Tuple> generateAllTuples(ArrayList<Model> models){
		return generateAllTuples(models, true);
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<Tuple> generateAllTuples(ArrayList<Model> models, boolean generateGraph) {
		//if(tuplesMemo.get(models) != null){
		//	return (ArrayList<Tuple>) tuplesMemo.get(models).clone();
		//}
		ArrayList<Tuple> lst = new ArrayList<Tuple>();
		int numOfModels = 0;
		for(Model m:models){
			numOfModels += m.getMergedFrom();
		}
		lst.add(new Tuple());
	//	try {
		for(Model m: models){
			for(int i=0, l = lst.size(); i<l ;i++){
				Tuple t = lst.get(i);
				ArrayList<Element> elems = m.getElements();
				for(Element e: elems){
					Tuple created = t.newExpanded(e,models);
					lst.add(created);
				}
			}
		}
		filterAndSetScaledWeight(lst, tresholdRatio.divide(new BigDecimal(numOfModels, N_WAY.MATH_CTX),  N_WAY.MATH_CTX));
	//	long startTime = System.currentTimeMillis();
		//if(models.size() <= 3 && generateGraph) prepareNeighborhoodGrap(lst);
	//	long endTime = System.currentTimeMillis();
		//long execTime = endTime - startTime;
		//tuplesMemo.put(models, lst);
	//	}catch(Throwable t){System.out.println("num of tuples = "+lst.size());}
		return lst; 
	}
	
	public static ArrayList<ArrayList<Element>> getElementsWithSharedProperties(Tuple target, ArrayList<Element> elems, int shared){
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		ArrayList<Element> compatible = new ArrayList<Element>();
		ArrayList<Element> incompatible = new ArrayList<Element>();
		for (Element e: elems){
			int count = 0;
			for (Element t: target.getElements()){
				count += getCommonProperties(t, e);
			}
			if (count >= shared){
				compatible.add(e);
			}
			else{
				incompatible.add(e);
			}
		}
		partition.add(compatible);
		partition.add(incompatible);
		return partition;
	}
	
	public static ArrayList<ArrayList<Element>> partitionShared(Element target, ArrayList<Element> elems, int shared){
		ArrayList<ArrayList<Element>> partition = new ArrayList<ArrayList<Element>>();
		ArrayList<Element> compatible = new ArrayList<Element>();
		ArrayList<Element> incompatible = new ArrayList<Element>();
		for (Element e: elems){
			if (haveCommonProperties(target, e, shared)){
				compatible.add(e);
			}
			else{
				incompatible.add(e);
			}
		}
		partition.add(compatible);
		partition.add(incompatible);
		return partition;
	}
	
	public static ArrayList<Element> removeElementsSameModelId(Element target, ArrayList<Element> elems){
		for (int i = elems.size() - 1; i >= 0; i--){
			if (elems.get(i).getModelId() == target.getModelId()){
				elems.remove(i);
			}
		}
		return elems;
	}
	
	public static ArrayList<Tuple> getTuplesOfNonMatchedCompositeElementsFromModel( Model m, HashSet<Element> elementsUsedInMatch) {
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		for(Element e:m.getElements()){
			if(elementsUsedInMatch.contains(e))
				continue;
			if(e.getBasedUponElements().size() != 1)
				tuples.add(e.getContainingTuple());
		}
		return tuples;
	}
	
	public static void printTuples(ArrayList<Tuple> tuples){
		ArrayList<Tuple> ttt = (ArrayList<Tuple>) tuples.clone();
		Collections.sort(ttt, new TupleComparator());
		System.out.println(ttt);
	}
	
	
	public static void trace(String msg){
		if(TRACE){
			System.out.println(msg);
		}
	}
	
	private static void prepareNeighborhoodGrap(ArrayList<Tuple> lst) {
//		for(int i=0;i<lst.size();i++){
//			Tuple t1 = lst.get(i);
//			Set<Element> e1 = t1.getRealElements();
//			t1.addNeighbour(t1);
//			for(int j=i+1;j<lst.size();j++){
//				Tuple t2 = lst.get(j);
//				Set<Element> e2 = t2.getRealElements();
//				for (Element element : e1) {
//					if(e2.contains(element)){
//						t1.addNeighbour(t2);
//						t2.addNeighbour(t1);
//					}
//				}
//			}
//		}
		
	}
	
	public static boolean shouldCompose(Tuple composed, Element e1, Element e2, ArrayList<Model> models){
		BigDecimal w1 = e1.getContainingTuple().calcWeight(models);
		BigDecimal w2 = e2.getContainingTuple().calcWeight(models);
		return composed.calcWeight(models).compareTo(w1.add(w2, N_WAY.MATH_CTX)) > 0;
	}
	
	// returns true if the tuple should be composed of its elements -> their sum is smaller than the tuple's weight
	public static boolean shouldCompose(Tuple t, ArrayList<Model> mdls){
		HashSet<Element> elements = new HashSet<Element> (t.getElements());
		BigDecimal sumOfComposingElements = BigDecimal.ZERO;
		for(Element e:elements){
			Tuple containing = e.getContainingTuple();
			if(e.getBasedUponElements().size() > 1){
				sumOfComposingElements = sumOfComposingElements.add(containing.calcWeight(mdls), N_WAY.MATH_CTX);
			}
		}
		return sumOfComposingElements.compareTo(t.getWeight()) < 0;
	}
	
	
	public static boolean isNonValidTuple(Tuple t){
		if(t.getWeight().compareTo(BigDecimal.ZERO) == 0)
			return true;
		if(t.getSize() < 2)
			return true;
		if (t.getWeight().compareTo(t.getRelativeThreshold()) <= 0)
			return true;	
		if(haveLowQualityElement(t, PROPERTIES_QUALITY_THRESHOLD))
			return true;
		for(Element e1:t.sortedElements()){
			boolean haveCommonPropsWithRestOfTuple = false;
			for(Element e2:t.sortedElements()){
				if(e1 == e2)
					continue;
				if(haveCommonProperty(e1,e2)){
					haveCommonPropsWithRestOfTuple = true;
					break;
				}
			}
			if(!haveCommonPropsWithRestOfTuple)
				return true;
		}
		return false;
	}
	
	public static int commonModel(Element e, Tuple t){
		for (int i = 0; i < t.getElements().size(); i++){
			if (t.getElements().get(i).getModelId() == e.getModelId())
				return i;
		}
		return -1;
	}

	private static boolean haveLowQualityElement(Tuple t, double elemQualityThreshold) {
		for(Element e:t.getRealElements()){
			HashSet<String> otherProps = new HashSet<String>();
			for(Element e1:t.getRealElements()){
				if(e1 != e){
					otherProps.addAll(e1.getProperties());
				}
			}
			
			double usedPropsCount = 0;
			for(String prp:e.getProperties()){
				if(otherProps.contains(prp))
					usedPropsCount++;
			}
			
			if( ( (double)e.getProperties().size()) /usedPropsCount < elemQualityThreshold  )
				return true;
			
		}
		
		return false;
	}

	private static boolean haveCommonProperty(Element e1, Element e2) {
		return haveCommonProperties(e1, e2, 1);
	}
	
	private static boolean haveCommonProperties(Element e1, Element e2, int x){
		Set<String> e1Props = e1.getProperties();
		int count = 0;
		for(String prp:e2.getProperties()){
			if(e1Props.contains(prp))
				count++;
				if (count == x){
					return true;
				}
		}
		return false;
	}
	
	public static int getCommonProperties(Element e1, Element e2){
		Set<String> e1Props = e1.getProperties();
		int count = 0;
		for(String prp:e2.getProperties()){
			if(e1Props.contains(prp))
				count++;
		}
		return count;
	}

	private static void filterAndSetScaledWeight(ArrayList<Tuple> all, BigDecimal weightTreshold) {
		int numOfTuples = all.size();
		for(int i=all.size()-1; i>=0;i--){
			Tuple t = all.get(i);
			if(t.getSize() < 2 ||(filterGeneration && isNonValidTuple(t)))
				all.remove(i);
			else
				t.setScaledWeight((t.getWeight().multiply(new BigDecimal(100*numOfTuples))).longValue());
		}
		//System.out.println(all.size());
	}

	public static BigDecimal truncateWeight(BigDecimal w){
		long tmp = w.multiply(new BigDecimal(100000, N_WAY.MATH_CTX), N_WAY.MATH_CTX).longValue();
		return new  BigDecimal(tmp).divide(new BigDecimal(100000, N_WAY.MATH_CTX), N_WAY.MATH_CTX);
	}
	
	public static void sortTuples(ArrayList<Tuple> tuples){
		Collections.sort(tuples, new TupleComparator());
	}
	
	public static class TupleComparator implements Comparator<Tuple> {
	    
	    private boolean asc = false;
	    
	    public TupleComparator(){}
	    public TupleComparator(boolean asc){this.asc = asc;}
	    @Override
	    public int compare(Tuple t1, Tuple t2) {
	    	int comparisonResult = t1.getWeight().compareTo(t2.getWeight());
	        if(!asc)
	        	return -comparisonResult;
	        else
	        	return comparisonResult;
	    }
	}
	
	public static BigDecimal calcGroupWeight(Collection<Tuple> c, boolean scaled){
		BigDecimal res = BigDecimal.ZERO;
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();) {
			Tuple t = (Tuple) iterator.next();
			if(scaled)
				res = res.add(new BigDecimal(t.getScaledWeight(), N_WAY.MATH_CTX),N_WAY.MATH_CTX);
			else
				res = res.add(t.getWeight(),N_WAY.MATH_CTX);
		}
		return res;
	}
	
	public static double calcPercentCorrect(Collection<Tuple> c){
		/*Set<String> correct = new HashSet<String>();
		Set<String> incorrect = new HashSet<String>();
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			Tuple t = (Tuple) iterator.next();
			Set<String> labels = new HashSet<String>();
			for (Element e :t.getElements()){
				labels.add(e.getLabel());
			}
			if (labels.size() > 1){
				for (String label: labels){
					correct.remove(label);
				}
				incorrect.addAll(labels);
			}
			else{
				String label = labels.iterator().next(); 
				if (correct.contains(label)){
					correct.remove(label);
					incorrect.add(label);
				}
				else{
					correct.add(label);
				}
			}
		}
		return ((double) correct.size()) / (correct.size() + incorrect.size());*/
		Map<String, Double[]> ratios = new HashMap<String, Double[]>();
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			Tuple t = (Tuple) iterator.next();
			Map<String, Integer> labels = new HashMap<String, Integer>();
			for (Element e :t.getElements()){
				String label = e.getLabel();
				if (labels.containsKey(label)) labels.put(label, labels.get(label));
				else labels.put(label, 1);
			}
			int size = t.getSize();
			/*if (size == 1){
				String label = labels.keySet().iterator().next();
				if (ratios.containsKey(label)){
					Double[] value = ratios.get(label);
					value[1]++;
					ratios.put(label, value);
				}
				else{
					Double[] value = {0., 1.};
					ratios.put(label, value);
				}
			}*/
			for (String label: labels.keySet()){
				if (ratios.containsKey(label)){
					Double[] value = ratios.get(label);
					value[0] += labels.get(label) / size;
					value[1]++;
					ratios.put(label, value);
				}
				else{
					Double[] value = {((double) labels.get(label)) / size, 1.};
					ratios.put(label, value);
				}	
			}
		}
		double score = 0;
		for (String key: ratios.keySet()){
			score += ratios.get(key)[0] / ratios.get(key)[1];
		}
		return score;
	}
	
	public static ArrayList<Double> calcQualityMetrics(Collection<Tuple> c){
		// Reduction in # classes
		ArrayList<Double> metrics = new ArrayList<Double>();
		metrics.add(calcPercentCorrect(c));
		int classesTotal = 0;
		int classesMerged = 0;
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			classesMerged++;
			Tuple t = (Tuple) iterator.next();
			for (Element e :t.getElements()){
				classesTotal++;
			}
		}
		metrics.add((1 - ((double) classesMerged) / classesTotal) * 100);
		// Reduction in # attributes
		int attTotal = 0;
		int attMerged = 0;
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			Tuple t = (Tuple) iterator.next();
			Set<String> propUnion = new HashSet<String>();
			for (Element e :t.getElements()){
				attTotal += e.getProperties().size();
				propUnion.addAll(e.getProperties());
			}
			attMerged += propUnion.size();
		}
		metrics.add((1 - ((double) attMerged) / attTotal) * 100);
		// Reduce % of variable class attributes (1)
		/*int attVar = 0;
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			Tuple t = (Tuple) iterator.next();
			Set<String> propVar = new HashSet<String>();
			Set<String> propMerge = new HashSet<String>();
			for (Element e :t.getElements()){
				for (String p: e.getProperties()){
					if (propVar.contains(p)){
						propVar.remove(p);
						propMerge.add(p);
					}
					else if (!propMerge.contains(p)){
						propVar.add(p);
					}
				}
			}
			attVar += propVar.size();
		}
		metrics.add((1 - ((double) attVar) / attTotal) * 100);*/
		// Reduce % variable class attributes (2)
		int attAll = 0;
		int attNotAll = 0;
		for (Iterator<Tuple> iterator = c.iterator(); iterator.hasNext();){
			Tuple t = (Tuple) iterator.next();
			Map<String, Integer> propFreq = new HashMap<String, Integer>();
			for (Element e :t.getElements()){
				attNotAll += e.getProperties().size();
				for (String p: e.getProperties()){
					if (propFreq.get(p) != null)
						propFreq.put(p, propFreq.get(p) + 1);
					else
						propFreq.put(p, 1);
				}
			}

			for (String p: propFreq.keySet()){
				if (propFreq.get(p) == t.getSize())
					attAll++;
			}
			//attNotAll += propFreq.keySet().size();
		}
		metrics.add((((double) attAll) / attNotAll) * 100);
		return metrics;
	}
	
	public static String getLexicographicRepresentation(Element elem){
		ArrayList<String> mdls = new ArrayList<String>();
		for(Element e:elem.getBasedUponElements()){
			mdls.add(e.getModelId());
		}
		Collections.sort(mdls);
		return mdls.toString();
	}
	
	public static boolean areNeighbours(Tuple t1, Tuple t2) {
		return t1.isNeighborOf(t2);
	}

	public static BigDecimal calcGroupWeight(ArrayList<Tuple> tuples) {
		return calcGroupWeight(tuples, false);
	}

	public static ArrayList<HungarianMerger> generateAllModelPairs(ArrayList<Model> models) {
		ArrayList<HungarianMerger> pairs = new ArrayList<HungarianMerger>();
		for(int i=0;i<models.size();i++){
			for(int j=i+1;j<models.size();j++){
				HungarianMerger hm = new HungarianMerger(models.get(i), models.get(j), models.size());
				pairs.add(hm);
				hm.runPairing();
			}
		}
		return pairs;
	}
	
	public static ArrayList<Tuple> calcOptimalScore(String modelsFile){
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		ArrayList<Tuple> allTuples = generateAllTupleCombos(new Tuple(), null, models, 0);
		ArrayList<ArrayList<Tuple>> allSolutions = generateAllSolutionCombos(new ArrayList<Tuple>(), allTuples);
		ArrayList<Tuple> bestSolution = new ArrayList<Tuple>();
		BigDecimal currMax = BigDecimal.ZERO;
		for (ArrayList<Tuple> solution: allSolutions){
			if (isValidSolution(solution, models)){
				BigDecimal solutionWeight = AlgoUtil.calcGroupWeight(solution);
				if (solutionWeight.compareTo(currMax) > 0){
					currMax = solutionWeight;
					bestSolution = solution;
				}
			}
		}
		System.out.println(bestSolution);
		return bestSolution;
	}
	
	private static ArrayList<Tuple> generateAllTupleCombos(Tuple current, Element expanded, 
			ArrayList<Model> models, int model){
		/**
		 * Generates all valid tuple combinations for some model suite.
		 */
		ArrayList<Tuple> allTuples = new ArrayList<Tuple>();
		if (model == models.size()){
			if (expanded != null){
				current = current.newExpanded(expanded, models);
			}
			if (current.getSize() > 0){
				allTuples.add(current);
			}
		}
		else{
			if (expanded != null){
				current = current.newExpanded(expanded, models);
			}
			allTuples.addAll(generateAllTupleCombos(current, null, models, model + 1));
			for (Element e: models.get(model).getElements()){
				allTuples.addAll(generateAllTupleCombos(current, e, models, model + 1));
			}
		}
		return allTuples;
	}
	
	private static ArrayList<ArrayList<Tuple>> generateAllSolutionCombos(ArrayList<Tuple> currSolution,
			ArrayList<Tuple> allTuples){
		
		ArrayList<ArrayList<Tuple>> solutions = new ArrayList<ArrayList<Tuple>>();
		if (allTuples.size() == 0){
			if (currSolution.size() > 0){
				solutions.add(currSolution);
			}
		}
		else{
			Tuple currTuple = allTuples.remove(allTuples.size() - 1);
			solutions.addAll(generateAllSolutionCombos(currSolution,allTuples));
			ArrayList<Tuple> currSolutionCopy = new ArrayList<Tuple>();
			currSolutionCopy.add(currTuple);
			solutions.addAll(generateAllSolutionCombos(currSolutionCopy, allTuples));
		}
		return solutions;
	}
	
	private static boolean isValidSolution(ArrayList<Tuple> solution, ArrayList<Model> models){
		int elemsCount = 0;
		for (Model m: models){
			elemsCount += m.getElements().size();
		}
		int[] elemsInTuple = new int[elemsCount + 1];
		for (Tuple t: solution){
			for (Element e: t.getElements()){
				int eId = e.getId();
				if (elemsInTuple[eId] == 0){
					elemsInTuple[eId]++;
				}
				else{
					return false;
				}
			}
		}
		return true;
	}
	/*private static ArrayList<ArrayList<Tuple>> generateAllSolutions(ArrayList<Tuple> current, ArrayList<Tuple> allTuples, 
			ArrayList<Model> models, int model){
		ArrayList<ArrayList<Tuple>> solutions = new ArrayList<ArrayList<Tuple>>();
		if (model == models.size() - 1){
			for (Tuple t: allTuples){
				ArrayList<Tuple> newSolution = new ArrayList<Tuple>();
				newSolution.addAll(current);
				newSolution.add(t);
				solutions.add(newSolution);
			}
		}
		else{
			int prevModelSizes = 1;
			for (int i = 0; i < model; i++) prevModelSizes *= models.get(i).size();
			int currModelSize = models.get(model).size() + 1;
			int segment = allTuples.size() / prevModelSizes;
			int smallSegment = segment / currModelSize;
			for (int j = 0; j < currModelSize; j++){
				ArrayList<Tuple> compatible = new ArrayList<Tuple>();
				ArrayList<Tuple> incompatible = new ArrayList<Tuple>();
				for (int i = 0; i < prevModelSizes; i++){
					compatible.addAll(allTuples.subList(segment * i, segment * i + smallSegment * j));
					incompatible.addAll(allTuples.subList(segment * i + smallSegment * j, 
							segment * i + smallSegment * (j + 1)));
					compatible.addAll(allTuples.subList(segment * i + smallSegment * (j + 1), 
							segment * (i + 1)));

				}
				//System.out.println(compatible);
				//System.out.println(incompatible);
				for (Tuple t: incompatible){
					if (t.getSize() != 0){
						current.add(t);
						solutions.addAll(generateAllSolutions(current, compatible, models, model + 1));
						current.remove(current.size() - 1);
					}
				}
			}
		}
		return solutions;
	}*/
	
	/*private static ArrayList<ArrayList<Tuple>> buildAllSolutions(Tuple current, Element expanded,
			ArrayList<Model> models, int model){
		ArrayList<ArrayList<Tuple>> variants = new ArrayList<ArrayList<Tuple>>();
		if (model == models.size()){
			if (expanded != null) current = current.newExpanded(expanded, models);
			if (current.getSize() > 0 && !current.calcWeight(models).equals(BigDecimal.ZERO)){
				ArrayList<Tuple> self = new ArrayList<Tuple>();
				self.add(current);
				variants.add(self);
			}
		}
		else if (expanded != null){
			current = current.newExpanded(expanded, models);
			variants.addAll(buildAllSolutions(current, null, models, model + 1));
			for (Element e: models.get(model).getElements()){
				variants.addAll(buildAllSolutions(current, e, models, model + 1));
			}
		}
		else{
			ArrayList<ArrayList<Tuple>> solutions = new ArrayList<ArrayList<Tuple>>();
			variants.addAll(buildAllSolutions(current, null, models, model + 1));
			ArrayList<Element> modelElems = models.get(model).getElements();
			modelElems.add(null);
			for (Element e: modelElems){
				ArrayList<ArrayList<Tuple>> oldSolutions = new ArrayList<ArrayList<Tuple>>(solutions);
				oldSolutions.add(new ArrayList<Tuple>());
				ArrayList<ArrayList<Tuple>> newSolutions = new ArrayList<ArrayList<Tuple>>();
				for (ArrayList<Tuple> variant: variants){
					for (ArrayList<Tuple> oldSolution: oldSolutions){
						boolean canCombine = true;
						for (Tuple t1: oldSolution){
							for (Tuple t2: variant){
								if (t1.hasCommonElement(t2, model)){
									canCombine = false;
									break;
								}
							}
						}
						if (canCombine){
							ArrayList<Tuple> newSolution = new ArrayList<Tuple>();
							newSolution.addAll(oldSolution);
							newSolution.addAll(variant);
							newSolutions.add(newSolution);
						}
					}
				}
				variants = buildAllSolutions(current, e, models, model + 1);
				solutions.addAll(newSolutions);
			}
			return solutions;
		}
		return variants;
	}*/
	
	
	public static ArrayList<Model> getModelsByCohesiveness(ArrayList<Model> models,final boolean asc){
		ArrayList<HungarianMerger> merges = generateAllModelPairs(models);
		HashMap<Model, BigDecimal> acccumMap = new HashMap<Model, BigDecimal>();
		HashMap<Model, BigDecimal> cntMap = new HashMap<Model, BigDecimal>();
		for(HungarianMerger hm:merges){
			//BigDecimal avgW = hm.getWeight(); // cohesiveness is the size of the edge
			BigDecimal avgW = (hm.getWeight().compareTo(BigDecimal.ZERO) == 0)?BigDecimal.ZERO: 
			                       hm.getWeight().divide(new BigDecimal(hm.getTuplesInMatch().size(), N_WAY.MATH_CTX), N_WAY.MATH_CTX  ); // cohesiveness is the avg weight of the edge

			for(Model m:hm.getModels()){
				BigDecimal accumedWeight = acccumMap.get(m);
				BigDecimal cnt = cntMap.get(m);
				if(accumedWeight == null){
					accumedWeight = BigDecimal.ZERO;
					cnt = BigDecimal.ZERO;
				}
				acccumMap.put(m,accumedWeight.add(avgW,N_WAY.MATH_CTX));
				cntMap.put(m, cnt.add(BigDecimal.ONE));
			}
		}
		@SuppressWarnings("unchecked")
		ArrayList<Model> retVal = (ArrayList<Model>) models.clone();
		HashMap<Model, BigDecimal> avgWeightOfModel = new HashMap<Model, BigDecimal>();
		for(Model m:models){
			avgWeightOfModel.put(m, acccumMap.get(m).divide(cntMap.get(m), N_WAY.MATH_CTX) );
		}
		@SuppressWarnings("unchecked")
		final HashMap<Model, BigDecimal> avgWeight = (HashMap<Model, BigDecimal>) avgWeightOfModel.clone();
		Collections.sort(retVal, new Comparator<Model>() {

			@Override
			public int compare(Model m1, Model m2) {
				if(asc){
					return (avgWeight.get(m1).compareTo(avgWeight.get(m2)) < 0)?-1:1;
				}else{
					return (avgWeight.get(m1).compareTo(avgWeight.get(m2)) <0)?1:-1;
				}
					
			}
		});
		return retVal;
	}

	public static void reset() {
	}
	
	public static String nameOfMergeDescription(MergeDescriptor md, int splitSize){
		String res = "";
		if(md.algPolicy == N_WAY.ALG_POLICY.PAIR_WISE) res = "PW, ";
		if(md.algPolicy == N_WAY.ALG_POLICY.GREEDY) res = "G";
		if(md.algPolicy == N_WAY.ALG_POLICY.REPLACE_FIRST) res = "LS, ";
		if(md.algPolicy == N_WAY.ALG_POLICY.REPLACE_BEST) res = "GTLS, ";
		if (md.algPolicy == N_WAY.ALG_POLICY.RANDOM){
			res = "SH (";
			switch (md.seed){
				case 0: res = res+"sd0";
						break;
				case 1: res = res+"sd1";
						break;
				case 2: res = res+"sd2";
						break;
				case 3: res = res+"sd3";
						break;
				case 4: res = res+"sd4";
						break;
				case 5: res = res+"sd5";
						break;
			}
			if (md.seed == 4){
				if (md.asc)
					res = res+"-a";
				else
					res = res+"-d";
			}
			if (md.seed == 1 || md.seed == 3 || md.seed == 2 || md.seed == 4 || md.seed == 5){
				if (md.elementAsc)
					res = res+"-a";
				else
					res = res+"-d";
			}
			if(md.highlight == 0)
				res = res+"_hl0";
			else if (md.highlight == 1)
				res = res+"_hl1";
			else if (md.highlight == 2)
				res = res+"_hl2";
			else
				res = res+"_hl3";
			if (md.choose == 0)
				res = res+"_ch0";
			else if (md.choose == 1)
				res= res+"_ch1";
			else
				res = res+"_ch2";
			if (md.reshuffle == 0)
				res = res+"_rs0)";
			else if (md.reshuffle == 1)
				res = res+"_rs1)";
			else
				res =res+"_rs2)";
		}
		else{
			if(splitSize > 2)
				res = res+splitSize+", ";
			if(md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE)
				res = res+"size ";
			else if(md.orderBy == N_WAY.ORDER_BY.COHESIVENESS)
				res = res+"cohesiveness ";
			else if(md.orderBy == N_WAY.ORDER_BY.MATCH_QUALITY)
				res = res+"Best Match ";
			else if(md.orderBy == N_WAY.ORDER_BY.SPARSITY)
				res = res+"Most sparse ";
			else if(md.orderBy == N_WAY.ORDER_BY.MODEL_ID)
				res = res+"by id ";
			else //if (md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE_ELEMENT_SIZE || md.orderBy == N_WAY.ORDER_BY.PROPERTY)
				res = res+"mSize-";
			
			if(md.asc)
				res = res+"asc";
			else
				res = res+"desc";
			
		}
		return res;
	}
	
}
