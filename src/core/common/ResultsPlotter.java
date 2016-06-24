package core.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;


public class ResultsPlotter extends ApplicationFrame {
	 private final DefaultCategoryDataset catDataset;
	 private final DefaultBoxAndWhiskerCategoryDataset bwDataset;
	 private int cases;
	 private String alg1;
	 private String alg2;
	
	public ResultsPlotter(String alg1, String alg2){
		super(alg1 + " vs " + alg2);
		this.alg1 = alg1;
		this.alg2 = alg2;
		this.cases = 0;
		this.catDataset = new DefaultCategoryDataset();
		this.bwDataset = new DefaultBoxAndWhiskerCategoryDataset();
	}
	public void setAlg1Label(String alg1){
		this.alg1 = alg1;
	}
	public void setAlg2Label(String alg2){
		this.alg2 = alg2;
	}
	
	public void addDataPoint(double alg1, double alg2, String category){
		catDataset.addValue(Math.pow(1.01, cases) + (alg1 / alg2 - 1),"% diff", category);
		catDataset.addValue(Math.pow(1.01, cases), "x = y", category);
		cases++;
	}
	
	public void addSingleValueDataPoint(double alg1, double alg2, String category){
		ArrayList<Double> datapoint = new ArrayList<Double>();
		datapoint.add((alg1 / alg2 - 1) * 100);
		bwDataset.add(datapoint, "% diff", category);
	}
	
	public void addMultipleValueDatapoint(double alg1[], double alg2[], String category){
		ArrayList<Double> datapoint = new ArrayList<Double>();
		for (int i = 0; i < alg1.length; i++){
			datapoint.add((alg1[i] / alg2[i] - 1) * 100);
		}
		bwDataset.add(datapoint, "% diff", category);
	}
	
	public void createDataset(double[] alg1, double[] alg2, ArrayList<String> categories){
		for (int i = 0; i < alg1.length; i++){
			catDataset.addValue(Math.pow(1.01, i) + (alg1[i] / alg2[i] - 1),"% diff", categories.get(i));
			catDataset.addValue(Math.pow(1.01, i), "x = y", categories.get(i));
		}
	}
	
	public void createChartSingle(){
        final LogAxis rangeAxis = new LogAxis("SmartHuman");
        rangeAxis.setBase(1.01);
        rangeAxis.setRange(Math.pow(1.01, -25), Math.pow(1.01, 50));
        //rangeAxis.setTickUnit(new NumberTickUnit(10));
        //rangeAxis.setAllowNegativesFlag(true);
        //rangeAxis.setUpperBound(0.1);
        final CategoryAxis domainAxis = new CategoryAxis("NwM");
        //rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        final CategoryPlot plot = new CategoryPlot(catDataset, domainAxis, rangeAxis, renderer);
        plot.addRangeMarker(new ValueMarker(0.0));
        plot.setDomainGridlinesVisible(true);
 
        final JFreeChart result = new JFreeChart(
            this.alg1,
            new Font("SansSerif", Font.BOLD, 12),
            plot,
            true
        );
        result.removeLegend();
        final ChartPanel chartPanel = new ChartPanel(result);
        setContentPane(chartPanel);
        
        try{
        	chartPanel.setMinimumDrawHeight(500);
            chartPanel.setMinimumDrawWidth(1000);
        	BufferedImage bi = ScreenImage.createImage(chartPanel);
        	String picPath = "/home/amit/Documents/Results Webpage/graphs/";
        	//String picPath = "graphs/";
        	File file = new File(picPath + this.alg1.substring(alg1.indexOf("(")) +".jpeg");
        	ChartUtilities.saveChartAsJPEG(file, 1f, result, 1000, 500);
        }
        catch (IOException e){
        	System.out.println(e.getMessage());
        }
	}
	
	public void createChart(){
		final CategoryAxis xAxis = new CategoryAxis("Case");
        final NumberAxis yAxis = new NumberAxis("Percent Increase (%)");
        //yAxis.setAutoRangeIncludesZero(false);
        yAxis.setRange(-30, 30);
        //final ExtendedBoxAndWhiskerRenderer renderer = new ExtendedBoxAndWhiskerRenderer();
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setMedianVisible(false);
        renderer.setMaximumBarWidth(0.02);
        renderer.setSeriesOutlinePaint(0, Color.WHITE);
        renderer.setWhiskerWidth(0.2);
        final CategoryPlot plot = new CategoryPlot(bwDataset, xAxis, yAxis, renderer);
        final Marker start = new ValueMarker(0.0);
        start.setPaint(Color.blue);
        plot.addRangeMarker(start);
        final JFreeChart chart = new JFreeChart(
            alg1 + " Percentage Increase Over " + alg2,
            new Font("SansSerif", Font.BOLD, 14),
            plot,
            true
        );
        chart.removeLegend();
        final ChartPanel chartPanel = new ChartPanel(chart);
       
	}

}

