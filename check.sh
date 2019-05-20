#!/usr/bin/bash

set -o errexit # -e does not work in Shebang-line!
set -o pipefail
set -o nounset

for i in $(ls */[1-9]*.ttl);
do
    f=$(echo $i | cut -f 2 -d '/' )	
    if [ $(grep -c "ex:${f:0:${#f}-4}" $i) -lt 1 ];
    then 
        echo $i;
        exit 1;
    fi;
    if [ $(rapper -q -i turtle -c $i) ];
    then
	  echo $i;
	  exit 2;
    fi 
done
