package core;

import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.jfree.ui.RefineryUtilities;

import core.common.AlgoUtil;
import core.common.ResultsPlotter;
import core.common.Statistics;
import core.domain.Model;
import core.execution.BatchRunner;
import core.execution.BatchRunner.BatchRunDescriptor;
import core.execution.RunResult;
import core.execution.Runner;




public class Main {
	

	private static void workOnBatch(String modelsFile, String resultsFile){
		BatchRunner batcher = new BatchRunner(modelsFile, 10, resultsFile);
		batcher.run();
	}
	
	private static void createBatch(String resultsFile, int minNumOfElements, int maxNumOfElements,
			int minPropLength, int maxPropLength, int commonVacabularyMin, int diffVacabularyMin){		
		BatchRunDescriptor desc = new BatchRunDescriptor();
		desc.numOfBatches = 10;
		desc.numOfModelsInBatch = 10;
		desc.minNumOfElements = minNumOfElements;
		desc.maxNumOfElements = maxNumOfElements;
		desc.minPropLength = minPropLength;
		desc.maxPropLength = maxPropLength;
		desc.commonVacabularyMin = commonVacabularyMin;
		desc.diffVacabularyMin = diffVacabularyMin;
		BatchRunner batcher = new BatchRunner(desc, resultsFile);
		batcher.run();
	}
	
