#!/usr/bin/env bash

task=$1
producer_data=$2
producer_interval=$3
producer_chunk=$4
transfer_buffer=$5
consumer_interval=$6
consumer_buffer=$7
repeat=${8-1}

# C + max(Pd / Pc * Pi, Pd / min(Pc, Tb) * max(1, min(Pc, Tb) / Cb) * Ci)

echo "Submitted $repeat $task $producer_data $producer_interval $producer_chunk $transfer_buffer $consumer_interval $consumer_buffer"
# We explicitly set stdin to /dev/null because otherwise it captures stdin from start_tasks.sh
adb shell am broadcast \
        -a com.brufino.android.START_TRANSFER \
        --es task ${task} \
        --ei producer_data $(($producer_data * 1024)) \
        --ei producer_interval ${producer_interval} \
        --ei producer_chunk $(($producer_chunk * 1024)) \
        --ei transfer_buffer $(($transfer_buffer * 1024)) \
        --ei consumer_interval ${consumer_interval} \
        --ei consumer_buffer $(($consumer_buffer * 1024)) \
        --ei repeat ${repeat} \
        com.brufino.android.playground \
        < /dev/null \
        > /dev/null
exit $?
