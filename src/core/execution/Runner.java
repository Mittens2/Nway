package core.execution;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import core.Main;
import core.alg.TupleReader;
import core.alg.merge.ChainingOptimizingMerger;
import core.alg.merge.GreedyMerger;
import core.alg.merge.LocalSearchMerger;
import core.alg.merge.MergeDescriptor;
import core.alg.merge.Merger;
import core.alg.merge.MultiModelMerger;
import core.alg.merge.PairWiseMerger;
import core.alg.merge.RandomizedMatchMerger;
import core.alg.pair.ModelSizeBasedPairWiseMatcher;
import core.common.AlgoUtil;
import core.common.ModelComparator;
import core.common.N_WAY;
import core.common.Statistics;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.UMLParser.UMLClass;

public class Runner extends ResultsWriter{

	private ArrayList<Model> models;
	private ArrayList<RunResult> results;
	private String manualResultsFileLoc;
	private boolean toChunkify;
	

	public Runner(ArrayList<Model> models){
		this(models, null, null, models.size(), true);
	}
	
	public Runner(ArrayList<Model> models, String excelFilePath, String manualResultsFileLoc, int numOfModelsToUse, boolean toChunkify) {
		super(excelFilePath);
		ArrayList<Model> modelSubset = new ArrayList<Model>();
		for(int i=0;i<numOfModelsToUse;i++){
			modelSubset.add(models.get(i));
		}
		if(modelSubset.size() == 0)
			this.models = models;
		else
			this.models = modelSubset;
		this.manualResultsFileLoc = manualResultsFileLoc;
		this.toChunkify = toChunkify;
		results = new ArrayList<RunResult>();
	}
	
	private void addManualRun() {
		if(manualResultsFileLoc == null)
			return;
		TupleReader reader = new TupleReader(models, manualResultsFileLoc);
		RunResult manual = reader.getResult();
		ArrayList<RunResult> man = new ArrayList<RunResult>();
		man.add(manual);
		writeResults(man, "Manual");
	}

	public void execute(String caseName){
				
//		runOnLocalSearch(N_WAY.ALG_POLICY.REPLACE_BEST, "LS triwise");
	//	runOnLocalSearch(N_WAY.ALG_POLICY.REPLACE_FIRST_BY_SQUARES, "LS triwise");
		//addManualRun();
		AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		//runOnPairs();
		/*if(!toChunkify){
			runOnGreedy(models.size());
		}*/
		//else{
		//	runOnGreedy(3);
			//runOnGreedy(4);
		//runOnGreedy(5);
		//}
		AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;
		//results.addAll(runBigHungarian(caseName));
		results.addAll(runRandomizedMatch());
		//results.addAll(runNwMwithHS(caseName));
		
		//runLocalSearches(3);
		/*if(! toChunkify){
			runLocalSearches(models.size());
		}
		else{
	//		runLocalSearches(3);
		}*/
	//	runOnGreedy(5);
		//runOnGreedy(6);
		//runOnGreedy(7);
		//runOnGreedy(8);
		//runOnLocalSearch(3); // without graph
		//runOnLocalSearchWithBuckets(3, 1600, 17);
		//runOnLocalSearchWithBuckets(4, 40^3, 17);
		//runOnLocalSearchWithBuckets(5, 40^4, 17);
		//runOnLocalSearchWithBuckets(6, 40^5, 17);
		//runOnLocalSearchWithBuckets(7, 40^6, shift2);
		//runOnLocalSearchWithBuckets(8, 40^7, shift2);
		//runOnTotalLocalSearchWithBuckets(3, 1600, shift2);
		
		
	}
	
	private void runLocalSearches(int splitSize) {
		runLSOnThread(N_WAY.ALG_POLICY.REPLACE_FIRST, "Replace first triwise", splitSize);
		//runLSOnThread(N_WAY.ALG_POLICY.REPLACE_BEST, "Replace best triwise",splitSize);
		//runLSOnThread(N_WAY.ALG_POLICY.REPLACE_BEST_BY_SQUARES, "Replace best triwise by squares",splitSize);
	}