	private static void runDetailedExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles){
		/**
		 * Runs and experiment on any algorithm against Nwm.
		 * Outputs results to a graph.
		 */
		double[][] nwmScores = new double[][]{
				{4.595},
				{1.522},
				{0.977, 0.917, 0.839, 0.915, 1.005, 0.991, 1.066, 0.870, 1.077, 0.887},
				{0.998, 0.695, 0.933, 1.055, 0.830, 1.274, 1.049, 0.987, 0.736, 1.027},
				{0.958, 1.001, 0.950, 0.900, 0.893, 0.940, 1.008, 0.974, 0.948, 0.840},
				{0.237},
				{0.279},
				{0.289},
				};
		ResultsPlotter rp = new ResultsPlotter("SmartHuman", "NwM");

		for (int i = 0; i < modelsFiles.size(); i++){
			String mf = modelsFiles.get(i);
			String rf = resultsFiles.get(i);
			String subCase = mf.substring(mf.indexOf("/") + 1, mf.indexOf("."));
			if (subCase.charAt(0) == 'm'){
				subCase = subCase.substring(mf.indexOf("/") + 1);
				//rp.addMultipleValueDatapoint(multipleBatchRun(mf, rf, 10)[0], nwmScores[i], subCase);
			}
			else{
				ArrayList<RunResult> runResults = singleBatchRun(mf, rf, -1, true);
				rp.addSingleValueDataPoint(runResults.get(0).weight.doubleValue(), nwmScores[i][0], subCase);
				if (i == 0){
					rp.setAlg1Label(runResults.get(0).title);
				}
			}
		}
		rp.createChart();
		rp.setMinimumSize(new Dimension(1000, 500));
		rp.pack();
        RefineryUtilities.centerFrameOnScreen(rp);
        rp.setVisible(true);
	}
	
	private static void runSimpleExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles, 
			int runsToAvg){
		double[] nwmScores = {
				4.595,
				1.522,
				0.977, 0.917, 0.839, 0.915, 1.005, 0.991, 1.066, 0.870, 1.077, 0.887,
				0.998, 0.695, 0.933, 1.055, 0.830, 1.274, 1.049, 0.987, 0.736, 1.027,
				0.958, 1.001, 0.950, 0.900, 0.893, 0.940, 1.008, 0.974, 0.948, 0.840
				};
		double[] shScores = new double[nwmScores.length];
		int currInd = 0;
		ArrayList<String> subcases = new ArrayList<String>();
		ResultsPlotter rp = new ResultsPlotter("SmartHuman", "NwM");
		System.out.println(modelsFiles.size());
		for (int i = 0; i < modelsFiles.size(); i++){
			String mf = modelsFiles.get(i);
			String rf = resultsFiles.get(i);
			String subCase = mf.substring(mf.indexOf("/") + 1, mf.indexOf("."));
			System.out.println(subCase);
			if (subCase.charAt(0) == 'm'){
				subCase = subCase.substring(mf.indexOf("/") + 1);
				if (subCase.equals("random"))
					subCase = "r";
				else if (subCase.equals("randomLoose"))
					subCase = "rl";
				else
					subCase = "rt";
				double[][] runScores = new double[10][runsToAvg];
				for (int j = 0; j < runsToAvg; j++){
					double[] result = multipleBatchRun(mf, rf, 10)[0];
					for (int k = 0; k < 10; k++){
						runScores[k][j] = result[k];
					}
				}
				int run = 0;
				for (int j = 0; j < runScores.length; j++){
					Statistics stats = new Statistics(runScores[j]);
					shScores[currInd] = stats.getMean();
					currInd++;
					run++;
					subcases.add(subCase + "" + run);
				}
			
			}
			else{
				double[] runScores = new double[runsToAvg];
				for (int j = 0; j < runsToAvg; j++){
					RunResult run = singleBatchRun(mf, rf, -1, true).get(0);
					if (i == 0 && j == 0){
						rp.setAlg1Label(run.title);
					}
					runScores[j] = run.weight.doubleValue(); 
				}
				Statistics stats = new Statistics(runScores);
				shScores[currInd] = stats.getMean();
				currInd++;
				subcases.add(subCase.charAt(0) + "");
			}	
		}
		rp.createDataset(shScores, nwmScores, subcases);
		rp.createChartSingle();
		rp.setMinimumSize(new Dimension(1000, 500));
		rp.pack();
        RefineryUtilities.centerFrameOnScreen(rp);
        rp.setVisible(true);
	}
	
	private static ArrayList<RunResult> singleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse, boolean toChunkify){
		/**
		 * Runs algorithms over the first numOfModelsToUse models from modelsFile in a single run.
		 * 
		 * @param modelsFile The name of the file to derive the models.
		 * @param resultsFile The name of the file which results should be written to.
		 * @param numOfModelsToUse The number of models to be used in the run.
		 */
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		System.out.println(modelsFile.substring(modelsFile.indexOf("/") + 1, modelsFile.indexOf(".")) + ", num models: " + models.size());
		Runner runner = new Runner(models, resultsFile, null, numOfModelsToUse, toChunkify);
		runner.execute();
		ArrayList<RunResult> runResults = runner.getRunResults();
		/*double[] scores = new double[runResults.size()];
		for (int i = 0; i < runResults.size(); i++){
			scores[i] = runResults.get(i).weight.doubleValue();
		}
		return scores;*/
		return runner.getRunResults();
	}
	
	private static double[][] multipleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse){
		/**
		 * Divides the case defined by modelsFile into batches of numOfModelsToUse models.
		 * Runs algorithms over the set of models designated as many times as numOfModelsToUse
		 * can be taken out of the total models designated by modelsFile. 
		 * Prints average weight of the runs at the end.
		 * 
		 * @param modelsFile The name of the file to derive the models.
		 * @param resultsFile The name of the file which results should be written to.
		 * @param numOfModelsToUse The number of models to be used in each run.
		 * @see singleBatchRun
		 */
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		System.out.println(modelsFile.substring(modelsFile.indexOf("/") + 1, modelsFile.indexOf(".")) + ", num models: " + models.size());
		int runs = models.size() / numOfModelsToUse;
		ArrayList<ArrayList<BigDecimal>> runScores = new ArrayList<ArrayList<BigDecimal>>();
		for (int i = 0; i < runs; i++){
			Runner runner = new Runner(new ArrayList<Model>(models.subList(i * numOfModelsToUse, (i + 1) * numOfModelsToUse)), resultsFile, null, numOfModelsToUse, true);
			runner.execute();
			ArrayList<BigDecimal> currScores = new ArrayList<BigDecimal>();
			for (RunResult rr : runner.getRunResults()){
				currScores.add(rr.weight);
			}
			runScores.add(currScores);
		}
		//print stats
		int cases = runScores.get(0).size();
		double[][] allScores = reorder(runScores);
		double[] averages = new double[cases];
		int count = 0;
		for (int i = 0; i < cases; i++){
			Statistics stats = new Statistics(allScores[i]);
			System.out.println("Case " + i);
			System.out.println("average: " + stats.getMean() + "+-" + stats.getStdDev());
			System.out.println("max run: " + stats.getMax() + ", min run: " + stats.getMin()+ "\n");
			averages[i] = stats.getMean();
		}
		//return averages;
		return allScores;
	}
	
	/*public static void graphData(){
	}*/
	public static double[][] reorder(ArrayList<ArrayList<BigDecimal>> runScores){
		/**
		 * Prints out the average, standard deviation as well as the maximum and minimum scores for each set of runs
		 * in runScores.
		 * 
		 * @param runScores The set of sets of run scores (if there are multiple conditions per some algorithm).
		 */
		int cases = runScores.get(0).size();
		int runs = runScores.size();
		double[][] allScores = new double[cases][runs];
		for (int i = 0; i < runs; i++){
			for (int j = 0; j < cases; j++){
				allScores[j][i] = runScores.get(i).get(j).doubleValue();
			}
		}
		return allScores;
	}
	
	public static void main(String[] args) {
		

		
		String hospitals = "models/hospitals.csv";
		String warehouses = "models/warehouses.csv";
		String random = "models/models/random.csv";
		String randomLoose = "models/models/randomLoose.csv";
		String randomTight = "models/models/randomTight.csv";
		String level2a = "models/level2a.csv";
		String level2b = "models/level2b.csv";
		String level3a = "models/level3a.csv";
		
		/*String testPath = "models/test.csv";
		String hospitals = "models/hospitals.csv";
		String warehouses = "models/warehouses.csv";
		String random10 = "models/random10.csv";
		String randomTMP = "models/random3y.csv";
		String randomTMP1= "models/3x6_models.csv";
		String runningExample = "models/runningExample2.csv";*/
		
		
		String resultsHospitals = "results/hospital_results.xls";
		String resultsWarehouses = "results/warehouses_results.xls";
		String resultsRandom = "results/random_results.xls";
		String resultsRandomLoose = "results/randomLoose_results.xls";
		String resultsRandomTight = "results/randomTight_results.xls";
		String resultsLevel2a = "results/results_level2a.xls";
		String resultsLevel2b = "results/results_level2b.xls";
		String resultsLevel3a = "results/results_level3a.xls";
		
		ArrayList<String> models = new ArrayList<String>();
		models.add(hospitals);
		models.add(warehouses);
		models.add(random);
		models.add(randomLoose);
		models.add(randomTight);
		//models.add(level2a);
		//models.add(level2b);
		//models.add(level3a);
		
		ArrayList<String> results = new ArrayList<String>();
		results.add(resultsHospitals);
		results.add(resultsWarehouses);
		results.add(resultsRandom);
		results.add(resultsRandomLoose);
		results.add(resultsRandomTight);
		//results.add(resultsLevel2a);
		//results.add(resultsLevel2b);
		//results.add(resultsLevel3a);
				
		AlgoUtil.useTreshold(true);
		
		runSimpleExperiment(models, results, 10);
		
		//singleBatchRun(randomTMP, null);
		
		
		//singleBatchRun(runningExample, resultsRunningExample);
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		//workOnBatch(random10, resultRandom10);
		
		//singleBatchRun(randomTMP, null,3, true);
		
		//singleBatchRun(hospitals, resultsHospitals,-1, true);
		//singleBatchRun(warehouses, resultsWarehouses,-1, true);
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;
		
		/*singleBatchRun(warehouses, resultsWarehouses,-1, true);	
		singleBatchRun(hospitals, resultsHospitals,-1, true);
		multipleBatchRun(random, resultsRandom, 10);	
		multipleBatchRun(randomLoose, resultsRandomLoose, 10);	
		multipleBatchRun(randomTight, resultsRandomTight, 10);
		singleBatchRun(level2a, resultsLevel2a,-1, true);
		singleBatchRun(level2b, resultsLevel2b,-1, true);
		singleBatchRun(level3a, resultsLevel3a,-1, true);*/
		
		
		//workOnBatch(random10, resultRandom10);
		//workOnBatch("models/randomH.csv", "results/randomH.xls");
		//workOnBatch("models/randomWH.csv", "results/randomWH.xls");
		//workOnBatch("models/randomBad.csv", "results/randomBad.xls");

		//createBatch("models/randomH.xls", 18,38,2,9,60,100);
		//createBatch("models/randomWH.xls", 15,44,2,7,60,280);
		//createBatch("models/randomMid.xls",15,44,2,7,60,280);
		//createBatch("models/randomBad.xls",20,30,2,16,60,60);
		
		//singleBatchRun(randomTMP, null,3, true);
		
		//singleBatchRun(hospitals, resultsHospitals,-1, true);
		//singleBatchRun(warehouses, resultsWarehouses,-1, true);
		
		
//		singleBatchRun(hospitals, resultsHospitals,3, false);
//		singleBatchRun(hospitals, resultsHospitals,4, false);
//		singleBatchRun(hospitals, resultsHospitals,5, false);
//		singleBatchRun(hospitals, resultsHospitals,6, false);
		
		//singleBatchRun(warehouses, resultsWarehouses,3);
		//singleBatchRun(warehouses, resultsWarehouses,4);
		//singleBatchRun(warehouses, resultsWarehouses,5);


		//singleBatchRun(hospitals, resultsHospitals);
		
		//ArrayList<Model> models = Model.readModelsFile(randomTMP);
		//MultiModelMerger mmm = new MultiModelMerger(models);
		//mmm.run();
//		Runner runner = new Runner(models, resultsHospitals, null);
//		runner.execute();

		//TupleReader tr = new TupleReader(Model.readModelsFile(hospitals),"models/Models.xls");
		//System.out.println(tr.getResult());
		
//		ArrayList<Model> models = Model.readModelsFile(hospitals);
//		ArrayList<Model> tst = new ArrayList<Model>();
//		tst.add(models.get(0));
//		tst.add(models.get(1));
//		tst.add(models.get(2));	
//		tst.add(models.get(3));	
		//MergeDescriptor md = new MergeDescriptor( N_WAY.ALG_POLICY.GREEDY, false, true);
		//MultiModelMerger mmm = new MultiModelMerger(models, md, 3, 20);
		//mmm.run();
		//System.out.println(mmm.getRunResult(models.size()));
		//MergeByBuckets mbb = new MergeByBuckets(models, 20, N_WAY.ALG_POLICY.GREEDY);
		//mbb.run()
		//doit(3, models, N_WAY.ALG_POLICY.GREEDY);
		//doit(2, models, N_WAY.ALG_POLICY.PAIR_BY_AVG);
		//doit(2, models, N_WAY.ALG_POLICY.PAIR_BY_MODEL);
		//doit(3, models, N_WAY.ALG_POLICY.GREEDY);
		//doit(4, models, N_WAY.ALG_POLICY.GREEDY);
		//doit(5, models, N_WAY.ALG_POLICY.GREEDY);
		//ExecutionMixer em = new ExecutionMixer(models);
		
//		RunResult rr = em.run(2, N_WAY.ALG_POLICY.PAIR_BY_AVG, true);
//		System.out.println(rr.toString());
//		
//		em = new ExecutionMixer(Model.readModelsFile(hospitals));
//		rr = em.run(2, N_WAY.ALG_POLICY.PAIR_BY_AVG, false);
//		System.out.println(rr.toString());

		
//		Experiment e = new Experiment(tst, N_WAY.Strategy.ENTIRE_INPUT ,N_WAY.FIRST_LOCAL_SEARCH , "hospitals_triplets");
//		e.run();
//		System.out.println(e.reportOnResults());
		//e.writeResultsAsCSV();
		
		//e =  new Experiment(warehouses, N_WAY.Strategy.MODEL_TRIPLETS ,N_WAY.ALL_ALGOS , "warehouses_triplets");
		//e.run();
		//System.out.println(e.reportOnResults());
		//e.writeResultsAsCSV();

		
		//e =  new Experiment(hospitals, N_WAY.Strategy.MODEL_TRIPLETS ,N_WAY.ALL_ALGOS , "hospitals_triplets");
		//e.run();
		//System.out.println(e.reportOnResults());
		//e.writeResultsAsCSV();
		
		//e =  new Experiment(hospitals, N_WAY.Strategy.ENTIRE_INPUT ,N_WAY.PAIR_WISE , "hospitals_all");
		//e.run();
		//System.out.println(e.reportOnResults());
		//e.writeResultsAsCSV();
		
		
//		SingleRunDescriptor ed1 = new SingleRunDescriptor("models/test.csv", null);
//		SingleRunDescriptor ed2 = new SingleRunDescriptor("models/hospital.csv", new int[] {1,2,3});
//		SingleRunDescriptor ed3 = new SingleRunDescriptor("models/hospital.csv", new int[] {3,4,5});
//		SingleRunDescriptor ed4 = new SingleRunDescriptor("models/hospital.csv", new int[] {5,6,7});
//		SingleRunDescriptor ed5 = new SingleRunDescriptor("models/hospital.csv", new int[] {7,8,1});
//		SingleRunDescriptor ed6 = new SingleRunDescriptor("models/hospital.csv", null);
//		SingleRunDescriptor ed7 = new SingleRunDescriptor("models/hospital.csv", new int[] {1,2,3,4});
//		
//		SingleRunDescriptor used = ed7;
//		SingleRun e = new SingleRun(used.path,used.idsToUse);
//		e.setK(3);
//		e.setL(4);
//		e.setN(4);
//		e.run();
	}
	
//	private static void doit(int m, ArrayList<Model> models, N_WAY.ALG_POLICY pol){
//	ExecutionMixer em = new ExecutionMixer(models);
//	
//	MergeDescriptor md  = new MergeDescriptor(pol, true, true);
//	RunResult rr = em.run(m, md);
//	System.out.println(rr.toString());		
//	
//	md  = new MergeDescriptor(pol, true, false);
//	rr = em.run(m, md);
//	System.out.println(rr.toString());		
//	
//	md  = new MergeDescriptor(pol, false, true);
//	rr = em.run(m, md);
//	System.out.println(rr.toString());		
//	
//	md  = new MergeDescriptor(pol, false, false);
//	rr = em.run(m, md);
//	System.out.println(rr.toString());				
//}
}
