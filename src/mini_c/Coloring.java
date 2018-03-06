package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

class Coloring {
	Map<Register, Operand> colors = new HashMap<>();
	int nlocals = 0; // nombre d'emplacements sur la pile
	int K;
	int current_spilled_index = 0; // FIXME: Has to represent the position of the first spilled item, if any. Multiple of 8
	Liveness uses;

	Coloring(Interference ig, Liveness uses){
		K = Register.allocatable.size();
		this.uses = uses;
		simplify(ig);
	}

	private void simplify(Interference ig) {
		Register selected = null;
		int min_degree = K;
		Iterator<Entry<Register, Arcs>> it = ig.graph.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Register, Arcs> pair = it.next();
			if (pair.getValue().prefs.isEmpty() && pair.getValue().intfs.size() < min_degree) {
				selected = pair.getKey();
				min_degree = pair.getValue().intfs.size();
			}
		}

		if(selected != null) select(ig, selected);
		else coalesce(ig);
	}

	private void coalesce(Interference ig) {
		Iterator<Entry<Register, Arcs>> it = ig.graph.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Register, Arcs> pair = it.next();
			// Double vérification, non optimal
			for (Register r : pair.getValue().prefs) {
				if (satisfiesGeorge(ig, pair.getKey(), r)) {
					// Fusion des registres
					Register r1 = ig.coalesce_register(pair.getKey(), r);
					Register r2 = (r.equals(r1) ? pair.getKey() : r);

					this.simplify(ig);

					//Assignation de la même couleur
					colors.put(r2, colors.get(r1));
					return;
				}
			}
		}
		freeze(ig);
	}

	private boolean satisfiesGeorge(Interference ig, Register r1, Register r2) {
		// We'll add the condition that there isn't any coalescing if both are physical registers
		if (Register.allocatable.contains(r2) && Register.allocatable.contains(r1)) return false;
		if (Register.allocatable.contains(r2)) {
			// r2 est un registre physique
			for (Register r3 : ig.graph.get(r1).intfs) {
				if ((!Register.allocatable.contains(r3) 
						|| ig.graph.get(r3).intfs.size() >= this.K) 
						&& !ig.graph.get(r2).intfs.contains(r3) ) return false;
			}
		}
		else {
			// r2 n'est pas un registre physique
			for (Register r3 : ig.graph.get(r1).intfs) {
				if ((Register.allocatable.contains(r3) 
						|| ig.graph.get(r3).intfs.size() >= this.K) 
						&& !ig.graph.get(r2).intfs.contains(r3) ) return false;
			}
		}
		return true;
	}

	private void freeze(Interference ig) {
		Register selected = null;
		int min_degree = K;
		Iterator<Entry<Register, Arcs>> it = ig.graph.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Register, Arcs> pair = it.next();
			if (pair.getValue().intfs.size() < min_degree) {
				selected = pair.getKey();
				min_degree = pair.getValue().intfs.size();
			}
		}

		if(selected != null) {
			for (Register r : ig.graph.get(selected).prefs) ig.graph.get(r).prefs.remove(selected);
			ig.graph.get(selected).prefs = new HashSet<Register>();
			simplify(ig);
		}
		else spill(ig);
	}

	private void spill(Interference ig) {
		if (ig.graph.isEmpty()) return;
		double min_cost = -1;
		Register candidate = null;
		for (Register r : ig.graph.keySet()) {
			double new_cost = cost(ig, r);
			if (min_cost == -1 || new_cost < min_cost) {
				min_cost = new_cost;
				candidate = r;
			}
		}
		select(ig, candidate);
	}

	private double cost(Interference ig, Register r) {
		int nb_utilizations = 0;
		int degree = 1 + ig.graph.get(r).intfs.size();
		Iterator<Entry<Label, LiveInfo>> it = uses.info.entrySet().iterator();
		while(it.hasNext()) {
			Entry<Label, LiveInfo> pair = it.next();
			if (pair.getValue().uses.contains(r)) nb_utilizations++;
		}
		return nb_utilizations / (double) degree;
	}

	private void select(Interference ig, Register r) {
		Arcs removed_node = ig.remove_register(r);
		simplify(ig);
		LinkedList<Register> possibilities = new LinkedList<Register>(Register.allocatable);
		for (Register rn : removed_node.intfs) {
			if (colors.get(rn) instanceof Reg) possibilities.remove(((Reg)colors.get(rn)).r);
		}
		if (!possibilities.isEmpty()) this.colors.put(r, new Reg(possibilities.pop()));
		else {
			this.colors.put(r, new Spilled(this.current_spilled_index));
			this.current_spilled_index += 8;
		}
	}

	void print() {
		System.out.println("coloring output:");
		for (Register r: colors.keySet()) {
			Operand o = colors.get(r);
			System.out.println("  " + r + " --> " + o);
		}
	}
}
