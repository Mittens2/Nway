package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import core.Main;
import core.alg.merge.ChainingOptimizingMerger;
import core.alg.merge.MergeDescriptor;
import core.alg.merge.MultiModelMerger;
import core.alg.merge.RandomizedMatchMerger;
import core.alg.search.ACO;
import core.common.AlgoUtil;
import core.common.GameSolutionParser;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class ACORunner {
	private int[] workers;
	private int[] numIters;
	private double[] alpha;
	private double[] beta;
	private double[] rho;
	
	public ACORunner(){
		// Parameters used in JADM paper: workers = 100, numIters = 50, alpha = 5, beta = 0.5, rho = 0.1
		this.workers = new int[]{2, 32};
		this.numIters = new int[]{5, 10};
		this.alpha = new double[]{1, 5};
		this.beta = new double[]{0.5, 1};
		this.rho = new double[]{0.1, 0.5};
	}
	
	public ACORunner(int[] workers, int[] numIters,double[] alpha, double[] beta, double[] rho){
		this.workers = workers;
		this.numIters = numIters;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
	}
	
	public void runHyperParams(ArrayList<String> modelsFiles){
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(new File("results/ACOresults.xls"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("HyperParams");
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("case");
		header.createCell(1).setCellValue("workers");
		header.createCell(2).setCellValue("numIters");
		header.createCell(3).setCellValue("alpha");
		header.createCell(4).setCellValue("beta");
		header.createCell(5).setCellValue("rho");
		header.createCell(6).setCellValue("score");
		header.createCell(7).setCellValue("time");
		int k = 0;
		for (String mf: modelsFiles){
			for (int w: workers){
				for (int i: numIters){
					for (double a: alpha){
						for (double b: beta){
							for (double r: rho){
								ArrayList<Model> models = Model.readModelsFile(mf);
								if (models.size() > 50){
									models = new ArrayList<Model>(models.subList(0, 10));
								}
								ACO aco = new ACO(models, w, i, a, b, r, k / 3);
								RunResult rr = aco.runACO();
								Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
								newRow.createCell(0).setCellValue(mf.substring(mf.lastIndexOf("/") + 1,
										mf.indexOf(".")));
								newRow.createCell(1).setCellValue(w);
								newRow.createCell(2).setCellValue(i);
								newRow.createCell(3).setCellValue(a);
								newRow.createCell(4).setCellValue(b);
								newRow.createCell(5).setCellValue(r);
								newRow.createCell(6).setCellValue(df.format(rr.weight));
								newRow.createCell(7).setCellValue(df.format(rr.execTime));
								System.out.println(rr);
							}
						}
					}
				}
			}
			k++;
		}
		try {
			workbook.write(fileOut);
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
