package core.common;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
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
	
	public static void testRMMandNwMDiff(ArrayList<Model> models, String nwmFilePath, boolean printNums){
		//MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		//nwm.run();
		//ArrayList<Tuple> nwmTuples = nwm.getTuplesInMatch();
		ArrayList<Tuple> nwmTuples = readNwMSolution(nwmFilePath, models);
		for (Tuple t: nwmTuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md_hl1_sd2a);
		rmm.improveSolution(nwmTuples);
		ArrayList<Tuple> rmmTuples = rmm.getTuplesInMatch();
		addMissingElements(models, nwmTuples);
		ArrayList<ArrayList<Tuple>> disjoint = getDisjoinTuples(rmmTuples, nwmTuples);
		ArrayList<Tuple> rmmOnly = disjoint.get(0);
		ArrayList<Tuple> nwmOnly = disjoint.get(1);
		final DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("Tuples belonging exclusively to NwM: " + AlgoUtil.calcGroupWeight(nwmTuples));
		AlgoUtil.printTuples(nwmTuples);
		if (printNums) printNumberForm(nwmOnly, models);
		System.out.print(df.format(AlgoUtil.calcPercentCorrect(nwmTuples) * 100));
		System.out.println();
		System.out.println("Tuples belonging exclusively to HSim: " + AlgoUtil.calcGroupWeight(rmmTuples));
		//AlgoUtil.printTuples(rmmOnly);
		if (printNums) printNumberForm(rmmOnly, models);
		System.out.print(df.format(AlgoUtil.calcPercentCorrect(rmmTuples) * 100));
	}
	
	public static void testMMandNwMDiff(ArrayList<Model> models, String gameFilePath, String nwmFilePath, boolean printNums){
		if (models.size() > 60){
			models = new ArrayList<Model>(models.subList(0, 10));
		}
		GameSolutionParser parser = new GameSolutionParser(gameFilePath, models);
		ArrayList<Tuple> mmTuples = parser.solutionCalculator();
		//MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		//nwm.run();
		ArrayList<Tuple> nwmTuples = readNwMSolution(nwmFilePath, models);
		addMissingElements(models, nwmTuples);
		getDisjoinTuples(nwmTuples, mmTuples);
		System.out.println("Tuples belonging exclusively to MM: " + AlgoUtil.calcGroupWeight(mmTuples));
		AlgoUtil.printTuples(mmTuples);
		if (printNums) printNumberForm(mmTuples, models);
		System.out.println();
		System.out.println("Tuples belonging exclusively to NwM: " + AlgoUtil.calcGroupWeight(nwmTuples));
		AlgoUtil.printTuples(nwmTuples);
		if (printNums) printNumberForm(nwmTuples, models);
	}
	
	public static void testMMandRMMDiff(ArrayList<Model> models, String filePath, boolean printNums){
		GameSolutionParser parser = new GameSolutionParser(filePath, models);
		ArrayList<Tuple> mmTuples = parser.solutionCalculator();
		//MultiModelMerger nwm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
		//nwm.run();
		//ArrayList<Tuple> nwmTuples = nwm.getTuplesInMatch();
		/*for (Tuple t: nwmTuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}*/
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md_hl1_sd2a);
		for (Tuple t: mmTuples){
			for (Element e: t.getElements()){
				e.setContainingTuple(t);
			}
		}
		rmm.improveSolution(mmTuples);
		ArrayList<Tuple> rmmTuples = rmm.getTuplesInMatch();
		ArrayList<ArrayList<Tuple>> disjoint = getDisjoinTuples(rmmTuples, mmTuples);
		ArrayList<Tuple> rmmOnly = disjoint.get(0);
		ArrayList<Tuple> mmOnly = disjoint.get(1);
		System.out.println("Tuples belonging exclusively to MM: " + AlgoUtil.calcGroupWeight(mmTuples));
		AlgoUtil.printTuples(mmOnly);
		if (printNums) printNumberForm(rmmOnly, models);
		System.out.println();
		System.out.println("Tuples belonging exclusively to HSim: " + AlgoUtil.calcGroupWeight(rmmTuples));
		AlgoUtil.printTuples(rmmOnly);
		if (printNums) printNumberForm(mmOnly, models);
	}
	
	private static ArrayList<ArrayList<Tuple>> getDisjoinTuples(ArrayList<Tuple> tuples1,ArrayList<Tuple> tuples2){
		ArrayList<Tuple> intersection = new ArrayList<Tuple>(tuples1);
		ArrayList<Tuple> t1Copy = new ArrayList<Tuple>(tuples1);
		ArrayList<Tuple> t2Copy = new ArrayList<Tuple>(tuples2);
		intersection.retainAll(t2Copy);
		t1Copy.removeAll(intersection);
		t2Copy.removeAll(intersection);
		Collections.sort(t1Copy, new TupleComparator(false, true));
		Collections.sort(t2Copy, new TupleComparator(false, true));
		ArrayList<ArrayList<Tuple>> modified  = new ArrayList<ArrayList<Tuple>>();
		modified.add(t1Copy);
		modified.add(t2Copy);
		return modified;
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
	
	private static ArrayList<Tuple> readNwMSolution(String filePath, ArrayList<Model> models){
		ArrayList<Tuple> nwmSolution = new ArrayList<Tuple>();
		try{
			Scanner scan = new Scanner(new File(filePath));
			int tupNum = 0;
			Tuple currTuple = new Tuple();
			while(scan.hasNext()){
				String[] line = scan.next().split(";");
				if (Integer.parseInt(line[0]) != tupNum){
					nwmSolution.add(currTuple);
					currTuple = new Tuple();
					tupNum++;
				}
				currTuple = currTuple.newExpanded(models.get(Integer.parseInt(line[1]) - 1).
						getElementByLabel(line[2]), models);
			}
			nwmSolution.add(currTuple);
		} catch (Exception e){
			e.printStackTrace();
		}
		return nwmSolution;
	}
}
