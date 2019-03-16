#!/usr/bin/env bash

SOURCE="${BASH_SOURCE[0]}"
while [[ -h "$SOURCE" ]]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

while read line
do
    if [[ "$line" = "clear" ]]; then
        adb shell am broadcast \
            -a com.brufino.android.CLEAR_HISTORY \
            com.brufino\.android.playground \
            < /dev/null \
            > /dev/null
    elif [[ ! "$line" == \#* && ! -z "$line" ]]; then
        ${DIR}/start_task.sh ${line}
    fi
done < $1
