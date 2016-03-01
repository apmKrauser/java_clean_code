package com.cipsoft.candidate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * Class for reading and parsing tour data from a text file
 */
public class FileParser {

    /** Number of days in which the tour shall be finished  */
    public int days = 0;

    /** Distances between overnight accommodations */
    public int[] availableOvernightDistances = new int[0];


    /**
     * FileParser constructor. Reads and parses text file.
     * availableOvernightDistances.length will remain 0 if parsing fails
     * @param filepath file containing tour data
     */
    public FileParser(String filepath) {
        Scanner scanner = null;
        String msgFileFormatError = "Failed reading number in '%s' line %d.";
        try {
            scanner = new Scanner(new File(filepath));

            // Number of tour stages
            int stages;
            if (scanner.hasNextInt()) {
                stages = scanner.nextInt();
            } else throw new IOException(String.format(msgFileFormatError, filepath, 1));

            // Number of days
            if (scanner.hasNextInt()) {
                days = scanner.nextInt();
            } else throw new IOException(String.format(msgFileFormatError, filepath, 2));

            // do not accept negative values or zero
            if (stages <= 0)
                throw new IOException(String.format(msgFileFormatError, filepath, 1));
            if (days <= 0)
                throw new IOException(String.format(msgFileFormatError, filepath, 2));

            // allocate stages array
            availableOvernightDistances = new int[stages];

            // read distances
            int i = 0;
            while (scanner.hasNextInt() && (i < stages)) {
                if ( (availableOvernightDistances[i++] = scanner.nextInt()) < 1 /* distance negative or 0 */ )
                {
                    throw new IOException(String.format("'%s' (line %d): Distance has to be >= 1!",
                            filepath, i+2));
                }
            }

            // Not a number or not enough stages defined
            if (i < stages)
                throw new IOException(String.format(msgFileFormatError, filepath, i+2));

        } catch (FileNotFoundException e) {
            System.out.println(String.format("[FileParser]: File '%s' not found.", filepath));
        } catch (IOException e) {
            System.out.println("[FileParser]: " + e.getMessage());
            availableOvernightDistances = new int[0]; // indicates failed file parsing or empty file
        } finally {
            if (scanner != null)
                scanner.close();
        }
    }


}
