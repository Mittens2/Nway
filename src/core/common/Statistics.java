package core.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

public class Statistics 
{
    double[] data;
    int size;   

    public Statistics(double[] data) 
    {
        this.data = data;
        size = data.length;
    } 
    
    public Statistics(ArrayList<BigDecimal> data){
    	this.data = new double[data.size()];
    	for (int i = 0; i < data.size(); i++){
    		this.data[i] = data.get(i).doubleValue();
    	}
    	size = this.data.length;
    }

    public double getMean()
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/size;
    }

    public double getVariance()
    {
        double mean = getMean();
        double temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/size;
    }

    public double getStdDev()
    {
        return Math.sqrt(getVariance());
    }

    public double median() 
    {
       Arrays.sort(data);

       if (data.length % 2 == 0) 
       {
          return (data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0;
       } 
       else 
       {
          return data[data.length / 2];
       }
    }
    
    public double getMax(){
    	double max = 0;
    	for (double d: data){
    		if (d > max){
    			max = d;
    		}
    	}
    	return max;
    }
    public double getMin(){
    	double min = Double.POSITIVE_INFINITY;
    	for (double d: data){
    		if (d < min){
    			min = d;
    		}
    	}
    	return min;
    }
}

