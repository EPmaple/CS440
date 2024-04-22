# !/bin/bash

start_rate=1
end_rate=15

for rate in $(seq $start_rate $end_rate); do
    echo "Iteration: $rate"
    java -cp "./lib/*:." edu.bu.battleship.Main --p1Agent src.pas.battleship.agents.ProbabilisticAgent --p2Agent edu.bu.battleship.agents.HardAgent --difficulty HARD -s
    wait
done



#!/bin/bash

# Set the number of runs
# NUM_RUNS=10
# WIN_COUNT=0

# # Run the Java command and count wins
# for ((i=1; i<=$NUM_RUNS; i++)); do
#     echo "Running loop $i/$NUM_RUNS"
#     OUTPUT=$(javac -cp "./lib/*:." @battleship.srcs)
#     OUTPUT=$(java -cp "./lib/*:." edu.bu.battleship.Main --p1Agent src.pas.battleship.agents.ProbabilisticAgent --p2Agent edu.bu.battleship.agents.HardAgent --difficulty HARD -s)
#     if [[ $OUTPUT == *"winner=player 1"* ]]; then
#         ((WIN_COUNT++))
#     fi
# done

# # Display results
# echo "Total runs: $NUM_RUNS"
# echo "Wins: $WIN_COUNT"

# Initialize variable to count wins
# wins=0

# # Run the commands 10 times
# for ((i = 1; i <= 10; i++)); do
#     # Compile Java files
#     javac -cp "./lib/*:." @battleship.srcs

#     # Run the Java program and capture the output
#     output=$(java -cp "./lib/*:." edu.bu.battleship.Main --p1Agent src.pas.battleship.agents.ProbabilisticAgent --p2Agent edu.bu.battleship.agents.HardAgent --difficulty HARD -s)

#     # Check if "winner=player 1" is present in the output
#     if [[ "$output" =~ "winner=player 1" ]]; then
#         ((wins++))  # Increment wins if player 1 wins
#     fi
# done

# # Print the number of wins
# echo "Number of wins: $wins"

