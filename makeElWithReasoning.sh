#!/bin/sh
cd `dirname $0`
export tempcp=elvira.jar
for i in lib/*jar; do tempcp="$tempcp:$i"; done
java -Xmx2048M -Xms250M \
     -cp $tempcp \
    de.bioonto.elvira.MakeElWithReasoning $1 $2 $3 $4 $5 $6 $7 $8 $9