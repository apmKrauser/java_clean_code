package com.cipsoft.candidate;


/**
 * This class represents the data structure of the tour partitioned into several days.
 * It offers methods for manipulation used by the optimization algorithm.
 */
public class TrackPlan {

    /** Number of days in which the tour shall be finished  */
    public int days = 0;


    // absolute distance of each stage from tour start point
    protected int[] availableOvernights;
    // '1' based array which makes it more human readable and
    // less error prone while programming; [0] is a dummy element;
    protected HikingDay[] hikingDays;
    // backup array used by undo function
    protected HikingDay[] hikingDaysUndo;
    // the distance between to stages if day trips where equal distributed
    // and overnights could occur everywhere
    protected double averageDayTrip = 0;

    /**
     * TrackPlan constructor
     * Calculates a '1st Guess' of optimal distributed overnights when called
     * @param availableOvernightDistances as read from FileParser; Distances between every stage
     * @param days The number of days the tour shall last
     */
    public TrackPlan(int[] availableOvernightDistances, int days) {
        // construct an array that contains the absolute from every available overnight to the starting point
        availableOvernights = new int[availableOvernightDistances.length + 1];
        availableOvernights[0] = 0;
        for (int i = 0; i < availableOvernightDistances.length; i++) {
            availableOvernights[i + 1] = availableOvernights[i] + availableOvernightDistances[i];
        }
        this.days = days;
        // generate '1' based arrays and dummy element 0
        hikingDays = new HikingDay[days + 1];
        hikingDaysUndo = new HikingDay[days + 1];
        hikingDays[0] = new HikingDay();
        hikingDays[0].tripBegin = new OvernightStay(0);
        hikingDays[0].tripEnd = new OvernightStay(0);
        hikingDays[0].tripDistance = 0;
        hikingDaysUndo[0] = new HikingDay();
        hikingDaysUndo[0].tripBegin = new OvernightStay(0);
        hikingDaysUndo[0].tripEnd = new OvernightStay(0);
        hikingDaysUndo[0].tripDistance = 0;

        // The optimal distance, if an overnight could be chosen everywhere.
        // This corresponds to the most relaxed state of this system.
        averageDayTrip = ((double) availableOvernights[availableOvernights.length - 1]) / (double) days;

        // construct first guess of overnight distribution:
        // The entirely relaxed state of the system is fitted to
        // the 'quantized'/fixed overnight places.
        // This is managed by finding the nearest existing overnight compared to the optimal overnight position.  
        {
            double optTrip = averageDayTrip; // optimal trip distance
            double mismatch = 0;
            int remainingStages = availableOvernights.length;
            // set prevmismatch larger than possible mismatch between days
            // this is the starting value
            double prevmismatch = averageDayTrip + 1.0;
            int i = 1;

            // find the best overnights
            for (int day = 1; day <= days; i++) {

                // mismatch of optimal trip distance per day and the current overnight opportunity
                if (i <  availableOvernights.length)
                    mismatch = Math.abs(((double) availableOvernights[i]) - optTrip);

                // last day ends where tour ends
                if (day == days)
                    i = availableOvernights.length; // last stage +1

                remainingStages--;
                // we have passed the nearest stage or begin the last day
                if ((mismatch > prevmismatch) || (day == days) || !((days-day) < remainingStages)) {
                    hikingDays[day] = new HikingDay();
                    hikingDays[day].tripBegin = hikingDays[day - 1].tripEnd;
                    hikingDays[day].tripEnd = new OvernightStay(i - 1);
                    hikingDays[day].recalcDistance();
                    hikingDaysUndo[day] = new HikingDay();
                    hikingDaysUndo[day].tripBegin = hikingDaysUndo[day - 1].tripEnd;
                    hikingDaysUndo[day].tripEnd = new OvernightStay(i - 1);
                    hikingDaysUndo[day].recalcDistance();
                    optTrip += averageDayTrip;
                    prevmismatch = averageDayTrip + 1.0;
                    day++;
                } else prevmismatch = mismatch;
            } // for

        } // 1st guess block
    }


    /**
     * wrapper for stationNr in order to let day1's end and day2's begin point to the same stationNr
     * even if changed independently, so the days build a chain where each trackEnd is connected
     * to the trackBegin of the following day
     */
    protected class OvernightStay {

        /**  */
        public int stationNr;

        public int getStageDistance() {
            return availableOvernights[stationNr];
        }

        /**
         * Constructor
         * @param stationNr number of stage (in availableOvernights) used for overnight
         */
        public OvernightStay(int stationNr) {
            this.stationNr = stationNr;
        }
    }

    /**
     * One day of the tour
     */
    protected class HikingDay {
        public OvernightStay tripBegin;
        public OvernightStay tripEnd;
        public int tripDistance; // only recalculated on changes

