package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class Stack {
  private static int max_size = 1000000;
  static long init_rsp = 8 * max_size; 
  private long[] stack = new long[max_size];
  private int index(long p) {
    if (p % 8 != 0) throw new Error("mis-aligned stack access");
    p /= 8;
    if (p < 0 || p >= max_size) throw new Error("access out of stack");
    return (int)p;
  }
  long get(long p, int ofs) {  return this.stack[index(p + ofs)]; }
  void set(long p, int ofs, long v) { this.stack[index(p + ofs)] = v; }
}

/** Interprète de code LTL */
public class LTLinterp  implements LTLVisitor {

  private Map<String, LTLfun> funs;
  private Map<Register, Long> regs; // physiques
  private Memory mem;
  private Stack stack;
  private Label next;
  
  /** interprète un programme RTL donné, à partir de la fonction "main" */
  LTLinterp(LTLfile file) {
    this.funs = new HashMap<String, LTLfun>();
    for (LTLfun f: file.funs)
      this.funs.put(f.name, f);
    this.regs = new HashMap<Register, Long>();
    this.mem = new Memory();
    this.stack = new Stack();
    this.regs.put(Register.rsp, Stack.init_rsp);
    call("main");
  }

  private void call(String name) {
    LTLfun f = this.funs.get(name);
    assert f != null; // programme bien typé
    push(0l); // adresse de retour fictive
    this.next = f.entry;
    while (true) {
      LTL i = f.body.graph.get(this.next);
      if (i == null) throw new Error("no LTL instruction at label " + this.next);
      if (i instanceof Lreturn) break;
      i.accept(this);
    }
    pop(); // dépile l'adresse de retour fictive
  }
  
  private void push(long v) {
    long rsp = get(Register.rsp) - 8;
    set(Register.rsp, rsp);
    this.stack.set(rsp, 0, v);
  }
  private long pop() {
    long rsp = get(Register.rsp);
    long v = this.stack.get(rsp, 0);
    set(Register.rsp, rsp + 8);
    return v;
  }
  
  private void set(Register r, long v) {
    if (r.isPseudo()) throw new Error("unknown register " + r);
    this.regs.put(r, v);
  }
  private long get(Register r) {
    if (r.isPseudo()) throw new Error("unknown register " + r);
    return this.regs.containsKey(r) ? this.regs.get(r) : 0L;
  }
  private void set(Operand o, long v) {
    if (o instanceof Reg)
      set(((Reg)o).r, v);
    else {
      int ofs = ((Spilled)o).n;
      this.stack.set(get(Register.rbp), ofs, v);
    }
  }
  private void set(Operand o, boolean b) {
    set(o, b ? 1l: 0l);
  }
  private long get(Operand o) {
    if (o instanceof Reg)
      return get(((Reg)o).r);
    else {
      int ofs = ((Spilled)o).n;
      return this.stack.get(get(Register.rbp), ofs);
    }
  }
  
  @Override
  public void visit(Lconst o) {
    set(o.o, o.i);
    this.next = o.l;
  }

  @Override
  public void visit(Lload o) {
    long p = get(o.r1);
    set(o.r2, this.mem.get(p, o.i));
    this.next = o.l;
  }

  @Override
  public void visit(Lstore o) {
    long p = get(o.r2);
    long v = get(o.r1);
    this.mem.set(p, o.i, v);
    this.next = o.l;
  }

  @Override
  public void visit(Lmunop o) {
    long v = get(o.o);
    if (o.m instanceof Maddi)
      set(o.o, v + ((Maddi)o.m).n);
    else if (o.m instanceof Msetei)
      set(o.o, v == ((Msetei)o.m).n);
    else // Msetnei
      set(o.o, v != ((Msetnei)o.m).n);
    this.next = o.l;
  }

  @Override
  public void visit(Lmbinop o) {
    long v1 = get(o.o1);
    if (o.m == Mbinop.Mmov)
      set(o.o2, v1);
    else {
      long v2 = get(o.o2);
      switch (o.m) {
      case Madd: set(o.o2, v2 + v1); break;
      case Msub: set(o.o2, v2 - v1); break;
      case Mmul: set(o.o2, v2 * v1); break;
      case Mdiv:
        if (!o.o2.equals(new Reg(Register.rax))) throw new Error("div: r2 must be %rax");
        set(o.o2, v2 / v1); break;
      case Msete: set(o.o2, v2 == v1); break;
      case Msetne: set(o.o2, v2 != v1); break;
      case Msetl: set(o.o2, v2 < v1); break;
      case Msetle: set(o.o2, v2 <= v1); break;
      case Msetg: set(o.o2, v2 > v1); break;
      case Msetge: set(o.o2, v2 >= v1); break;
      default: assert false; // Mmov déjà traité
      }
    }
    this.next = o.l;
  }

  @Override
  public void visit(Lmubranch o) {
    long v = get(o.r);
    boolean b;
    if      (o.m instanceof Mjz  ) b = v == 0L;
    else if (o.m instanceof Mjnz ) b = v != 0L;
    else if (o.m instanceof Mjlei) b = v <= ((Mjlei)o.m).n;
    else /*  o.m instanceof Mjgi */b = v > ((Mjgi)o.m).n;
    this.next = b ? o.l1 : o.l2;
  }

  @Override
  public void visit(Lmbbranch o) {
    long v1 = get(o.r1);
    long v2 = get(o.r2);
    boolean b = true; // parce que le compilo Java n'est pas assez malin
    switch (o.m) {
    case Mjl : b = v2 <  v1; break;
    case Mjle: b = v2 <= v1; break;
    }
    this.next = b ? o.l1 : o.l2;
  }

  @Override
  public void visit(Lcall o) {
    switch (o.s) {
    case "sbrk":
      set(Register.result, this.mem.malloc((int)get(Register.rdi)));
      break;
    case "putchar":
      long n = get(Register.rdi);
      System.out.print((char)n);
      set(Register.result, n);
      break;
    default:
      call(o.s);
    }
    this.next = o.l;
  }

  @Override
  public void visit(Lgoto o) {
    this.next = o.l;
  }

  @Override
  public void visit(LTLfun o) {
    assert false; // inutilisé
  }

  @Override
  public void visit(LTLfile o) {
    assert false; // inutilisé
  }

 @Override
  public void visit(Lpush o) {
    push(get(o.o));
    this.next = o.l;
  }

  @Override
  public void visit(Lreturn o) {
    assert false; // inutilisé
  }

  @Override
  public void visit(Lpop o) {
    set(o.r, pop());
    this.next = o.l;
  }
  
}
