// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wybs.io;

import java.io.PrintWriter;

import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;

public class SyntacticHeapPrinter {
	private final PrintWriter out;
	/**
	 * Number of spaces per indentation.
	 */
	private final int indent;
	private final boolean structured;

	public SyntacticHeapPrinter(PrintWriter out, int indent, boolean structured) {
		this.out = out;
		this.indent = indent;
		this.structured = structured;
	}

	public void print(SyntacticHeap heap) {
		if(structured) {
			print(heap.getRootItem(), 0, heap);
		} else {
			printRaw(heap);
		}
		out.flush();
	}

	private void printRaw(SyntacticHeap heap) {
		out.println("root=" + heap.getRootItem().getIndex());
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
					out.print(item.get(j).getIndex());
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
					out.print("0x" + Integer.toHexString(data[j]));
				}
				out.print("] ");
				// FIXME: there should be a better way of doing this really
				out.print(item);
			}
			out.println();
		}
	}

	public void print(SyntacticItem item, int indent, SyntacticHeap heap) {
		printIndent(indent);
		if(item.size() > 0) {
			out.print("(");
			out.println(item.getClass().getSimpleName());
			for(int j=0;j!=item.size();++j) {
				if(j!=0) {
					out.println(",");
				}
				print(item.get(j), indent + 1, heap);
			}
			out.println();
			printIndent(indent);
			out.print(")");
		} else {
			out.print(item);
		}
	}

	private void printIndent(int indent) {
		indent = indent * this.indent;
		for(int i=0;i!=indent;++i) {
			out.print(" ");
		}
	}
}
