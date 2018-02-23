package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/** Interprète de code RTL */
public class ERTLinterp implements ERTLVisitor {

  private Map<String, ERTLfun> funs;
  private Map<Register, Long> regs;
  private Map<Register, Long> hwregs;
  private Memory mem;
  private LinkedList<LinkedList<Long>> frames;
  private LinkedList<Long> next_frame;
  private Label next;
  
  /** interprète un programme RTL donné, à partir de la fonction "main" */
  ERTLinterp(ERTLfile file) {
    this.funs = new HashMap<String, ERTLfun>();
    this.hwregs = new HashMap<Register, Long>();
    this.frames = new LinkedList<LinkedList<Long>>();
    this.next_frame = new LinkedList<Long>();
    for (ERTLfun f: file.funs)
      this.funs.put(f.name, f);
    this.mem = new Memory();
    call("main");
  }

  private void call(String name) {
    ERTLfun f = this.funs.get(name);
    assert f != null; // programme bien typé
    Map<Register, Long> saved_regs = this.regs, new_regs = new HashMap<>();
    for (Register r: f.locals) new_regs.put(r, 0L);
    this.regs = new_regs;
    this.next = f.entry;
    while (true) {
      ERTL i = f.body.graph.get(this.next);
      if (i == null) throw new Error("no ERTL instruction at label " + this.next);
      if (i instanceof ERreturn) break;
      i.accept(this);
    }
    this.regs = saved_regs;
  }
  
  private void set(Register r, long v) {
    (r.isHW() ? this.hwregs : this.regs).put(r, v);
  }
  private void set(Register r, boolean b) {
    this.regs.put(r, b ? 1L : 0L);
  }
  private long get(Register r) {
    if (r.isHW()) return this.hwregs.containsKey(r) ? this.hwregs.get(r) : 0L;
    if (!this.regs.containsKey(r)) throw new Error("unknown register " + r);
    return this.regs.get(r);
  }
  
  @Override
  public void visit(ERconst o) {
    set(o.r, o.i);
    this.next = o.l;
  }

  @Override
  public void visit(ERload o) {
    long p = get(o.r1);
    set(o.r2, this.mem.get(p, o.i));
    this.next = o.l;
  }

  @Override
  public void visit(ERstore o) {
    long p = get(o.r2);
    long v = get(o.r1);
    this.mem.set(p, o.i, v);
    this.next = o.l;
  }

  @Override
  public void visit(ERmunop o) {
    long v = get(o.r);
    if (o.m instanceof Maddi)
      set(o.r, v + ((Maddi)o.m).n);
    else if (o.m instanceof Msetei)
      set(o.r, v == ((Msetei)o.m).n);
    else // Msetnei
      set(o.r, v != ((Msetnei)o.m).n);
    this.next = o.l;
  }

  @Override
  public void visit(ERmbinop o) {
    long v1 = get(o.r1);
    if (o.m == Mbinop.Mmov)
      set(o.r2, v1);
    else {
      long v2 = get(o.r2);
      switch (o.m) {
      case Madd: set(o.r2, v2 + v1); break;
      case Msub: set(o.r2, v2 - v1); break;
      case Mmul: set(o.r2, v2 * v1); break;
      case Mdiv:
        if (!o.r2.equals(Register.rax)) throw new Error("div: r2 must be %rax");
        set(o.r2, v2 / v1); break;
      case Msete: set(o.r2, v2 == v1); break;
      case Msetne: set(o.r2, v2 != v1); break;
      case Msetl: set(o.r2, v2 < v1); break;
      case Msetle: set(o.r2, v2 <= v1); break;
      case Msetg: set(o.r2, v2 > v1); break;
      case Msetge: set(o.r2, v2 >= v1); break;
      default: assert false; // Mmov déjà traité
      }
    }
    this.next = o.l;
  }

  @Override
  public void visit(ERmubranch o) {
    long v = get(o.r);
    boolean b;
    if      (o.m instanceof Mjz  ) b = v == 0L;
    else if (o.m instanceof Mjnz ) b = v != 0L;
    else if (o.m instanceof Mjlei) b = v <= ((Mjlei)o.m).n;
    else /*  o.m instanceof Mjgi */b = v > ((Mjgi)o.m).n;
    this.next = b ? o.l1 : o.l2;
  }

  @Override
  public void visit(ERmbbranch o) {
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
  public void visit(ERcall o) {
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
  public void visit(ERgoto o) {
    this.next = o.l;
  }

  @Override
  public void visit(ERTLfun o) {
    assert false; // inutilisé
  }

  @Override
  public void visit(ERTLfile o) {
    assert false; // inutilisé
  }

  @Override
  public void visit(ERalloc_frame o) {
    this.frames.push(this.next_frame);
    this.next_frame = new LinkedList<Long>();
    this.next = o.l;
  }

  @Override
  public void visit(ERdelete_frame o) {
    if (this.frames.isEmpty()) throw new Error("delete_frame: empty stack");
    this.frames.pop();
    this.next_frame = new LinkedList<Long>();
    this.next = o.l;
  }

  @Override
  public void visit(ERget_param o) {
    if (this.frames.isEmpty()) throw new Error("get_param: missing frame");
    LinkedList<Long> frame = this.frames.peek();
    int n = frame.size();
    if (o.i % 8 != 0) throw new Error("get_param: mis-aligned frame access");
    if (o.i < 16 || o.i > 16 + 8*(n-1)) throw new Error("get_param: access out of frame");
    set(o.r, frame.get((o.i - 16) / 8));
    this.next = o.l;
  }

  @Override
  public void visit(ERpush_param o) {
    this.next_frame.push(get(o.r));
    this.next = o.l;
  }

  @Override
  public void visit(ERreturn o) {
    // rien à faire ici
  }
  
}
