package mini_c;

public class ToLTL extends EmptyERTLVisitor {
	
	LTLfile file;
	
	public LTLfile translate(ERTLfile f) {
		f.accept(this);
		return file;
	}
	

	@Override
	public void visit(ERTLfile n) {
		file = new LTLfile();
	}
}
