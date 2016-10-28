// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.*;

import wycc.util.ArrayUtils;

public class ArrayUtilTests {
	@Test public void range_1() {
		assertTrue(Arrays.equals(ArrayUtils.range(0, 0), new int[]{}));
	}
	@Test public void range_2() {
		assertTrue(Arrays.equals(ArrayUtils.range(0, 1), new int[]{0}));
	}
	@Test public void range_3() {
		assertTrue(Arrays.equals(ArrayUtils.range(0, 2), new int[]{0,1}));
	}
	@Test public void range_4() {
		assertTrue(Arrays.equals(ArrayUtils.range(-1, 0), new int[]{-1}));
	}
	@Test public void range_5() {
		assertTrue(Arrays.equals(ArrayUtils.range(0, -1), new int[]{}));
	}
}
