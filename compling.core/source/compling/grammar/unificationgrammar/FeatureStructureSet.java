package compling.grammar.unificationgrammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.GrammarException;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.DefaultStructureFormatter;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.FeatureStructureFormatter;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.Interner;

public class FeatureStructureSet implements Cloneable {

	private int unificationCounter = 0;
	private static FeatureStructureFormatter formatter = new DefaultStructureFormatter();
	private static TypeConstraint listTypeConstraint = null;
	private static Interner<String> interner = new Interner<String>();
	private Set<Slot> roots = new LinkedHashSet<Slot>();
	private Set<Slot> allSlots = new HashSet<Slot>();
	private Slot myRoot;

	public FeatureStructureSet(TypeConstraint tc) {
		myRoot = new Slot();
		myRoot.setTypeConstraint(tc);
		allSlots.add(myRoot);
		roots.add(myRoot);
	}

	public FeatureStructureSet() {
		this(null);
	}

	public Set<Slot> getRootSlots() {
		return roots;
	}

	public Slot getMainRoot() {
		return myRoot;
	}

	public Slot getSlot(SlotChain chain) {
		return getSlot(myRoot, chain);
	}

	public Slot getSlot(int uniqueSlotID) {
		for (Slot slot : allSlots) {
			if (slot.getID() == uniqueSlotID) {
				return slot;
			}
		}
		return null;
	}

	public boolean hasSlot(Slot root, SlotChain chain) {
		Slot slot = root;
		for (Role role : chain.getChain()) {
			if (slot.features == null) {
				return false;
			}
			Map<Role, Slot> map = slot.features;
			// now the slot var is going to be set up with the one for the next
			// iteration
			if (map.containsKey(role)) {
				slot = map.get(role);
			} else {
				return false;

			}
		}
		return true;
	}

	public Slot getSlot(Slot root, SlotChain chain) {
		Slot slot = root;
		for (Role role : chain.getChain()) {
			if (slot.features == null) {
				slot.features = new HashMap<Role, Slot>();
			}
			Map<Role, Slot> map = slot.features;
			// now the slot var is going to be set up with the one for the next
			// iteration
			if (map.containsKey(role)) {
				slot = map.get(role);
			} else {
				slot = new Slot(role);
				map.put(role, slot);
				allSlots.add(slot);
			}
		}
		return slot;
	}

	public Collection<Slot> getSlots() {
		return allSlots;
	}

	public boolean coindexAcrossFeatureStructureSets(SlotChain chain1, SlotChain chain2, FeatureStructureSet that) {
		// System.out.println(chain1+"  "+chain2);
		this.unificationCounter = Math.max(this.unificationCounter, that.unificationCounter) + 1;
		Slot thatRootCopy = slotCopier(that.allSlots, that.roots, that.myRoot);
		if (chain2.getChain().size() == 0) {
			roots.remove(thatRootCopy);
		}
		Slot chain1Slot = getSlot(chain1);
		Slot chain2Slot = getSlot(thatRootCopy, chain2);
		if (coindex(chain1Slot, chain2Slot)) {
			return true;
		}
		return false;
	}

	public Slot mergeFeatureStructureSets(FeatureStructureSet that) {
		this.unificationCounter = Math.max(this.unificationCounter, that.unificationCounter);
		return slotCopier(that.allSlots, that.roots, that.myRoot);
	}

	public boolean coindex(SlotChain chain1, SlotChain chain2) {
		// System.out.println(chain1+"    "+chain2);
		return coindex(getSlot(chain1), getSlot(chain2));
	}

	public boolean coindex(Slot root1, SlotChain chain1, Slot root2, SlotChain chain2) {
		return coindex(getSlot(root1, chain1), getSlot(root2, chain2));
	}

	public boolean coindex(Slot slot1, Slot slot2) {
		unificationCounter++;
		return slot1.unify(slot2);
	}

	public boolean fill(SlotChain chain, String atom) {
		return fill(getSlot(chain), atom);
	}

