// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wybs.io;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;

import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wyfs.io.BinaryOutputStream;

/**
 * <p>
 * Responsible for writing a WyilFile to an output stream in binary form. The
 * binary format is structured to given maximum flexibility and to avoid
 * built-in limitations in terms of e.g. maximum sizes, etc.
 * </p>
 *
 * @author David J. Pearce
 *
 */
public abstract class SyntacticHeapWriter {
	protected final BinaryOutputStream out;
	protected final SyntacticItem.Schema[] schema;

	public SyntacticHeapWriter(OutputStream output, SyntacticItem.Schema[] schema) {
		this.out = new BinaryOutputStream(output);
		this.schema = schema;
	}

	public void close() throws IOException {
		out.close();
	}

	public void write(SyntacticHeap module) throws IOException {
		// first, write magic number
		writeHeader();
		// third, write syntactic items
		out.write_uv(module.size());
		for(int i=0;i!=module.size();++i) {
			writeSyntacticItem(module.getSyntacticItem(i));
		}
		// finally, flush to disk
		out.flush();
	}

	public abstract void writeHeader() throws IOException;

	public void writeSyntacticItem(SyntacticItem item) throws IOException {
		// Write opcode
		out.write_u8(item.getOpcode());
		// Write operands
		writeOperands(item);
		// Write data (if any)
		writeData(item);
		// Pad to next byte boundary
		out.pad_u8();
	}

	private void writeOperands(SyntacticItem item) throws IOException {
		// Determine operand layout
		SyntacticItem.Operands layout = schema[item.getOpcode()].getOperandLayout();
		// Write operands according to layout
		switch(layout) {
		case MANY:
			out.write_uv(item.size());
			break;
		default:
			if(layout.ordinal() != item.size()) {
				throw new IllegalArgumentException(
						"invalid number of operands for \"" + item.getClass().getSimpleName() + "\" (got " + item.size()
								+ ", expecting " + layout.ordinal() + ")");
			}
		}
		//
		for (int i = 0; i != item.size(); ++i) {
			SyntacticItem operand = item.getOperand(i);
			out.write_uv(operand.getIndex());
		}
	}

	public void writeData(SyntacticItem item) throws IOException {
		// Determine data layout
		SyntacticItem.Data layout = schema[item.getOpcode()].getDataLayout();
		byte[] bytes = item.getData();
		// Write data according to layout
		switch (layout) {
		case MANY:
			out.write_uv(bytes.length);
			break;
		default:
			if(bytes != null && layout.ordinal() != bytes.length) {
				throw new IllegalArgumentException(
						"invalid number of data bytes for " + item.getClass().getSimpleName() + " (got " + bytes.length
								+ ", expecting " + layout.ordinal() + ")");
			} else if(bytes == null && layout.ordinal() != 0) {
				throw new IllegalArgumentException(
						"invalid number of data bytes for " + item.getClass().getSimpleName() + " (got none, expecting "
								+ layout.ordinal() + ")");
			}
		}
		if(bytes != null) {
			for (int i = 0; i != bytes.length; ++i) {
				out.write_u8(bytes[i]);
			}
		}
	}
}
