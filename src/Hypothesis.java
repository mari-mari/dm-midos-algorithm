package midosAlgorithm;

import java.util.Arrays;

public class Hypothesis implements Comparable<Hypothesis>{
	private double quality;
	private int[] attributes;

	public Hypothesis(int[] attributes){
		this.attributes = Arrays.copyOf(attributes, attributes.length);
	}
	
	public double getQuality() {
		return quality;
	}
	
	public void setQuality(double quality) {
		this.quality = quality;
	}
	
	public int[] getAttributes() {
		return attributes;
	}

	public int compareTo(Hypothesis o) {
		return Double.compare(this.quality, o.quality);
	}
	
	public String toString(){
		return "Hypo: "+"atributes: "+ Arrays.toString(this.attributes)+" quality = "+this.quality;
	}
}
