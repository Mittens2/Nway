package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import core.Main;
import core.common.ResultsPlotter;
import core.common.Statistics;
import core.domain.Model;

public class ExperimentsRunner{
	static class ExperimentRunner implements Callable<Double[][]>{  
		private String modelsFile;
		private String resultsFile;
		private int runsToAvg;
		private int divideUp;
		private int numOfModelsToUse;
		private int numCases;
		
			ExperimentRunner(String modelsFiles, String resultsFiles, int runsToAvg,
				int divideUp, int numOfModelsToUse, int numCases) {
	        this.modelsFile = modelsFiles;
	        this.resultsFile = resultsFiles;
	        this.runsToAvg = runsToAvg;
	        this.divideUp = divideUp;
	        this.numOfModelsToUse = numOfModelsToUse;
	        this.numCases = numCases;
		}
    
        @Override
        public Double[][] call(){
        	/**
    		 * Runs a simple experiment (i.e. produces graphs that do not show the spread of scores).
    		 * Saves all of the graphs of the different setting of the algorithm being compared to NwM.
    		 */
    		//String[] settings = {"NwM", "hl:1_sb:0", "hl:1_sb:1", "hl:0_sb:0", "hl:0_sb:1"};
    		Double[][] expResults = new Double[4][numCases];
			ArrayList<Model> models = Model.readModelsFile(modelsFile);
			String subcase = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
			if (models.size() > divideUp){
				models = new ArrayList<Model>(models.subList(0, numOfModelsToUse));
			}
			double[][] scores = null;
			double[] timeSums = null;
			int[] iterSums = null;
			for (int j = 0; j < runsToAvg; j++){
				Runner runner = new Runner(models, resultsFile, null, -1, false);
				runner.execute(subcase);
				ArrayList<RunResult> rrs = runner.getRunResults();
				if (scores == null){
					scores = new double[rrs.size()][runsToAvg];
					timeSums = new double[rrs.size()];
					iterSums = new int[rrs.size()];
				}
				for (int k = 0; k < rrs.size(); k++){
					timeSums[k] += rrs.get(k).execTime / 1000.0;
					iterSums[k] += rrs.get(k).iterations;
					scores[k][j] = rrs.get(k).weight.doubleValue();
				}
			}
			ResultsPlotter rp = new ResultsPlotter(subcase, "");
			for (int j = 0; j < scores.length; j++){
				//expResults[3][j] = ((double) iterSums[j]) / runsToAvg;
				//expResults[2][j] = timeSums[j] / runsToAvg;
				//rp.addBarDataPoint(scoreSums[j] / runsToAvg, settings[j], subcase);
				//for (int k = 0; k < scoreSums[j].length; k++) System.out.println(scoreSums[j][k]);
				//rp.addMultipleValueDatapoint(scoreSums[j], scoreSums[0][0], settings[j]);
				Statistics stats = new Statistics(scores[j]);
				expResults[0][j] = stats.getMean();
				expResults[1][j] = stats.getStdDev();
				
			}
			return expResults;
        }
	}
	
	public static void runConcurrentExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles){
		ExecutorService executor = Executors.newFixedThreadPool(15);
		List<ExperimentRunner> exps = new ArrayList<ExperimentRunner>();
		for (int i = 0; i < modelsFiles.size(); i++){
			exps.add(new ExperimentRunner(modelsFiles.get(i), resultsFiles.get(i), 5, 50, 10, 224));
		}
		List<Future<Double[][]>> results = null;
		try {
			results = executor.invokeAll(exps);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		executor.shutdown();
		ArrayList<String> subcases = new ArrayList<String>();
		for (String mf: modelsFiles){
			subcases.add(mf.substring(mf.lastIndexOf("/") + 1, mf.indexOf(".")));
		}
		FileInputStream fileIn;
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet sheet;
		try{
			fileIn = new FileInputStream(new File(Main.home + "results/experimentResults.xls"));
			workbook = new HSSFWorkbook(fileIn);
			sheet = workbook.getSheet("Block Form");
			if (sheet == null){
				sheet = workbook.createSheet("Block Form");
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Case");
				for (int i = 1; i <= 224; i++){
					header.createCell(i).setCellValue("c" + i);
				}
			}
			for (int i = 0; i < subcases.size(); i++) {
				Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
				newRow.createCell(0).setCellValue(subcases.get(i));
				int cell = 1;
				for (Double score: results.get(i).get()[0]){
					newRow.createCell(cell).setCellValue(score);
					cell++;
				}
			}   
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		} 
		try {
			fileOut = new FileOutputStream(new File(Main.home + "results/experimentResults.xls"));
			workbook.write(fileOut); 
			fileIn.close();
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}