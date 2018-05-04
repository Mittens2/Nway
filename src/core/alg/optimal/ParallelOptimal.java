package core.alg.optimal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import core.domain.Model;
import core.domain.Tuple;
import core.domain.Element;
import core.execution.RunResult;


public class ParallelOptimal {

	
	private ArrayList<Model> models;
	private ArrayList<Tuple> tuples;
	
	public ParallelOptimal(ArrayList<Model> models){
		this.models = models;
	}
	
	
	public RunResult optimalSolution(){
		long startTime = System.currentTimeMillis();
		Collector<Model, List<Tuple>, List<Tuple>> modelCollector =
			    Collector.of(
			    	() -> initialize(), // supplier
			        (l, m) -> l.addAll(combine(l, m, models)), // accumulator
			        (l1, l2) -> merge(l1, l2)); // combiner         
		
		tuples = (ArrayList<Tuple>) models
				.parallelStream()
				.collect(modelCollector);
		tuples = (ArrayList<Tuple>) tuples.parallelStream()
				.filter(t -> t.getSize() > 0)
				.collect(Collectors.toList());
		long endTime = System.currentTimeMillis();
		long execTime = endTime - startTime;
		return new RunResult(execTime, BigDecimal.ZERO, BigDecimal.ZERO, (ArrayList<Tuple>) tuples);
	}
	
	private List<Tuple> initialize(){
		Tuple t = new Tuple();
		ArrayList<Tuple> init = new ArrayList<Tuple>();
		init.add(t);
		return init;
	}
	
	private List<Tuple> combine(List<Tuple> l, Model m, ArrayList<Model> mdls){
		List<Tuple> combinations = 
			     l.parallelStream()
			        .flatMap(t1 -> m.getElements().parallelStream()
			        		.filter(e -> t1.getSize() == 0 || intersection(getProperties(t1), e.getProperties()).size() != 0)			  
			        		.map(e -> t1.newExpanded(e, mdls)))
			        .collect(Collectors.toList());
		return combinations;
	}
	
	private List<Tuple> merge(List<Tuple> l1, List<Tuple> l2){
		List<Tuple> combinations = 
			     l1.parallelStream()
			        .flatMap(t1 -> l2.parallelStream()
			        		.filter(t2 -> t2.getSize() == 0 || t1.getSize() == 0 || intersection(getProperties(t1), getProperties(t2)).size() != 0)
			        		.map(t2 -> join(t1, t2)))
			        .collect(Collectors.toList());
		return combinations;
	}
	
	private Tuple join(Tuple t1, Tuple t2){
		Tuple t3 = new Tuple();
		t3.addElements(t1.getElements());
		t3.addElements(t2.getElements());
		if (t3.getSize() > 0)
			t3.setWeight(t3.calcWeight(models));
		return t3;
	}
	
	private Set<String> getProperties(Tuple t){ 
		return t.getElements().parallelStream()
				 .flatMap(e -> e.getProperties().parallelStream())
			     .collect(Collectors.toSet());
	}
	
	private Set<String> intersection(Set<String> s1, Set<String> s2){
		s1.retainAll(s2);
		return s1;
	}
	
	public ArrayList<Tuple> getTuplesInMatch(){
		return tuples;
	}

}
