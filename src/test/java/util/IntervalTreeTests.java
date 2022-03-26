package util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntervalTreeTests {

    @Test
    public void basicIntersectAndRemoveTest() {
        IntervalTree it = new IntervalTree();

        it.add(17,19);
        it.add(21,24);
        it.add(5,8);
        it.add(4,8);
        it.add(15,18);
        it.add(7,10);
        it.add(16,22);
        it.printTree();

        assertTrue(it.intersects(21,23));

        it.remove(16,22);
        it.remove(21,24);
        it.printTree();

        assertFalse(it.intersects(21,23));
    }

    @Test
    public void insertStressTest() {
        int lowerBound = 0;
        int upperBound = 50;
        int cycles = 20;
        IntervalTree it = new IntervalTree();

        for (int i = 0; i < cycles; i++) {
            int lo = ThreadLocalRandom.current().nextInt(lowerBound,upperBound);
            int hi = lo + ThreadLocalRandom.current().nextInt(lowerBound,upperBound);
            System.out.println("inserting:" + lo + "-" + hi);
            it.add(lo, hi);
            assertTrue(it.testTreeCorrectness());
        }
        System.out.println("Final tree structure:");
        it.printTree();

    }

    /**
     * Adds a certain number of interval to the interval tree and then removes them in random order.
     * The tree correctness is checked after each add/remove operation.
     */
    @Test
    public void insertAndRemoveStressTest() {

        class Interval {
            final int lo;
            final int hi;

            public Interval(int lo, int hi) {
                this.lo = lo;
                this.hi = hi;
            }
        }

        int lowerBound = 0;
        int upperBound = 5000;
        int cycles = 1000;

        ArrayList<Interval> insertedIntervals = new ArrayList<>();
        IntervalTree it = new IntervalTree();

        for (int i = 0; i < cycles; i++) {
            int lo = ThreadLocalRandom.current().nextInt(lowerBound,upperBound);
            int hi = lo + ThreadLocalRandom.current().nextInt(lowerBound,upperBound);
            System.out.println("Inserting :" + lo + "-" + hi);
            insertedIntervals.add(new Interval(lo,hi));
            it.add(lo, hi);
        }
        System.out.println("Number of inserted intervals: " + insertedIntervals.size());
        assertTrue(it.testTreeCorrectness());

        for (int i = 0; i < cycles; i++) {
            int nextIndex = ThreadLocalRandom.current().nextInt(0, insertedIntervals.size());
            Interval curInterval = insertedIntervals.get(nextIndex);
            insertedIntervals.remove(nextIndex);
            System.out.println("Removing : " + curInterval.lo + "-" + curInterval.hi);
            it.remove(curInterval.lo, curInterval.hi);
            assertTrue(it.testTreeCorrectness());
        }

        it.printTree();
        assertTrue(insertedIntervals.isEmpty());
        assertFalse(it.intersects(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void doubleChildDeleteTest() {
        IntervalTree it = new IntervalTree();

        it.add(20,50);
        it.add(10,12);
        it.add(5,8);
        it.add(2,14);
        it.add(8,21);
        it.add(15,17);
        it.add(13,15);
        it.add(17,20);

        it.printTree();
        assertTrue(it.testTreeCorrectness());

        it.remove(10,12);
        it.printTree();
        assertTrue(it.testTreeCorrectness());
    }

    @Test
    public void sameIntervalInsertAndRemoveTest() {
        IntervalTree it = new IntervalTree();

        it.add(20,50);
        it.add(20,50);
        it.add(10,30);
        it.add(25,45);
        it.printTree();

        it.remove(20,50);

        it.printTree();
        assertTrue(it.testTreeCorrectness());

    }

}
