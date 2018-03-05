package mini_c;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Liveness {

	  Map<Label, LiveInfo> info;

	  Liveness(ERTLgraph g) { 
		  this.init(g);
		  this.precedence();
		  this.kildall();
	  }
	  
	  private void init(ERTLgraph g) {
		  // Initialize well-defined fields of info
		  
		  	Iterator<Entry<Label, ERTL>> it = g.graph.entrySet().iterator();
		    while (it.hasNext()) {
		        Entry<Label, ERTL> pair = it.next();
		        
		        LiveInfo new_item = new LiveInfo();
		        new_item.defs = pair.getValue().def();
		        new_item.succ = pair.getValue().succ();
		        new_item.uses = pair.getValue().use();
		        
		        new_item.pred = new HashSet<Label>();
		        new_item.outs = new HashSet<Register>();
		        new_item.ins = new HashSet<Register>();
		        
		        info.put(pair.getKey(), new_item);
		        it.remove(); // avoids a ConcurrentModificationException
		    }
	  }
	  
	  private void precedence() {
		  // Determine precedence in info
		  
		  Iterator<Entry<Label, LiveInfo>> it = this.info.entrySet().iterator();
		    while (it.hasNext()) {
		        Entry<Label, LiveInfo> pair = it.next();
		        for (Label lb : pair.getValue().succ) {
		        	this.info.get(lb).pred.add(pair.getKey());
		        }
		        it.remove(); // avoids a ConcurrentModificationException
		    }
	  }
	  
	  private void kildall() {
		  // Apply Kildall algorithm
		  
		  LinkedList<Label> WS = new LinkedList<Label>(this.info.keySet());
		  while (!WS.isEmpty()) {
			  Label l = WS.poll();
			  this.info.get(l).outs = out(l);
			  Set<Register> in = in(l);
			  if (!in.equals(this.info.get(l).ins)) {
				  for (Label lb : this.info.get(l).pred)
					  if (!WS.contains(lb)) WS.add(lb);
			  }
			  this.info.get(l).ins = in;
		  }
	  }
	  
	  private Set<Register> in(Label l){
		  
		  HashSet<Register> use = new HashSet<Register>(this.info.get(l).uses);
		  HashSet<Register> out = new HashSet<Register>(this.info.get(l).outs);
		  out.removeAll(this.info.get(l).defs);
		  use.addAll(out);
		  return use;
	  }
	 
	  private Set<Register> out(Label l){
		  
		  HashSet<Register> rtn = new HashSet<Register>();
		  for (Label lb : this.info.get(l).succ) {
			  rtn.addAll(this.info.get(lb).ins);
		  }
		  return rtn;
	  }
	  
	  private void print(Set<Label> visited, Label l) {
			if (visited.contains(l)) return;
			visited.add(l);
			LiveInfo li = this.info.get(l);
			System.out.println("  " + String.format("%3s", l) + ": " + li);
			for (Label s: li.succ) print(visited, s);
	  }

	  public void print(Label entry) {
		  print(new HashSet<Label>(), entry);
	  }
	  
	}