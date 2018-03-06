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
	
	Map<Register, Operand> regMap;
	Operand toOp(Register r) {
		return regMap.get(r);
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
		instr = new Lload(o.r1, o.i, o.r2, o.l);
	}


	@Override
	public void visit(ERstore o) {
		instr = new Lstore(o.r1, o.r2, o.i, o.l);
	}


	@Override
	public void visit(ERmunop o) {
		instr = new Lmunop(o.m, toOp(o.r), o.l);
	}


	@Override
	public void visit(ERmbinop o) {
		instr = new Lmbinop(o.m, toOp(o.r1), toOp(o.r2), o.l);
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
		// TODO Auto-generated method stub
		
	}


	@Override
	public void visit(ERalloc_frame o) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void visit(ERdelete_frame o) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void visit(ERget_param o) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void visit(ERpush_param o) {
		// TODO Auto-generated method stub
		
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
		Coloring col = new Coloring(inter);
		regMap = col.colors;
		
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
