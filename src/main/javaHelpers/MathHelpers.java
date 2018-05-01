package javaHelpers;

public class MathHelpers {

	/**Rotates a matrix clockwise. Returns null if the degrees are not multiple of 90!
	 * It works by copying over the values but doing this in reverse or rotated order.
	 * @param mat		The matrix to be rotated
	 * @param degree	Valid are of course only multiples of 90!
	 * @return			The rotated matrix, a copy.
	 */
	public static boolean[][] rotateBoolMatrixClockw(boolean[][] mat, int degree) {
		degree = degree%360;
	    final int M = mat.length;
	    final int N = mat[0].length;
	    boolean[][] ret;
	    switch (degree) {							// 'break' statements not needed as we return in each case.
		case 0:				// nothing to turn
			return mat;
		case 90:			// clockwise
			ret = new boolean[N][M];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[N-1-c][r] = mat[r][c];
		        }
		    }
		    return ret;
		case 180:			// on its head (not flipped!)
			ret = new boolean[M][N];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[M-1-r][N-1-c] = mat[r][c];
		        }
		    }
		    return ret;
		case 270:			// counter-clockwise
			ret = new boolean[N][M];
		    for (int r = 0; r < M; r++) {
		        for (int c = 0; c < N; c++) {
		            ret[c][M-1-r] = mat[r][c];
		        }
		    }
		    return ret;
		default:			// degree must be off
			return null;
		}
	}
	
	
	/**Sine function taking degrees.
	 * @param deg	a degree in int.
	 * @return 		a double.
	 */
	public static double sind(int deg) {
		return Math.sin(Math.toRadians(deg));
	}
	
	
	/**Cosine function taking degrees.
	 * @param deg	a degree in int.
	 * @return 		a double.
	 */
	public static double cosd(int deg) {
		return Math.cos(Math.toRadians(deg));
	}
	
}
