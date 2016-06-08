package core.common;

import java.util.Comparator;

import core.domain.Element;

public class ElementComparator implements Comparator<Element> {
	
	boolean asc;
	boolean prop;
	public ElementComparator(boolean asc, boolean prop){
		this.asc = asc;
		this.prop = prop;
	}
	@Override
	public int compare(Element e1, Element e2) {
		if (prop){
			if(asc){
				return e1.getPropScore() - e2.getPropScore();
			}
			else{
				return e2.getPropScore() - e1.getPropScore();
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
