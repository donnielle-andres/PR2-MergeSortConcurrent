import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        // TODO: Seed your randomizer
        Random rand = new Random(123);

        // TODO: Get array size and thread count from user
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter array size: ");
        int array_size = sc.nextInt();
        System.out.print("Enter thread count: ");
        int thread_count = sc.nextInt();

        if (thread_count == 0) {
            System.out.println("Number of threads cannot be 0.");
            System.exit(1);
        }

        // TODO: Generate a random array of given size
        Integer[] integers = new Integer[array_size];
        for (int i = 0; i < array_size; i++) {
            integers[i] = i+1;
        }

        // Shuffle list and turn it into int array
        List<Integer> listArray = Arrays.asList(integers);
        Collections.shuffle(listArray, rand);
        int[] array = new int[array_size];
        for (int i = 0; i < array_size; i++) {
            array[i] = listArray.get(i);
        }


        //TEMP DISPLAY SHUFFLED ARRAY

        System.out.println("SHUFFLED ARRAY:");
        for (int i = 0; i < array_size; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("\n");



        // TODO: Call the generate_intervals method to generate the merge sequence
        List<Interval> intervals = generate_intervals(0, array_size-1);

        // TODO: Call merge on each interval in sequence
        long startTime;

        // if thread count is 1
        if (thread_count == 1) {
            // Record the start time
            startTime = System.currentTimeMillis();

            for (Interval interval : intervals) {
                merge(array, interval.getStart(), interval.getEnd());
            }
        }
        else {
            BlockingQueue<Interval> queue = new ArrayBlockingQueue<>(intervals.size());
            queue.addAll(intervals);

            ExecutorService executorService = Executors.newFixedThreadPool(thread_count);

            // Record the start time
            startTime = System.currentTimeMillis();

            for (int i = 0; i < thread_count; i++) {
                executorService.submit(new thread(array, queue));
            }
            executorService.shutdown();

            // Wait until all tasks are finished
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // Record the end time
        long endTime = System.currentTimeMillis();

        //TEMP DISPLAY SORTED ARRAY
        System.out.println("\nSORTED ARRAY:");
        for (int i = 0; i < array_size; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println("\n");

        System.out.println("\nRuntime: " + (endTime - startTime) + " milliseconds");


        // SANITY CHECK
        for (int i = 0; i < array_size; i++) {
            if (i != array_size-1 && array[i] != i+1) {
                System.out.println("Array is not sorted.");
                System.out.println("array[" + i + "] = " + array[i]);
            }
        }



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
        frontier.add(new Interval(start,end));

        int i = 0;
        while(i < frontier.size()){
            int s = frontier.get(i).getStart();
            int e = frontier.get(i).getEnd();

            i++;

            // if base case
            if(s == e){
                continue;
            }

            // compute midpoint
            int m = s + (e - s) / 2;

            // add prerequisite intervals
            frontier.add(new Interval(m + 1,e));
            frontier.add(new Interval(s,m));
        }

        List<Interval> retval = new ArrayList<>();
        for(i = frontier.size() - 1; i >= 0; i--) {
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
    public static void merge(int[] array, int s, int e) {
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
        /*
        System.out.println("Intervals: [" + s + ", " + e + "]");
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.print("\n");

         */
    }

    static class thread implements Runnable {
        private int[] array;
        private BlockingQueue<Interval> queue;

        public thread(int[] array, BlockingQueue<Interval> queue) {
            this.array = array;
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                Interval interval;
                do {
                    interval = queue.poll(1, TimeUnit.SECONDS);

                    if (interval != null)
                        merge(array, interval.getStart(), interval.getEnd());
                }
                while (interval != null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class Interval {
    private int start;
    private int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
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
}

