package core.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class OptimalSolutionSolver {
	final static int THREAD_MAX = 20;
	private ArrayList<Model> models;
	
	public OptimalSolutionSolver(ArrayList<Model> models){
		this.models = models;
	}
	
	class TupleGenerator implements Callable<ArrayList<Tuple>>{  
		private ArrayList<Model> models;
		private Tuple current;
		private int model;
		private int threadThreshold;
		Element expanded;
		
		public TupleGenerator (ArrayList<Model> models, Tuple current, int model, Element expanded, int threadThreshold){
			this.models = models;
			this.current = current;
			this.model = model;
			this.expanded = expanded;
			this.threadThreshold = threadThreshold;
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
				int newThreadThreshold = (threadThreshold - (models.get(model).size() + 1)) / 
						(models.get(model).size() + 1);
				List<TupleGenerator> generators = new ArrayList<TupleGenerator>();
				ArrayList<Element> modelElems = models.get(model).getElements();
				int newThreads = Math.min(threadThreshold, models.get(model).size() + 1);
				for (int i = 0; i < newThreads; i++){
					//if (i == (threadThreshold - (models.get(model).size() + 1)) % (models.get(model).size() + 1))
						//newThreadThreshold--;
					if (i == 0){
						generators.add(new TupleGenerator(models, current, model + 1, null, newThreadThreshold));
					}
					else{
						generators.add(new TupleGenerator(models, current, model + 1, modelElems.get(i - 1), newThreadThreshold));
					}
				}
				for (int i = newThreads; i < models.get(model).size() + 1; i++){
					if (i == 0){
						allTuples.addAll(generateAllTupleCombos(current, null, model + 1));
					}
					else{
						allTuples.addAll(generateAllTupleCombos(current, modelElems.get(i - 1), model + 1));
					}
				}
				ExecutorService executor = Executors.newFixedThreadPool(models.get(model).size() + 1);
				List<Future<ArrayList<Tuple>>> results = null;
				try {
					results = executor.invokeAll(generators);
					executor.shutdown();
					for (Future<ArrayList<Tuple>> result: results){
						allTuples.addAll(result.get());
					}		
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return allTuples;
		}
	}

	class SolutionGenerator implements Callable<ArrayList<ArrayList<Tuple>>>{
		private ArrayList<Tuple> currSolution;
		private ArrayList<Tuple> allTuples;
		private int threadThreshold;
		
		public SolutionGenerator (ArrayList<Tuple> currSolution, ArrayList<Tuple> allTuples, int threadThreshold){
			this.currSolution = currSolution;
			this.allTuples = allTuples;
			this.threadThreshold = threadThreshold;
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
				currSolutionCopy.addAll(currSolution);
				currSolutionCopy.add(currTuple);
				int newThreadThreshold = (threadThreshold - 2) / 2;
				ArrayList<SolutionGenerator> generators = new ArrayList<SolutionGenerator>();
				if (threadThreshold == 0){
					generators.add(new SolutionGenerator(currSolution, allTuples, newThreadThreshold));
					generators.add(new SolutionGenerator(currSolutionCopy, allTuples, newThreadThreshold));
				}
				else if (threadThreshold == 1){
					generators.add(new SolutionGenerator(currSolution, allTuples, newThreadThreshold));
					solutions.addAll(generateAllSolutionCombos(currSolutionCopy, allTuples));
				}
				else{
					solutions.addAll(generateAllSolutionCombos(currSolution, allTuples));
					solutions.addAll(generateAllSolutionCombos(currSolutionCopy, allTuples));
				}
				ExecutorService executor = Executors.newFixedThreadPool(2);
				List<Future<ArrayList<ArrayList<Tuple>>>> results = null;
				try {
					results = executor.invokeAll(generators);
					executor.shutdown();
					for (Future<ArrayList<ArrayList<Tuple>>> result: results){
						solutions.addAll(result.get());
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return solutions;
		}
	}
	
	
	
	public ArrayList<Tuple> calcOptimalScore(String modelsFile){
		long startTime = System.currentTimeMillis();
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		ArrayList<Model> subModels = new ArrayList<Model>();
		subModels.addAll(models.subList(0, 8));
		models = subModels;
		TupleGenerator tupGen = new TupleGenerator(models, new Tuple(), 0, null, THREAD_MAX);
		ArrayList<Tuple> allTuples = tupGen.call();
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		System.out.println(allTuples.size() + " time:" + execTime / (1000));
		SolutionGenerator solnGenerator = new SolutionGenerator(new ArrayList<Tuple>(), allTuples, THREAD_MAX);
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