	public boolean fill(Slot root, SlotChain chain, String atom) {
		return fill(getSlot(root, chain), atom);
	}

	private boolean fill(Slot slot, String atom) {
		atom = interner.intern(atom);
		if (slot.hasAtomicFiller()) {
			slot.atom = atom;  // TODO: @seantrott (sets "*" to "8", for example)
			return slot.atom == atom;
		} else if (!slot.hasFiller()) {
			slot.atom = atom;
			slot.realFiller = true;
			return true;
		}
		return false;
	}

	public boolean addSlot(SlotChain slotChain) {
		List<Role> chain = slotChain.getChain();
		TypeConstraint typeConstraint = chain.get(chain.size() - 1).getTypeConstraint();
		return getSlot(slotChain).compatibleTypes(typeConstraint);
	}

	// returns the copy of the original's root.
	private Slot slotCopier(Set<Slot> sourceSet, Set<Slot> sourceRootSet, Slot sourceRoot) {
		HashMap<Slot, Slot> oldSlotToNewSlot = new HashMap<Slot, Slot>();
		for (Slot oldSlot : sourceSet) {
			Slot newSlot = this.new Slot(oldSlot);
			oldSlotToNewSlot.put(oldSlot, newSlot);
			allSlots.add(newSlot);
			if (sourceRootSet.contains(oldSlot)) {
				roots.add(newSlot);
			}
		}
		for (Slot oldSlot : sourceSet) {
			Slot newSlot = oldSlotToNewSlot.get(oldSlot);
			if (oldSlot.features != null) {
				for (Role role : oldSlot.features.keySet()) {
					// if (oldSlotToNewSlot.get(oldSlot.features.get(role))==
					// null){System.out.println("testing null slotcopier:");}
					newSlot.features.put(role, oldSlotToNewSlot.get(oldSlot.features.get(role)));
				}
			}
		}
		return oldSlotToNewSlot.get(sourceRoot);
	}

	public FeatureStructureSet clone() {
		try {
			FeatureStructureSet fss = (FeatureStructureSet) super.clone();
			fss.allSlots = new LinkedHashSet<Slot>();
			fss.roots = new HashSet<Slot>();
			fss.myRoot = fss.slotCopier(this.allSlots, this.roots, this.myRoot);
			return fss;
		} catch (Exception e) {
			System.out.println("Inexplicable meltdown in FeatureStructureSet.clone: " + e);
			throw new GrammarException("Inexplicable meltdown in FeatureStructureSet.clone: " + e);
		}
	}

	public final class Slot {

		private int unificationCntr = 0;
		Map<Role, Slot> features = null;
		String atom = null;
		// rolesThatPointToMe only gets updated in the base case and in
		// coindexing. unifying should change it.
		private List<Role> rolesThatPointToMe = new ArrayList<Role>();
		public List<Slot> listValue = null;
		private TypeConstraint typeConstraint = null;
		private int uniqueID = -1; // this is available for external programs,
									// and gets preserved in clones
		private boolean realFiller = false;

		private Slot(Role role) {
			this();
			if (role != null) {
				rolesThatPointToMe.add(role);
				setTypeConstraint(role.getTypeConstraint());
			}
		}

		public Slot() {
		}
		
		public void setAtom(String at) {
			this.atom = at;
		}

		public Slot(Slot that) {
			this.unificationCntr = that.unificationCntr;
			this.atom = that.atom;
			if (that.features != null) {
				this.features = new HashMap<Role, Slot>();
			}
			this.setTypeConstraint(that.typeConstraint);
			this.uniqueID = that.uniqueID;
			this.realFiller = that.realFiller;
			this.rolesThatPointToMe.addAll(that.rolesThatPointToMe);
		}

		public int getID() {
			return uniqueID;
		}

		public void setID(int id) {
			this.uniqueID = id;
		}

		public String getAtom() {
			return atom;
		}

		public Map<Role, Slot> getFeatures() {
			return features;
		}

		// returns true only if an atom, a non-empty list or a "real" structured
		// filler is in there.
		public boolean hasRealFiller() {
			return realFiller == true;
		}

