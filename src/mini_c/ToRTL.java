package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class ToRTL extends EmptyVisitor {

	RTLfile   file;
	RTLfun    fun;
	Register  rd;
	Label     ret_label;
	boolean   in_cond;
	
	public RTLfile translate(File f) {
		f.accept(this);
		return file;
	}
	public RTLfun visitFun(Decl_fun f) {
		f.accept(this);
		return fun;
	}
	public Label visitStmt(Stmt s, Label l) {
		ret_label = l;
		s.accept(this);
		return ret_label;
	}
	public Label visitExpr(Expr e, Register r, Label l) {
		in_cond = false;
		ret_label = l;
		rd = r;
		e.accept(this);
		return ret_label;
	}
	public Label visitCond(Expr e, Register r, Label l) {
		in_cond = true;
		ret_label = l;
		rd = r;
		e.accept(this);
		return ret_label;
	}
	
	LinkedList<HashMap<String, Register>> locals = new LinkedList<>();
	
	public Register getRegsiter(String var) {
		// Fetch a register by its associated local variable name
		Register rtn = null;
		for(HashMap<String, Register> set : locals) {
			rtn = set.get(var);
			if (rtn != null) break;
		}
		return rtn;
	}

	@Override
	public void visit(Structure n) {
		// Nothing to do
		
	}

	@Override
	public void visit(Field n) {
		// Nothing to do
		
	}

	@Override
	public void visit(Decl_var n) {
		// Nothing to do ???
		
	}

	@Override
	public void visit(Econst n) {
		ret_label = fun.body.add(new Rconst(n.i, rd, ret_label));
	}

	@Override
	public void visit(Eaccess_local n) {
		Register r = getRegsiter(n.i);
		ret_label = fun.body.add(new Rload(r, 0, rd, ret_label));	
	}

	@Override
	public void visit(Eaccess_field n) {
		Register r = new Register();
		ret_label = fun.body.add(new Rload(r, 0, rd, ret_label));
		ret_label = visitExpr(n.e, r, ret_label);
	}

	@Override
	public void visit(Eassign_local n) {
		Register r1 = getRegsiter(n.i);
		Register r2 = new Register();
		ret_label = fun.body.add(new Rstore(r2, r1, 0, ret_label));
		ret_label = visitExpr(n.e, r2, ret_label);
	}

	@Override
	public void visit(Eassign_field n) {
		Register r1 = new Register();
		Register r2 = new Register();
		ret_label = fun.body.add(new Rstore(r1, r2, 0, ret_label));
		ret_label = visitExpr(n.e1, r2, ret_label);
		ret_label = visitExpr(n.e2, r1, ret_label);
	}

	@Override
	public void visit(Eunop n) {
		switch(n.u) {
			case Unot:
				ret_label = fun.body.add(new Rmunop(new Msetnei(0), rd, ret_label));
				break;
			case Uneg:
				Register r = new Register();
				ret_label = fun.body.add(new Rmbinop(Mbinop.Msub, r, rd, ret_label));
				ret_label = fun.body.add(new Rconst(0, r, ret_label));
				break;
		}
		ret_label = visitExpr(n.e, rd, ret_label);

	}

	@Override
	public void visit(Ebinop n) {
		Register r = new Register();
		Mbinop op;
		switch(n.b) {
			case Badd:
				op = Mbinop.Madd;
				break;
			case Bsub:
				op = Mbinop.Msub;
				break;
			case Bmul:
				op = Mbinop.Mmul;
				break;
			case Bdiv:
				op = Mbinop.Mdiv;
				break;
			//case Bor:
			//case Band:
			case Bneq:
				op = Mbinop.Msetne;
				break;
			case Blt:
				op = Mbinop.Msetl;
				break;
			case Ble:
				op = Mbinop.Msetle;
				break;
			case Bgt:
				op = Mbinop.Msetg;
				break;
			case Bge:
				op = Mbinop.Msetge;
				break;
			case Beq:
				op = Mbinop.Msete;
				break;
			default:
				return;
		}
		
		ret_label = fun.body.add(new Rmbinop(op, r, rd, ret_label));
		ret_label = visitExpr(n.e1, r, ret_label);
		ret_label = visitExpr(n.e2, rd, ret_label);
	}

	@Override
	public void visit(Ecall n) {
		LinkedList<Register> RTLparams = new LinkedList<>();
		ret_label = fun.body.add(new Rcall(rd, n.i, RTLparams, ret_label));
		for ( Expr i : n.el) {
			Register r = new Register();
			ret_label = visitExpr(i, r, ret_label);
		}
		
	}

	@Override
	public void visit(Esizeof n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Sskip n) {
		// Nothing to do
	}

	@Override
	public void visit(Sexpr n) {
		Register r = new Register();
		ret_label = visitExpr(n.e, r, ret_label);
	}

	@Override
	public void visit(Sif n) {
		// dest : destination label after structure
		// ifl : destination label if condition is true
		// ell : destination label if condition is false
		
		Label dest = ret_label;
		Label ell = visitStmt(n.s2, dest);
		Label ifl = visitStmt(n.s1, dest);
		
		Register condR = new Register();
		Label choose = fun.body.add(new Rmubranch(new Mjz(), condR, ifl, ell));
		Label cond = visitCond(n.e, condR, choose);
		
		ret_label = cond;
	}

	@Override
	public void visit(Swhile n) {
		Label out = ret_label;
		Label last_goto = fun.body.add(null);
		
		Label in = visitStmt(n.s, last_goto);
		Register condR = new Register();
		Label choose = fun.body.add(new Rmubranch(new Mjz(), condR, in, out));
		Label cond = visitCond(n.e, condR, choose);
		
		fun.body.graph.put(last_goto, new Rgoto(cond));
		
		ret_label = cond;
	}

	@Override
	public void visit(Sblock n) {
		ListIterator<Stmt> it = n.sl.listIterator(n.sl.size());
		locals.addFirst(new HashMap<String, Register>());
		while (it.hasPrevious()) {
			Stmt s = it.previous();
			ret_label = visitStmt(s, ret_label);
		}
		locals.removeFirst();
	}

	@Override
	public void visit(Sreturn n) {
		Label ret = fun.body.add(new Rgoto(fun.exit));
		ret_label = visitExpr(n.e, fun.result, ret);
	}

	@Override
	public void visit(Decl_fun n) {
		fun = new RTLfun(n.fun_name);
		fun.body = new RTLgraph();
		fun.exit = new Label();
		fun.result = new Register();
		
		// Ajout des parametres formels
		locals.addFirst(new HashMap<String, Register>());
		
		for (Decl_var formal : n.fun_formals) {
			Register form = new Register();
			locals.getFirst().put(formal.name, form);
			fun.formals.add(form);
		}
		
		// Traitement du corps
		fun.entry = visitStmt(n.fun_body, fun.exit);
		
		// Retrait des variables locales
		locals.removeFirst();
	}

	@Override
	public void visit(File n) {
		file = new RTLfile();
		for(Decl_fun f: n.funs) {
			file.funs.add(this.visitFun(f));
		}
	}

}
