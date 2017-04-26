package core.common;

import java.util.ArrayList;
import java.util.Comparator;

import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class ElementComparator implements Comparator<Element> {
	
	boolean asc;
	private Sort sort;
	private Tuple relative;
	private ArrayList<Model> models;
	public enum Sort{
		BAR,
		SIZE,
		PAIRWISE
	}
	public ElementComparator(boolean asc, Sort sort, Tuple relative, ArrayList<Model> models){
		this.asc = asc;
		this.sort = sort;
		this.relative = relative;
		this.models = models;
	}
	@Override
	public int compare(Element e1, Element e2) {
		/*if (prop){
			if(asc){
				//return e1.getPropScore() - e2.getPropScore();
				if (e1.getPropScore() > e2.getPropScore())
					return 1;
				else if (e1.getPropScore() == e2.getPropScore())
					return 0;
				else
					return -1;
			}
			else{
				//return e2.getPropScore() - e1.getPropScore();
				if (e2.getPropScore() > e1.getPropScore())
					return 1;
				else if (e1.getPropScore() == e2.getPropScore())
					return 0;
				
				else
					return -1;
			}

		}*/
		if (sort == Sort.BAR){
			if(asc){
				if (e1.getBar().compareTo(e2.getBar()) < 0)
					return 1;
				else if (e1.getBar().equals(e2.getBar()))
					return 0;
				else
					return -1;
			}
			else{
				if (e2.getBar().compareTo(e1.getBar()) > 0)
					return 1;
				else if (e1.getBar().equals(e2.getBar()))
					return 0;
				else
					return -1;
			}

		}
		else if (sort == Sort.SIZE){
			if(asc)
				return e1.getSize() - e2.getSize();
			else
				return e2.getSize() - e1.getSize();
		}
		else{ //sort == Sort.PAIRWISE
			Tuple e1tup = relative.newExpanded(e1, models);
			Tuple e2tup = relative.newExpanded(e2, models);
			if (asc)
				return e1tup.calcWeight(models).compareTo(e2tup.calcWeight(models));
			else
				return e2tup.calcWeight(models).compareTo(e1tup.calcWeight(models));
		}
	}
}
