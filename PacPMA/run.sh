#!/bin/bash
file=$1
property=$2
parameters=$3
constants=$4
call=(java -jar pacpma.jar -f $file -p "`echo $property`" --parameters $parameters --lpsolver matlab --seed 12345 --format matlab --log 4 --logfile pacpma.log --degree 2 --showrange)
if [ $constants ]; then
	call+=(--constants $constants)
fi
"${call[@]}"
