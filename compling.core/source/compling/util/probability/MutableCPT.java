package compling.util.probability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import compling.util.Counter;

/**
 * MutableCPT is intended to be a simple simple simple and easy implementation of a CPT. The goal is to make it easy to
 * update and allow different domains for each evidence set.
 * 
 * Note that this implementation isn't fast. It's not intended to be fast. It's intended for gathering up obersvations
 * about a domain before you know the domain's complete scope.
 * 
 * The CPT allows evidence lists of different sizes unless you specify the size with the constructor in which case it
 * will check to make sure that the evidence list is the right size();
 * 
 * Additionally, make sure to pass evidence into this class in the same order every time. If you don't the two events
 * will be treated as unique. i.e. P(a | x, y) will be different from p(a | y, x)
 * 
 * Finally, note that if you ask for the probability of an event where the conditioning evidence has never been seen
 * before, currently, it throws an exception. Later if this implementation gets smoothed, this will change.
 **/

public class MutableCPT<VARTYPE, EVIDENCETYPE> implements ProbabilityDistribution<VARTYPE, EVIDENCETYPE> {

	private int numConditioningVariables = 0;
	private HashMap<String, Counter<VARTYPE>> table = new HashMap<String, Counter<VARTYPE>>();

	/** A constructor that allows different numbers of evidence variables in the same distribution. */
	public MutableCPT() {
		table = new HashMap<String, Counter<VARTYPE>>();
	}

	/** A constructor that defines a fixed number of evidence variables. */
	public MutableCPT(int numConditioningVariables) {
		this();
		this.numConditioningVariables = numConditioningVariables;
	}

	private String makeKey(List<EVIDENCETYPE> evidence) {
		if (numConditioningVariables != 0 && evidence.size() != numConditioningVariables) {
			throw new RuntimeException("Wrong number of evidence variables");
		}
		StringBuffer sb = new StringBuffer();
		for (EVIDENCETYPE e : evidence) {
			sb.append(e).append("|");
		}
		return sb.toString();
	}

	/** For adding a single observation in. Increments count of that var by 1 */
	public void addObservation(VARTYPE v, List<EVIDENCETYPE> e) {
		String key = makeKey(e);
		if (!table.containsKey(key)) {
			table.put(key, new Counter<VARTYPE>());
		}
		table.get(key).incrementCount(v, 1);
	}

	/** Sets the count of for this observation. Clobbers the old value */
	public void setObservationCount(VARTYPE v, List<EVIDENCETYPE> e, double count) {
		String key = makeKey(e);
		if (!table.containsKey(key)) {
			table.put(key, new Counter<VARTYPE>());
		}
		table.get(key).setCount(v, count);
	}

	/** Given the obervations so far, return P(v | e) */
	public double getConditionalProbability(VARTYPE v, List<EVIDENCETYPE> e) {
		String key = makeKey(e);
		if (!table.containsKey(key)) {
			throw new RuntimeException("Unseen set of evidence!");
		}
		return table.get(key).getCount(v) / table.get(key).totalCount();
	}

	/** Given the obervations so far, return log(P(v | e)) */
	public double getConditionalLogProbability(VARTYPE v, List<EVIDENCETYPE> e) {
		return Math.log(getConditionalProbability(v, e));
	}

	public static void main(String[] args) {
		MutableCPT<String, String> mcpt = new MutableCPT<String, String>(3);
		List<String> evidence1 = new ArrayList<String>();
		evidence1.add("a");
		evidence1.add("b");
		evidence1.add("c");

		mcpt.addObservation("d", evidence1);
		mcpt.addObservation("e", evidence1);

		System.out.println("P(d | " + evidence1 + " ) =" + mcpt.getConditionalProbability("d", evidence1));
		System.out.println("P(e | " + evidence1 + " ) =" + mcpt.getConditionalProbability("e", evidence1));
		System.out.println("P(f | " + evidence1 + " ) =" + mcpt.getConditionalProbability("f", evidence1));

		List<String> evidence2 = new ArrayList<String>();
		evidence2.add("c");
		evidence2.add("a");
		evidence2.add("b");

		mcpt.addObservation("d", evidence2);
		mcpt.addObservation("e", evidence2);
		mcpt.addObservation("f", evidence2);

		System.out.println("P(d | " + evidence2 + " ) =" + mcpt.getConditionalProbability("d", evidence2));
		System.out.println("P(e | " + evidence2 + " ) =" + mcpt.getConditionalProbability("e", evidence2));
		System.out.println("P(f | " + evidence2 + " ) =" + mcpt.getConditionalProbability("f", evidence2));
		System.out.println("P(g | " + evidence2 + " ) =" + mcpt.getConditionalProbability("g", evidence2));

		evidence2.add("q");
		mcpt.addObservation("d", evidence2); // this line should throw an exception
	}

}
