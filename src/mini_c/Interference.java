package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Interference {
	Map<Register, Arcs> graph = new HashMap<Register, Arcs>();

	Interference(Liveness lg) {
		// First iteration to set preference vertices
		Iterator<Entry<Label, LiveInfo>> it = lg.info.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<Label, LiveInfo> pair = it.next();
	    	if (pair.getValue().instr instanceof ERmbinop && ((ERmbinop) pair.getValue().instr).m == Mbinop.Mmov) {
	    		Register r1 = ((ERmbinop) pair.getValue().instr).r1;
	    		Register r2 = ((ERmbinop) pair.getValue().instr).r2;
	    		if (!r1.equals(r2)) {
	    			if (!this.graph.containsKey(r1)) this.graph.put(r1, new Arcs());
	    			this.graph.get(r1).prefs.add(r2);
	    			if (!this.graph.containsKey(r2)) this.graph.put(r2, new Arcs());
	    			this.graph.get(r2).prefs.add(r1);
	    		}
	    	}
	    }
	    
	    // Second iteration to set interference vertices
	    it = lg.info.entrySet().iterator();
	    while (it.hasNext()){
	    	Entry<Label, LiveInfo> pair = it.next();
	    	for (Register r1 : pair.getValue().defs) {
	    		for (Register r2 : pair.getValue().outs) {
	    			if (!pair.getValue().defs.contains(r2)) {
	    				if (!this.graph.containsKey(r1)) this.graph.put(r1, new Arcs());
	        			this.graph.get(r1).intfs.add(r2);
	        			this.graph.get(r1).prefs.remove(r2);
	        			if (!this.graph.containsKey(r2)) this.graph.put(r2, new Arcs());
	        			this.graph.get(r2).intfs.add(r1);
	        			this.graph.get(r2).prefs.remove(r1);
	    			}
	    			else if (!r1.equals(r2)) {
	    				if (!this.graph.containsKey(r1)) this.graph.put(r1, new Arcs());
	        			this.graph.get(r1).intfs.add(r2);
	        			this.graph.get(r1).prefs.remove(r2);
	    			}
	    		}
	    	}
	    	
	    	//Special cases (setcc instructions in ERmbinop and Munop)
	    	final List<Mbinop> specials = new LinkedList<Mbinop>();
    		specials.add(Mbinop.Msete); specials.add(Mbinop.Msetg); specials.add(Mbinop.Msetge);
    		specials.add(Mbinop.Msetl); specials.add(Mbinop.Msetle); specials.add(Mbinop.Msetne);
    		
    		final List<Register> non_reducable = new LinkedList<Register>();
    		non_reducable.add(Register.rsi); non_reducable.add(Register.rdi);
    		
	    	boolean condition = pair.getValue().instr instanceof ERmbinop && specials.contains(((ERmbinop) pair.getValue().instr).m);
	    	condition |= pair.getValue().instr instanceof ERmunop && ( (((ERmunop) pair.getValue().instr).m) instanceof Msetei || (((ERmunop) pair.getValue().instr).m) instanceof Msetnei );
	    	

	    	if (condition) {
	    	 	for (Register r1 : pair.getValue().defs) {
		    		for (Register r2 : non_reducable) {
	    				if (!this.graph.containsKey(r1)) this.graph.put(r1, new Arcs());
	        			this.graph.get(r1).intfs.add(r2);
	        			this.graph.get(r1).prefs.remove(r2);
	        			if (!this.graph.containsKey(r2)) this.graph.put(r2, new Arcs());
	        			this.graph.get(r2).intfs.add(r1);
	        			this.graph.get(r2).prefs.remove(r1);
		    		}
		    	}
	    		
	    	}
	    	
	    	}
    	
	}
	
	boolean onlyPhysical() {
		for (Register r : this.graph.keySet()) { if (r.isPseudo()) return false; }
		return true;
	}
	
	void print() {
	    System.out.println("interference:");
	    for (Register r: graph.keySet()) {
	    		Arcs a = graph.get(r);
	    		System.out.println("  " + r + " pref=" + a.prefs + " intf=" + a.intfs);
	    }
	}
	
	public String toString() {
	    String rtn = "interference:\n";
	    for (Register r: graph.keySet()) {
	    		Arcs a = graph.get(r);
	    		rtn += "  " + r.toString() + " pref=" + a.prefs.toString() + " intf=" + a.intfs.toString() + "\n";
	    }
	    return rtn;
	}
	
	Arcs remove_register(Register r) {
		Iterator<Entry<Register, Arcs>> it = this.graph.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Register, Arcs> pair = it.next();
			if (pair.getKey().equals(r)) continue;
			// Remove vertices going towards the removed register
			pair.getValue().intfs.remove(r);
			pair.getValue().prefs.remove(r);
		}
		return this.graph.remove(r);
	}
	
	Register coalesce_register(Register r1, Register r2) {
		if (r2.isHW()) {
			Register tmp = r1;
			r1 = r2;
			r2 = tmp;
		}
		//Removing old links
		for (Register r : this.graph.get(r2).intfs) this.graph.get(r).intfs.remove(r2);
		for (Register r : this.graph.get(r2).prefs) this.graph.get(r).prefs.remove(r2);
		for (Register r : this.graph.get(r1).intfs) this.graph.get(r).intfs.remove(r1);
		for (Register r : this.graph.get(r1).prefs) this.graph.get(r).prefs.remove(r1);
		
		Arcs new_node = this.graph.get(r1);
		//Coalescing vertices
		new_node.intfs.addAll(this.graph.get(r2).intfs);
		new_node.prefs.addAll(this.graph.get(r2).prefs);
		
		//Removing self-referential vertices
		new_node.prefs.remove(r1);
		new_node.prefs.remove(r2);
		
		//removing preference arcs that interfere
		new_node.prefs.removeAll(new_node.intfs);
		
		//Adding new links
		for (Register r : new_node.prefs) this.graph.get(r).prefs.add(r1);
		for (Register r : new_node.intfs) this.graph.get(r).intfs.add(r1);
		
		//Removing old register
		this.graph.remove(r2);
		return r1;
	}
}

class Arcs {
	Set<Register> prefs = new HashSet<>();
	Set<Register> intfs = new HashSet<>();
	
	Arcs(){
		this.prefs = new HashSet<>();
		this.intfs = new HashSet<>();
	}
}

