package mini_c;

import java.util.Iterator;
import java.util.Map;

public class ToLTL implements ERTLVisitor {
	
	LTLfile file;
	public LTLfile translate(ERTLfile f) {
		f.accept(this);
		return file;
	}
	

	LTLfun fun;
	LTLfun visitFun(ERTLfun f) {
		f.accept(this);
		return fun;
	}
	
	Coloring col;
	Operand toOp(Register r) {
		if (Register.allocatable.contains(r)) return new Reg(r);
		return col.colors.get(r);
	}
	
	LTL instr;
	LTL visitInstr(ERTL e) {
		e.accept(this);
		return instr;
	}
	

	@Override
	public void visit(ERconst o) {
		instr = new Lconst(o.i, toOp(o.r), o.l);
	}


	@Override
	public void visit(ERload o) {
		Label l = o.l;
		Operand o1 = toOp(o.r1), o2 = toOp(o.r2);
		Register r2;
		
		// If result is on stack, add the corresponding mov after the load
		if (o2 instanceof Spilled) {
			l = fun.body.add(new Lmbinop(Mbinop.Mmov, new Reg(Register.tmp1), o2, l));
			r2 = Register.tmp1;
		} else {
			r2 = ((Reg)o2).r;
		}

		// Then, depending on the address type, load directly, or add a mov
		if (o1 instanceof Spilled) {
			l = fun.body.add(new Lload(Register.tmp1, o.i, r2, l));
			instr = new Lmbinop(Mbinop.Mmov, o1, new Reg(Register.tmp1), l);
		} else {
			instr = new Lload(((Reg)o1).r, o.i, r2, l);
		}
	}


	@Override
	public void visit(ERstore o) {
		Operand o1 = toOp(o.r1), o2 = toOp(o.r2);
		Register r1 = (o1 instanceof Spilled) ? Register.tmp1 : ((Reg)o1).r;
		Register r2 = (o2 instanceof Spilled) ? Register.tmp2 : ((Reg)o2).r;
		
		instr = new Lstore(r1, r2, o.i, o.l);
		
		if (o2 instanceof Spilled) {
			Label l = fun.body.add(instr);
			instr = new Lmbinop(Mbinop.Mmov, o2, new Reg(Register.tmp2), l);
		}

		if (o1 instanceof Spilled) {
			Label l = fun.body.add(instr);
			instr = new Lmbinop(Mbinop.Mmov, o1, new Reg(Register.tmp1), l);
		}
	}


	@Override
	public void visit(ERmunop o) {
		instr = new Lmunop(o.m, toOp(o.r), o.l);
	}


	@Override
	public void visit(ERmbinop o) {
		Operand o1 = toOp(o.r1), o2 = toOp(o.r2);
		
		// Particular case for 'mov a a', changed into a goto (i.e. skipped)
		if (o.m == Mbinop.Mmov && o1.equals(o2)) {
			instr = new Lgoto(o.l);
		}
		
		// If we have a multiply, check second operand is register
		else if (o.m == Mbinop.Mmul && o2 instanceof Spilled ) {
			instr = new Lmbinop(Mbinop.Mmov, Reg.tmp1, o2, o.l);
			instr = new Lmbinop(o.m, o1, Reg.tmp1, fun.body.add(instr));
			instr = new Lmbinop(Mbinop.Mmov, o2, Reg.tmp1, fun.body.add(instr));
		}
		
		// Two operand on stack is not authorized. Place first operand in register and add a mov
		else if (o1 instanceof Spilled && o2 instanceof Spilled) {
			instr = new Lmbinop(o.m, Reg.tmp1, o2, o.l);
			instr = new Lmbinop(Mbinop.Mmov, o1, Reg.tmp1, fun.body.add(instr));
		}
		
		// Otherwise, keep the same
		else {
			instr = new Lmbinop(o.m, o1, o2, o.l);
		}
	}


	@Override
	public void visit(ERmubranch o) {
		instr = new Lmubranch(o.m, toOp(o.r), o.l1, o.l2);
	}


	@Override
	public void visit(ERmbbranch o) {
		instr = new Lmbbranch(o.m, toOp(o.r1), toOp(o.r2), o.l1, o.l2);
	}


	@Override
	public void visit(ERgoto o) {
		instr = new Lgoto(o.l);
	}


	@Override
	public void visit(ERcall o) {
		instr = new Lcall(o.s, o.l);
	}


	@Override
	public void visit(ERalloc_frame o) {
		Label l = o.l;
		if (col.nlocals > 0) {
			l = fun.body.add(new Lmunop(new Maddi(-col.nlocals*8), Reg.rsp, o.l));
		}
		instr = new Lmbinop(Mbinop.Mmov, Reg.rsp, Reg.rbp, l);
		instr = new Lpush(Reg.rbp, fun.body.add(instr));
	}


	@Override
	public void visit(ERdelete_frame o) {
		instr = new Lpop(Register.rbp, o.l);
		if (col.nlocals > 0) {
			instr = new Lmunop(new Maddi(col.nlocals*8), Reg.rsp, fun.body.add(instr));
		}
	}


	@Override
	public void visit(ERget_param o) {
		Label l = o.l;
		Operand or = toOp(o.r);
		Register r;
		
		// If result is on stack, add the corresponding mov after the load
		if(or instanceof Spilled) {
			l = fun.body.add(new Lmbinop(Mbinop.Mmov, new Reg(Register.tmp1), or, l));
			r = Register.tmp1;
		} else {
			r = ((Reg)or).r;
		}
		// Then load the param from the stack (add 16 to address because of ret address, and former rbp)
		instr = new Lload(Register.rbp, -(o.i+16), r, l);
	}


	@Override
	public void visit(ERpush_param o) {
		instr = new Lpush(new Reg(o.r), o.l);
	}


	@Override
	public void visit(ERreturn o) {
		instr = new Lreturn();
	}


	@Override
	public void visit(ERTLfun o) {
		fun = new LTLfun(o.name);
		fun.entry = o.entry;
		
		Liveness live = new Liveness(o.body);
		Interference inter = new Interference(live);
		col = new Coloring(inter, live);
		
		fun.body = new LTLgraph();
		Iterator<Map.Entry<Label, ERTL>> it = o.body.graph.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Label, ERTL> pair = it.next();
	        fun.body.graph.put(pair.getKey(), visitInstr(pair.getValue()));
	    }
	}
	

	@Override
	public void visit(ERTLfile n) {
		file = new LTLfile();
		for(ERTLfun f: n.funs) {
			file.funs.add(visitFun(f));
		}
	}

}
