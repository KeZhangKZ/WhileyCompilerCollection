package wybs.io;

import java.io.PrintWriter;

import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;

public class SyntacticHeapPrinter {
	private final PrintWriter out;

	public SyntacticHeapPrinter(PrintWriter out) {
		this.out = out;
	}

	public void print(SyntacticHeap heap) {
		for(int i=0;i!=heap.size();++i) {
			SyntacticItem item = heap.getSyntacticItem(i);
			out.print("#" + i);
			out.print(" ");
			out.print(item.getClass().getSimpleName());
			if(item.size() > 0) {
				out.print("(");
				for(int j=0;j!=item.size();++j) {
					if(j!=0) {
						out.print(",");
					}
					out.print(item.getOperand(j).getIndex());
				}
				out.print(")");
			}
			byte[] data = item.getData();
			if(data != null && data.length > 0) {
				out.print("[");
				for(int j=0;j!=data.length;++j) {
					if(j!=0) {
						out.print(",");
					}
					out.print(Integer.toHexString(data[j]));
				}
				out.print("]");
			}
			out.println();
			out.flush();
		}
	}
}
