#!/usr/bin/bash

set -o errexit # -e does not work in Shebang-line!
set -o pipefail
set -o nounset
set -eE -o functrace

failure() {

  local lineno=$1
  local msg=$2
  echo "Failed at $lineno: $msg"
}
trap 'failure ${LINENO} "$BASH_COMMAND"' ERR

prefixes=$(sparql --results=TSV --data=prefixes.ttl "PREFIX sh:<http://www.w3.org/ns/shacl#> SELECT ?s WHERE {?pn sh:prefix ?prefix ; sh:namespace ?namespaceI . BIND(CONCAT('PREFIX ',?prefix, ':<',(STR(?namespaceI)),'>') AS ?s)}"|grep -v "^\?s$" |tr -d '"')
echo "Prefixes found"

project="*"
while getopts nuhrsgmcbop: option; do
    case "$option" in
        p) project="$OPTARG";;
        u) project="uniprot";;
        h) project="hamap";;
        r) project="rhea";;
        s) project="swisslipids";;
        g) project="glyconnect";;
        m) project="metanetx";;
        c) project="covid";;
        b) project="bgee";;
	    n) project="nextprot";;
        o) project="oma";;
        h) help; exit 0;;
        *) help; exit 1;;
    esac
done

for p in $(ls -d $project);
do
	if [ ! -f "$p" ]
	then
	    project_prefixes=""
	    if [ -f $p/prefixes.ttl ]
	    then
		    project_prefixes=$(sparql --results=TSV --data=$p/prefixes.ttl "PREFIX sh:<http://www.w3.org/ns/shacl#> SELECT ?s WHERE {?pn sh:prefix ?prefix ; sh:namespace ?namespaceI . BIND(CONCAT('PREFIX ',?prefix, ':<',(STR(?namespaceI)),'>') AS ?s)}"|grep -v "^\?s$" |tr -d '"')
		    echo "Found project specific prefixes"
	    fi
	    for i in $(ls -t $p/*.ttl);
	    do
		    if [[ "$i" != "$p/prefixes.ttl" ]]
		    then
		        echo "Checking $i"
		        f=$(echo $i | cut -f 2 -d '/' )
		        if [ $(grep -c "ex:${f:0:${#f}-4}" $i) -lt 1 ];
		        then
        		    echo "$i is NOT ok"
			        exit 4;
		        fi;
		        if [ $(rapper -q -i turtle -c $i) ];
		    then
			    echo "$i is NOT ok"
    			exit 2;
	    	fi
		    q=$(sparql --results=TSV --data=$i "PREFIX sh:<http://www.w3.org/ns/shacl#> SELECT ?qs WHERE {?q sh:select|sh:describe|sh:construct|sh:ask ?qs}"|grep -vP "^\?qs$");
    		pq="${q:1:${#q}-2}";
	    	if [[ ! -z "$pq" ]]
		    then
        	    	query="$prefixes $project_prefixes $pq"
		            if [[ ! $(echo -e $query | sed 's|\\"|"|g' | qparse --strict --query=/dev/stdin) ]]
    		        then
	    			echo "$i is NOT ok"
		    		echo -e "__${pq}__"
			    	exit 3;
	        	    fi
    		fi
	    	echo "$i is ok"
		fi
	done
    fi
done
