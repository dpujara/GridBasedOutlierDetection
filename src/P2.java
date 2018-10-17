import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class P2 {
	private static int windowSize = 100;
	private static int counterWindow = 0;
	private static int maxNumber = 32767;
	private static int minNumber = -32768;
	private static int dimension = -1;
	private static int tempDim = -1;
	private static int outlierCount = 0;

	public static void main(String args[]) throws UnknownHostException, IOException {
		String serverAddr = "127.0.0.1";
		int port = 0;
		boolean alreadyExecuted = false;
		boolean tempDimAssigned = false;
		int gridLength = -1;
		int count = 1;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<String> tempStr = new ArrayList<String>();

		String s1;
		while ((s1 = in.readLine()) != null) {
			try {
				if (count == 1) {
					windowSize = Integer.parseInt(s1.trim());
				}
				if (count == 2) {
					serverAddr = s1.split(":")[0].trim();
					port = Integer.parseInt(s1.split(":")[1].trim());
					break;
				}
				count++;
			} catch (Exception e) {
				System.out.println("Error in dat file");
			}
		}

		ArrayList<String> storeData = new ArrayList<String>();
		Map<String, Integer> plotGridData = new HashMap<>();

		Socket client = new Socket(serverAddr, 62066);

		try {
			client.setKeepAlive(true);
			BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
			String s;
			while (true) {
				if ((s = input.readLine()) != null) {

					counterWindow = counterWindow + 1;
					//System.out.println(s);
					validateNumber(s);

					dimension = checkDimension(s, dimension, tempDim);

					if (!tempDimAssigned) {
						tempDim = dimension;
						tempDimAssigned = true;
					}

					if (!alreadyExecuted) {
						gridLength = calculateGridSize(dimension, windowSize);
						alreadyExecuted = true;
					}

					if (counterWindow <= windowSize) {
						initialDataStore(storeData, s);
						initialGridCreation(s, gridLength, plotGridData);
					}

					if (counterWindow > windowSize) {
						processDataAfterWindowSize(storeData, s, plotGridData, gridLength);
					}
				}
			}
		} catch (Exception e) {
			client.close();
			e.printStackTrace();
		}
	}

	private static void initialGridCreation(String s, int gridLength, Map<String, Integer> plotGridData) {
		String gridValueStr = new String();
		gridValueStr = generateGridValueStr(s, gridLength);

		if (plotGridData.get(gridValueStr.substring(0, gridValueStr.length() - 1)) == null) {
			plotGridData.put(gridValueStr.substring(0, gridValueStr.length() - 1), 1);
		} else {
			Integer gridDataValue = plotGridData.get(gridValueStr.substring(0, gridValueStr.length() - 1));
			gridDataValue = gridDataValue + 1;
			plotGridData.put(gridValueStr.substring(0, gridValueStr.length() - 1), gridDataValue);
		}
	}

	private static String generateGridValueStr(String s, int gridLength) {
		String[] val = s.split(",");
		int numberToProcess, gridNumber;
		String gridValueStrLocal = new String();
		for (int i = 1; i < val.length; i++) {
			numberToProcess = Integer.valueOf(val[i]);
			if (numberToProcess < 0) {
				gridNumber = (int) Math.floor(numberToProcess / (double) (gridLength));
				gridValueStrLocal = gridValueStrLocal + gridNumber + ",";
			} else if (numberToProcess >= 0) {
				gridNumber = (int) Math.ceil(numberToProcess / (double) gridLength);
				if (numberToProcess == 0) {
					gridNumber = 1;
				}
				gridValueStrLocal = gridValueStrLocal + gridNumber + ",";
			}
		}
		return gridValueStrLocal;
	}

	private static void processDataAfterWindowSize(ArrayList<String> storeData, String s,
			Map<String, Integer> plotGridData, int gridLength) {

		String generateGridStr = new String();
		generateGridStr = generateGridValueStr(s, gridLength);
		int gridDataValue;
		if (plotGridData.get(generateGridStr.substring(0, generateGridStr.length() - 1)) == null) {
			plotGridData.put(generateGridStr.substring(0, generateGridStr.length() - 1), 1);
			checkIfOutlier(generateGridStr.substring(0, generateGridStr.length() - 1), 1, s, plotGridData);
		} else {
			gridDataValue = plotGridData.get(generateGridStr.substring(0, generateGridStr.length() - 1));
			gridDataValue = gridDataValue + 1;
			plotGridData.put(generateGridStr.substring(0, generateGridStr.length() - 1), gridDataValue);
			checkIfOutlier(generateGridStr.substring(0, generateGridStr.length() - 1), gridDataValue, s, plotGridData);
		}

		String dataTobeRemoved = storeData.get(0);
		String generateGridStrToRemoveData = new String();
		generateGridStrToRemoveData = generateGridValueStr(dataTobeRemoved, gridLength);
		gridDataValue = plotGridData
				.get(generateGridStrToRemoveData.substring(0, generateGridStrToRemoveData.length() - 1));
		gridDataValue = gridDataValue - 1;
		plotGridData.put(generateGridStrToRemoveData.substring(0, generateGridStrToRemoveData.length() - 1),
				gridDataValue);
		storeData.remove(0);
		storeData.add(s);
	}

	private static void checkIfOutlier(String substring, int frequency, String s, Map<String, Integer> plotGridData) {
		int tau = (int) Math.ceil(Math.log10(Math.ceil(Math.pow(Math.E, Math.log(windowSize) / dimension))));
		if (tau == 0) {
			tau = 1;
		}
		if (frequency <= tau) {
			outlierCount = outlierCount + 1;
			if (!checkNeighbourPoints(substring, substring.split(","), 0, plotGridData, tau)) {
				System.out.println(s + " is an outlier");
			}
		}
	}

	private static boolean checkNeighbourPoints(String substring, String[] cell, int d,
			Map<String, Integer> plotGridData, int tau) {
		if (d >= (dimension - 1)) {
			String strToNeigh = new String();

			for (int l = 0; l < cell.length; l++) {
				strToNeigh = strToNeigh + cell[l] + ",";
			}
			strToNeigh = strToNeigh.substring(0, strToNeigh.length() - 1);
			if (plotGridData.containsKey(strToNeigh) && plotGridData.get(strToNeigh) > tau) {
				return true;
			}
			return false;
		}

		int tmp = Integer.valueOf(cell[d]);

		for (int j = -1; j <= 1; j++) {
			int k = Integer.valueOf(cell[d]) + j;
			cell[d] = Integer.toString(k);
			if (checkNeighbourPoints(substring, cell, d + 1, plotGridData, tau))
				return true;
			cell[d] = Integer.toString(tmp);
		}
		return false;

	}

	private static void initialDataStore(ArrayList<String> storeData, String s) {
		storeData.add(s);
	}

	private static int calculateGridSize(int dimension, int windowSize2) {
		return 65536 / (int) Math.ceil(Math.pow(Math.E, Math.log(windowSize2) / dimension));
	}

	private static void validateNumber(String s) {
		String[] val = s.split(",");
		for (int i = 1; i < val.length; i++) {
			try {
				if (Integer.valueOf(val[i]) > maxNumber || Integer.valueOf(val[i]) < minNumber) {
					System.out.println("Number out of range of [-32768,32767]");
					System.exit(0);
				}
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
	}

	private static int checkDimension(String s, int dimension, int tempDim) {
		String[] val = s.split(",");
		if (dimension == -1) {
			dimension = val.length;
		} else if (tempDim != val.length) {
			System.out.println("Dimension Chnaged. Exit stream");
			System.exit(0);
		}
		return dimension;
	}
}