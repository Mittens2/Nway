package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

public class ReshapeData {
	protected String inFilePath;
	
	public ReshapeData(String inFilePath){
		this.inFilePath = inFilePath;
	}
	
	public synchronized void  reshapeData() {
		FileOutputStream fileOut;
		FileInputStream fileIn;
		HSSFWorkbook workbook;
		String[] seedings = {"0", "1-a", "1-d", "2", "3-a", "3-d", "4-a-a", "4-a-d", "4-d-a", "4-d-d", "5-a", "5-d"};
		try {
			fileIn = new FileInputStream(new File(inFilePath));
			workbook = new HSSFWorkbook(fileIn);
			HSSFSheet origSheet = workbook.getSheet("sheet1");
			HSSFSheet reshapeSheet = workbook.getSheet("sheet2");
			if(reshapeSheet == null)
				reshapeSheet = workbook.createSheet("sheet2");
			int size = origSheet.getRow(0).getLastCellNum();
			Row newFirstRow = reshapeSheet.createRow(0);
			newFirstRow.createCell(0).setCellValue("case");
			newFirstRow.createCell(1).setCellValue("sd");
			newFirstRow.createCell(2).setCellValue("hl");
			newFirstRow.createCell(3).setCellValue("st");
			newFirstRow.createCell(4).setCellValue("score");
			for(int i = 1; i <= 32; i++){
				Row oldRow = origSheet.getRow(i);
				for (int j = 0; j < size - 1; j++){
					Row newRow = reshapeSheet.createRow((i - 1) * 96 + j + 1);
					newRow.createCell(0).setCellValue(oldRow.getCell(0).getStringCellValue());
					newRow.createCell(1).setCellValue(seedings[j % 12]);
					newRow.createCell(2).setCellValue(j / 24);
					newRow.createCell(3).setCellValue((j % 24) / 12);
					newRow.createCell(4).setCellValue(oldRow.getCell(j + 1).getNumericCellValue());
				}
			}
			fileIn.close();
			fileOut = new FileOutputStream(new File(inFilePath));
			workbook.write(fileOut); 
			fileOut.close();
		}catch(Exception e){
			e.printStackTrace();
			
		}
			
	}
}
