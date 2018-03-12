package mini_c;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;

public class Typing implements Pvisitor {

	private File file;
	private LinkedList<Sblock> block_stack = new LinkedList<Sblock>();
	private HashMap<String, Structure> structs = new HashMap<String, Structure>();
	
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	Decl_fun getFunction(String name) {
		for (Decl_fun d: file.funs) {
			if (d.fun_name == name) {
				return d;
			}
		}
		return null;
	}
	Structure getStruct(String name) {
		return structs.get(name);
	}
	Decl_var getLocal(String name) {
		for (Sblock bloc: block_stack) {
			for (Decl_var d: bloc.dl) {
				if (d.name == name) {
					return d;
				}
			}
		}
		return null;
	}
	
	
	private Expr last_expr;
	private Stmt last_stmt;
	private Typ last_type;
	private Typ ret_type;
	
	Expr visitExpr(Pexpr e) {
		e.accept(this);
		return last_expr;
	}
	Stmt visitStmt(Pstmt s) {
		s.accept(this);
		return last_stmt;
	}
	Typ visitType(Ptype t) {
		t.accept(this);
		return last_type;
	}
	
	
	
	@Override
	public void visit(Pfile n) {
        file = new File(new LinkedList<Decl_fun>());
        
        // Define putchar
        LinkedList<Decl_var> putcharArgs = new LinkedList<>();
        putcharArgs.add(new Decl_var(new Tint(), "c"));
        Decl_fun putchar = new Decl_fun(new Tint(), "putchar", putcharArgs, null);
        file.funs.add(putchar);

        // Define sbrk
        LinkedList<Decl_var> sbrkArgs = new LinkedList<>();
        sbrkArgs.add(new Decl_var(new Tint(), "n"));
        Decl_fun sbrk = new Decl_fun(new Tvoidstar(), "sbrk", sbrkArgs, null);
        file.funs.add(sbrk);
        
        // Visit all definitions
        for (Pdecl d : n.l) {
        	if (d instanceof Pstruct) {
        		Pstruct s = (Pstruct)d;
        		s.accept(this);
        	}
        	else if (d instanceof Pfun) {
        		Pfun f = (Pfun)d;
            	f.accept(this);
        	}
        	else {
        		throw new Error("Invalid declaration");
        	}
        }
        
        // Check that main exists
        Decl_fun main = getFunction("main");
        if (main == null) {
        	throw new Error("Missing main function: int main();");
        }
        if (!main.fun_typ.compat(new Tint())) {
        	throw new Error("Main function should return int: int main();");
        }
        if (main.fun_formals.size() > 0) {
        	throw new Error("Main function should not take any argument: int main();");
        }
	}

	@Override
    public void visit(PTint n) {
        last_type = new Tint();
    }

    @Override
    public void visit(PTstruct n) {
        Structure str = getStruct(n.id.id);
        if (str == null) {
            throw new Error("Reference to undefined structure \"" + n.id.id + "\"");
        }
        last_type = new Tstructp(str);
    }

	@Override
	public void visit(Pint n) {
		last_expr = new Econst(n.n);
		if (n.n == 0) {
			last_expr.typ = new Ttypenull();
		} else {
			last_expr.typ = new Tint();
		}
	}

	@Override
	public void visit(Pident n) {
		Decl_var d = getLocal(n.id);
		if (d == null) {
			throw new Error("Trying to access undeclared variable \"" + n.id + "\"");
		}
		last_expr = new Eaccess_local(n.id);
		last_expr.typ = d.t;
	}

	@Override
	public void visit(Punop n) {
		Expr e = visitExpr(n.e1);
		if (n.op == Unop.Uneg) {
			if (!e.typ.compat(new Tint())) {
				throw new Error("Invalid type in negation operation. Can't apply negation on: \""+e.typ+"\"");
			}
			last_expr = new Eunop(n.op, e);
			last_expr.typ = new Tint();
		}
		else if (n.op == Unop.Unot) {
			last_expr = new Eunop(n.op, e);
			last_expr.typ = new Tint();
		}
		else {
			throw new Error("Unsupported unary operator");
		}
	}

