package core.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import core.alg.merge.ChainingOptimizingMerger;
import core.alg.merge.MergeDescriptor;
import core.alg.merge.MultiModelMerger;
import core.alg.merge.RandomizedMatchMerger;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class SolverDifference {

	final static MergeDescriptor md_hl1_sd2a = new MergeDescriptor(true, true, 1, 1, true, true, 2, 2);
	
	public static void testRMMandNWMdiff(ArrayList<Model> models, boolean printNums){
		MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		nwm.run();
		ArrayList<Tuple> nwmTuples = nwm.getTuplesInMatch();
		for (Tuple t: nwmTuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md_hl1_sd2a);
		rmm.improveSolution(nwmTuples);
		ArrayList<Tuple> rmmTuples = rmm.getTuplesInMatch();
		addMissingElements(models, nwmTuples);
		getDisjoinTuples(rmmTuples, nwmTuples);
		System.out.println("Tuples belonging exclusively to NwM");
		AlgoUtil.printTuples(nwmTuples);
		if (printNums) printNumberForm(nwmTuples, models);
		System.out.println();
		System.out.println("Tuples belonging exclusively to HSim");
		AlgoUtil.printTuples(rmmTuples);
		if (printNums) printNumberForm(rmmTuples, models);
	}
	
	public static void testMMandNwMDiff(ArrayList<Model> models, String filePath, boolean printNums){
		GameSolutionParser parser = new GameSolutionParser(filePath, models);
		ArrayList<Tuple> mmTuples = parser.solutionCalculator();
		MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		nwm.run();
		ArrayList<Tuple> nwmTuples = nwm.getTuplesInMatch();
		addMissingElements(models, nwmTuples);
		getDisjoinTuples(nwmTuples, mmTuples);
		System.out.println("Tuples belonging exclusively to MM");
		AlgoUtil.printTuples(mmTuples);
		if (printNums) printNumberForm(mmTuples, models);
		System.out.println();
		System.out.println("Tuples belonging exclusively to NwM");
		AlgoUtil.printTuples(nwmTuples);
		if (printNums) printNumberForm(nwmTuples, models);
	}
	
	@Test
	public static void testMMandRMMDiff(ArrayList<Model> models, String filePath, boolean printNums){
		GameSolutionParser parser = new GameSolutionParser(filePath, models);
		ArrayList<Tuple> mmTuples = parser.solutionCalculator();
		MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		nwm.run();
		ArrayList<Tuple> nwmTuples = nwm.getTuplesInMatch();
		for (Tuple t: nwmTuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md_hl1_sd2a);
		rmm.improveSolution(nwmTuples);
		ArrayList<Tuple> rmmTuples = rmm.getTuplesInMatch();
		getDisjoinTuples(rmmTuples, mmTuples);
		System.out.println("Tuples belonging exclusively to MM");
		AlgoUtil.printTuples(mmTuples);
		if (printNums) printNumberForm(mmTuples, models);
		System.out.println();
		System.out.println("Tuples belonging exclusively to HSim");
		AlgoUtil.printTuples(rmmTuples);
		if (printNums) printNumberForm(rmmTuples, models);
	}
	
	private static void getDisjoinTuples(ArrayList<Tuple> tuples1,ArrayList<Tuple> tuples2){
		ArrayList<Tuple> intersection = new ArrayList<Tuple>(tuples1);
		intersection.retainAll(tuples2);
		tuples1.removeAll(intersection);
		tuples2.removeAll(intersection);
		Collections.sort(tuples1, new TupleComparator(false, true));
		Collections.sort(tuples1, new TupleComparator(false, true));
	}
	
	private static void addMissingElements(ArrayList<Model> models, ArrayList<Tuple> tuples){
		Set<Element> elems = new HashSet<Element>(); 
		for (Tuple t: tuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
				elems.add(e);
			}
		}
		for (Model m: models){
			for (Element e: m.getElements()){
				if (!elems.contains(e)){
					tuples.add(e.getContainingTuple());
				}
			}
		}
	}
	
	private static void printNumberForm(ArrayList<Tuple> tuples, ArrayList<Model> models){
		for (Tuple t: tuples){
			for (Element e: t.sortedElements()){
				int sum = 0;
				for (int i = 0; i < (Integer.parseInt(e.getModelId()) - 1); i++){
					sum += models.get(i).size();
				}
				sum = e.getId() - sum;
				System.out.print(e.getModelId() + "<" + sum + ">,");
			}
			System.out.println();
		}
	}
}