        public void recalcDistance() {
            tripDistance = tripEnd.getStageDistance() - tripBegin.getStageDistance();
        }

    }

    /**
     * Distance between trip begin and end of a day
     * @param day The day number of the tour
     * @return Distance between trip begin and end of a day
     */
    public int getDistance(int day) {
        return hikingDays[day].tripDistance;
    }

    /**
     * Get the number of the day with the longest trip
     * @return The number of the day with the farthest trip
     */
    public int getLongestDay() {
        int dist = 0;
        int distMax = 0;
        int longestDay = 0;
        for (int day = 1; day <= days; day++) {
            dist = getDistance(day);
            if (dist > distMax) {
                distMax = dist;
                longestDay = day;
            }
        }
        return longestDay;
    }


    /**
     * Get the farthest daily trip of the tour
     * @return The farthest daily trip of the tour
     */
    public int getLongestDailyTrack() {
        return getLongestDailyTrack(1, days);
    }

    /**
     * Get the farthest daily trip of a sub tour
     * @param fromDay sub tour starting at this day
     * @param toDay sub tour ending at this day
     * @return Trip of sub tour
     */
    public int getLongestDailyTrack(int fromDay, int toDay) {
        int dist = 0;
        int distMax = 0;
        for (int day = fromDay; day <= toDay; day++) {
            dist = getDistance(day);
            if (dist > distMax) distMax = dist;
        }
        return distMax;
    }

    /**
     * In order to shorten the distance of a day trip shift its end one step earlier.
     * This will stretch the trip of the following day.
     * @param day The day to be resized
     * @return True: if possible; False: if it is not possible to use nearer overnight
     */
    public boolean trackEndEarlier(int day) {
        int dBegin = hikingDays[day].tripBegin.stationNr;
        int dEnd = hikingDays[day].tripEnd.stationNr;
        // at least 2 stages so we can drop one
        // and we are not on the last trip
        if (((dEnd - dBegin) > 1) && (day < days)) {
            hikingDays[day].tripEnd.stationNr--;
            hikingDays[day].recalcDistance();
            hikingDays[day + 1].recalcDistance();
            return true;
        } else return false;
    }

    /**
     * In order to shorten the distance of a day trip shift its begin one step forward.
     * This will stretch the trip of the previous day.
     * @param day The day to be resized
     * @return True: if possible; False: if it is not possible to use nearer overnight
     */
    public boolean trackBeginLater(int day) {
        int dBegin = hikingDays[day].tripBegin.stationNr;
        int dEnd = hikingDays[day].tripEnd.stationNr;
        // at least 2 stages so we can drop one
        // and we are not at the beginning
        if (((dEnd - dBegin) > 1) && (day > 1)) {
            hikingDays[day].tripBegin.stationNr++;
            hikingDays[day].recalcDistance();
            hikingDays[day - 1].recalcDistance();
            return true;
        } else return false;
    }


    /**
     * Undo all shift operations since last commit.
     */
    public void undoShift() {
        undoShift(1, days);
    }

    /**
     * Save all shift operations to the undo backup buffer.
     * The next undoShift() will restore exactly this state.
     */
    public void commitShift() {
        commitShift(1, days);
    }

    /**
     * Same as undoShift(), but operates only within a sub tour
     * @param fromDay Sub tour begin
     * @param toDay Sub tour end
     */
    public void undoShift(int fromDay, int toDay) {
        hikingDays[fromDay].tripBegin.stationNr = hikingDaysUndo[fromDay].tripBegin.stationNr;
        for (int i = fromDay; i <= toDay; i++) {
            hikingDays[i].tripEnd.stationNr = hikingDaysUndo[i].tripEnd.stationNr;
            hikingDays[i].tripDistance = hikingDaysUndo[i].tripDistance;
        }
    }

    /**
     * Same as commitShift(), but operates only within a sub tour
     * @param fromDay Sub tour begin
     * @param toDay Sub tour end
     */
    public void commitShift(int fromDay, int toDay) {
        hikingDaysUndo[fromDay].tripBegin.stationNr = hikingDays[fromDay].tripBegin.stationNr;
        for (int i = fromDay; i <= toDay; i++) {
            hikingDaysUndo[i].tripEnd.stationNr = hikingDays[i].tripEnd.stationNr;
            hikingDaysUndo[i].tripDistance = hikingDays[i].tripDistance;
        }
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        for (int day = 1; day <= days; day++) {
            txt.append(String.format("%d. Tag: %d km\n", day, getDistance(day)));
        }
        txt.append("\n");
        txt.append(String.format("Maximum: %d km\n", getLongestDailyTrack()));
        return txt.toString();
    }
}
