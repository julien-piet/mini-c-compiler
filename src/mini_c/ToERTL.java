package mini_c;

//Vérifier le bon sens des arguments quand il y a deux registres 
//Vérifier que les enregistrements caller/callee/stack sont dans le bon ordre


import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

public class ToERTL extends EmptyRTLVisitor {

	ERTLfile   file;
	ERTLfun    fun;
	ERTL	   instr; //Holds the latest translated instruction
	
	public ERTLfile translate(RTLfile f) {
		f.accept(this);
		return file;
	}
	public ERTLfun visitFun(RTLfun f) {
		f.accept(this);
		return fun;
	}
	
	public void visit(Rconst o) {
		instr = new ERconst(o.i, o.r, o.l);
	}
	
	public void visit(Rload o) {
		instr = new ERload(o.r1, o.i, o.r2, o.l);
	}
	
	public void visit(Rstore o) {
		instr = new ERstore(o.r1, o.r2, o.i, o.l);
	}
	
	public void visit(Rmunop o) {
		instr = new ERmunop(o.m, o.r, o.l);
	}
	
	public void visit(Rmbinop o) {
		if (o.m != Mbinop.Mdiv) {
			instr = new ERmbinop(o.m, o.r1, o.r2, o.l);
			return;
		}
		Label intermediate = fun.body.add(new ERmbinop(Mbinop.Mmov, Register.rax, o.r2, o.l));
		intermediate = fun.body.add(new ERmbinop(Mbinop.Mdiv, o.r1, Register.rax, intermediate));
		instr = new ERmbinop(Mbinop.Mmov, o.r2, Register.rax, intermediate);
	}
	
	public void visit(Rmubranch o) {
		instr = new ERmubranch(o.m, o.r, o.l1, o.l2);
	}
	
	public void visit(Rmbbranch o) {
		instr = new ERmbbranch(o.m, o.r1, o.r2, o.l1, o.l2);
	}
	  
	public void visit(Rcall o) {
		// First, we'll determine the number of arguments
		int nbOfArguments = o.rl.size();
		
		Label inter = o.l;
		// Remove the excess stack space
		if(nbOfArguments > 6) {
			Register offset = new Register();
			inter = fun.body.add(new ERmbinop(Mbinop.Madd, offset, Register.rsp, inter));
			inter = fun.body.add(new ERconst(8*(nbOfArguments - 6), offset, inter));
		}
		
		//Copy rax into result register
		inter = fun.body.add(new ERmbinop(Mbinop.Mmov, Register.result, o.r, inter));
		
		//Execute call
		instr = new ERcall(o.s, nbOfArguments, inter);
		
		// Push arguments
		int argIndex;
		ListIterator<Register> argIt = o.rl.listIterator(nbOfArguments);

		// Stack parameters
		for(argIndex = nbOfArguments; argIndex > Register.parameters.size(); argIndex--) {
			Register arg = argIt.previous();
			inter = fun.body.add(instr);
			instr = new ERpush_param(arg, inter);
		}
		
		// Register parameters
		ListIterator<Register> regParamsIt = Register.parameters.listIterator(argIndex);
		for(; argIndex > 0; argIndex--) {
			Register arg = argIt.previous();
			Register regParam = regParamsIt.previous();
			inter = fun.body.add(instr);
			instr = new ERmbinop(Mbinop.Mmov, arg, regParam, inter);
		}
	}
	
	public void visit(Rgoto o) {
		instr = new ERgoto(o.l);
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
		
		Label next_label = fun.body.add(new ERreturn());
		
		//Removing activation table
		next_label = fun.body.add(new ERdelete_frame(next_label));
		
		//Restoring callee-saved registers
		for(Register r : Register.callee_saved) {
			Register rr = new Register();
			callee_saved.put(r, rr);
			next_label = fun.body.add(new ERmbinop(Mbinop.Mmov, rr, r, next_label));
		}
		
		//Moving result to %rax
		fun.body.graph.put(o.exit, new ERmbinop(Mbinop.Mmov, o.result, Register.result, next_label));
		next_label = o.exit;
		
		//Translating function into ERTL
		Iterator<Map.Entry<Label, RTL>> it = o.body.graph.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Label, RTL> pair = it.next();
	        pair.getValue().accept(this);
	        fun.body.graph.put(pair.getKey(), instr);
	    }
		
		//Fetching parameters
		next_label = o.entry;
		Iterator<Register> parameter = o.formals.iterator();
		for(Register r : Register.parameters) {
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
		
		//Allocation activation table
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
