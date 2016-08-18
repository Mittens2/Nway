package core.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class OptimalSolutionSolver {
	class TupleGenerator implements Callable<ArrayList<Tuple>>{  
		private ArrayList<Model> models;
		private Tuple current;
		private int model;
		private int threads;
		Element expanded;
		
		public TupleGenerator (ArrayList<Model> models, Tuple current, int model, Element expanded, int threads){
			this.models = models;
			this.current = current;
			this.model = model;
			this.expanded = expanded;
			this.threads = threads;
		}
		
		@Override
		public ArrayList<Tuple> call(){
			ArrayList<Tuple> allTuples = generateAllTupleCombos(current, expanded, model);
			return allTuples;
		}
		
		private ArrayList<Tuple> generateAllTupleCombos(Tuple current, Element expanded, int model){
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
				if (threads < 50){
					List<TupleGenerator> generators = new ArrayList<TupleGenerator>();
					int newThreads = threads + models.get(model).size();
					generators.add(new TupleGenerator(models, current, model + 1, null, newThreads));
					for (Element e: models.get(model).getElements()){
						generators.add(new TupleGenerator(models, current, model + 1, e, newThreads));
					}
					ExecutorService executor = Executors.newFixedThreadPool(models.get(model).size() + 1);
					List<Future<ArrayList<Tuple>>> results = null;
					try {
						results = executor.invokeAll(generators);
						executor.shutdown();
						for (Future<ArrayList<Tuple>> result: results){
							allTuples.addAll(result.get());
						}		
					} catch (InterruptedException | ExecutionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				else{
					allTuples.addAll(generateAllTupleCombos(current, null, model + 1));
					for (Element e: models.get(model).getElements()){
						allTuples.addAll(generateAllTupleCombos(current, e, model + 1));
					}
				}
			}
			return allTuples;
		}
	}
	
	class SolutionGenerator implements Callable<ArrayList<ArrayList<Tuple>>>{
		private ArrayList<Tuple> currSolution;
		private ArrayList<Tuple> allTuples;
		private int threads;
		
		public SolutionGenerator (ArrayList<Tuple> currSolution, ArrayList<Tuple> allTuples, int threads){
			this.currSolution = currSolution;
			this.allTuples = allTuples;
			this.threads = threads;
		}
		
		@Override
		public ArrayList<ArrayList<Tuple>> call(){
			ArrayList<ArrayList<Tuple>> allSolutions = generateAllSolutionCombos(currSolution, allTuples);
			return allSolutions;
		}
		
		private ArrayList<ArrayList<Tuple>> generateAllSolutionCombos(ArrayList<Tuple> currSolution, ArrayList<Tuple> allTuples){
			ArrayList<ArrayList<Tuple>> solutions = new ArrayList<ArrayList<Tuple>>();
			if (allTuples.size() == 0){
				if (currSolution.size() > 0){
					solutions.add(currSolution);
				}
			}
			else{
				Tuple currTuple = allTuples.remove(allTuples.size() - 1);
				ArrayList<Tuple> currSolutionCopy = new ArrayList<Tuple>();
				currSolutionCopy.add(currTuple);
				if (threads < 16){
					int newThreads = threads + 2;
					ArrayList<SolutionGenerator> generators = new ArrayList<SolutionGenerator>();
					generators.add(new SolutionGenerator(currSolution, allTuples, newThreads));
					generators.add(new SolutionGenerator(currSolutionCopy, allTuples, newThreads));
					ExecutorService executor = Executors.newFixedThreadPool(2);
					List<Future<ArrayList<ArrayList<Tuple>>>> results = null;
					try {
						results = executor.invokeAll(generators);
						executor.shutdown();
						for (Future<ArrayList<ArrayList<Tuple>>> result: results){
							solutions.addAll(result.get());
						}
					} catch (InterruptedException | ExecutionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				else{
					solutions.addAll(generateAllSolutionCombos(currSolution,allTuples));
					solutions.addAll(generateAllSolutionCombos(currSolutionCopy, allTuples));
				}
			}
			return solutions;
		}
	}
	
	public ArrayList<Tuple> calcOptimalScore(String modelsFile){
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		TupleGenerator tupGenerator = new TupleGenerator(models, new Tuple(), 0, null, 0);
		ArrayList<Tuple> allTuples = tupGenerator.call();
		//System.out.println(allTuples);
		SolutionGenerator solnGenerator = new SolutionGenerator(new ArrayList<Tuple>(), allTuples, 0);
		ArrayList<ArrayList<Tuple>> allSolutions = solnGenerator.call();
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
}
