package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class ToRTL extends EmptyVisitor {

	RTLfile   file;
	RTLfun    fun;
	Register  rd;
	Label     ret_label;
	
	public RTLfile translate(File f) {
		f.accept(this);
		return file;
	}
	public RTLfun visitFun(Decl_fun f) {
		f.accept(this);
		return fun;
	}
	public Label visitStmt(Stmt s, Label l) {
        Label ret_save = ret_label;

		ret_label = l;
		s.accept(this);
        Label r = ret_label;

        ret_label = ret_save;
		return r;
	}
	public Label visitExpr(Expr e, Register r, Label l) {
        Label ret_save = ret_label;
        Register rd_save = rd;
        
		ret_label = l;
		rd = r;
		e.accept(this);
        Label rl = ret_label;

        ret_label = ret_save;
        rd = rd_save;
		return rl;
	}
	
	public Label visitCond(Expr e, Label success, Label failure) {
		if (e instanceof Eunop) {
			Eunop unopExpr = (Eunop) e;
			
			if (unopExpr.u == Unop.Unot) {
				return visitCond(unopExpr.e, failure, success);	
			}
		}
		
		else if (e instanceof Ebinop) {
			Ebinop be = (Ebinop) e;
			
			if (Binop.Compop.contains(be.b)) {
				if (be.e1 instanceof Econst || be.e2 instanceof Econst) {
					boolean invert = (be.e1 instanceof Econst);
					Mubranch op = null;
					switch(be.b) {
					case Beq:
						op = invert ? new Mjei(((Econst)be.e1).i) : new Mjei(((Econst)be.e2).i);
						break;
					case Bneq:
						op = invert ? new Mjnei(((Econst)be.e1).i) : new Mjnei(((Econst)be.e2).i);
						break;
					case Bge:
						op = invert ? new Mjli(((Econst)be.e1).i) : new Mjgei(((Econst)be.e2).i);
						break;
					case Bgt:
						op = invert ? new Mjlei(((Econst)be.e1).i) : new Mjgi(((Econst)be.e2).i);
						break;
					case Ble:
						op = invert ? new Mjgi(((Econst)be.e1).i) : new Mjlei(((Econst)be.e2).i);
						break;
					case Blt:
						op = invert ? new Mjgei(((Econst)be.e1).i) : new Mjli(((Econst)be.e2).i);
						break;
					default:
						break;
					}
					Register r = new Register();
					ret_label = fun.body.add(new Rmubranch(op, r, success, failure));
					ret_label = invert ? visitExpr(be.e2, r, ret_label) : visitExpr(be.e1, r, ret_label);
					return ret_label;
				}
				else {
					Mbbranch op = null;
					switch(be.b) {
					case Beq:
						op = Mbbranch.Mje;
						break;
					case Bneq:
						op = Mbbranch.Mjne;
						break;
					case Bge:
						op = Mbbranch.Mjge;
						break;
					case Bgt:
						op = Mbbranch.Mjg;
						break;
					case Ble:
						op = Mbbranch.Mjle;
						break;
					case Blt:
						op = Mbbranch.Mjl;
						break;
					default:
						break;
					}
					Register r1 = new Register();
					Register r2 = new Register();
					ret_label = fun.body.add(new Rmbbranch(op, r1, r2, success, failure));
					ret_label = visitExpr(be.e1, r2, ret_label);
					ret_label = visitExpr(be.e2, r1, ret_label);
					return ret_label;
				}
			}
		}
		
		Register r = new Register();
		Label branch_label = fun.body.add(new Rmubranch(new Mjnz(), r, success, failure));
		return visitExpr(e, r, branch_label);
	}
	
	LinkedList<HashMap<String, Register>> locals = new LinkedList<>();
	
	public Register getRegister(String var) {
		// Fetch a register by its associated local variable name
		Register rtn = null;
		for(HashMap<String, Register> set : locals) {
			rtn = set.get(var);
			if (rtn != null) break;
		}
		return rtn;
	}

	@Override
	public void visit(Econst n) {
		ret_label = fun.body.add(new Rconst(n.i, rd, ret_label));
	}

	@Override
	public void visit(Eaccess_local n) {
		Register r = getRegister(n.i);
        ret_label = fun.body.add(new Rmbinop(Mbinop.Mmov, r, rd, ret_label));
	}

	@Override
	public void visit(Eassign_local n) {
		Register r = getRegister(n.i);
        ret_label = fun.body.add(new Rmbinop(Mbinop.Mmov, rd, r, ret_label));
		ret_label = visitExpr(n.e, rd, ret_label);
	}

	@Override
	public void visit(Eaccess_field n) {
		Register r = new Register();
		ret_label = fun.body.add(new Rload(r, n.f.pos, rd, ret_label));
		ret_label = visitExpr(n.e, r, ret_label);
	}

	@Override
	public void visit(Eassign_field n) {
		Register r = new Register();
		ret_label = fun.body.add(new Rstore(rd, r, n.f.pos, ret_label));
		ret_label = visitExpr(n.e1, r, ret_label);
		ret_label = visitExpr(n.e2, rd, ret_label);
	}

	@Override
	public void visit(Eunop n) {
		switch(n.u) {
			case Unot:
				ret_label = fun.body.add(new Rmunop(new Msetei(0), rd, ret_label));
				ret_label = visitExpr(n.e, rd, ret_label);
				break;
			case Uneg:
				Register r = new Register();
				ret_label = fun.body.add(new Rmbinop(Mbinop.Msub, r, rd, ret_label));
				ret_label = fun.body.add(new Rconst(0, rd, ret_label));
				ret_label = visitExpr(n.e, r, ret_label);
				break;
		}

	}

	@Override
	public void visit(Ebinop n) {
		if (n.b == Binop.Band || n.b == Binop.Bor) {
			Mubranch brtype = (n.b == Binop.Band) ? new Mjnz() : new Mjz();
				
			ret_label = fun.body.add(new Rmunop(new Msetnei(0), rd, ret_label));
			Label le2 = visitExpr(n.e2, rd, ret_label);
			Label lazychoice = fun.body.add(new Rmubranch(brtype, rd, le2, ret_label));
			Label le1 = visitExpr(n.e1, rd, lazychoice);
			ret_label = le1;
			return;
		}

		Register r = new Register();
		Mbinop op;
		switch(n.b) {
			case Badd:
				if (n.e1 instanceof Econst) {
					ret_label = fun.body.add(new Rmunop(new Maddi(((Econst) n.e1).i), rd, ret_label));
					ret_label = visitExpr(n.e2, rd, ret_label);
					return;
				}
				if (n.e2 instanceof Econst) {
					ret_label = fun.body.add(new Rmunop(new Maddi(((Econst) n.e2).i), rd, ret_label));
					ret_label = visitExpr(n.e1, rd, ret_label);
					return;
				}
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
		ret_label = visitExpr(n.e2, r, ret_label);
		ret_label = visitExpr(n.e1, rd, ret_label);
	}

	@Override
	public void visit(Ecall n) {
		LinkedList<Register> RTLparams = new LinkedList<>();
		ret_label = fun.body.add(new Rcall(rd, n.i, RTLparams, ret_label));
		for (Expr i : n.el) {
			Register r = new Register();
			RTLparams.add(r);
			ret_label = visitExpr(i, r, ret_label);
		}
	}

	@Override
	public void visit(Esizeof n) {
		ret_label = fun.body.add(new Rconst(n.s.size, rd, ret_label));
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
		
		ret_label = visitCond(n.e, ifl, ell);
	}

	@Override
	public void visit(Swhile n) {
        // -------------\/
        //     in --> branch --> out
        //     /\-------\/
		
		Label out = ret_label;
		Label in = new Label();
		Label branch = visitCond(n.e, in, out);
		
		Label in2 = visitStmt(n.s, branch);
		fun.body.graph.put(in, new Rgoto(in2));
		
		ret_label = branch;
	}

	@Override
	public void visit(Sblock n) {
        // Add locals 
		locals.addFirst(new HashMap<String, Register>());
        for (Decl_var d: n.dl) {
            Register vr = new Register();
            locals.getFirst().put(d.name, vr);
            fun.locals.add(vr);
        }

        // Treat statements
		ListIterator<Stmt> it = n.sl.listIterator(n.sl.size());
		while (it.hasPrevious()) {
			Stmt s = it.previous();
			ret_label = visitStmt(s, ret_label);
		}

        // Remove locals
        locals.removeFirst();
	}

	@Override
	public void visit(Sreturn n) {
		Label ret = fun.body.add(new Rgoto(fun.exit));
		ret_label = visitExpr(n.e, fun.result, ret);
	}

	@Override
	public void visit(Decl_fun n) {
		// Don't compile function declarations, only definitions
		if (n.fun_body == null) {
			fun = null;
			return;
		}
		
		fun = new RTLfun(n.fun_name);
		fun.body = new RTLgraph();
		fun.exit = new Label();
		fun.result = new Register();
		
		// Add formal parameters
		locals.addFirst(new HashMap<String, Register>());
		
		for (Decl_var formal : n.fun_formals) {
			Register form = new Register();
			locals.getFirst().put(formal.name, form);
			fun.formals.add(form);
		}
		
		// Treat function body 
		fun.entry = visitStmt(n.fun_body, fun.exit);
		
		// Remove parameters
		locals.removeFirst();
	}

	@Override
	public void visit(File n) {
		file = new RTLfile();
		for(Decl_fun f: n.funs) {
			RTLfun rtlf = this.visitFun(f);
			if (rtlf != null) {
				file.funs.add(rtlf);
			}
		}
	}

}