	private void runLSOnThread(final N_WAY.ALG_POLICY pol, final String sheetName,int splitSize) {
//		RunnerThread rt;
//		rt = new RunnerThread(this) {
//			@Override
//			protected void perform(Runner rnr) { 
				runOnLocalSearch(pol, sheetName, splitSize);
//				}};
//		rt.start();
	}

	private RunResult runLS(MergeDescriptor md, int splitSize){
		return performMerge(md, splitSize);
	}
	
	public ArrayList<RunResult> runOnLocalSearch(N_WAY.ALG_POLICY pol, String sheetName, int splitSize){
		ArrayList<RunResult> results = new ArrayList<RunResult>();
		ArrayList<MergeDescriptor> mds = allPermOnAlg(pol);
		for(MergeDescriptor md:mds){
			results.add(runLS(md, splitSize));
		}
		writeResults(results, sheetName+splitSize);
		return results;
	}
	
	public ArrayList<RunResult> runOnGreedy(int splitSize) {
		ArrayList<RunResult> results = new ArrayList<RunResult>();
		ArrayList<MergeDescriptor> mds = allPermOnAlg(N_WAY.ALG_POLICY.GREEDY);
		for(MergeDescriptor md:mds){
			results.add(performMerge(md, splitSize));
		}
		
		writeResults(results, "Greedy "+splitSize+(toChunkify?"-chunked":"-on-all")+(AlgoUtil.COMPUTE_RESULTS_CLASSICALLY?"-classic":"-improved"));
		return results;
	}
	
	private Merger getMergerBasedOnPolicy( N_WAY.ALG_POLICY pol, ArrayList<Model> modelsToUse){
		N_WAY.ALG_POLICY policyToUse = pol;
		if(pol == N_WAY.ALG_POLICY.GREEDY)
			return new GreedyMerger(modelsToUse);
		boolean doSquaring = false;
		if(pol == N_WAY.ALG_POLICY.REPLACE_FIRST_BY_SQUARES || pol == N_WAY.ALG_POLICY.REPLACE_BEST_BY_SQUARES)
			 doSquaring = true; 
		
		if(pol == N_WAY.ALG_POLICY.REPLACE_FIRST_BY_SQUARES)
			policyToUse =  N_WAY.ALG_POLICY.REPLACE_FIRST;
		
		if(pol == N_WAY.ALG_POLICY.REPLACE_BEST_BY_SQUARES)
			policyToUse =  N_WAY.ALG_POLICY.REPLACE_BEST;
		return new LocalSearchMerger(modelsToUse, policyToUse, doSquaring);
	}
	
	private RunResult performMerge(MergeDescriptor md, int splitSize){
		long startTime = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		ArrayList<Model> mdls = (ArrayList<Model>) models.clone();
		GreedyMerger merger = null;
		Model matched = null;
		while(mdls.size() >= 2){
			mdls = sortModels(mdls, md);
			ArrayList<Model> modelsToUse = new ArrayList<Model>();
			for(int i=0;i<splitSize && i < mdls.size();i++){
				modelsToUse.add(mdls.get(i));
			}
			merger = (GreedyMerger) getMergerBasedOnPolicy(md.algPolicy, modelsToUse);
			matched = merger.mergeMatchedModels();
			mdls.removeAll(modelsToUse);
			mdls.add(matched);
		}
		RunResult rr = null;
		
		boolean storedVal = AlgoUtil.COMPUTE_RESULTS_CLASSICALLY;
		AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;

		
		if(mdls.size() == 2){
			PairWiseMerger pwm = new PairWiseMerger(mdls,md, true);
			pwm.run();
			pwm.refreshResultTuplesWeight(pwm.mergeMatchedModels());
			rr = pwm.getRunResult(models.size());
		}
		else{
			if(storedVal)
				merger.refreshTuplesElements();
			else
				merger.refreshResultTuplesWeight(matched);
			if(AlgoUtil.areThereDuplicates(merger.extractMerge())){
				System.out.println("PROBLEM");
			}
			AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = storedVal;		
			rr = merger.getRunResult(models.size());
			AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;
			//System.out.println(merger.mergeMatchedModels());
			//AlgoUtil.printTuples(merger.extractMerge());
		}
		
		rr.setTitle(AlgoUtil.nameOfMergeDescription(md, splitSize));
		long endTime = System.currentTimeMillis();
		rr.setExecTime(endTime-startTime);
		System.out.println(rr);
		AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = storedVal;		
		return rr;
	}

