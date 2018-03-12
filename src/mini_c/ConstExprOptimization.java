package mini_c;

import java.util.ListIterator;

public class ConstExprOptimization implements Visitor {

	static void optimize(File f) {
		(new ConstExprOptimization()).visit(f);
	}
	
	@Override
	public void visit(Unop n) {}
	@Override
	public void visit(Binop n) {}
	@Override
	public void visit(String n) {}
	@Override
	public void visit(Tint n) {}
	@Override
	public void visit(Tstructp n) {}
	@Override
	public void visit(Tvoidstar n) {}
	@Override
	public void visit(Ttypenull n) {}
	@Override
	public void visit(Structure n) {}
	@Override
	public void visit(Field n) {}
	@Override
	public void visit(Decl_var n) {}

	
	Expr expr;
	Expr visitExpr(Expr e) {
		e.accept(this);
		return expr;
	}
	
	Stmt stmt;
	Stmt visitStmt(Stmt s) {
		s.accept(this);
		return stmt;
	}
	
	

	@Override
	public void visit(Econst n) {
		expr = n;
	}

	@Override
	public void visit(Eaccess_local n) {
		expr = n;
	}

	@Override
	public void visit(Eaccess_field n) {
		n.e = visitExpr(n.e);
		expr = n;
	}

	@Override
	public void visit(Eassign_local n) {
		n.e = visitExpr(n.e);
		expr = n;
	}

	@Override
	public void visit(Eassign_field n) {
		n.e1 = visitExpr(n.e1);
		n.e2 = visitExpr(n.e2);
		expr = n;
	}

	@Override
	public void visit(Eunop n) {
		n.e = visitExpr(n.e);
		expr = n;
		
		if (n.e instanceof Econst) {
			int i = ((Econst)n.e).i;
			boolean r;
			
			switch (n.u) {
				case Uneg:
					expr = new Econst(-i);
					break;
					
				case Unot:
					r = (i != 0);
					expr = new Econst(r ? 0 : 1);
					break;
					
				default:
					break;
			}
		}
	}

	@Override
	public void visit(Ebinop n) {
		n.e1 = visitExpr(n.e1);
		n.e2 = visitExpr(n.e2);
		expr = n;
		
		if (n.e1 instanceof Econst && n.e2 instanceof Econst) {
			int n1 = ((Econst)n.e1).i;
			int n2 = ((Econst)n.e2).i;
			boolean r;
			
			switch (n.b) {
				case Badd:
					expr = new Econst(n1 + n2);
					break;
				case Bsub:
					expr = new Econst(n1 - n2);
					break;
				case Bmul:
					expr = new Econst(n1 * n2);
					break;
				case Bdiv:
					if (n2 != 0) {
						expr = new Econst(n1 / n2);
					}
					break;
					
				case Band:
					r = (n1 != 0) && (n2 != 0);
					expr = new Econst(r ? 1 : 0);
					break;
				case Bor:
					r = (n1 != 0) || (n2 != 0);
					expr = new Econst(r ? 1 : 0);
					break;
					
				case Beq:
					r = (n1 == n2);
					expr = new Econst(r ? 1 : 0);
					break;
				case Bneq:
					r = (n1 != n2);
					expr = new Econst(r ? 1 : 0);
					break;
				case Bge:
					r = (n1 >= n2);
					expr = new Econst(r ? 1 : 0);
					break;
				case Bgt:
					r = (n1 > n2);
					expr = new Econst(r ? 1 : 0);
					break;
				case Ble:
					r = (n1 <= n2);
					expr = new Econst(r ? 1 : 0);
					break;
				case Blt:
					r = (n1 < n2);
					expr = new Econst(r ? 1 : 0);
					break;
				default:
					break;
			}
		}
	}

	@Override
	public void visit(Ecall n) {
		for (final ListIterator<Expr> it = n.el.listIterator(); it.hasNext();) {
			final Expr e = it.next();
			it.set(visitExpr(e));
		}
		expr = n;
	}

	@Override
	public void visit(Esizeof n) {
		expr = new Econst(n.s.size);
	}

	
	
	@Override
	public void visit(Sskip n) {
		stmt = n;
	}

	@Override
	public void visit(Sexpr n) {
		n.e = visitExpr(n.e);
		stmt = n;
	}

	@Override
	public void visit(Sif n) {
		n.e = visitExpr(n.e);
		n.s1 = visitStmt(n.s1);
		n.s2 = visitStmt(n.s2);
		stmt = n;
		
		if (n.e instanceof Econst) {
			int i = ((Econst)n.e).i;
			stmt = (i != 0) ? n.s1 : n.s2;
		}
	}

	@Override
	public void visit(Swhile n) {
		n.e = visitExpr(n.e);
		n.s = visitStmt(n.s);
		stmt = n;
		
		if (n.e instanceof Econst) {
			int i = ((Econst) n.e).i;
			if (i == 0) { stmt = new Sskip(); }
		}
	}

	@Override
	public void visit(Sblock n) {
		for (final ListIterator<Stmt> it = n.sl.listIterator(); it.hasNext();) {
			final Stmt s = it.next();
			it.set(visitStmt(s));
		}
		stmt = n;
	}

	@Override
	public void visit(Sreturn n) {
		n.e = visitExpr(n.e);
		stmt = n;
	}

	@Override
	public void visit(Decl_fun n) {
		if (n.fun_body != null) {
			n.fun_body = visitStmt(n.fun_body);
		}
	}

	@Override
	public void visit(File n) {
		for(Decl_fun f: n.funs) {
			f.accept(this);
		}
	}

}
