package core;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RefineryUtilities;

import core.common.AlgoUtil;
import core.common.GameSolutionParser;
import core.common.OptimalSolutionSolver;
import core.common.ResultsPlotter;
import core.common.SolverDifference;
import core.common.Statistics;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.BatchRunner;
import core.execution.BatchRunner.BatchRunDescriptor;
import core.execution.UMLParser.UMLClass;
import core.execution.ExperimentsRunner;
import core.execution.RunResult;
import core.execution.Runner;
import core.execution.UMLParser;
import core.test.RandomizedMatchMergerTest;




public class Main {
	public static String home;
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
		
	private static ArrayList<RunResult> singleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse, boolean toChunkify){
		/**
		 * Runs algorithms over the first numOfModelsToUse models from modelsFile in a single run.
		 * 
		 * @param modelsFile The name of the file to derive the models.
		 * @param resultsFile The name of the file which results should be written to.
		 * @param numOfModelsToUse The number of models to be used in the run.
		 */
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		ArrayList<Model> newModels = new ArrayList<Model>();
		for (Model m: models){
			if (m.size() > 60){
				Collections.shuffle(m.getElements(), new Random(System.nanoTime()));
				int keep = new Random().nextInt(20) + 40;
				ArrayList<Element> newElems = new ArrayList<Element>();
				newElems.addAll(m.getElements().subList(0, keep));
				Model newModel = new Model(m.getId(), newElems);
				newModels.add(newModel);
			}
			else{
				newModels.add(m);
			}
		}
		models = newModels;
		String caseName = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
		System.out.println(caseName + ", num models: " + models.size());
		Runner runner = new Runner(models, resultsFile, null, numOfModelsToUse, toChunkify);
		runner.execute(caseName);
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
		String caseName = modelsFile.substring(modelsFile.indexOf("/") + 1, modelsFile.indexOf("."));
		System.out.println(caseName + ", num models: " + models.size());

		int runs = models.size() / numOfModelsToUse;
		ArrayList<ArrayList<BigDecimal>> runScores = new ArrayList<ArrayList<BigDecimal>>();
		for (int i = 0; i < runs; i++){
			Runner runner = new Runner(new ArrayList<Model>(models.subList(i * numOfModelsToUse, (i + 1) * numOfModelsToUse)), resultsFile, null, numOfModelsToUse, true);
			runner.execute(caseName);
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
	
	public static double[][] reorder(ArrayList<ArrayList<BigDecimal>> runScores){
		/**
		 * Reorders data from multipleBatchRun so can be printed.
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
	
	public static void printStats(String modelsFile){
		/**
		 * Prints out max number of elements, min number of elements, avergae number of elements,
		 * and total amount of properties in a specific case study.
		 */
		String name = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		if (models.size() > 50){
			models = new ArrayList<Model>(models.subList(0, 10));
		}
		int min = 100;
		int max = 0;
		double totes = 0;
		int possibs = 1;
		for (Model mod: models){
			//System.out.println(mod.size());
			int size = mod.size();
			totes += size;
			possibs *= (mod.size() + 1);
			if (size > max)
				max = size;
			if (size < min)
				min = size;
		}
		int incompat = 0;
		//possibs -= 1;
		for (int i = 0; i < models.size(); i++){
			incompat += (((double) possibs) / (models.get(i).size() + 1));
			for (int j = 0; j < i; j++){
				incompat -= (((double) possibs) / (models.get(i).size() + 1) / (models.get(j).size() + 1));
			}
		}
		Set<String> props = new HashSet<String>();
		for (Model m: models){
			for (Element e: m.getElements()){
				for (String p: e.getProperties()){
					if (!props.contains(p))
						props.add(p);
				}
			}
		}
		System.out.println(name + ": " + "elements: " + totes + ", max:" + max + ", min:" + min + ", avg:" +  
		(totes / models.size()) + ", possible tuples:" + possibs + ", possible solutions:" + possibs * (possibs - incompat)
				+ ", props:" + props.size());
		//System.out.println(name + ": " + props.size());
	}
	
	public static void testTuples(String modelsFile){
		// 1<6>,2<4>,3<8>,4<3>,5<3>,6<2>,7<7>,8<6>
		//
		// 2<4>,3<8>,4<3>,5<3>,6<2>,7<7>,8<6>
		// 1<6>,3<7>,8<7>
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		int[][] elemMap = new int[9][3];
		int numTuples = 2;
		//
		elemMap[0][0] = 9;
		elemMap[0][1] = 28;
		elemMap[0][2] = 0;
		elemMap[1][0] = 10;
		elemMap[1][1] = 38;
		elemMap[1][2] = 0;
		elemMap[2][0] = 1;
		elemMap[2][1] = 38;
		elemMap[2][2] = 0;
		elemMap[3][0] = 4;
		elemMap[3][1] = 38;
		elemMap[3][2] = 0;
		elemMap[4][0] = 5;
		elemMap[4][1] = 29;
		elemMap[4][2] = 0;
		elemMap[5][0] = 6;
		elemMap[5][1] = 38;
		elemMap[5][2] = 0;
		elemMap[6][0] = 7;
		elemMap[6][1] = 29;
		elemMap[6][2] = 0;
		elemMap[7][0] = 8;
		elemMap[7][1] = 38;
		elemMap[7][2] = 0;
		elemMap[8][0] = 3;
		elemMap[8][1] = 19;
		elemMap[8][2] = 0;
		Tuple[] tuples = new Tuple[numTuples];
		for (int i = 0; i < tuples.length; i++){
			tuples[i] = new Tuple();
		}
		for (int i = 0; i < elemMap.length; i++){
			tuples[elemMap[i][2]] = tuples[elemMap[i][2]].newExpanded(models.get(elemMap[i][0]).getElements().get(elemMap[i][1]), models);
		}
		ArrayList<Tuple> newTuples = new ArrayList<Tuple>();
		for (Tuple t: tuples){
			newTuples.add(t);
		}
		AlgoUtil.printTuples(newTuples);
		System.out.println(AlgoUtil.calcGroupWeight(newTuples));
	}
	
	private static void writeToFile(ArrayList<Model> models, String modelName){
		try{
			PrintWriter writer = new PrintWriter("models/FH_nogen/fixed_" + modelName + ".csv", "utf-8");
			//for (Element e: elements){
			for (Model m: models){
				for (Element e: m.getElements()){
					writer.print(e.getModelId());
					writer.print("," + e.getLabel() + ",");
					//ArrayList<String> properties = new ArrayList<String>(e.getProperties());
					Set<String> properties = new HashSet<String>(e.getProperties());
					for (Iterator<String> iterator = properties.iterator(); iterator.hasNext();){
						writer.print(iterator.next() + ";");
					}
					//writer.print(properties.get(0));
					writer.println();
				}
			}
			writer.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private static ArrayList<Element> loadTuplesFromFile(File f, ArrayList<Model> models){
		ArrayList<Element> subElements = new ArrayList<Element>();
		try{
			Scanner scan = new Scanner(f);
			while(scan.hasNext()){
				String[] line = scan.next().split(";");
				subElements.add(models.get(Integer.parseInt(line[1]) - 1).getElementByLabel(line[2]));
			}
			scan.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return subElements;
	}
	
	private static void writeSubModel(String casename){
		ArrayList<Model> models = Model.readModelsFile(Main.home + "models/FH_nogen/" + casename + ".csv");
		//writeToFile(loadTuplesFromFile(new File(Main.home + "models/NwMsolutions/" + casename + ".csv"), models), casename);
		writeToFile(models, casename);
	}
	
	
	public static void main(String[] args) {
		// Set home to path where models/results files are located.
		if (args.length == 1){
			Main.home = args[0];
		}
		else{
			Main.home = "";
		}
		
		// All models files.
		String hospitals = home + "models/Julia_study/hospitals.csv";
		String warehouses = home + "models/Julia_study/warehouses.csv";
		String random = home + "models/Julia_study/random.csv";
		String randomLoose =home +  "models/Julia_study/randomLoose.csv";
		String randomTight = home + "models/Julia_study/randomTight.csv";
		String level2a = home + "models/aliens/level2a.csv";
		String level2b = home + "models/aliens/level2b.csv";
		String level3a = home + "models/aliens/level3a.csv";
		String toycase = home + "models/toycases/toycase.csv";
		String toycase2 = home + "models/toycases/toycase2.csv";
		String toycase3 = home + "models/toycases/toycase3.csv";
		String toycase4 = home + "models/toycases/toycase4.csv";
		String toycase5 = home + "models/toycases/toycase5.csv";
		String toycase6 = home + "models/toycases/toycase6.csv";
		String toycase7 = home + "models/toycases/toycase7.csv";
		String toyChat = home + "models/toycases/toyChat.csv";
		String toyTank = home + "models/toycases/toyTank.csv";
		String gasBoilerSystem = home + "models/FH/GasBoilerSystem.csv";
		String audioControlSystem = home + "models/FH/AudioControlSystem.csv";
		String conferenceManagementSystem = home +  "models/FH/ConferenceManagementSystem.csv";
		String ajStats = home + "models/FH/AJStats.csv";
		String tankWar = home + "models/FH/TankWar.csv";
		String PKJab = home + "models/FH/PKJab.csv";
		String chatSystem = home + "models/FH_nogen/ChatSystem.csv";
		String notepad = home + "models/FH_nogen/fixed_Notepad.csv";
		String ahead = home + "models/FH_nogen/ahead.csv";
		String mobileMedia = home + "models/Egyed/MobileMedia.csv";
		String vod1 = home + "models/Egyed/VOD1.csv";
		String vod2 = home + "models/Egyed/VOD2.csv";
		String gameOfLife = home + "models/FH/GameOfLife.csv";
		String GPL = home + "models/FH/GPL.csv";
		String BerkeleyDB = home + "models/FH/subBerkeleyDB.csv";
		String mobileMedia8 = home + "models/FH/MobileMedia8.csv";

		// All Results files.
		String resultsHospitals =home +  "results/hospital_results.xls";
		String resultsWarehouses = home + "results/warehouses_results.xls";
		String resultsRandom = home + "results/random_results.xls";
		String resultsRandomLoose = home + "results/randomLoose_results.xls";
		String resultsRandomTight = home + "results/randomTight_results.xls";
		String resultsLevel2a = home + "results/results_level2a.xls";
		String resultsLevel2b = "results/results_level2b.xls";
		String resultsLevel3a = "results/results_level3a.xls";
		String resultsToycase = "results/toycase_results.xls";
		String resultsToycase2 = "results/toycase2_results.xls";
		String resultsToycase3 = "results/toycase3_results.xls";
		String resultsGasBoilerSystem = home + "results/gasBoilerSystem_results.xls";
		String resultsAudioControlSystem = home + "results/audioControlSystem_results.xls";
		String resultsConferenceManagementSystem = home + "results/conferenceManagementSystem_results.xls";
		String resultsPKJab = home + "results/PKJab_results.xls";
		String resultsAJStats = home + "results/AJStats_results.xls";
		String resultsTankWar = home + "results/TankWar_results.xls";
		String resultsChatSystem = home + "results/chatSystem_results.xls";
		String resultsNotepad = home + "results/notepad_results.xls";
		String resultsAhead = home + "results/ahead_results.xls";
		String resultsMobileMedia = home + "results/mobileMedia_results.xls";
		String resultsVod1 = home + "results/vod1_results.xls";
		String resultsVod2 = home + "results/vod2_results.xls";
		
		// All MM solution files.
		String mmHospital = home + "models/MMsolutions/Hospital.txt";
		String mmGasBoiler = home + "models/MMsolutions/GasBoiler.txt";
		String mmRandomTight = home + "models/MMsolutions/RandomTight.txt";
		
		ArrayList<String> models = new ArrayList<String>();
		ArrayList<String> results = new ArrayList<String>();
		ArrayList<String> mmFiles = new ArrayList<String>();
		
		//models.add(hospitals);
		//models.add(warehouses);
		models.add(random);
		//models.add(randomLoose);
		//models.add(randomTight);
		/*models.add(gasBoilerSystem);
		models.add(audioControlSystem);
		models.add(ajStats);
		models.add(tankWar);
		models.add(PKJab);
		models.add(chatSystem);
		models.add(notepad);
		models.add(mobileMedia);
		models.add(vod1);
		models.add(vod2);*/

		results.add(resultsHospitals);
		results.add(resultsWarehouses);
		results.add(resultsRandom);
		results.add(resultsRandomLoose);
		results.add(resultsRandomTight);
		results.add(resultsGasBoilerSystem);
		results.add(resultsAudioControlSystem);
		results.add(resultsAJStats);
		results.add(resultsTankWar);
		results.add(resultsPKJab);
		results.add(resultsChatSystem);
		results.add(resultsNotepad);
		results.add(resultsMobileMedia);
		results.add(resultsVod1);
		results.add(resultsVod2);
		
		mmFiles.add(mmHospital);
		mmFiles.add(mmRandomTight);
		mmFiles.add(mmGasBoiler);
		
		printStats(hospitals);
		//testTuples(ajStats);
		//writeSubModel("notepad");
		
		//AlgoUtil.useTreshold(true);
		
		//ArrayList<Model> testModels = new ArrayList<Model>(Model.readModelsFile(random).subList(0, 10));
	
		//ArrayList<Model> testModels = Model.readModelsFile(audioControlSystem);
		//GameSolutionParser parser = new GameSolutionParser(Main.home + "models/MMsolutions/audio_buckets.txt", testModels);
		//parser.solutionCalculator();
		//parser.createNewGame(Main.home + "models/newHopsitals.txt");
	
		//String[] solvers =  {"NwM", "HSim", "MM"};
		//ExperimentsRunner.runMetricsExperiment(models, mmFiles, solvers);
		//ExperimentsRunner.runConcurrentExperiment(models, results, 3, 50, 10, 84);
		//ExperimentsRunner.runSeedExperiment(models, results, 3,  50, 10);
		
		//ArrayList<Model> audio = Model.readModelsFile(audioControlSystem);
		//OptimalSolutionSolver oss = new OptimalSolutionSolver(audio);
		//oss.calcOptimalScore(audioControlSystem);
		//oss.calcOptimalScore(toyChat);
		
		//UMLParser.createFeatureLists("MobileMedia8", true, 8);
		//UMLParser.UMLtoCSV("MobileMedia8", 8);
		
		//for (String caseStudy: models){
		//String caseStudy = ajStats;
		//ArrayList<Model> testModels = Model.readModelsFile(caseStudy);
		//String gameFilePath = home + "models/MMsolutions/audio_buckets.txt";
		//String nwmFilePath = home + "models/NwMsolutions/" + caseStudy.substring(caseStudy.lastIndexOf("/"));
		//SolverDifference.testRMMandNwMDiff(testModels, nwmFilePath, true);
		//}
		
		//testTuples(testModels);
		
		//writeSubModel("BerkeleyDB");
		singleBatchRun(hospitals, resultsHospitals, -1, true);
		//singleBatchRun(warehouses, resultsWarehouses, -1, true);	
		//singleBatchRun(random, resultsRandom, 10, true);	
		//singleBatchRun(randomLoose, resultsRandomLoose, 10, true);	
		//singleBatchRun(randomTight, resultsRandomTight, 10, true);
		//multipleBatchRun(random, resultsRandom, 10);	
		//multipleBatchRun(randomLoose, resultsRandomLoose, 10);	
		//multipleBatchRun(randomTight, resultsRandomTight, 10);
		//singleBatchRun(level2a, resultsLevel2a,-1, true);
		//singleBatchRun(level2b, resultsLevel2b,-1, true);
		//singleBatchRun(level3a, resultsLevel3a,-1, true);
		//singleBatchRun(toycase, resultsToycase, -1, true);
		//singleBatchRun(toycase2, resultsToycase2, -1, true);
		//singleBatchRun(toycase3, resultsToycase3, -1, true);
		//singleBatchRun(toycase4, resultsToycase3, -1, true);
		//singleBatchRun(toycase5, resultsToycase3, -1, true);
		//singleBatchRun(toycase6, resultsToycase, -1, true);
		//singleBatchRun(toycase7, resultsToycase, -1, true);
		//singleBatchRun(toyChat, resultsToycase, -1, true);
		//singleBatchRun(toyTank, resultsToycase, -1, true);
		//singleBatchRun(gasBoilerSystem, resultsGasBoilerSystem, -1, true);
		//singleBatchRun(audioControlSystem, resultsAudioControlSystem, -1, true);
		//singleBatchRun(conferenceManagementSystem, resultsConferenceManagementSystem, -1, true);
		//singleBatchRun(ajStats, resultsAJStats, -1, true);
		//singleBatchRun(tankWar, resultsTankWar, -1, true);
		//singleBatchRun(PKJab, resultsPKJab, -1, true);
		//singleBatchRun(chatSystem, resultsChatSystem, -1, true);
		//singleBatchRun(notepad, resultsNotepad, -1, true);
		//singleBatchRun(notepad, resultsNotepad, -1, true);
		//singleBatchRun(mobileMedia, resultsMobileMedia, -1, true);
		//singleBatchRun(vod1, resultsVod1, -1, true);
		//singleBatchRun(vod2, resultsVod2, -1, true);
		//singleBatchRun(ahead, resultsAhead, 3, true);
		//singleBatchRun(gameOfLife, resultsVod1, -1, true);
		//singleBatchRun(GPL, resultsVod1, -1, true);
		//singleBatchRun(BerkeleyDB, resultsVod1, -1, true);
		//singleBatchRun(mobileMedia8, resultsVod1, -1, true);
		
		//workOnBatch(random10, resultRandom10);
		//workOnBatch("models/randomH.csv", "results/randomH.xls");
		//workOnBatch("models/randomWH.csv", "results/randomWH.xls");
		//workOnBatch("models/randomBad.csv", "results/randomBad.xls");

		//createBatch("models/randomH.xls", 18,38,2,9,60,100);
		//createBatch("models/randomWH.xls", 15,44,2,7,60,280);
		//createBatch("models/randomMid.xls",15,44,2,7,60,280);
		//createBatch("models/randomBad.xls",20,30,2,16,60,60);
		
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
