#!/bin/bash

get_millis() {
    local s=$(date +%s)
    local n=$(python -c 'import time; print(int(time.time() * 1000))')
    echo $((s*1000 + n%1000))
}

echo "Starting the experiment script..."

# Compile Java program
echo "Compiling the Java program..."
javac Main1.java

# Set array size
array_size=$((2**23))
echo "Array size set to: $array_size"

# Initialize file for results
results_file="experiment_results.csv"
echo "Initializing results file: $results_file"
echo "Thread Count,Run 1,Run 2,Run 3,Run 4,Run 5,Average" > $results_file

# Loop through thread counts
for thread_count in 1 2 4 8 16 32 64 128 256 512 1024; do
    echo "-------------------------------------------"
    echo "Running experiments for thread count: $thread_count"

    # Prepare input for Java program
    input="$array_size\n$thread_count\n"

    # Run the program to allow OS to cache memory locations
    echo "Warming up the cache..."
    for warmup in {1..3}; do
        echo -n "Warm-up run $warmup..."
        start_warmup_time=$(get_millis)
        echo -e $input | java Main > /dev/null 2>&1
        end_warmup_time=$(get_millis)
        warmup_time=$((end_warmup_time - start_warmup_time))
        echo " Completed in $warmup_time ms."
    done
    echo "Cache warmup completed."

    # Run the program 5 times and record runtimes
    echo "Running timed experiments..."
    runtimes=()
    for run in {1..5}; do
        echo -n "Run $run..."
        start_time=$(get_millis)
        echo -e $input | java Main > /dev/null 2>&1
        end_time=$(get_millis)
        runtime=$((end_time - start_time))
        echo " Runtime: $runtime ms"
        runtimes+=($runtime)
        echo -n "$runtime," >> $results_file
    done

    # Calculate average runtime
    echo "Calculating average runtime..."
    total=0
    for runtime in "${runtimes[@]}"; do
        let total+=$runtime
    done
    average=$(echo "$total / 5" | bc -l)
    echo "Average Runtime: $average ms"

    # Save results
    echo "$average" >> $results_file
    echo "Results for thread count $thread_count saved."
done

echo "-------------------------------------------"
echo "All experiments completed. Results saved in $results_file"

# # Generate graph using Gnuplot
# echo "Generating graph using Gnuplot..."
# gnuplot -persist <<-EOFMarker
#     set title "Runtime vs Thread Count"
#     set xlabel "Thread Count"
#     set ylabel "Average Runtime (ms)"
#     set datafile separator ","
#     set terminal png
#     set output "runtime_vs_thread_count.png"
#     plot "$results_file" using 1:7 with linespoints title 'Average Runtime'
# EOFMarker

# echo "Graph generation completed."
# echo "Graph saved as: runtime_vs_thread_count.png"
echo "Experiment script finished."