	@Override
	public void visit(Passign n) {
		Expr l = visitExpr(n.e1);
		Expr r = visitExpr(n.e2);
		
		if (!l.typ.compat(r.typ)) {
			throw new Error("Invalid types in variable assignement\n. Can't assign: \""+r.typ+"\"\nTo: \""+l.typ+"\"");
		}
		
		if (l instanceof Eaccess_local) {
			Eaccess_local lal = (Eaccess_local)l;
			last_expr = new Eassign_local(lal.i, r);
		}
		else if (l instanceof Eaccess_field) {
			Eaccess_field laf = (Eaccess_field)l;
			last_expr = new Eassign_field(laf.e, laf.f, r);
		}
		else {
			throw new Error("Unsupported lvalue");
		}
		
		last_expr.typ = r.typ;
	}

	@Override
	public void visit(Pbinop n) {
		Expr e1 = visitExpr(n.e1);
		Expr e2 = visitExpr(n.e2);
		
		if (n.op == Binop.Beq || n.op == Binop.Bneq || n.op == Binop.Blt || n.op == Binop.Ble || n.op == Binop.Bgt  || n.op == Binop.Bge ) {
			if (!e1.typ.compat(e2.typ)) {
				throw new Error("Invalid type in binary operation. Can't apply comparison beetween: \""+e1.typ+"\" and \""+e2.typ+"\"");
			}
			last_expr = new Ebinop(n.op, e1, e2);
			last_expr.typ = new Tint();
		}
		else if (n.op == Binop.Band || n.op == Binop.Bor) {
			last_expr = new Ebinop(n.op, e1, e2);
			last_expr.typ = new Tint();
		}
		else if (n.op == Binop.Badd || n.op == Binop.Bsub || n.op == Binop.Bmul || n.op == Binop.Bdiv) {
			if (!e1.typ.compat(new Tint())) {
				throw new Error("Invalid type in binary operation. Can't apply arithmetic operator to non integer: \""+e1.typ+"\"");
			}
			if (!e2.typ.compat(new Tint())) {
				throw new Error("Invalid type in binary operation. Can't apply arithmetic operator to non integer: \""+e2.typ+"\"");
			}
			last_expr = new Ebinop(n.op, e1, e2);
			last_expr.typ = new Tint();
		}
		else {
			throw new Error("Unsupported binary operator");
		}
	}

	@Override
	public void visit(Parrow n) {
		Expr e = visitExpr(n.e);
		if (!(e.typ instanceof Tstructp)) {
			throw new Error("Can't use arrow operator on non struct values. Got type: \""+e.typ+"\"");
		} 
		
		Tstructp t = (Tstructp)e.typ;
		Field f = t.s.fields.get(n.f);
		if (f == null) {
			throw new Error("Trying to access non existing field \""+n.f+"\" in struct. Got type: \""+e.typ+"\"");
		}
		
		last_expr = new Eaccess_field(e, f);
		last_expr.typ = f.type;
	}

	@Override
	public void visit(Pcall n) {
		Decl_fun f = getFunction(n.f);
		if (f == null) {
			throw new Error("In function call \""+n.f+"\": no such function");
		}
		
		int fsize = f.fun_formals.size();
		int csize = n.l.size();
		int diff = fsize - csize;
		if (diff > 0) {
			throw new Error("In function call \""+n.f+"\": too few arguments where given ("+csize+"), need "+fsize+" arguments");
		}
		else if (diff < 0) {
			throw new Error("In function call \""+n.f+"\": too many arguments where given ("+csize+"), need "+fsize+" arguments");
		}
		
		LinkedList<Expr> params = new LinkedList<Expr>();
		
		int i = 0;
		Iterator<Decl_var> fit = f.fun_formals.iterator();
		Iterator<Pexpr> cit = n.l.iterator();
		while(cit.hasNext() && fit.hasNext()) {
			Decl_var arg = fit.next();
			Pexpr pe = cit.next();
			i++;
			
			Expr e = visitExpr(pe);
			if (!e.typ.compat(arg.t)) {
				throw new Error("In function call \""+n.f+"\": invalid type in argument "+i+"\nGot: "+e.typ+"\nExcpected: "+arg.t);
			}
			params.add(e);
		}
		
		last_expr = new Ecall(n.f, params);
		last_expr.typ = f.fun_typ;
	}

