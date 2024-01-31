import java.util.*;
import java.util.concurrent.*;

public class Main1 {
    public static List<Interval> intervals;
    public static ConcurrentHashMap<Interval, Boolean> mergedIntervals = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // TODO: Seed your randomizer
        Random rand = new Random(123);

        // TODO: Get array size and thread count from user
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter array size: ");
        int array_size = sc.nextInt();
        System.out.print("Enter thread count: ");
        int thread_count = sc.nextInt();

        if (thread_count <= 0 || (thread_count & (thread_count - 1)) != 0) {
            System.out.println("Thread count must be a power of 2 and greater than 0.");
            System.exit(1);
        }

        // TODO: Generate a random array of given size
        int[] array = new int[array_size];
        for (int i = 0; i < array_size; i++) {
            array[i] = i + 1;
        }

        // Shuffle the array
        for (int i = array_size - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Swap array[i] and array[index]
            int temp = array[i];
            array[i] = array[index];
            array[index] = temp;
        }

        /*TEMP DISPLAY SHUFFLED ARRAY
        System.out.println("SHUFFLED ARRAY:");
        for (int i = 0; i < array_size; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("\n");*/

        // TODO: Call the generate_intervals method to generate the merge sequence
        List<Interval> intervals = generate_intervals(0, array_size - 1);

        // TODO: Call merge on each interval in sequence
        long startTime;

        // if thread count is 1
        if (thread_count == 1) {
            // Record the start time
            startTime = System.currentTimeMillis();

            for (Interval interval : intervals) {
                merge(array, interval);
            }
        } else {
            // Record the start time
            startTime = System.currentTimeMillis();

            ForkJoinPool pool = new ForkJoinPool(thread_count);
            pool.invoke(new MergeTask(array, intervals, 0, intervals.size() - 1));

            pool.shutdown();
        }

        // Record the end time
        long endTime = System.currentTimeMillis();

        /*TEMP DISPLAY SORTED ARRAY
        System.out.println("\nSORTED ARRAY:");
        for (int i = 0; i < array_size; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("\n");

         */
        // SANITY CHECK
        
        System.out.println("\nRuntime: " + (endTime - startTime) + " milliseconds");

        System.out.println("Running Sanity Check...");

        for (int i = 0; i < array_size; i++) {
            if (i != array_size - 1 && array[i] != i + 1) {
                System.out.println("Array is not sorted.");
                break;
            }
            else if (i == array_size - 1)
                System.out.println("Array is sorted.");
        }

        sc.close();

        // Once you get the single-threaded version to work, it's time to
        // implement the concurrent version. Good luck :)
    }

    /*
    This function generates all the intervals for merge sort iteratively, given
    the range of indices to sort. Algorithm runs in O(n).

    Parameters:
    start : int - start of range
    end : int - end of range (inclusive)

    Returns a list of Interval objects indicating the ranges for merge sort.
    */
    public static List<Interval> generate_intervals(int start, int end) {
        List<Interval> frontier = new ArrayList<>();
        frontier.add(new Interval(start, end));

        int i = 0;
        while (i < frontier.size()) {
            int s = frontier.get(i).getStart();
            int e = frontier.get(i).getEnd();
            Interval parent = frontier.get(i);

            i++;

            // if base case
            if (s == e) {
                continue;
            }

            // compute midpoint
            int m = s + (e - s) / 2;

            // add prerequisite intervals
            Interval leftChild = new Interval(s, m);
            Interval rightChild = new Interval(m + 1, e);
            parent.addDependency(leftChild);
            parent.addDependency(rightChild);

            frontier.add(leftChild);
            frontier.add(rightChild);
        }

        List<Interval> retval = new ArrayList<>();
        for (i = frontier.size() - 1; i >= 0; i--) {
            retval.add(frontier.get(i));
        }

        return retval;
    }

    /*
   This function performs the merge operation of merge sort.

   Parameters:
   array : vector<int> - array to sort
   s     : int         - start index of merge
   e     : int         - end index (inclusive) of merge
   */
    public static void merge(int[] array, Interval interval) {
        int s = interval.getStart();
        int e = interval.getEnd();

        int m = s + (e - s) / 2;
        int[] left = new int[m - s + 1];
        int[] right = new int[e - m];
        int l_ptr = 0, r_ptr = 0;
        for (int i = s; i <= e; i++) {
            if (i <= m) {
                left[l_ptr++] = array[i];
            } else {
                right[r_ptr++] = array[i];
            }
        }
        l_ptr = r_ptr = 0;

        for (int i = s; i <= e; i++) {
            // no more elements on left half
            if (l_ptr == m - s + 1) {
                array[i] = right[r_ptr];
                r_ptr++;

                // no more elements on right half or left element comes first
            } else if (r_ptr == e - m || left[l_ptr] <= right[r_ptr]) {
                array[i] = left[l_ptr];
                l_ptr++;
            } else {
                array[i] = right[r_ptr];
                r_ptr++;
            }
        }

        mergedIntervals.put(interval, true);
    }

    static class MergeTask extends RecursiveAction {
        private final int[] array;
        private final List<Interval> intervals;
        private final int start;
        private final int end;

        public MergeTask(int[] array, List<Interval> intervals, int start, int end) {
            this.array = array;
            this.intervals = intervals;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (start < end) {
                int mid = start + (end - start) / 2;

                MergeTask leftTask = new MergeTask(array, intervals, start, mid);
                MergeTask rightTask = new MergeTask(array, intervals, mid + 1, end);

                invokeAll(leftTask, rightTask);

                merge(array, intervals.get(mid));
            }
        }
    }

    static class Interval {
        private int start;
        private int end;

        private List<Interval> dependencies;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
            this.dependencies = new ArrayList<>();
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public void addDependency(Interval interval) {
            this.dependencies.add(interval);
        }

        public List<Interval> getDependencies() {
            return dependencies;
        }
    }
}