	public ArrayList<RunResult> runOnPairs() {
		ArrayList<RunResult> results = new ArrayList<RunResult>();
		ArrayList<MergeDescriptor> mds = allPermOnAlg(N_WAY.ALG_POLICY.PAIR_WISE);
		for(MergeDescriptor md:mds){
			results.add(runPair(md));
		}
		writeResults(results, "Pairwise"+models.size()+(AlgoUtil.COMPUTE_RESULTS_CLASSICALLY?"-classic":"-improved"));
		return results;
	}

	private RunResult runPair(MergeDescriptor md) {
		@SuppressWarnings("unchecked")
		PairWiseMerger pwm = new PairWiseMerger((ArrayList<Model>) models.clone(), md, false);
		pwm.run();
		pwm.refreshResultTuplesWeight(pwm.mergeMatchedModels());
		RunResult rr = pwm.getRunResult(models.size());
		rr.setTitle(AlgoUtil.nameOfMergeDescription(md, -1));
		System.out.println(rr);
		return rr;
	}
	
	public ArrayList<RunResult> runBigHungarian(String caseName){
		@SuppressWarnings("unchecked")
		RunResult rr = null;
		File file = new File(Main.home + "models/NwMsolutions/" + caseName + ".csv");
		ArrayList<Tuple> solution = null;
		if (file.exists()){
			solution = loadTuplesFromFile(file);
			rr = new RunResult(0, AlgoUtil.calcGroupWeight(solution), BigDecimal.ZERO, solution);
		}
		else{
			MultiModelMerger mmm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
			mmm.run();
			solution = mmm.getTuplesInMatch();
			rr = mmm.getRunResult(models.size());
			rr.setTitle("New Hungarian");
		}
		ArrayList<RunResult> result = new ArrayList<RunResult>();
		result.add(rr);
		System.out.println(rr);
		//AlgoUtil.printTuples(mmm.getTuplesInMatch());
		AlgoUtil.printTuples(solution);
		//writeResults(result, "New Hungarian");
		return result;
	}
	
	public ArrayList<RunResult> runNwMwithHS(String caseName){
		@SuppressWarnings("unchecked")
		ArrayList<RunResult> results = new ArrayList<RunResult>();
		ArrayList<MergeDescriptor> mds = allPermOnAlg(N_WAY.ALG_POLICY.RANDOM);
		ArrayList<Tuple> prevSolution = new ArrayList<Tuple>();
		File file = new File(Main.home + "models/NwMsolutions/" + caseName + ".csv");
		if (!file.exists()){
			MultiModelMerger mmm = new ChainingOptimizingMerger((ArrayList<Model>) models.clone());
			mmm.run();
			System.out.println(AlgoUtil.calcGroupWeight(mmm.getTuplesInMatch()));
			writeTuplesToFile(mmm.getTuplesInMatch(), file.getPath());
		}
		prevSolution = loadTuplesFromFile(file);
		System.out.println("NwM alone: " + AlgoUtil.calcGroupWeight(prevSolution));
		//AlgoUtil.printTuples(prevSolution);
		for(MergeDescriptor md:mds){
			for (Tuple t: prevSolution){
				for (Element e: t.getElements()){
					e.setContainingTuple(t);
				}
			}
			RunResult rr = runBestAlgo(md, prevSolution);
			results.add(rr);
		}
		writeResults(results, "Randomized");
		return results;
	}
	
