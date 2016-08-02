package core.common;

import java.util.Comparator;

import core.domain.Tuple;

public class TupleComparator implements Comparator<Tuple>{
	
	private boolean asc;
	private boolean score;
	
	public TupleComparator(boolean asc, boolean score){
		this.asc = asc;
		this.score = score;
	}
	
	@Override
	public int compare(Tuple t1, Tuple t2){
		if (score){
			if (asc){
				if (t1.getWeight().compareTo(t2.getWeight()) > 0){
					return 1;
				}
				else if (t1.getWeight().compareTo(t2.getWeight()) == 0){
					return 0;
				}
				else{
					return -1;
				}
			}
			else {
				if (t1.getWeight().compareTo(t2.getWeight()) < 0){
					return 1;
				}
				else if (t1.getWeight().compareTo(t2.getWeight()) == 0){
					return 0;
				}
				else{
					return -1;
				}
			}
		}
		else{
			if (asc){
				if (t1.getSize() > t2.getSize()){
					return 1;
				}
				else if (t1.getSize() == t2.getSize()){
					return 0;
				}
				else{
					return -1;
				}
			}
			else {
				if (t1.getSize() < t2.getSize()){
					return 1;
				}
				else if (t1.getSize() == t2.getSize()){
					return 0;
				}
				else{
					return -1;
				}
			}
		}
	}

}
