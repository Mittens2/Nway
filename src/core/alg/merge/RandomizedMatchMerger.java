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
	private GeneticAlgorithm geneticAlgorithm;

	public RandomizedMatchMerger(ArrayList<Model> models, MergeDescriptor md, double uniRate, double mutRate){
		super(models);
		this.md = md;
		geneticAlgorithm = new GeneticAlgorithm(uniRate, mutRate, 2000);
	}
	
	public void run(){
		long startTime = System.currentTimeMillis();
		unusedElements = joinAllModels();
		solution = execute();
		System.out.println(solution);
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
		if (md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE_ELEMENT_SIZE || md.orderBy == N_WAY.ORDER_BY.PROPERTY){
			Collections.sort(models, new ModelComparator(md.asc));
		}
		for(Model m:models){
			ArrayList<Element> modelElems = m.getElements();
			if (md.orderBy == N_WAY.ORDER_BY.MODEL_SIZE_ELEMENT_SIZE){
				Collections.sort(modelElems, new ElementComparator(md.elementAsc, false));
			}
			elems.addAll(modelElems);
		}
		if (md.orderBy == N_WAY.ORDER_BY.PROPERTY){
			HashMap<String, Integer> propFreq = getSortedProperties(elems);
			for (Element e: elems){
				int score = 0;
				for (String p: e.getProperties()){
					score += propFreq.get(p);
				}
				e.setPropScore(score);
			}
			//Collections.sort(elems, new ElementComparator(md.asc, true));
			elems = new ArrayList<Element>();
			for (Model m: models){
				ArrayList<Element> modelElems = m.getElements();
				Collections.sort(modelElems, new ElementComparator(md.elementAsc, true));
				elems.addAll(modelElems);
			}
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
	
	private ArrayList<Tuple> execute(){
		/**
		 * Executes an instance of the randomized merge algorithm (DumbHuman).
		 * 
		 * @return The list of Tuples derived from performing randomized merge on given models.
		 */
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		while(unusedElements.size() > 0){
			Element picked = unusedElements.get(0);
			unusedElements.remove(0);
			Tuple bestTuple = getBestTuple(new ArrayList<Element>(unusedElements), picked);
			result.add(bestTuple);
		}
		if (md.randomize){
			result = runGeneticAlgorithm(result);
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
			if (!md.classic){
				incompatible.addAll(partition.get(1));
				//for (Element e: best.getElements()){
					//partition = AlgoUtil.partitionShared(e, incompatible, best.getSize());
					partition = AlgoUtil.getElementsWithSharedProperties(best, incompatible, 
							(int) Math.pow(best.getSize(), 2));
					elems.addAll(partition.get(0));
					incompatible = partition.get(1);
				//}
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
	    private final static int defaultGeneLength = 8;
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
