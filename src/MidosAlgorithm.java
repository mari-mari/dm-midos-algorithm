package midosAlgorithm;

import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class MidosAlgorithm {
	private int[][] data; // values of instances over attributes and target values in last column
	int functionChoice; // quality function choice
	int numberOfInterestingSubGroups; // desired number of interesting subgroups
	int numberOfInstances; // number of lines in file - number of population
	int numberOfAttributes; // number of attributes, +1 for target value
	int label; // target label - positive or negative (1 or 0) - for quality calculation
	double significanceLevel = 0.01;
	double Zn = 2.58;
	PriorityQueue<Hypothesis> solutions; // output, hypothesis, S
	LinkedList<Hypothesis> hypothesis; // input hypothesis, Q

	public MidosAlgorithm(String file) {
		readData(file);
		label = 1; // target label positive
		solutions = new PriorityQueue<>();
		hypothesis = new LinkedList<>();
	}
	
	/*Reads data as required in the task:
	 *first line of file contains parameters
	 *next lines contain binary attribute values*/

	private void readData(String file) {

		try (Scanner sc = new Scanner(Paths.get(file))) {
			String[] params = sc.nextLine().split(",");
			this.numberOfInstances = Integer.parseInt(params[0]);
			this.numberOfAttributes = Integer.parseInt(params[1]);
			this.numberOfInterestingSubGroups = Integer.parseInt(params[2]);
			this.functionChoice = Integer.parseInt(params[3]);
			data = new int[this.numberOfInstances][this.numberOfAttributes + 1];
			for (int i = 0; i < data.length; i++) {
				String[] str = sc.nextLine().split(",");
				for (int j = 0; j < data[0].length; j++)
					data[i][j] = Integer.parseInt(str[j]);
			}
		} catch (IOException e) {
			System.out.println("File does not exist!");
		}
	}
	
	/*Refinement operator: builds prefix tree.
	 *New hypothesis is obtained by adding one more attribute to old hypothesis
	 *Note that attributes are ordered*/

	private LinkedList<Hypothesis> refine(Hypothesis hypo) {
		LinkedList<Hypothesis> refinedHypos = new LinkedList<>();
		if (hypo.getAttributes().length == 0) // if root (h0 = {} - hypothesis that does not contain any attribute)
			for (int i = 0; i < this.numberOfAttributes; i++) {
				int[] newHypo = { i }; // create child hypo of h0 for each attribute
				refinedHypos.add(new Hypothesis(newHypo));
			}
		else {//if not root, add attribute that is higher by oredering to existing attributes in hypothesis
			for (int i = hypo.getAttributes()[hypo.getAttributes().length - 1] + 1; i < this.numberOfAttributes; i++) {
				int[] newHypo = Arrays.copyOf(hypo.getAttributes(), hypo.getAttributes().length + 1);
				newHypo[newHypo.length - 1] = i;
				refinedHypos.add(new Hypothesis(newHypo));
			}
		}
		return refinedHypos;
	}
	
	/*Helper function, checks, whether an instance satisfies a hypothesis*/

	private boolean contains(Hypothesis h, int[] instance) {
		int count = 0;
		for (int j = 0; j < h.getAttributes().length; j++) {
			if (instance[h.getAttributes()[j]] != label)
				break;
			count++;
		}
		return count == h.getAttributes().length;
	}
	
	/*Calculates extention size - number of instances that satisfy a hypothesis*/

	private double extensionSize(Hypothesis hypo) {
		double count = 0.0;
		for (int i = 0; i < this.numberOfInstances; i++)
			if (contains(hypo, data[i]))
				count++;
		return count;
	}
	
	/*Calculates extention(+) with certain label size (here positives) - number of instances that satisfy a hypothesis, and have class "1" */

	private double extentionLabeledSize(Hypothesis hypo) {
		double count = 0.0;
		for (int i = 0; i < this.numberOfInstances; i++)
			if (contains(hypo, data[i]) && data[i][data[0].length - 1] == label)
				count++;
		return count;
	}
	
	/*Calculates number of certain class (here positives) instances within the whole population*/

	private double populationLabeledSize() {
		double count = 0.0;
		for (int i = 0; i < this.numberOfInstances; i++)
			if (data[i][data[0].length - 1] == label)
				count++;
		return count;
	}
	
	/*Calculates the value of quality/interestingness function depending on function choice*/

	private double calculateQuality(int functionChoice, Hypothesis hypo) {
		double g = extensionSize(hypo) / this.numberOfInstances;
		double p0 = populationLabeledSize() / this.numberOfInstances;
		double p = 0.0;
		if (extensionSize(hypo) > 0)
			p = extentionLabeledSize(hypo) / extensionSize(hypo);
		switch (functionChoice) {
		case 1: return Math.sqrt(g) * Math.abs(p - p0);
		case 2: return (g / (1 - g)) * (p - p0)*(p-p0);
		case 3: return g * (2 * p - 1) + 1 - p0;
		}
		return 0.0;
	}
	
	/*Calculates optimistic estimate of a hypothesis, usefull for pruning*/

	private double optimisticEstimate(Hypothesis hypo) {
		double g = extensionSize(hypo) / this.numberOfInstances;
		double p0 = populationLabeledSize() / this.numberOfInstances;
		return Math.sqrt(g) * Math.max(p0,(1 - p0));
	}
	
	/*MIDOS algorithm*/

	public PriorityQueue<Hypothesis> runAlgorithm() {
		//set root as empty hypothesis
		int[] noneOfAttributes = {};
		Hypothesis root = new Hypothesis(noneOfAttributes);
		root.setQuality(calculateQuality(this.functionChoice, root));
		//add to the population of hypothesises
		hypothesis.add(root);
		//whyle population of hypothesises is not empty
		while (!hypothesis.isEmpty()) {
			double min = Double.MIN_VALUE;
			if (!solutions.isEmpty())
				min = solutions.peek().getQuality();
				//take hypothesis from population of hypothesis and refine it
			Hypothesis hypo = hypothesis.removeFirst();
			//for each hypothesis h' from set of refinement of initial hypothesis
			for (Hypothesis h : refine(hypo)) {
				//calculate interestingness
				double quality = calculateQuality(this.functionChoice, h);
				h.setQuality(quality);
				//if optimistic estimate of h' is smaller than quality of worst hypothesis in solutions
				if (optimisticEstimate(h) < min)
				//prune(do not explore this branch anymore)
					continue;
				else {//otherwise if Solutions's size is less than desired
					//add h' to the Solutions
					if (solutions.size() < this.numberOfInterestingSubGroups) {
						solutions.add(h);
					}
					//if not, check if h' is more interesting than worst h in Solutions
					//if yes, remove worst, add h' 
					else {
						if (quality > solutions.peek().getQuality()) {
							solutions.remove();
							solutions.add(h);
						}
					}
					//add h' to the population of hypothesises
					hypothesis.add(h);
				}
			}
		}
		//return desired amount of the most interesting hypothesises 
		return solutions;
	}
	
	/*Calculates significance of hypothesis*/

	private double calculateSignificance(Hypothesis hypo) {
		double n = extensionSize(hypo);
		double p0 = populationLabeledSize() / this.numberOfInstances;
		double p = extentionLabeledSize(hypo) / extensionSize(hypo);
		return (p - p0) / (Math.sqrt(p0 * (1 - p0) / n));
	}
	
	/*Checks if hypo's significance is within defined significance interval Zn*/

	private boolean isSignificant(Hypothesis hypo) {
		return Math.abs(calculateSignificance(hypo)) < Zn;
	}
	
	/*Writes set of the most interesting hypothesises to file*/
	
	public void writeToFile(String fileName){
		try(PrintWriter pw = new PrintWriter(fileName)){
			for(Hypothesis h: solutions)
				pw.write(h.toString()+"\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		MidosAlgorithm ma = new MidosAlgorithm("SPECT_union.txt");
		ma.runAlgorithm().forEach(x -> System.out.println(x));
		ma.writeToFile("output.txt");

	}

}
