package com.cipsoft.candidate;

/**
 * This algorithm uses the first solution of TrackPlan and
 * evolutionarily optimizes the distribution of the daily trips on the tour
 * in order to minimize the maximum day trip.
 */
public class HeuristicOptimizer {

    /** The data structure representing the tour */
    public TrackPlan trackPlan = null;

    /**
     * Initializes the algorithm
     * @param availableOvernightDistances Distances between available overnights
     * @param days The days the tour shall be finished in
     */
    public HeuristicOptimizer(int[] availableOvernightDistances, int days) {
        trackPlan = new TrackPlan(availableOvernightDistances, days);
    }

    /**
     * This is the optimization algorithm.
     * First it looks for the farthest day trip. The pivot day.
     * Then depending on its position on the tour and relative to the last pivot day,
     * the day gets reduced by its start or end point.
     * Of course this affects the previous or following day. If the adjacent day trip rises
     * farther than the last trip of the pivot day it will be reduced at the cost of the next adjacent day.
     * This process continues until all day trips are shorter than the maximum distance of the pivot day,
     * or until one end of the tour will be reached.
     * The same procedure is repeated going in the opposite direction.
     * The whole process repeats until one pivot day cannot be further reduced.
     */
    public void Optimize() {
        int lastDay = 0;      // last pivot day
        boolean redForward;   // successfully end point; algorithm traveling forwards
        boolean redBackward;  // successfully start point; algorithm traveling backwards
        do {
            redForward = false;
            redBackward = false;
            // find the day with the longest daily trip
            int pivotDay = trackPlan.getLongestDay();

            // this is the first day; so there is only one direction to walk through
            if (pivotDay == 1) {
                redForward = reduceForwardDirection(pivotDay);
                if (redForward)
                    trackPlan.commitShift();  // The optimization was successful
                else
                    trackPlan.undoShift();  // The optimization made things worse, so rollback to the last state

            // or the last day
            } else if (pivotDay == trackPlan.days) {
                redBackward = reduceBackwardDirection(pivotDay);
                if (redBackward)
                    trackPlan.commitShift();
                else
                    trackPlan.undoShift();

            // pivot day is inside the tour
            } else {
                // This is the first time the algorithm starts, or the last pivot day is still the current one
                if ((lastDay == 0) || (lastDay == pivotDay)) {
                    // choose the direction with more days first
                    if ((double) pivotDay > trackPlan.days / 2) {
                        redBackward = reduceBackwardDirection(pivotDay);
                        if (redBackward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();

                        redForward = reduceForwardDirection(pivotDay);
                        if (redForward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();

                    } else {
                        redForward = reduceForwardDirection(pivotDay);
                        if (redForward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();

                        redBackward = reduceBackwardDirection(pivotDay);
                        if (redBackward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();
                    }
                // the last pivot day is a different one than the current
                // the algorithm chooses the direction "away" from the last pivot day first,
                // so any previous optimization will not be ruined
                } else {
                    if (pivotDay > lastDay) {
                        redForward = reduceForwardDirection(pivotDay);
                        if (redForward) {
                            trackPlan.commitShift();
                        } else {
                            trackPlan.undoShift();
                        }

                        redBackward = reduceBackwardDirection(pivotDay);
                        if (redBackward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();

                    } else {
                        redBackward = reduceBackwardDirection(pivotDay);
                        if (redBackward)
                            trackPlan.commitShift();
                        else
                            trackPlan.undoShift();

                        redForward = reduceForwardDirection(pivotDay);
                        if (redForward) {
                            trackPlan.commitShift();
                        } else {
                            trackPlan.undoShift();
                        }

                    } // pivotDay < lastDay
                } // not first loop, not day = pivotDay
            }

            lastDay = pivotDay;
            // abort if we can not improve the situation either forwards or backwards
        } while (redForward || redBackward);
        /// commit /undo
    }

    /**
     * Reduces the day trip by shifting the end points backwards.
     * @param pivotDay The day with the longest trip
     * @return True: Optimization successful; False: otherwise
     */
    protected boolean reduceBackwardDirection(int pivotDay) {
        int distDay;
        // find longest day's distance
        int distPivotDay = trackPlan.getDistance(pivotDay);
        // begin tour one stage later (sleep one stage later on the previous day)
        // if this affects the previous days negatively,
        // reduce their distance as well
        for (int day = pivotDay; day > 1; day--) {
            do {
                if (!trackPlan.trackBeginLater(day)) return false;
                distDay = trackPlan.getDistance(day);
                // The pivot day gets reduced by only one step
                // in order to check the forward direction first
                // before reducing further.
                // Abort as soon as there is a better solution than before
            } while ((day != pivotDay) && (distDay >= distPivotDay));

            // longest track is reduced without creating a higher maximum
            if (Math.max(trackPlan.getDistance(day - 1), distDay) < distPivotDay)
                return true;
        }
        // reached the tour start, there is no previous day,
        // hence, this approach failed
        return false;
    }

    /**
     * Reduces the day trip by shifting the start points forwards.
     * @param pivotDay The day with the longest trip
     * @return True: Optimization successful; False: otherwise
     */
    protected boolean reduceForwardDirection(int pivotDay) {
        int distDay;
        // find longest day's distance
        int distPivotDay = trackPlan.getDistance(pivotDay);
        // sleep one stage earlier.
        // if this affects the following days negatively,
        // reduce their distance as well
        for (int day = pivotDay; day < trackPlan.days; day++) {
            do {
                if (!trackPlan.trackEndEarlier(day)) return false;
                distDay = trackPlan.getDistance(day);
                // the pivot day gets reduced by only one step
                // in order to check the Backward direction first
                // before reducing further
                // abort as soon as there is a better solution than before
            } while ((day != pivotDay) && (distDay >= distPivotDay));

            // longest track is reduced without creating a higher maximum
            if (Math.max(trackPlan.getDistance(day + 1), distDay) < distPivotDay)
                return true;
        }
        // reached the end, there is no following day,
        // hence, this approach failed
        return false;
    }

    @Override
    public String toString() {
        return trackPlan.toString();
    }
}
