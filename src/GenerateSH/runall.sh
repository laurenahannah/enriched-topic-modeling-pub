#!/bin/bash

scripts=($(find -maxdepth 1 -not -type d))
num_script=${#scripts[@]}
pids=()

#printf '%s\n' "${ARRAY[@]}"

TMP=$(mktemp -u XXXXXXXX)

mkdir -p /tmp/raylin.log

for (( i=0; i<${num_script}; i++));
do
  ${scripts[$i]} > /tmp/raylin.log/$(basename ${scripts[$i]}).${TMP} 2>&1 &
  pids+=($!)
done

num_pids=${#pids[@]}

num_finished=0

while [ $num_finished -ne $num_pids ]
do
  sleep 1
  for (( i=0; i<${num_pids}; i++));
  do
    #echo "pids $i = ${pids[$i]}"
    if [ ${pids[$i]} -ne 0 ]
    then
      kill -0 ${pids[$i]} 2> /dev/null
      if [ $? -ne 0 ]
      then
        wait ${pids[$i]}
        echo "${scripts[$i]} returned $?"
        pids[$i]=0
        let "num_finished += 1"
      fi
    fi
  done
done

echo "done"
                                                                                           