	private ArrayList<Tuple> loadTuplesFromFile(File f){
		ArrayList<Tuple> nwmSolution = new ArrayList<Tuple>();
		try{
			Scanner scan = new Scanner(f);
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
	
	private void writeTuplesToFile(ArrayList<Tuple> nwmSolution, String filePath){
		try{
			PrintWriter writer = new PrintWriter(filePath, "utf-8");
			for (int i = 0; i < nwmSolution.size(); i++){
				for (Element e: nwmSolution.get(i).getElements()){
					writer.println(i + ";" + e.getModelId() + ";" + e.getLabel());
				}
			}
			writer.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private RunResult runBestAlgo(MergeDescriptor md, ArrayList<Tuple> prevSolution){
		
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md);
		//System.out.println(models);
		rmm.improveSolution(prevSolution);
		RunResult rr = rmm.getRunResult(models.size());
		//System.out.println(models.size());
		rr.setTitle(AlgoUtil.nameOfMergeDescription(md, -1));
		System.out.println(rr);
		//AlgoUtil.printTuples(rmm.getTuplesInMatch());
		return rr;
	}
	
	public ArrayList<RunResult> runRandomizedMatch(){
		@SuppressWarnings("unchecked")
		ArrayList<RunResult> results = new ArrayList<RunResult>();
		ArrayList<MergeDescriptor> mds = allPermOnAlg(N_WAY.ALG_POLICY.RANDOM);
		for(MergeDescriptor md:mds){
			RunResult rr = runRandomMatch(md);
			results.add(rr);
		}
		writeResults(results, "Randomized");
		return results;
	}
	
	private RunResult runRandomMatch(MergeDescriptor md){
		RandomizedMatchMerger rmm = new RandomizedMatchMerger((ArrayList<Model>) models.clone(), md);
		//System.out.println(models);
		rmm.improveSolution(new ArrayList<Tuple>());
		RunResult rr = rmm.getRunResult(models.size());
		//System.out.println(models.size());
		rr.setTitle(AlgoUtil.nameOfMergeDescription(md, -1));
		System.out.println(rr);
		//AlgoUtil.printTuples(rmm.getTuplesInMatch());
		return rr;
	}
	
	private ArrayList<MergeDescriptor> allPermOnAlg(N_WAY.ALG_POLICY pol){
		ArrayList<MergeDescriptor> retVal = new ArrayList<MergeDescriptor>();
		if (pol == N_WAY.ALG_POLICY.RANDOM){
			//boolean randomize = false;
			for (int highlight = 3; highlight < 4; highlight++){
				for (int choose = 1; choose < 2; choose++){
					for (int reshuffle = 2; reshuffle < 3; reshuffle++){
//						boolean switchTuples = true;
//						boolean switchModels = false;
						// Seedings used for improving on NwM.
//						retVal.add(new MergeDescriptor(false, false, highlight, choose, reshuffle, 0));
//						retVal.add(new MergeDescriptor(true, true, highlight, choose, reshuffle, 1));
//						retVal.add(new MergeDescriptor(false, false, highlight, choose,  reshuffle, 1));
//						retVal.add(new MergeDescriptor(true, true, highlight, choose, reshuffle, 2));
//						retVal.add(new MergeDescriptor(false, false, highlight, choose,reshuffle, 2));
						retVal.add(new MergeDescriptor(true, true, highlight, choose, reshuffle, 3));
						//retVal.add(new MergeDescriptor(false, false, highlight, choose, reshuffle, 3));
					}
				}
			}
			return retVal;
		}
		retVal.add(new MergeDescriptor(pol, true, N_WAY.ORDER_BY.MODEL_ID));
		if(toChunkify || pol == N_WAY.ALG_POLICY.PAIR_WISE){
			retVal.add(new MergeDescriptor(pol, true, N_WAY.ORDER_BY.MODEL_SIZE));
			retVal.add(new MergeDescriptor(pol, false, N_WAY.ORDER_BY.MODEL_SIZE));
		}
//		if(pol == N_WAY.ALG_POLICY.PAIR_WISE){
//			retVal.add(new MergeDescriptor(pol, true, N_WAY.ORDER_BY.MATCH_QUALITY));
//			retVal.add(new MergeDescriptor(pol, false, N_WAY.ORDER_BY.MATCH_QUALITY));
//		}

		//retVal.add(new MergeDescriptor(pol, true, N_WAY.ORDER_BY.COHESIVENESS));
		//retVal.add(new MergeDescriptor(pol, false, N_WAY.ORDER_BY.COHESIVENESS));

	
		
		//retVal.add(new MergeDescriptor(pol, false, N_WAY.ORDER_BY.MODEL_ID));
		return retVal;
	}

	private ArrayList<Model> sortModels(ArrayList<Model> mdls, final MergeDescriptor desc){
		if(desc.orderBy == N_WAY.ORDER_BY.MODEL_SIZE){
			Collections.sort(mdls, new ModelComparator(desc.asc));
			return mdls;
		}
		else if (desc.orderBy == N_WAY.ORDER_BY.COHESIVENESS){
			return AlgoUtil.getModelsByCohesiveness(mdls, desc.asc);
		}
		else{
			Comparator<Model> cmp = new Comparator<Model>() {
				@Override
				public int compare(Model m1, Model m2) {
					if(desc.asc)
						return  (int) (m1.getId().compareTo(m2.getId()));
					else
						return  (int) (m2.getId().compareTo(m1.getId()));
				}
				
			};
			Collections.sort(mdls, cmp);
			return mdls;
		}
			
	}
	 public ArrayList<RunResult> getRunResults(){
		 return results;
	 }
	
//	private RunResult runOn(MergeDescriptor md, int modelSplitSize, int bucketSize){
//		 
//		@SuppressWarnings("unchecked")
//		MultiModelMerger mmm = new MultiModelMerger((ArrayList<Model>) models.clone(), md, modelSplitSize, bucketSize);
//		mmm.run();
//		RunResult retVal = mmm.getRunResult(models.size());
//		System.out.println(retVal);
//		return retVal;
//	}

//	private void runOnBuckets(int modelSplitSize, int bucketSize){
//	ArrayList<RunResult> results = new ArrayList<RunResult>();
//	ArrayList<MergeDescriptor> descriptors = getDescriptors();
//	for(MergeDescriptor md:descriptors){
//		results.add(runOn(md, modelSplitSize, bucketSize));
//	}
//	String sheetName = "b="+bucketSize+", m="+modelSplitSize;
//	writeResults(results, sheetName);
//}
//	private ArrayList<MergeDescriptor> getDescriptors() {
//	ArrayList<MergeDescriptor> retVal = new ArrayList<MergeDescriptor>();
//	retVal.addAll(allPermOnAlg(N_WAY.ALG_POLICY.PAIR_WISE));
//	retVal.addAll(allPermOnAlg(N_WAY.ALG_POLICY.GREEDY));
//	retVal.addAll(allPermOnAlg(N_WAY.ALG_POLICY.REPLACE_FIRST));
//	retVal.addAll(allPermOnAlg(N_WAY.ALG_POLICY.REPLACE_BEST));
//	return retVal;
//}

	public static abstract class RunnerThread extends Thread {
		
		private Runner rnr;

		public RunnerThread(Runner rnr){
			this.rnr = rnr;
		}
		
	    public void run() {
	    	perform(rnr);
	    }
	    
	    protected abstract void perform(Runner rnr);
    }
	

}


