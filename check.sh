#!/usr/bin/bash

set -o errexit # -e does not work in Shebang-line!
set -o pipefail
set -o nounset

for i in $(ls [1-9]*.ttl);
do 
    if [ $(grep -c "ex:${i:0:${#i}-4}" $i) -lt 1 ];
    then 
        echo $i;
        exit 1;
    fi;
done
