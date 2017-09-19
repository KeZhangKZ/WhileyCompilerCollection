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
					out.print(Integer.toHexString(data[j]));
				}
				out.print("]");
			}
			out.println();
			out.flush();
		}
	}
}
