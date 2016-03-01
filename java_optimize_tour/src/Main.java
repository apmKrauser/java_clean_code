package com.cipsoft.candidate;


/**
 * Loesung zur Wanderer-Aufgabe fuer Bewerber bei CipSoft
 * @author Matthias Krause
 * @version 2015.11
 */
public class Main {

    public static void main(String[] args) {

        String file;

        // Take the first parameter as file name
        if (args.length > 0) {
            file = args[0];
        } else file = "test1.txt";

        // read tour data
        FileParser tourInfo = new FileParser(file);

        // no data is loaded in order to plan a tour, so quit
        if (tourInfo.availableOvernightDistances.length == 0)
            return;

        // If there are more days than overnights
        if (tourInfo.days > tourInfo.availableOvernightDistances.length) {
            int days = tourInfo.days;
            int stages = tourInfo.availableOvernightDistances.length;
            System.out.printf("There are %d days to walk a %d stages tour.\nThis is more time than necessary.\n",
                    tourInfo.days, tourInfo.availableOvernightDistances.length);
            tourInfo.days = stages;
            System.out.printf("A route in %d days is suggested below.\nUse the remaining %d days for relaxing, if you wouldn't mind.\n",
                    tourInfo.days, days - tourInfo.days);
        }

        // initialize the optimization algorithm
        HeuristicOptimizer optimalTour = new HeuristicOptimizer(tourInfo.availableOvernightDistances, tourInfo.days);

        // store the first approach as backup
        String txtFirstGuess = optimalTour.toString();

        try {
            // successive optimize the tour plan
            optimalTour.Optimize();
        } catch (Exception e) {
            System.out.printf("An error occured while optimizing. Sorry, this should not happen.\n(%s)\n\n", e.getMessage());
            System.out.println("Indeed there is a nearly optimal route:");
            System.out.println();
            System.out.println(txtFirstGuess);
            return;
        }

        // Display the optimized tour
        System.out.println();
        System.out.println(optimalTour.toString());
        System.out.println();

    }
}
