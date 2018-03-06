package mini_c;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Interference {
	Map<Register, Arcs> graph;

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
	        			if (!this.graph.containsKey(r2)) this.graph.put(r2, new Arcs());
	        			this.graph.get(r2).intfs.add(r1);
	    			}
	    			else if (!r1.equals(r2)) {
	    				if (!this.graph.containsKey(r1)) this.graph.put(r1, new Arcs());
	        			this.graph.get(r1).intfs.add(r2);
	    			}
	    		}
	    	}
    	}
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

