package core.common;

import java.util.Comparator;

import core.domain.Element;

public class ElementComparator implements Comparator<Element> {
	
	boolean asc;
	boolean bar;
	public ElementComparator(boolean asc, boolean bar){
		this.asc = asc;
		this.bar = bar;
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
		if (bar){
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
		else{
			if(asc){
				return e1.getSize() - e2.getSize();
			}
			else{
				return e2.getSize() - e1.getSize();
			}
		}
	}

}
