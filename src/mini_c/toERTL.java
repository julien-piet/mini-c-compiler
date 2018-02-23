package mini_c;

//Vérifier le bon sens des arguments quand il y a deux registres 
//Vérifier que les enregistrements caller/callee/stack sont dans le bon ordre


import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

public class toERTL extends EmptyRTLVisitor {

	ERTLfile   file;
	ERTLfun    fun;
	Label 	   next_label;
	ERTL	 	   instr; //Holds the latest translated instruction
	
	Label 	   current_exit; //Holds the current exit label for the function, to enable visitor to change on the go
	//This function replaces the label by next_label if the old label used to be the exit
	public Label update_exit(Label input) {
		return input == current_exit ? next_label: input;
	}
	
	public ERTLfile translate(RTLfile f) {
		f.accept(this);
		return file;
	}
	public ERTLfun visitFun(RTLfun f) {
		f.accept(this);
		return fun;
	}
	
	public void visit(Rconst o) {
		instr = new ERconst(o.i, o.r, update_exit(o.l));
	}
	
	public void visit(Rload o) {
		instr = new ERload(o.r1, o.i, o.r2, update_exit(o.l));
	}
	
	public void visit(Rstore o) {
		instr = new ERstore(o.r1, o.r2, o.i, update_exit(o.l));
	}
	
	public void visit(Rmunop o) {
		instr = new ERmunop(o.m, o.r, update_exit(o.l));
	}
	
	public void visit(Rmbinop o) {
		if (o.m != Mbinop.Mdiv) {
			instr = new ERmbinop(o.m, o.r1, o.r2, update_exit(o.l));
			return;
		}
		Label intermediate = fun.body.add(new ERmbinop(Mbinop.Mmov, Register.rax, o.r2, update_exit(o.l)));
		intermediate = fun.body.add(new ERmbinop(Mbinop.Mdiv, o.r1, Register.rax, intermediate));
		instr = new ERmbinop(Mbinop.Mmov, o.r2, Register.rax, intermediate);
	}
	
	public void visit(Rmubranch o) {
		instr = new ERmubranch(o.m, o.r, update_exit(o.l1), update_exit(o.l2));
	}
	
	public void visit(Rmbbranch o) {
		instr = new ERmbbranch(o.m, o.r1, o.r2, update_exit(o.l1), update_exit(o.l2));
	}
	  
	public void visit(Rcall o) {
		// First, we'll determine the number of arguments
		int nbOfArguments = o.rl.size();
		
		Label inter = update_exit(o.l);
		// Remove the excess stack space
		if(nbOfArguments > 6) {
			Register offset = new Register();
			inter = fun.body.add(new ERmbinop(Mbinop.Madd, offset, Register.rsp, inter));
			inter = fun.body.add(new ERconst(8*(nbOfArguments - 6), offset, inter));
		}
		
		//Copy rax into result register
		inter = fun.body.add(new ERmbinop(Mbinop.Mmov, Register.rax, o.r, inter));
		
		//Execute call
		if(nbOfArguments > 0) inter = fun.body.add(new ERcall(o.s, nbOfArguments, inter));
		else instr = new ERcall(o.s, nbOfArguments, inter);
		
		//Copy excess parameters into caller-saved register
		ListIterator<Register> it = o.rl.listIterator(nbOfArguments);
		int index = nbOfArguments;
		while(index-- > 6) inter = fun.body.add(new ERpush_param(it.previous(), inter));
		
		//Copy first parameters into caller-saved registers
		ListIterator<Register> caller = Register.caller_save.listIterator(index);
		while(index-- > 1) inter = fun.body.add(new ERmbinop(Mbinop.Mmov, it.previous(), caller.previous(), inter));
		if(index == 1) instr = new ERmbinop(Mbinop.Mmov, it.previous(), caller.previous(), inter);
		
	}
	
	public void visit(Rgoto o) {
		instr = new ERgoto(update_exit(o.l));
	}
	
	public void visit(RTLfun o) {
		//Set up function
		int nbOfFormals = o.formals.size();
		fun = new ERTLfun(o.name, nbOfFormals);
		for (Register local : o.locals) {
			fun.locals.add(local);
		}
		fun.body = new ERTLgraph();
		
		//Creating a hashmap to associate callee-saved registers and another to associate parameters
		HashMap<Register, Register> callee_saved = new HashMap<>();
		
		//Removing activation table
		next_label = fun.body.add(new ERdelete_frame(o.exit));
		
		//Restoring callee-saved registers
		for(Register r : Register.callee_saved) {
			Register rr = new Register();
			callee_saved.put(r, rr);
			fun.locals.add(rr);
			next_label = fun.body.add(new ERmbinop(Mbinop.Mmov, rr, r, next_label));
		}
		
		//Moving result to %rax
		next_label = fun.body.add(new ERmbinop(Mbinop.Mmov, o.result, Register.rax, next_label));
		
		//Translating function into ERTL
		Iterator<Map.Entry<Label, RTL>> it = o.body.graph.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Label, RTL> pair = it.next();
	        pair.getValue().accept(this);
	        fun.body.graph.put(pair.getKey(), instr);
	        it.remove(); // avoids a ConcurrentModificationException
	    }
		
		//Fetching parameters
		next_label = o.entry;
		Iterator<Register> parameter = o.formals.iterator();
		for(Register r : Register.caller_save) {
			if(parameter.hasNext()) {
				next_label = fun.body.add(new ERmbinop(Mbinop.Mmov, r, parameter.next(), next_label));
			}
			else break;
		}
		if(parameter.hasNext()) {
			int location = nbOfFormals - 5;
			while(parameter.hasNext()) {
				next_label = fun.body.add(new ERget_param(8*(location), parameter.next(), next_label));
				location--;
			}
		}
		
		//Saving callee-saved registers
		for(Register r : Register.callee_saved) {
			next_label = fun.body.add(new ERmbinop(Mbinop.Mmov, r, callee_saved.get(r), next_label));
		}
		
		//Allocation actiavtion table
		next_label = fun.body.add(new ERalloc_frame(next_label));
		fun.entry = next_label;
		
	}
	
	public void visit(RTLfile o) {
		  file = new ERTLfile();
		  for (RTLfun fun : o.funs) {
			  file.funs.add(this.visitFun(fun));
		  }
	}
	
	
	
}
