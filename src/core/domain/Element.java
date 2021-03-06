package core.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Row;

import core.common.AlgoUtil;
import core.common.N_WAY;

public class Element {
	private Set<String> properties;
	private String label;
	public static int maxModelElems;
	private ArrayList<String> sortedProps = null;
	
	private ArrayList<Element> basedUponElements;
	private LinkedList<Integer> containingTupleId;
	private Tuple containingTuple;
	
	private String modelId;
	
	private final static String alphabet = "abcdefghijklmnopqrstuvwxyz1234";
	
	private String identifyingLabel;
	private static int ID = 0;
	
	private int id = ID++;
	private boolean isRaw;
	
	//private int propScore;
	private double propScore;
	private BigDecimal bar = BigDecimal.ZERO;
	
	private static Random random = new Random(System.currentTimeMillis()+1165);
	
	public static void setMaxElems(int k){
		maxModelElems = k;
	}
	
	public Element(String id){
		containingTupleId = new LinkedList<Integer>();
		modelId = id;
		properties = new HashSet<String>();
		basedUponElements = new ArrayList<Element>();
		if(! modelId.equals(AlgoUtil.NO_MODEL_ID)){
			basedUponElements.add(this);
			//containingTuple = new LinkedList<Tuple>();
			//containingTupleIds = new LinkedList<Integer>();
			Tuple contain = new Tuple();
			contain.addElement(this);
			containingTuple = contain;
			containingTupleId.add(contain.getId());
			//containingTupleIds.add(contain.getId());
		}
	}
	
	public Element(Tuple t){
		this(AlgoUtil.NO_MODEL_ID);
		containingTupleId = new LinkedList<Integer>();
		label = "";
		for(Element e:t.getRealElements()){
			properties.addAll(e.getProperties());
			basedUponElements.add(e);
			label = label +e.getLabel()+"+";
		}
		containingTuple = t;
		containingTupleId.add(t.getId());
		if(AlgoUtil.COMPUTE_RESULTS_CLASSICALLY){
			label =label+ properties.toString().replace(" ", "");
		}
		else
			label = "{"+t.toString()+"}";

	}
	
	public Element(int l, Model m, int commonVacabularyMin, int diffVacabularyMin){
		this(m.getId());
		containingTupleId = new LinkedList<Integer>();
	   
	    for (int i = 0; i < l; i++) {
	        this.addProperty(pickRandomProperty(commonVacabularyMin, diffVacabularyMin));
	    }
	    label = properties.toString().replace(" ", "");
	}
	
	public Element(String lbl, String props, String mId){ // used when read from file
		this(mId);
		String[]pr = props.split(";");
		sortedProps = new ArrayList<String>();
		for(int i=0;i<pr.length;i++){
			String property = pr[i];
			if(property.startsWith("\""))
				property = property.substring(1);
			if(property.endsWith("\""))
				property = property.substring(0,property.length()-1);
			this.addProperty(property.toLowerCase());
			sortedProps.add(property);
		}
		label = lbl;
	}
	
	public void writeElementToRow(Row r){
		r.createCell(0).setCellValue(modelId);
		r.createCell(1).setCellValue(getLabel());
		r.createCell(2).setCellValue(getPropertiesAsString(";"));
	}
	
	//julia here
	private String pickRandomProperty(int commonVacabulary, int diffVacabulary){
		int whichAlphabet = random.nextInt(2);
		if(whichAlphabet == 0){
			// int n = alphabet.length();
			return ""+random.nextInt(commonVacabulary);//alphabet.charAt(random.nextInt(n));
		}
		else{
			return  ""+(100+random.nextInt(diffVacabulary));
		}
	}
	
	private String getPropertiesAsString(String sep) {
		Set<String> props = getProperties();
		StringBuilder sb = new StringBuilder();
		for(String prop:props){
			sb.append(prop);
			sb.append(sep);
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	public String getIdentifyingLabel(){
		if(identifyingLabel == null){
			ArrayList<Element> elems = getBasedUponElements();
			ArrayList<String> fusedProps = new ArrayList<String>();
			for(Element e:elems){
				fusedProps.addAll(e.getProperties());
			}
			Collections.sort(fusedProps);
			identifyingLabel = fusedProps.toString();
		}
		return identifyingLabel;
	}
	
	public int getId(){
		return id;
	}
	
	public Tuple getContainingTuple(){
		return containingTuple;
	}
	
	public void setContainingTuple(Tuple t){
		containingTuple = t;
		setContainingTupleId(t.getId());
	}
	
	public int getContainingTupleId(){
		return containingTupleId.getLast();
	}
	
	public void setContainingTupleId(int tId){
		if (tId != containingTupleId.getLast()){
			//System.out.println(tId + "=" + containingTupleId.getLast() + "?");
			if (containingTupleId.size() == 2){
				containingTupleId.removeFirst();
			}
			containingTupleId.addLast(tId);
		}
	}
	
	public int resetContainingTupleId(){
		if (containingTupleId.size() == 2){
			containingTupleId.removeLast();
			return containingTupleId.getLast();
		}
		else{
			return -1;
		}
	}
	
	public int getContainingTupleIdSize(){
		return containingTupleId.size();
	}
	
	public String getLabel(){
		return label;
	}
	
	public String getPrintLabel(){
		return label.replaceAll(",", " ");
	}
	
	public ArrayList<Element> getBasedUponElements(){
		if(AlgoUtil.COMPUTE_RESULTS_CLASSICALLY){
			ArrayList<Element> retVal =  new ArrayList<Element>();
			retVal.add(this);
			return retVal;
			
		}
		return basedUponElements;
	}
	
	public ArrayList<Element> getConstructingElements(){
		return basedUponElements;
	}
	
	public String getModelId(){
		return modelId;
	}
	
	public void setModelId(String mId){
		if(modelId == AlgoUtil.NO_MODEL_ID)
			modelId = mId;
	}
	
	public ArrayList<String> sortedProperties(){
		if(sortedProps == null){
			sortedProps = new ArrayList<String>(properties);
			Collections.sort(sortedProps);
		}
		return sortedProps;
			
	}
	
//	@Override
//	public boolean equals(Object o) {
//		if (o instanceof Element){
//			Element e = (Element)o;
//			
//			return e.getModelId() == modelId && properties.equals(((Element)o).getProperties());
//		}
//		return false;
//	}
	
	public Set<String> getProperties(){
		return this.properties;
	}
	
	public void addProperty(String p){
		properties.add(p);
	}
	
	public int getSize(){
		return properties.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(label);//toPrint());
		return sb.append("<").append(modelId).append(">").toString();
	}
	
	public String toPrint(){
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> iter = sortedProperties().iterator(); iter.hasNext();) {
			String s = iter.next();
			sb.append(s);
			if(iter.hasNext())
				sb.append(";");
		}
		return sb.toString();
	}

	public void setAsRaw() {
		this.isRaw = true;
	}
	
	public boolean isRaw(){
		return this.isRaw;
	}
	
	/*public void setPropScore(int score){
		this.propScore = score;
	}
	
	public int getPropScore(){
		return this.propScore;
	}*/
	public void setPropScore(double score){
		this.propScore = score;
	}
	
	public double getPropScore(){
		return this.propScore;
	}
	
	public void setBar(BigDecimal bar){
		this.bar = bar;	
	}
	
	public BigDecimal getBar(){ 
		return this.bar;
	}
	
	public int hashCode(){
		return Integer.parseInt(getModelId()) * Element.maxModelElems + getId();
	}
}

