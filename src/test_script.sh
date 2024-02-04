#!/bin/bash

echo "Starting the experiment script..."

# Compile Java program
echo "Compiling the Java program..."
javac Main.java  # Ensure the filename is correct

# Set array size
array_size=$((2**23))
echo "Array size set to: $array_size"

# Initialize file for results
results_file="experiment_results.csv"
echo "Initializing results file: $results_file"
echo "Thread Count,Run 1,Run 2,Run 3,Run 4,Run 5,Average" > $results_file

# Loop through thread counts
for thread_count in 1 2 4 8 16 32 64 128 256 512 1024 2048; do
    echo "-------------------------------------------"
    echo "Running experiments for thread count: $thread_count"

    # Prepare input for Java program
    input="$array_size\n$thread_count"

    # Warm-up runs
    echo "Warming up the cache..."
    for warmup in {1..3}; do
        echo -n "Warm-up run $warmup..."
        echo -e "$input" | java Main > /dev/null 2>&1
        echo " Completed."
    done
    echo "Cache warmup completed."

    # Run the program 5 times and record runtimes
    echo "Running timed experiments..."
    runtimes=()
    for run in {1..5}; do
        echo -n "Run $run..."
        runtime_output=$(echo -e "$input" | java Main | grep "Runtime:" | awk '{print $2}')
        echo " Runtime: $runtime_output ms"
        runtimes+=($runtime_output)
        echo -n "$runtime_output," >> $results_file
    done

    # Calculate average runtime
    echo "Calculating average runtime..."
    total=0
    for runtime in "${runtimes[@]}"; do
        total=$(echo "$total + $runtime" | bc)
    done
    average=$(echo "scale=2; $total / 5" | bc)
    echo "Average Runtime: $average ms"

    # Save results
    echo "$thread_count,$average" >> $results_file
    echo "Results for thread count $thread_count saved."
done

echo "-------------------------------------------"
echo "All experiments completed. Results saved in $results_file"

# Generate graph using Gnuplot
echo "Generating graph using Gnuplot..."
gnuplot -persist <<-EOFMarker
    set title "Runtime vs Thread Count"
    set xlabel "Thread Count"
    set ylabel "Average Runtime (ms)"
    set datafile separator ","
    set terminal png
    set output "runtime_vs_thread_count.png"
    plot "<(tail -n +2 $results_file)" using 1:7 with linespoints title 'Average Runtime'
EOFMarker

echo "Graph generation completed."
echo "Graph saved as: runtime_vs_thread_count.png"
echo "Experiment script finished."
