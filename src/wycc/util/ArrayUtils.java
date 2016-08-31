package wycc.util;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

public class ArrayUtils {

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
	public static boolean[] append(boolean[] lhs, boolean[] rhs) {
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
	public static int[] append(int[] lhs, int[] rhs) {
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
	public static <T> T[] append(T[] lhs, T... rhs) {
		T[] rs = java.util.Arrays.copyOf(lhs, lhs.length + rhs.length);
		System.arraycopy(rhs, 0, rs, lhs.length, rhs.length);
		return rs;
	}

	/**
	 * Add all elements from an array into a given collection of the same type.
	 *
	 * @param lhs
	 *            The left-hand side. Elements of this array will be added to
	 *            the collection.
	 * @param rhs
	 *            The right-hand side. Elements from the left-hand side will be
	 *            added to this collection.
	 */
	public static <T> void addAll(T[] lhs, Collection<T> rhs) {
		for(int i=0;i!=lhs.length;++i) {
			rhs.add(lhs[i]);
		}
	}

	/**
	 * Convert a collection of strings into a string array.
	 *
	 * @param items
	 * @return
	 */
	public static String[] toStringArray(Collection<String> items) {
		String[] result = new String[items.size()];
		int i = 0;
		for(String s : items) {
			result[i++] = s;
		}
		return result;
	}

	/**
	 * Remove duplicate types from an unsorted array. This produces a potentially
	 * smaller array with all duplicates removed. Null is permitted in the array
	 * and will be preserved, though duplicates of it will not be. Items in the
	 * array are compared using <code>Object.equals()</code>.
	 *
	 * @param items
	 *            The array for which duplicates are to be removed
	 * @return
	 */
	public static <T> T[] removeDuplicates(T[] items) {
		int count = 0;
		// First, identify duplicates and store this information in a bitset.
		BitSet duplicates = new BitSet(items.length);
		for (int i = 0; i != items.length; ++i) {
			T ith = items[i];
			for (int j = i + 1; j < items.length; ++j) {
				T jth = items[j];
				if(ith == null) {
					if(jth == null) {
						duplicates.set(i);
						count = count + 1;
						break;
					}
				} else if (ith.equals(jth)) {
					duplicates.set(i);
					count = count + 1;
					break;
				}
			}
		}
		// Second, eliminate duplicates (if any)
		if (count == 0) {
			// nothing actually needs to be removed
			return items;
		} else {
			T[] nItems = Arrays.copyOf(items, items.length - count);
			for (int i = 0, j = 0; i != items.length; ++i) {
				if (duplicates.get(i)) {
					// this is a duplicate, ignore
				} else {
					nItems[j++] = items[i];
				}
			}
			return nItems;
		}
	}

	/**
	 * Remove duplicate types from an sorted array, thus any duplicates are
	 * located adjacent to each other. This produces a potentially smaller array
	 * with all duplicates removed. Null is permitted in the array and will be
	 * preserved, though duplicates of it will not be. Items in the array are
	 * compared using <code>Object.equals()</code>.
	 *
	 * @param items
	 *            The array for which duplicates are to be removed
	 * @return
	 */
	public static <T> T[] sortedRemoveDuplicates(T[] items) {
		int count = 0;
		// First, identify duplicates and store this information in a bitset.
		BitSet duplicates = new BitSet(items.length);
		for (int i = 1; i != items.length; ++i) {
			T ith = items[i];
			T jth = items[i-1];
			if(ith != null) {
				if (ith.equals(jth)) {
					duplicates.set(i-1);
					count = count + 1;
					break;
				}
			} else if(jth == null) {
				duplicates.set(i-1);
				count = count + 1;
			}
		}
		// Second, eliminate duplicates (if any)
		if (count == 0) {
			// nothing actually needs to be removed
			return items;
		} else {
			T[] nItems = Arrays.copyOf(items, items.length - count);
			for (int i = 0, j = 0; i != items.length; ++i) {
				if (duplicates.get(i)) {
					// this is a duplicate, ignore
				} else {
					nItems[j++] = items[i];
				}
			}
			return nItems;
		}
	}

	/**
	 * Remove any occurrence of <code>null</code> from a given array. The
	 * resulting array may be shorter in length, but the relative position of
	 * all non-null items will remain unchanged.
	 *
	 * @param items
	 * @return
	 */
	public static <T> T[] removeNulls(T[] items) {
		int count = 0;
		for(int i=0;i!=items.length;++i) {
			if(items[i] == null) {
				count++;
			}
		}
		// Second, eliminate duplicates (if any)
		if (count == 0) {
			// nothing actually needs to be removed
			return items;
		} else {
			T[] nItems = Arrays.copyOf(items, items.length - count);
			for(int i=0, j = 0;i!=items.length;++i) {
				T ith = items[i];
				if(ith != null) {
					nItems[j++] = ith;
				}
			}
			return nItems;
		}
	}
}