		public void setRealFiller(boolean realFiller) {
			this.realFiller = realFiller;
		}

		public boolean hasFiller() {
			return features != null || atom != null || listValue != null;
		}

		public boolean isListSlot() {
			return listValue != null;
		}

		public boolean hasAtomicFiller() {
			return atom != null;
		}

		public boolean hasStructuredFiller() {
			return features != null;
		}

		public int getSlotIndex() {
			int i = 0;
			for (Slot s : allSlots) {
				if (s == this) {
					return i;
				}
				i++;
			}
			return -1;
		}

		public Slot getSlot(Role r) {
			return features.get(r);
		}

		public void setTypeConstraint(TypeConstraint tc) {
			if (tc != null) {
				this.typeConstraint = tc;
				if (tc == listTypeConstraint) {
					listValue = new ArrayList<Slot>();
				}
			}
		}

		public TypeConstraint getTypeConstraint() {
			return typeConstraint;
		}

		public List<Role> getParentSlots() {
			return rolesThatPointToMe;
		}

		private boolean unify(Slot that) {
			// DO NOT REMOVE THIS NEXT LINE!!
			if (this == that) {
				return true;
			}
			if (unificationCounter == this.unificationCntr) {
				return true;
			} // if true, we've already been here
			this.unificationCntr = unificationCounter;

			if (compatibleTypes(that.typeConstraint)) {
				rolesThatPointToMe.addAll(that.getParentSlots());
				if (this.isListSlot() && that.isListSlot()) {
					this.listValue.addAll(that.listValue);
					if (this.listValue.size() > 0) {
						this.realFiller = true;
					}
				}

				for (Slot slot : allSlots) {
					if (slot.features != null) {
						for (Role role : slot.features.keySet()) {
							if (slot.features.get(role) == that) {
								slot.features.put(role, this);
							}
						}
					}
				}

				if (!this.isListSlot() && !unifyFillers(that)) {
					return false;
				}
				allSlots.remove(that);
				if (roots.contains(that)) {
					roots.remove(that);
					roots.add(this);
					if (myRoot == that) {
						myRoot = this;
					}
				}
				return true;

			} else if (this.isListSlot() && !that.isListSlot()) {
				this.listValue.add(that);
				this.realFiller = true;
				return true;
			} else if (!this.isListSlot() && that.isListSlot()) {
				that.listValue.add(this);
				that.realFiller = true;
				return true;
			} else {
				// throw new
				// FailureToUnifyException("Failure to unify slot between this slot set: "+rolesThatPointToMe+" and this set "+that.rolesThatPointToMe);
				return false;
			}
		}

		private boolean compatibleTypes(TypeConstraint thatType) {
			TypeConstraint thisType = typeConstraint;
			if (thisType == null && thatType == null) {
				return true;
			} else if (thisType == null && thatType != null) {
				typeConstraint = thatType;
				return true;
			} else if (thisType != null && thatType == null) {
				return true;
			} else if (thisType.typeSystem != thatType.typeSystem) {
				return false;
			} else { // now that both slots have compatible type systems
				try {
					if (thisType.typeSystem.subtype(thisType.type, thatType.type)) {
						// this type is more specific
						return true;
					} else if (thisType.typeSystem.subtype(thatType.type, thisType.type)) {
						// that type is more specific
						typeConstraint = thatType;
						return true;
					} else {
						return false;
					}
				} catch (TypeSystemException tse) {
					throw new GrammarException(tse + ".\nThis.typeSystem=" + thisType.getTypeSystem().getName()
							+ " and thatType.typeSystem=" + thatType.getTypeSystem().getName());
				}
			}
		}

