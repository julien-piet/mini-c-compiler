package mini_c;

import java.util.HashSet;

public class Lin implements LTLVisitor {

	private X86_64 asm = new X86_64();
	private HashSet<Label> visited = new HashSet<Label>();
	private LTLgraph cfg;
	
	Lin(LTLfile f) {
		visit(f);
	}
	
	private void lin(Label l) {
	    if (visited.contains(l)) {
	    	asm.needLabel(l);
	    	asm.jmp(l.name);
	    } else {
	    	visited.add(l);
	    	asm.label(l);
	    	cfg.graph.get(l).accept(this);
	    }
	}
	
	private void linJmpTarget(Label l) {
		if (visited.contains(l)) {
			return;
		}
		else {
	    	visited.add(l);
	    	asm.label(l.toString());
	    	cfg.graph.get(l).accept(this);
		}
	}
	
	@Override
	public void visit(Lload o) {
		asm.movq(o.i + "(" + o.r1 + ")", o.r2.toString());
	}

	@Override
	public void visit(Lstore o) {
		asm.movq(o.r1.toString(), o.i + "(" + o.r2 + ")");
	}

	@Override
	public void visit(Lmubranch o) {
		if (o.m instanceof Mjz) {
			asm.testq(0, o.r.toString());
			asm.jz(o.l1.toString());
		}
		else if (o.m instanceof Mjnz) {
			asm.testq(0, o.r.toString());
			asm.jnz(o.l1.toString());
		}
		else if (o.m instanceof Mjlei) {
			asm.cmpq(((Mjlei)o.m).n, o.r.toString());
			asm.jle(o.l1.toString());
		}
		else if (o.m instanceof Mjgi) {
			asm.cmpq(((Mjgi)o.m).n, o.r.toString());
			asm.jg(o.l1.toString());
		}
		linJmpTarget(o.l2);
		linJmpTarget(o.l1);
	}

	@Override
	public void visit(Lmbbranch o) {
		asm.cmpq(o.r1.toString(), o.r2.toString());
		switch(o.m) {
			case Mjl:
				asm.jl(o.l1.toString());
				break;
			case Mjle:
				asm.jle(o.l1.toString());
				break;
			default:
				break;
		}
		linJmpTarget(o.l2);
		asm.jmp(o.l1.toString());
		linJmpTarget(o.l1);
	}

	@Override
	public void visit(Lgoto o) {
		lin(o.l);
	}

	@Override
	public void visit(Lreturn o) {
		asm.ret();
	}

	@Override
	public void visit(Lconst o) {
		asm.movq(o.i, o.o.toString());
		lin(o.l);
	}

	@Override
	public void visit(Lmunop o) {
		if (o.m instanceof Maddi) {
			asm.addq(((Maddi)o.m).n, o.o.toString());
		}
		else if (o.m instanceof Msetei) {
			asm.cmpq(((Msetei)o.m).n, o.o.toString());
			asm.setne(o.o.byteSized());
			asm.movzbq(o.o.byteSized(), o.o.toString());
		}
		else if (o.m instanceof Msetnei) {
			asm.cmpq(((Msetnei)o.m).n, o.o.toString());
			asm.setne(o.o.byteSized());
			asm.movzbq(o.o.byteSized(), o.o.toString());
		}
		lin(o.l);
	}

	@Override
	public void visit(Lmbinop o) {
		switch(o.m) {
			case Mmov:
				asm.movq(o.o1.toString(), o.o2.toString());
				break;
			case Madd:
				asm.addq(o.o1.toString(), o.o2.toString());
				break;
			case Msub:
				asm.subq(o.o1.toString(), o.o2.toString());
				break;
			case Mmul:
				asm.imulq(o.o1.toString(), o.o2.toString());
				break;
			case Mdiv:
				asm.cqto();
				asm.idivq(o.o1.toString());
				break;
			case Msete:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.sete(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			case Msetg:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.setg(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			case Msetge:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.setge(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			case Msetl:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.setl(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			case Msetle:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.setle(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			case Msetne:
				asm.cmpq(o.o1.toString(), o.o2.toString());
				asm.setne(o.o2.byteSized());
				asm.movzbq(o.o2.byteSized(), o.o2.toString());
				break;
			default:
				break;
		}
		lin(o.l);
	}

	@Override
	public void visit(Lpush o) {
		asm.pushq(o.o.toString());
		lin(o.l);
	}

	@Override
	public void visit(Lpop o) {
		asm.popq(o.r.toString());
		lin(o.l);
	}

	@Override
	public void visit(Lcall o) {
		asm.call(o.s);
		lin(o.l);
	}

	@Override
	public void visit(LTLfun o) {
		cfg = o.body;
	    asm.label(o.name);
	    lin(o.entry);
	}

	@Override
	public void visit(LTLfile o) {
		asm.globl("main");
		for(LTLfun f: o.funs) {
			visit(f);
		}
	}

	public void print() {
		asm.print();
	}
	public void output(String filename) {
		asm.printToFile(filename);
	}
}
