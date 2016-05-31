package core.common;

import java.util.Comparator;

import core.domain.Element;

public class ElementComparator implements Comparator<Element> {
	
	boolean asc;
	public ElementComparator(boolean asc){
		this.asc = asc;
	}
	@Override
	public int compare(Element e1, Element e2) {
		if(asc){
			return e1.getSize() - e2.getSize();
		}
		else{
			return e2.getSize() - e1.getSize();
		}
	}

}
