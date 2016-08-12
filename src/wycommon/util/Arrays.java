package wycommon.util;

public class Arrays {

	/**
	 * Return an integer array containing consecutive integers from a given
	 * start value upto (but not including) a given end value.
	 * 
	 * @param start
	 *            The start value from which the range begins. This value is
	 *            always the first element of the final array (assuming it's not
	 *            empty).
	 * @param end
	 *            The value up to which (exclusively) the range extends. This
	 *            value is not in the final array. If this value equals or is
	 *            less than the start value, then the empty array is returned.
	 * @return
	 */
	public static int[] range(int start, int end) {
		int[] rs = new int[Math.abs(end - start)];
		for (int i = start; i < end; ++i) {
			rs[i - start] = i;
		}
		return rs;
	}
	
	/**
	 * Append two arrays of boolean type together, producing a fresh array whose
	 * length equals that of the first and second added together.
	 * 
	 * @param lhs
	 *            The left-hand side. Elements of this array will be copied
	 *            first into the resulting array.
	 * @param rhs
	 *            The right-hand side. Elements of this array will be copied
	 *            last into the resulting array.
	 * @return
	 */
	private static boolean[] append(boolean[] lhs, boolean[] rhs) {
		boolean[] rs = java.util.Arrays.copyOf(lhs, lhs.length + rhs.length);
		System.arraycopy(rhs, 0, rs, lhs.length, rhs.length);
		return rs;
	}
	
	/**
	 * Append two arrays of integer type together, producing a fresh array whose
	 * length equals that of the first and second added together.
	 * 
	 * @param lhs
	 *            The left-hand side. Elements of this array will be copied
	 *            first into the resulting array.
	 * @param rhs
	 *            The right-hand side. Elements of this array will be copied
	 *            last into the resulting array.
	 * @return
	 */
	private static int[] append(int[] lhs, int[] rhs) {
		int[] rs = java.util.Arrays.copyOf(lhs, lhs.length + rhs.length);
		System.arraycopy(rhs, 0, rs, lhs.length, rhs.length);
		return rs;
	}
	
	/**
	 * Append two arrays of unknown type together, producing a fresh array whose
	 * length equals that of the first and second added together.
	 * 
	 * @param lhs
	 *            The left-hand side. Elements of this array will be copied
	 *            first into the resulting array.
	 * @param rhs
	 *            The right-hand side. Elements of this array will be copied
	 *            last into the resulting array.
	 * @return
	 */
	private static <T> T[] append(T[] lhs, T[] rhs) {
		T[] rs = java.util.Arrays.copyOf(lhs, lhs.length + rhs.length);
		System.arraycopy(rhs, 0, rs, lhs.length, rhs.length);
		return rs;
	}

}
