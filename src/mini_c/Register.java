package mini_c;

import java.util.LinkedList;
import java.util.List;

/** registre (physique ou pseudo-registres) */
public class Register {

	private static int next = 0;
	
	final String name;
	final String byteName;
	
	/** renvoie un pseudo-registre frais */ 
	Register() {
		next++;
		this.name = "#" + next;
		this.byteName = null;
	}
	
	/** s'agit-il d'un pseudo-registre ? */ 
	boolean isPseudo() {
		return this.name.charAt(0) == '#';
	}
	/** s'agit-il d'un registre physique ? */ 
	boolean isHW() {
		return !this.isPseudo();
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		Register that = (Register)o;
		return this.name.equals(that.name);
	}
	@Override
	public String toString() {
		return this.name;
	}
	
	private Register(String name) {
		this.name = name;
		this.byteName = null;
	}
	private Register(String name, String byteName) {
		this.name = name;
		this.byteName = byteName;
	}
	
	public final String byteReg() { return this.byteName; }
	
	static final Register rax = new Register("%rax", "%al");
	static final Register result = rax;
	
	static final Register rdi = new Register("%rdi");
	static final Register rsi = new Register("%rsi");
	static final Register rdx = new Register("%rdx", "%dl");
	static final Register rcx = new Register("%rcx", "%cl");
	static final Register r8  = new Register("%r8", "%r8b");
	static final Register r9  = new Register("%r9", "%r9b");

	static final List<Register> parameters = new LinkedList<Register>();
	static {
		parameters.add(rdi); parameters.add(rsi); parameters.add(rdx);
		parameters.add(rcx); parameters.add(r8); parameters.add(r9);
	}
	
	static final Register r10 = new Register("%r10", "%r10b");
	static final List<Register> caller_save = new LinkedList<Register>();
	static {	caller_save.add(rax); caller_save.add(r10); 
		for (Register r: parameters) caller_save.add(r);
	}

	static final Register rbx = new Register("%rbx", "%bl");
	static final Register r12 = new Register("%r12", "%r12b");
 	static final Register r13 = new Register("%r13", "%r13b");
	static final Register r14 = new Register("%r14", "%r14b");

	static final List<Register> callee_saved = new LinkedList<Register>();
	static {
		callee_saved.add(rbx); callee_saved.add(r12);
		callee_saved.add(r13); callee_saved.add(r14);
	}

	/** ensemble des registres participant à l'allocation de registres */
	static final List<Register> allocatable = new LinkedList<Register>();
	static {
		for (Register r: caller_save) allocatable.add(r);
		for (Register r: callee_saved) allocatable.add(r);
	}
	
	static final Register rsp = new Register("%rsp");
	static final Register rbp = new Register("%rbp");
	static final Register tmp1 = new Register("%r15");
	static final Register tmp2 = new Register("%r11");
	

	static final List<Register> hasNot8Bit = new LinkedList<Register>();
	static {
		hasNot8Bit.add(Register.rsi); hasNot8Bit.add(Register.rdi);
	}
	
}
