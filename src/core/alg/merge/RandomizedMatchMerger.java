package core.alg.merge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

import core.alg.Matchable;
import core.alg.local.BestFoundLocalSearch;
import core.alg.local.FirstFoundLocalSearch;
import core.common.AlgoUtil;
import core.common.ElementComparator;
import core.common.ModelComparator;
import core.common.N_WAY;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.RunResult;

public class RandomizedMatchMerger extends Merger implements Matchable {
	protected ArrayList<Tuple> solution;
	protected ArrayList<Element> unusedElements;
	private RunResult res;
	private MergeDescriptor md;
	private boolean classic;
	private boolean toRandomize = true;

	public RandomizedMatchMerger(ArrayList<Model> models, MergeDescriptor md, boolean classic){
		super(models);
		this.md = md;
		this.classic = classic;
	}
	
	public void run(){
		long startTime = System.currentTimeMillis();
		unusedElements = joinAllModels();
		solution = execute();
		BigDecimal weight = AlgoUtil.calcGroupWeight(solution);
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		BigDecimal avgTupleWeight = weight.divide(new BigDecimal(solution.size()), N_WAY.MATH_CTX);
		res = new RunResult(execTime, weight, avgTupleWeight, solution);
		res.setTitle("Randomized");
		
	}
	
	private ArrayList<Element> joinAllModels() {
		/**
		 * Joins all of the merger's models into a single Elements list.
		 * 
		 * @return The list of all of the models' Elements.
		 */
		
		ArrayList<Element> elems = new ArrayList<Element>();
		if (md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE_ELEMENT_SIZE){
			Collections.sort(models, new ModelComparator(md.asc));
		}
		for(Model m:models){
			ArrayList<Element> modelElems = m.getElements();
			if (md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE_ELEMENT_SIZE){
				Collections.sort(modelElems, new ElementComparator(md.elementAsc));
			}
			elems.addAll(modelElems);
		}
		return elems;
	}
	
	/*private HashMap<String, Integer> getSortedProperties(ArrayList<Element> elems){
		HashMap<String, Integer> propFreq = new HashMap<String, Integer>();
		for (Element e: elems){
			for (String p: e.getProperties()){
				int count = propFreq.containsKey(p) ? propFreq.get(p) : 0;
				propFreq.put(p, count + 1);
			}
		}
		return propFreq;
	}*/
	
	private ArrayList<Tuple> execute(){
		/**
		 * Executes an instance of the randomized merge algorithm (DumbHuman).
		 * 
		 * @return The list of Tuples derived from performing randomized merge on given models.
		 */
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		while(unusedElements.size() > 1){
			Element picked = unusedElements.get(0);
			unusedElements.remove(0);
			Tuple bestTuple = getBestTuple(new ArrayList<Element>(unusedElements), picked);
			result.add(bestTuple);
		}
		if (toRandomize){
			Algorithm geneticAlgorithm = new Algorithm();
			Population pop = geneticAlgorithm.evolvePopulation(new Population(result), 0);
			result = pop.convertToTuples();
		}
		return result;
	}
	
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
			// If want to consider larger pool of elements.
			if (!classic){
				incompatible.addAll(partition.get(1));
				for (Element e: best.getElements()){
					partition = AlgoUtil.partitionShared(e, incompatible, best.getSize());
					elems.addAll(partition.get(0));
					incompatible = partition.get(1);
				}
			}
			picked = getMaxElement(elems, best);
			unusedElements.remove(picked);
		}
		return best;
	}
	
	private Element getMaxElement(ArrayList<Element> elems, Tuple best){
		BigDecimal maxWeight = best.calcWeight(models);
		Element maxElement = null;
		for (Element e: elems){
			Tuple test = best.newExpanded(e, models);
			if (test.calcWeight(models).compareTo(maxWeight) > 0){
				maxElement = e;
				maxWeight = test.calcWeight(models);
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
	public class Individual {
	    static final int defaultGeneLength = 16;
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
	public class Population {
	    private ArrayList<Individual> individuals;
	    private ArrayList<Individual> sortedIndividuals;
	    private ArrayList<Element> mutations;
	    private Comparator<Individual> cmp;
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
	        cmp = new Comparator<Individual>() {
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
			};
	    }

	    /* Getters */
	    public Individual getIndividual(int index) {
	        return individuals.get(index);
	    }

	    public Individual getFittest(int start) {
	        return sortedIndividuals.get(0);
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
	    	return sortedIndividuals;
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
	    		tuples.add(t);
	    	}
	    	return tuples;
	    }

	}
	public class Algorithm {
	    /* GA parameters */
	    private static final double uniformRate = 0.01;
	    private static final double mutationRate = 0.015;
	    private static final int tournamentSize = 5;
	    private static final boolean elitism = true;
	    private static final int elitismSplit = 2;
	    private Random rand = new Random();
	    private int stableCycles = 1000;

	    /* Public methods */
	    
	    // Evolve a population
	    public Population evolvePopulation(Population pop, int stablePops) {
	    	if (stablePops == stableCycles){
	    		return pop;
	    	}
	    	int keep = pop.size() / elitismSplit;
	        ArrayList<Individual> oldIndividuals = pop.getSortedIndividuals();
	        ArrayList<Individual> newIndividuals = new ArrayList<Individual>();
	        // Keep our best individuals
        	for (int i = 0; i < keep; i++){
        		newIndividuals.add(oldIndividuals.get(i));
        	}
        	
	        // Loop over the population size and create new individuals with
	        // crossover
	        for (int i = keep; i < pop.size() - 1; i += 2) {
	            Individual indiv1 = oldIndividuals.get(i);
	            Individual indiv2 = oldIndividuals.get(i + 1);
	            newIndividuals.addAll(crossover(indiv1, indiv2));
	        }
	        
	        /*for (int i = pop.size() / elitismSplit; i < newIndividuals.size(); i++) {
	            mutate(newIndividuals.get(i), pop);
	        }*/
	        
	        Population newPop = new Population(new ArrayList<Tuple>());
	        for (Individual i: newIndividuals){
	    	   newPop.addIndividual(i);
	        }
	        //System.out.println(newPop.getFitness());
	        //System.out.println(pop.getFitness()); 
	        if (newPop.getFitness() > pop.getFitness()){
	        	System.out.println("here");
	        	return evolvePopulation(newPop, stablePops);
	        }
	        else{
	        	//System.out.println("moped");
	        	return evolvePopulation(pop, stablePops + 1);
	        }
	        //return newPopulation;
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
	    	int random = rand.nextInt(pop.mutations.size());
	        if (Math.random() <= mutationRate) {
	            // Create random gene
	        	Element mutant = pop.mutations.get(random);
	        	pop.mutations.remove(random);
	        	pop.mutations.add(indiv.getGene(Integer.parseInt(mutant.getModelId()) - 1));
	            indiv.setGene(Integer.parseInt(mutant.getModelId()) - 1, mutant);
	        }
	    }
	}

}
