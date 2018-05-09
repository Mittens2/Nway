package core.alg.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.domain.Element;

import core.alg.optimal.ParallelOptimal;
import core.common.AlgoUtil;
import core.common.N_WAY;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.RunResult;

public class ACO {
	private Map<List<Integer>, BigDecimal> tensor;
	private ArrayList<Model> models;
	private ArrayList<Tuple> tuples;
	private ArrayList<Tuple> solution;
	private double[] heuristics;
	private double[] tau;
	private Random rand;
	private int workers;
	private int numIters;
	private double alpha;
	private double beta;
	private double roe;
	private double total;
	private int minProps;
	
	public ACO(ArrayList<Model> models, int workers, int numIters, double alpha, double beta, double roe, int minProps){
		this.models = models;
		this.rand = new Random();
		this.workers = workers;
		this.numIters = numIters;
		this.alpha = alpha;
		this.beta = beta;
		this.roe = roe;
		this.minProps = minProps;
	}
	
	/**
	 * Runs ACO for NwM.
	 * 
	 * @return RunResult containing information about the final solution.
	 */
	public RunResult runACO(){
		long startTime = System.currentTimeMillis();
		
		// Get all tuples using ParallelOptimal
		ParallelOptimal po = new ParallelOptimal(models, minProps);
		po.generateTuples();
		tuples = po.getTuplesInMatch();
		System.out.println(tuples.size());
		
		// Initialize heuristic, tau and total
		tau = new double[tuples.size()];
		Arrays.parallelSetAll(tau, i -> 1);
		heuristics = new double[tau.length];
		IntStream.range(0, tau.length).parallel()
			.forEach(i -> heuristics[i] = tuples.get(i).getWeight().doubleValue());
		double[] totArr = new double[tau.length];
		Arrays.parallelSetAll(totArr, i -> Math.pow(heuristics[i], alpha) * Math.pow(tau[i], beta));
		total = Arrays.stream(totArr).parallel().sum();
		for (int i = 0; i < numIters; i++){
			List<Set<Integer>> solutions = new ArrayList<Set<Integer>>(workers);
			for (int j = 0; j< workers; j++)
				solutions.add(new HashSet<Integer>());
			solutions = solutions.parallelStream()
					.map(a -> singleRun())
					.collect(Collectors.toList());
			updateProbs(solutions);
			Arrays.parallelSetAll(totArr, j -> Math.pow(heuristics[j], alpha) * Math.pow(tau[j], beta));
			total = Arrays.stream(totArr).parallel().sum();
		}
//		long midTime = System.currentTimeMillis();
		//System.out.println(midTime - startTime);
		
		// Calculate final solution with single run of ACO
		Set<Integer> finalSolution = finalSolution();
		solution = (ArrayList<Tuple>) finalSolution.parallelStream()
				.map(i -> tuples.get(i))
				.collect(Collectors.toList());
		long endTime = System.currentTimeMillis();
		//System.out.println(endTime - midTime);
		long execTime = endTime - startTime;
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		return new RunResult(execTime, weight, 
				weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX), (ArrayList<Tuple>) solution);
		
	}
	
	/**
	 * Single run of a worker.
	 * 
	 * @return Solution of indices picked to form worker solution.
	 */
	public Set<Integer> singleRun(){
		Set<Integer> rem = IntStream.range(0, tuples.size()).boxed().collect(Collectors.toSet());
		Set<Integer> solution = new HashSet<Integer>();
		double tot = total;
		while (rem.size() != 0){
			int ind = choose(tot, rem);
			tot -= (Math.pow(tau[ind], beta) * Math.pow(heuristics[ind], alpha));
			solution.add(ind);
			rem.remove(ind);
			removeConflicting(rem, ind);
		}
		return solution;
	}
	
	/**
	 * Chooses random tuple to add to solution based on probabilities of tuples.
	 * 
	 * @param tot current total of probabilities (needed for normalization).
	 * @param rem The remaining indices that need to be considered for addition.
	 * @return
	 */
	private int choose(double tot, Set<Integer> rem){
		double prob = rand.nextDouble();
		List<Integer> keys = new ArrayList<Integer>(rem);
		Collections.sort(keys);
		double sum = 0;
		int i = keys.get(keys.size() - 1);
		for (Integer key: keys){
			sum += Math.pow(heuristics[key], alpha) * Math.pow(tau[key], beta);
			if (sum / tot >= prob){
				i = key;
				break;
			}
		}
		return i;
	}
	
	/**
	 * Removes all tuples that are not disjoint from tuple at ind.
	 * 
	 * @param rem Remaining tuples.
	 * @param ind Index of tuple picked to be added to solution.
	 */
	private void removeConflicting(Set<Integer> rem, int ind){
		Set<Element> els = new HashSet<Element>(tuples.get(ind).getElements());
		Set<Integer> toRem = new HashSet<Integer>();
		for (Integer r: rem){
			if (tuples.get(r).getElements().stream().anyMatch(e -> els.contains(e)))
				toRem.add(r);
		}
		rem.removeAll(toRem);
	}
	
	/**
	 * Updates the probabilities based on solutions constructed by workers.
	 * 
	 * @param solutions Set of worker solutions.
	 */
	private void updateProbs(List<Set<Integer>> solutions){
		// evaporation
		Arrays.stream(tau).parallel().map(p -> (1 - roe) * p);
		// reinforcement (Should probably figure out how to parallelize this)
		Map<Integer, Integer> chosen = new HashMap<Integer, Integer>();
		for (Set<Integer> solution: solutions){
			Iterator<Integer> iter = solution.iterator();
			while (iter.hasNext()){
				int tup = iter.next();
				if (chosen.containsKey(tup))
					chosen.put(tup, chosen.get(tup) + 1);
				else
					chosen.put(tup, 0);
			}
		}
		double totFit = Arrays.stream(heuristics).parallel().sum();
		chosen.entrySet().parallelStream()
			.forEach(e -> tau[e.getKey()] += e.getValue() * (heuristics[e.getKey()] / totFit));
	}
	
	/**
	 * Greedily choose final solution to ACO.
	 * 
	 * @return list of indices used to form final solution.
	 */
	private Set<Integer> finalSolution(){
		// Do Greedy to find final solution
		Set<Integer> rem = IntStream.range(0, tuples.size()).boxed().collect(Collectors.toSet());
		Set<Integer> solution = new HashSet<Integer>();
		while (rem.size() != 0){
			int ind = IntStream.range(0, tau.length).parallel().filter(i -> rem.contains(i))
			  .boxed().max(Comparator.comparingDouble(i -> Math.pow(tau[i], beta) * Math.pow(heuristics[i], alpha)))
			  .get();
			solution.add(ind);
			rem.remove(ind);
			removeConflicting(rem, ind);
		}
		return solution;
	}
	
	/**
	 * @return solution.
	 */
	public ArrayList<Tuple> getTuplesInMatch() {
		// TODO Auto-generated method stub
		return solution;
	}
	
}
