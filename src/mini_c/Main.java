package mini_c;

import java.io.IOException;
import java.io.InputStream;

public class Main {
	
	static boolean parse_only = false;
	static boolean type_only = false;
	static boolean interp_rtl = false;
	static boolean interp_ertl = false;
	static boolean interp_ltl = false;
	static boolean debug = false;
	static String file = null;
	
	static void usage() {
		System.err.println("mini-c [--parse-only] [--type-only] [--interp-rtl] [--interp-ertl] [--interp-ltl] file.c");
		System.exit(1);
	}
	
	public static void main(String[] args) throws Exception {
		for (String arg: args) {
			if (arg.equals("--parse-only"))
				parse_only= true;
			else if (arg.equals("--type-only"))
				type_only = true;
			else if (arg.equals("--interp-rtl"))
		        interp_rtl = true;
		    else if (arg.equals("--interp-ertl"))
		    	interp_ertl = true;
		    else if (arg.equals("--interp-ltl"))
		    	interp_ltl = true;
			else if (arg.equals("--debug"))
				debug = true;
			else {
				if (file != null) usage();
				if (!arg.endsWith(".c")) usage();
				file = arg;
			}
		}
		
		if (file == null) {
			if(!debug) {
				usage(); return;
			}
			file = "test.c";
		}
	
        java.io.Reader reader = new java.io.FileReader(file);
        Lexer lexer = new Lexer(reader);
        MyParser parser = new MyParser(lexer);
        Pfile pf = (Pfile) parser.parse().value;
        if (parse_only) System.exit(0);
        
        Typing typer = new Typing();
        typer.visit(pf);
        File tf = typer.getFile();
        if (type_only) System.exit(0);
        
        ConstExprOptimization.optimize(tf);
        
        RTLfile rtl = (new ToRTL()).translate(tf);
        if (debug) rtl.print();
        if (interp_rtl) { new RTLinterp(rtl); System.exit(0); }
        
        ERTLfile ertl = (new ToERTL()).translate(rtl);
        if (debug) ertl.print();
        if (interp_ertl) { new ERTLinterp(ertl); System.exit(0); }
  
        LTLfile ltl = (new ToLTL()).translate(ertl);
        if (debug) ltl.print();
        if (interp_ltl) { new LTLinterp(ltl); System.exit(0); }
        
        Lin lin = new Lin(ltl);
        if (debug) lin.print();
        
        String out = file.substring(0, file.lastIndexOf('.')) + ".s";
        lin.output(out);
	}
	
	static void cat(InputStream st) throws IOException {
		while (st.available() > 0) {
			System.out.print((char)st.read());
		}
	}
	
}
