package compling.gui.grammargui.util;

import java.util.HashMap;
import java.util.Map;

public class IdProvider {

	private static Map<String, IdProvider> instances;

	private String base;
	private int id;

	protected IdProvider(String base, int start) {
		this.base = base;
		this.id = start;
	}

	public static IdProvider build(String base) {
		return build(base, 0);
	}

	public static IdProvider build(String base, int start) {
		Map<String, IdProvider> instanceMap = IdProvider.instances();
		if (instanceMap.containsKey(base))
			throw new IllegalArgumentException(String.format("Id %s already present", base));

		IdProvider n = new IdProvider(base, start);
		instanceMap.put(base, n);
		return n;
	}

	public static String next(String base) {
		IdProvider idProvider = instances().get(base);
		if (idProvider != null)
			return idProvider.next();
		else
			return build(base).next();
	}

	public static String get(String base) {
		IdProvider idProvider = instances().get(base);
		if (idProvider != null)
			return idProvider.get();
		else
			return build(base).get();
	}

	public String next() {
		String ret = get(base);
		++id;
		return ret;
	}

	public String get() {
		return String.format("%s-%d", base, id);
	}

	/**
	 * @return the idMap
	 */
	protected static Map<String, IdProvider> instances() {
		if (instances == null)
			instances = new HashMap<String, IdProvider>();

		return instances;
	}
}
