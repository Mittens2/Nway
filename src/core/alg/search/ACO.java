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
	private int alpha;
	private int beta;
	private double roe;
	private double delta;
	private double total;
	
	public ACO(ArrayList<Model> models, int workers, int numIters, int alpha, int beta, double roe, double delta){
		this.models = models;
		this.rand = new Random();
		this.workers = workers;
		this.numIters = numIters;
		this.alpha = alpha;
		this.beta = beta;
		this.roe = roe;
		this.delta = delta;
	}
	
	public RunResult runACO(){
		long startTime = System.currentTimeMillis();
		// Get all tuples using ParallelOptimal
		ParallelOptimal po = new ParallelOptimal(models);
		po.optimalSolution();
		tuples = po.getTuplesInMatch();
		// Initialize heuristic, tau and total
		heuristics = new double[tuples.size()];
		tau = new double[tuples.size()];
		IntStream.range(0, tuples.size())
			.forEach(i -> heuristics[i] = tuples.get(i).getWeight().doubleValue());
		total = Math.pow(Arrays.stream(heuristics).parallel().sum(), alpha);
		// Run ACO for numIters
		for (int i = 0; i < numIters; i++){
			List<Set<Integer>> solutions = new ArrayList<Set<Integer>>(workers);
			for (int j = 0; j< workers; j++)
				solutions.add(new HashSet<Integer>());
			solutions = solutions.parallelStream()
					.map(a -> singleRun(tuples))
					.collect(Collectors.toList());
			updateProbs(solutions);
			total = Math.pow(Arrays.stream(heuristics).parallel().sum(), alpha) + 
					Math.pow(Arrays.stream(tau).parallel().sum(), beta);
		}
		
		// Calculate final solution with single run of ACO
		Set<Integer> finalSolution = finalSolution(tuples);
		solution = (ArrayList<Tuple>) finalSolution.parallelStream()
				.map(i -> tuples.get(i))
				.collect(Collectors.toList());
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		return new RunResult(execTime, weight, 
				weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX), (ArrayList<Tuple>) solution);
		
	}
	
	public Set<Integer> singleRun(List<Tuple> allTuples){
		Set<Integer> rem = IntStream.range(0, allTuples.size()).boxed().collect(Collectors.toSet());
		Set<Integer> solution = new HashSet<Integer>();
		double tot = total;
		while (rem.size() != 0){
			int ind = choose(tot, rem);
			//System.out.println(ind);
			tot -= (tau[ind] + heuristics[ind]);
			solution.add(ind);
			rem.remove(ind);
			removeConflicting(allTuples, rem, ind);
		}
		return solution;
	}
	
	private int choose(double tot, Set<Integer> rem){
		double prob = rand.nextDouble();
		List<Integer> keys = new ArrayList<Integer>(rem);
		Collections.sort(keys);
		double sum = 0;
		int i = keys.get(keys.size() - 1);
		for (Integer key: keys){
			sum += Math.pow(heuristics[key], alpha) + Math.pow(tau[key], beta);
			if (sum / tot >= prob){
				i = key;
				break;
			}
		}
		return i;
	}
	
	private void removeConflicting(List<Tuple> tuples, Set<Integer> rem, int ind){
		Set<Element> els = new HashSet<Element>(tuples.get(ind).getElements());
		Set<Integer> toRem = new HashSet<Integer>();
		for (Integer r: rem){
			if (tuples.get(r).getElements().stream().anyMatch(e -> els.contains(e)))
				toRem.add(r);
		}
		rem.removeAll(toRem);
	}
	
	private void updateProbs(List<Set<Integer>> solutions){
		// evaporation
		Arrays.stream(tau).parallel().map(p -> (1 - roe) * p);
		//reinforcement (Should probably figure out how to parallelize this)
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
		chosen.entrySet().parallelStream()
			.forEach(e -> tau[e.getKey()] += e.getValue() * delta);
	}
	
	private Set<Integer> finalSolution(List<Tuple> allTuples){
		// Do Greedy to find final solution
		Set<Integer> rem = IntStream.range(0, allTuples.size()).boxed().collect(Collectors.toSet());
		Set<Integer> solution = new HashSet<Integer>();
		while (rem.size() != 0){
			int ind = IntStream.range(0, tau.length).filter(i -> rem.contains(i))
			  .boxed().max(Comparator.comparingDouble(i -> Math.pow(tau[i], beta) + Math.pow(heuristics[i], alpha)))
			  .get();
			solution.add(ind);
			rem.remove(ind);
			removeConflicting(allTuples, rem, ind);
		}
		return solution;
	}
	
	public ArrayList<Tuple> getTuplesInMatch() {
		// TODO Auto-generated method stub
		return solution;
	}
	
}
