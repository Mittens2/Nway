package core;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RefineryUtilities;

import core.common.AlgoUtil;
import core.common.ResultsPlotter;
import core.common.Statistics;
import core.domain.Model;
import core.execution.BatchRunner;
import core.execution.BatchRunner.BatchRunDescriptor;
import core.execution.ReshapeData;
import core.execution.RunResult;
import core.execution.Runner;
import core.execution.UMLParser;
import core.test.RandomizedMatchMergerTest;




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
	private static void runSimpleExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles, int runsToAvg, 
			int divideUp, int numOfModelsToUse){
		/**
		 * Runs a simple experiment (i.e. produces graphs that do not show the spread of scores).
		 * Saves all of the graphs of the different setting of the algorithm being compared to NwM.
		 */
		double[] nwmScores = {
				4.595,
				1.522,
				0.987, 0.904, 0.833, 0.915, 0.996, 0.984, 1.066, 0.870, 1.089, 1.000,
				0.992, 0.910, 0.933, 1.036, 0.830, 1.278, 1.048, 0.9991, 0.740, 1.027,
				0.958, 1.001, 0.950, 0.900, 0.893, 0.940, 1.008, 0.970, 0.948, 0.840
				};
		FileInputStream fileIn;
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet sheet;
		try{
			fileIn = new FileInputStream(new File("results/experimentResults.xls"));
			workbook = new HSSFWorkbook(fileIn);
			sheet = workbook.getSheetAt(0);
			if (sheet == null)
				sheet = workbook.createSheet();
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Case");
			for (int i = 1; i <= 96; i++){
				header.createCell(i).setCellValue("c" + i);
			}
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		}
		int subcaseID = 0;
		//ArrayList<double[]> datasets = new ArrayList<double[]>();
		ArrayList<ResultsPlotter> rps = new ArrayList<ResultsPlotter>();
		for (int i = 0; i < modelsFiles.size(); i++){
			String mf = modelsFiles.get(i);
			String rf = resultsFiles.get(i);
			ArrayList<Model> models = Model.readModelsFile(mf);
			ArrayList<ArrayList<Model>> runModels = new ArrayList<ArrayList<Model>>();
			String subcase = mf.charAt(mf.indexOf('/') + 1) + "";
			//System.out.println(subcase);
			if (models.size() > divideUp){
				int ind = mf.lastIndexOf('/');
				subcase = mf.charAt(ind + 1) + "";
				char secondChar = mf.charAt(ind + 1 + "random".length());
				subcase = Character.isLetter(secondChar) ? subcase + secondChar : subcase;
				int runs = models.size() / numOfModelsToUse;
				for (int j = 0; j < runs; j++){
					runModels.add(new ArrayList<Model>(models.subList(j * numOfModelsToUse, (j + 1) * numOfModelsToUse)));
				}
			}
			else
				runModels.add(models);
			int subcaseNum = 0;
			for (ArrayList<Model> mods: runModels){
				double[] scoreSums = null;
				for (int j = 0; j < runsToAvg; j++){
					Runner runner = new Runner(mods, rf, null, -1, false);
					runner.execute();
					ArrayList<RunResult> rrs = runner.getRunResults();
					if (scoreSums == null) scoreSums = new double[rrs.size()];
					
					for (int k = 0; k < rrs.size(); k++){
						if (rps.size() == k) rps.add(new ResultsPlotter(rrs.get(k).title, "NwM"));
						scoreSums[k] += rrs.get(k).weight.doubleValue();
					}
				}
				Row newRow = sheet.createRow(subcaseID + 1);
				newRow.createCell(0).setCellValue(subcase + subcaseNum);
				for (int j = 0; j < scoreSums.length; j++){
					rps.get(j).addDataPoint(scoreSums[j] / runsToAvg, nwmScores[subcaseID], subcase + subcaseNum);
					newRow.createCell(j + 1).setCellValue(scoreSums[j] / runsToAvg);
				}
				subcaseID++;
				subcaseNum++;
				System.out.print(subcase + subcaseNum);
			}
			System.out.println();
		}
		try{
			fileIn.close();
			fileOut = new FileOutputStream(new File("results/experimentResults.xls"));
			workbook.write(fileOut); 
			fileOut.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		for (ResultsPlotter rp: rps){
			/*CategoryDataset dataset = rp.getDataSet();
			double[] data = new double[dataset.getColumnKeys().size()];
			int i = 0;
			for (Object key: dataset.getColumnKeys()){
				//data[i] = dataset.getValue("% diff", (Comparable) key).doubleValue() / nwmScores[i];
				i++;
			}
			//datasets.add(data);*/
			rp.createChartSingle();
		}
	}
	
	private static void runOutliers(String modelsFile, String resultsFile, int outlier){
		if (outlier == -1){
			singleBatchRun(modelsFile, resultsFile, -1, false);
		}
		else{
			ArrayList<Model> models = Model.readModelsFile(modelsFile);
			System.out.println(modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf(".")));
			Runner runner = new Runner(new ArrayList<Model>(models.subList(outlier * 10, (outlier + 1) * 10)), resultsFile, null, 10, true);
			runner.execute();
		}
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
		String toycase = "models/toycase.csv";
		String toycase2 = "models/toycase2.csv";
		String toycase3 = "models/toycase3.csv";
		String gasBoilerSystem = "models/GasBoilerSystem.csv";
		String audioControlSystem = "models/AudioControlSystem.csv";
		String conferenceManagementSystem = "models/ConferenceManagementSystem.csv";
		String ajStats = "models/AJStats.csv";
		String tankWar = "models/TankWar.csv";
		String gameOfLife = "models/GameOfLife.csv";
		
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
		String resultsToycase = "results/toycase_results.xls";
		String resultsToycase2 = "results/toycase2_results.xls";
		String resultsToycase3 = "results/toycase3_results.xls";
		String resultsGasBoilerSystem = "results/gasBoilerSystem_results.xls";
		String resultsAudioControlSystem = "results/audioControlSystem_results.xls";
		String resultsConferenceManagementSystem = "results/conferenceManagementSystem_results.xls";
		String resultsAJStats = "results/AJStats_results.xls";
		String resultsTankWar = "results/TankWar_results.xls";
		String resultsGameOfLife = "results/gameOfLife_results.xls";
		
		
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
		//results.add(resultsToycase);
		
		AlgoUtil.useTreshold(true);
		
		//runOutliers(warehouses, resultsWarehouses, -1);
		//runOutliers(random, resultsRandom, 9); 
		//runOutliers(randomLoose, resultsRandomLoose, 4);
		
		//runSimpleExperiment(models, results, 10, 50, 10);
		
		//ReshapeData rd = new ReshapeData("results/experimentResults.xls");
		//rd.reshapeData();
		
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/Java/" 
				//+ "GameOfLife" + "/" + "GameOfLife" + "Comp", true, 12);
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/Java/" 
		//+ "TankWar" + "/" + "TankWar" + "Comp", true, 16);
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/Java/" 
					//+ "AJStats" + "/" + "AJStats" + "Comp", true, 15);
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/UML/" 
			//	+ "ConferenceManagementSystem" + "/" + "ConferenceManagementSystem" + "Comp", false, -1);
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/UML/" 
		//								+ "AudioControlSystem" + "/" + "AudioControlSystem" + "Comp", false, -1);
		//UMLParser.creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/UML/" 
		//+ "GasBoilerSystem" + "/" + "GasBoilerSystem" + "Comp", 12);
		//UMLParser.UMLtoCSV();
		
		//singleBatchRun(randomTMP, null)
		//singleBatchRun(runningExample, resultsRunningExample);
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		//workOnBatch(random10, resultRandom10);
		
		//singleBatchRun(randomTMP, null,3, true);
		
		//singleBatchRun(hospitals, resultsHospitals,-1, true);
		//singleBatchRun(warehouses, resultsWarehouses,-1, true);
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = true;
		
		//AlgoUtil.COMPUTE_RESULTS_CLASSICALLY = false;
		
		//RandomizedMatchMergerTest test = new RandomizedMatchMergerTest();
		/*try{
			test.setUp();
			test.testRMMandNWMdiff(ajStats);
		} catch (Exception e){
			e.printStackTrace();
		}*/
		//singleBatchRun(warehouses, resultsWarehouses, 16, true);	
		//singleBatchRun(hospitals, resultsHospitals, 8, true);
		//singleBatchRun(random, resultsRandom, 3, true);	
		//singleBatchRun(randomLoose, resultsRandomLoose, 3, true);	
		//multipleBatchRun(random, resultsRandom, 10);	
		//multipleBatchRun(randomLoose, resultsRandomLoose, 10);	
		//multipleBatchRun(randomTight, resultsRandomTight, 10);
		//singleBatchRun(level2a, resultsLevel2a,-1, true);
		//singleBatchRun(level2b, resultsLevel2b,-1, true);
		//singleBatchRun(level3a, resultsLevel3a,-1, true);
		//singleBatchRun(toycase, resultsToycase, -1, true);
		//singleBatchRun(toycase2, resultsToycase2, -1, true);
		//singleBatchRun(toycase3, resultsToycase3, -1, true);
		//singleBatchRun(gasBoilerSystem, resultsGasBoilerSystem, -1, true);
		//singleBatchRun(audioControlSystem, audioControlSystem, -1, true);
		//singleBatchRun(conferenceManagementSystem, conferenceManagementSystem, -1, true);
		//singleBatchRun(ajStats, resultsAJStats, -1, true);
		singleBatchRun(tankWar, resultsTankWar, -1, true);
		//singleBatchRun(gameOfLife, resultsGameOfLife, -1, true);
		
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
