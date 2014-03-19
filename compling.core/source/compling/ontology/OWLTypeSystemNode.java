package compling.ontology;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntClass;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.util.Interner;

public class OWLTypeSystemNode implements TypeSystemNode {

	protected OntClass ontClass;
	protected String internedName;
	protected static Interner<String> interner = new Interner<String>(); 
	
	public OWLTypeSystemNode(OntClass ontClass) {
		this.ontClass = ontClass;
		this.internedName = interner.intern(ontClass.getLocalName());
		
		assert this.internedName != null;
	}
	
	@Override
	public int compareTo(TypeSystemNode that) {
		return this.getType().compareTo(that.getType());
	}

	@Override
	public String getType() {
		return internedName;
	}

	@Override
	public Set<String> getParents() {
		Set<String> parents = new HashSet<String>();
		for (Iterator<OntClass> i = ontClass.listSuperClasses(true); i.hasNext(); ) 
			parents.add(i.next().getLocalName());
		
		return parents;
	}

	// XXX: this shouldn't be public if these are to be used as keys
	@Override
	public void setType(String type) {
		internedName = type;
	}

	@Override
	public String toString() {
		return String.format("[%s sub %s]", getType(), getParents());
//		return ontClass.toString();
	}

	@Override
	public int hashCode() {
		return internedName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		
		if (obj == null) return false;
		
		if (getClass() != obj.getClass()) return false;
		
		OWLTypeSystemNode other = (OWLTypeSystemNode) obj;
		if (internedName == null) {
			if (other.internedName != null)
				return false;
		}
		else if (internedName != other.internedName)
			return false;
		
		return true;
	}
	
	/** return underlying ontological class */
	public Object getOntologicalClass() {
	  return ontClass;
	}
}