	@Override
	public void visit(Psizeof n) {
		Structure s = getStruct(n.id);
		if (s == null) {
			throw new Error("Unknow struct \""+n.id+"\" inside sizeof operation");
		}
		
		last_expr = new Esizeof(s);
		last_expr.typ = new Tint();
	}

	@Override
	public void visit(Pskip n) {
		last_stmt = new Sskip();
	}

	@Override
	public void visit(Peval n) {
		Expr e = visitExpr(n.e);
		last_stmt = new Sexpr(e);
	}

	@Override
	public void visit(Pif n) {
		Expr e = visitExpr(n.e);
		Stmt s1 = visitStmt(n.s1);
		Stmt s2 = visitStmt(n.s2);
		last_stmt = new Sif(e, s1, s2);	
	}

	@Override
	public void visit(Pwhile n) {
		Expr e = visitExpr(n.e);
		Stmt s1 = visitStmt(n.s1);
		last_stmt = new Swhile(e, s1);
	}

	@Override
	public void visit(Pbloc n) {
        HashSet<String> local_vars_name = new HashSet<String>();
		LinkedList<Decl_var> local_vars = new LinkedList<Decl_var>();
        for (Pdeclvar localVar : n.vl) {
        	if(!local_vars_name.add(localVar.id)) {
    			throw new Error("In block content, multiple declarations for variable \""+localVar.id+"\"");
        	}
            Typ localType = visitType(localVar.typ);
            local_vars.add(new Decl_var(localType, localVar.id));
        }
        
		Sblock block = new Sblock(local_vars, new LinkedList<Stmt>());
		
        block_stack.addFirst(block);
        for (Pstmt stmt : n.sl) {
        	Stmt s = visitStmt(stmt);
        	block.sl.add(s);
        }
        block_stack.removeFirst();
        
        last_stmt = block;
	}

	@Override
	public void visit(Preturn n) {
		Expr e = visitExpr(n.e);
		if (!e.typ.compat(ret_type)) {
			throw new Error("In return statement, invalid return type.\nGot: "+e.typ+"\nExpected: "+ret_type);
		}
		
		last_stmt = new Sreturn(e);
	}

	@Override
	public void visit(Pstruct n) {
		if(getStruct(n.s) != null) {
			throw new Error("In structure declaration, \"struct "+n.s+"\" has already been declared");
		}
		
		Structure s = new Structure(n.s);
		structs.put(n.s, s);
		
		for (Pdeclvar v : n.fl) {
			Typ t = visitType(v.typ);
			if (!s.addField(new Field(v.id, t))) {
				throw new Error("In structure declaration, field \""+v.id+"\" is declared multiple times inside \"struct "+n.s+"\"");
			}
		}
	}

	@Override
	public void visit(Pfun n) {	
		if(getFunction(n.s) != null) {
			throw new Error("In function declaration, \""+n.s+"\" has already been declared");
		}
		
		// Function return type
		ret_type = visitType(n.ty);
        
        // Function arguments
        LinkedList<Decl_var> args = new LinkedList<>();
        for (Pdeclvar arg : n.pl) {
            Typ argType = visitType(arg.typ);
            args.add(new Decl_var(argType, arg.id));
        }

		// Declare function for recursive calls
        Decl_fun fun = new Decl_fun(ret_type, n.s, args, null);
        file.funs.add(fun);
        
        // Create a virtual block containing formals as locals, used to check formal accesses and types
        Sblock virtualFormalsBlock = new Sblock(args, null);
        block_stack.addFirst(virtualFormalsBlock);
        visit(n.b);
        block_stack.removeFirst();
        
        // Finally define the function body
        fun.fun_body = last_stmt;
	}

}