		private boolean unifyFillers(Slot that) {
			if ((!this.hasFiller() && !that.hasFiller()) || (this.hasFiller() && !that.hasFiller())) {
				return true;
			} else if (!this.hasFiller() && that.hasFiller()) {
				this.features = that.features;
				this.atom = that.atom;
				realFiller = true;
				return true;
			} else if (this.hasAtomicFiller() && that.hasAtomicFiller()) {
				return this.atom == that.atom;
			} else if (this.hasStructuredFiller() && that.hasStructuredFiller()) {
				for (Role thatRole : that.features.keySet()) {
					if (this.features.containsKey(thatRole)) {
						if (this.features.get(thatRole) == null) {
							System.out.println("CHECK YOURSELF !!!!  " + thatRole.getName());
						}
						if (!this.features.get(thatRole).unify(that.features.get(thatRole))) {
							return false;
						}
					} else {
						this.features.put(thatRole, that.features.get(thatRole));
					}
				}
				realFiller = this.realFiller || that.realFiller;
				return true;
			} else {
				return false;
			}
		}

		public String toString() {
			return formatter.format(this);
		}

	}

	public String toString() {
		return formatter.format(this);
	}

	public static void setFormatter(FeatureStructureFormatter formatter) {
		FeatureStructureSet.formatter = formatter;
	}

	public static void setListTypeConstraint(TypeConstraint tc) {
		FeatureStructureSet.listTypeConstraint = tc;
	}

	public static void main(String[] args) {

		// FeatureStructureSet fss1 = new FeatureStructureSet();
		// fss1.coindex(new SlotChain("a.b"), new SlotChain("c.d"));
		// System.out.println("Before fill fs1:" + fss1.allSlots.size() + "\n" +
		// fss1);
		// fss1.fill(new SlotChain("a.b"), "6");
		// // fss1.fill(new SlotChain("a.c"), "7");
		// // fss1.getSlot(new SlotChain("a.f"));
		// // fss1.getSlot(new SlotChain("a.g"));
		// System.out.println("Original fs1:\n" + fss1);
		// FeatureStructureSet fss2 = (FeatureStructureSet) fss1.clone();
		// System.out.println("fss2 fill true or false: " + fss2.fill(new
		// SlotChain("a.f"), "9"));
		// System.out.println("fss2 coindex true or false: " + fss2.coindex(new
		// SlotChain("a.f"), new SlotChain("a.g")));
		// System.out.println("New fs1:\n" + fss1);
		// System.out.println("Fss2:\n" + fss2);
		// FeatureStructureSet fss3 = new FeatureStructureSet();
		// fss3.coindex(new SlotChain("p.q"), new SlotChain("x.y"));
		// fss3.fill(new SlotChain("p.q"), "13");
		// System.out.println("Fss3 before cfsc:\n" + fss3);
		// fss3.coindexAcrossFeatureStructureSets(new SlotChain("p"), new
		// SlotChain("a"), fss1);
		// // fss3.fill(new SlotChain("p.a.f"), "42");
		// System.out.println("Fss3 after cfsc:\n" + fss3);
		// System.out.println("final fss1:\n" + fss1); //
		// System.out.println("Now we should have a runtime exception");
		// // fss1.coindexAcrossFeatureStructureSets(new SlotChain(""), new
		// // SlotChain(""), fss2);

		TypeSystem<Construction> bogusTS = new TypeSystem<Construction>("bogus");

		FeatureStructureSet fss1 = new FeatureStructureSet();
		Set<Slot> rootSlots = fss1.getRootSlots();
		for (Slot root : rootSlots) {
			root.setTypeConstraint(new TypeConstraint("A", bogusTS));
		}
		fss1.getSlot(new SlotChain("m.a"));
		System.out.println("Original fss1:\n" + fss1);

		FeatureStructureSet fss2 = new FeatureStructureSet();
		Set<Slot> rootSlots2 = fss2.getRootSlots();
		for (Slot root : rootSlots2) {
			root.setTypeConstraint(new TypeConstraint("B", bogusTS));
		}
		fss2.getSlot(new SlotChain("m.b"));
		System.out.println("Original fss2:\n" + fss2);

		fss1.mergeFeatureStructureSets(fss2);
		System.out.println("New fss1:\n" + fss1);

		fss1.getSlot(new SlotChain("m.b"));
		System.out.println("Last fss1:\n" + fss1);
	}

}
