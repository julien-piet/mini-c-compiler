package mini_c;

import java.util.Set;

public class LiveInfo {
    ERTL instr;
	Label[] succ;   // successeurs
	Set<Label> pred;   // prédécesseurs
	Set<Register> defs;   // définitions
	Set<Register> uses;   // utilisations
	Set<Register> ins;    // variables vivantes en entrée
	Set<Register> outs;   // variables vivantes en sortie
	
	public String toString() {
		
		String rtn = "d={";
		for (Register reg : this.defs) {
			if (rtn.charAt(rtn.length()-1) != '{') rtn += ", ";
			rtn += reg.name;
		}
		
		rtn += "} u={";
		for (Register reg : this.uses) {
			if (rtn.charAt(rtn.length()-1) != '{') rtn += ", ";
			rtn += reg.name;
		}
		
		rtn += "} i={";
		for (Register reg : this.ins) {
			if (rtn.charAt(rtn.length()-1) != '{') rtn += ", ";
			rtn += reg.name;
		}
		
		rtn += "} o={";
		for (Register reg : this.outs) {
			if (rtn.charAt(rtn.length()-1) != '{') rtn += ", ";
			rtn += reg.name;
		}
		
		return rtn + "}";
	}
}