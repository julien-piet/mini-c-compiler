package mini_c;

import java.util.LinkedList;

enum Binop {
	Beq, Bneq, Blt, Ble, Bgt, Bge, Badd, Bsub, Bmul, Bdiv, Band, Bor;
	
	static LinkedList<Binop> Compop = new LinkedList<Binop>();
	static {
		Compop.add(Beq); Compop.add(Bneq);
		Compop.add(Blt); Compop.add(Ble);
		Compop.add(Bgt); Compop.add(Bge);
	}
};

enum Unop {
	Uneg, Unot
}

class Pstring {
  String id;
  int loc;
  public Pstring(String id, int loc) {
    super();
    this.id = id;
    this.loc = loc;
  }
  @Override
  public String toString() {
    return this.id;
  }
}

class Pfile {
	LinkedList<Pdecl> l;

	public Pfile(LinkedList<Pdecl> l) {
		super();
		this.l = l;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}	
}

abstract class Pdecl {
	abstract void accept(Pvisitor v);
}

class Pstruct extends Pdecl {
	String s;
	LinkedList<Pdeclvar> fl;

	public Pstruct(String s, LinkedList<Pdeclvar> fl) {
		super();
		this.s = s;
		this.fl = fl;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}	
}

class Pfun extends Pdecl {
	Ptype ty;
	String s;
	LinkedList<Pdeclvar> pl;
	Pbloc b;;

	public Pfun(Ptype ty, String s, LinkedList<Pdeclvar> pl, Pbloc b) {
		super();
		this.ty = ty;
		this.s = s;
		this.pl = pl;
		this.b = b;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}	
}

class Pdeclvar {
	Ptype typ;
	String id;

	public Pdeclvar(Ptype typ, String id) {
		super();
		this.typ = typ;
		this.id = id;
	}
}

/* types */

abstract class Ptype {
	static Ptype ptint = new PTint();
	abstract void accept(Pvisitor v);
}

class PTint extends Ptype {
	void accept(Pvisitor v) {
		v.visit(this);
	}	
}

class PTstruct extends Ptype {
	Pstring id;

	public PTstruct(Pstring id) {
		super();
		this.id = id;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

/* expressions */

abstract class Pexpr {
  abstract void accept(Pvisitor v);
}

abstract class Plvalue extends Pexpr{
}

class Pident extends Plvalue {
	String id;

	public Pident(String id) {
		super();
		this.id = id;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}	
}

class Pint extends Pexpr {
	int n;

	public Pint(int n) {
		super();
		this.n = n;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}

}

class Parrow extends Plvalue {
	Pexpr e;
	String f;
	public Parrow(Pexpr e, String f) {
		super();
		this.e = e;
		this.f = f;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Passign extends Pexpr {
  Plvalue e1;
  Pexpr e2;

  public Passign(Plvalue e1, Pexpr e2) {
    super();
    this.e1 = e1;
    this.e2 = e2;
  }
  void accept(Pvisitor v) {
    v.visit(this);
  }
}

class Pbinop extends Pexpr {
	Binop op;
	Pexpr e1, e2;

	public Pbinop(Binop op, Pexpr e1, Pexpr e2) {
		super();
		this.op = op;
		this.e1 = e1;
		this.e2 = e2;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Punop extends Pexpr {
	Unop op;
	Pexpr e1;

	public Punop(Unop op, Pexpr e1) {
		super();
		this.op = op;
		this.e1 = e1;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}


class Pcall extends Pexpr {
	final String f;
	final LinkedList<Pexpr> l;

	Pcall(String f, LinkedList<Pexpr> l) {
		super();
		this.f = f;
		this.l = l;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Psizeof extends Pexpr {
	String id;

	public Psizeof(String id) {
		super();
		this.id = id;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}

}

/* instructions */

abstract class Pstmt {
	abstract void accept(Pvisitor v);

}

class Pbloc extends Pstmt {
	LinkedList<Pdeclvar> vl;
	LinkedList<Pstmt> sl;

	public Pbloc(LinkedList<Pdeclvar> vl, LinkedList<Pstmt> sl) {
		super();
		this.vl = vl;
		this.sl = sl;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}

}

class Pskip extends Pstmt {
	void accept(Pvisitor v) {
		v.visit(this);
	}

}

class Preturn extends Pstmt {
	Pexpr e;

	public Preturn(Pexpr e) {
		super();
		this.e = e;
	}

	void accept(Pvisitor v) {
		v.visit(this);
	}

}

class Pif extends Pstmt {
	Pexpr e;
	Pstmt s1, s2;

	public Pif(Pexpr e, Pstmt s1, Pstmt s2) {
		super();
		this.e = e;
		this.s1 = s1;
		this.s2 = s2;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}

}

class Peval extends Pstmt {
	Pexpr e;

	public Peval(Pexpr e) {
		super();
		this.e = e;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Pwhile extends Pstmt {
	Pexpr e;
	Pstmt s1;

	public Pwhile(Pexpr e, Pstmt s1) {
		super();
		this.e = e;
		this.s1 = s1;
	}
	void accept(Pvisitor v) {
		v.visit(this);
	}
}

interface Pvisitor {

	public void visit(PTint n);
	
	public void visit(PTstruct n);
	
	public void visit(Pint n);

	public void visit(Pident n);

	public void visit(Punop n);
  
	public void visit(Passign n);

	public void visit(Pbinop n);

	public void visit(Parrow n);

	public void visit(Pcall n);

	public void visit(Psizeof n);

	public void visit(Pskip n);

	public void visit(Peval n);

	public void visit(Pif n);

	public void visit(Pwhile n);

	public void visit(Pbloc n);

	public void visit(Preturn n);

	public void visit(Pstruct n);

	public void visit(Pfun n);

	public void visit(Pfile n);
}
