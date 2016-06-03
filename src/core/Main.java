package core;

import java.math.BigDecimal;
import java.util.ArrayList;

import core.alg.merge.MultiModelHungarian;
import core.alg.merge.MultiModelMerger;
import core.common.AlgoUtil;
import core.domain.Model;
import core.execution.BatchRunner;
import core.execution.BatchRunner.BatchRunDescriptor;
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
	
	private static void singleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse, boolean toChunkify){
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
		System.out.println(runner.getRunResults().get(0).weight);
	}
	
	private static void multipleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse){
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
		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < runs; i++){
			Runner runner = new Runner(new ArrayList<Model>(models.subList(i * numOfModelsToUse, (i + 1) * numOfModelsToUse)), resultsFile, null, numOfModelsToUse, true);
			runner.execute();
			total = total.add(runner.getRunResults().get(0).weight);
		}
		System.out.println("Average score:" + total.divide(BigDecimal.TEN));
	}
	
	public static void main(String[] args) {
		
		String hospitals = "models/hospitals.csv";
		String warehouses = "models/warehouses.csv";
		String random = "models/models/random.csv";
		String randomLoose = "models/models/randomLoose.csv";
		String randomTight = "models/models/randomTight.csv";
		
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
		String resultsRandomTight = "results/random_resultsTight.xls";
				
		AlgoUtil.useTreshold(true);
		
		//singleBatchRun(randomTMP, null);
		
		
		//singleBatchRun(runningExample, resultsRunningExample);
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		//workOnBatch(random10, resultRandom10);
		
		//singleBatchRun(randomTMP, null,3, true);
		
		//singleBatchRun(hospitals, resultsHospitals,-1, true);
		//singleBatchRun(warehouses, resultsWarehouses,-1, true);
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;
		
		singleBatchRun(warehouses, resultsWarehouses,-1, true);	
		singleBatchRun(hospitals, resultsHospitals,-1, true);
		multipleBatchRun(random, resultsRandom, 10);	
		multipleBatchRun(randomLoose, resultsRandomLoose, 10);	
		multipleBatchRun(randomTight, resultsRandomTight, 10);
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