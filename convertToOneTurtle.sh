#!/usr/bin/bash



project="uniprot"
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
        n) project="nextprot";;
        b) project="bgee";;
        o) project="oma";;
        h) help; exit 0;;
        *) help; exit 1;;
    esac
done
case "$project" in
    trembl)
        ;&
    swiss-prot)
        if [ ! -L "${project}" ]
        then
            ln -s uniprot "${project}"
        fi
        ;;
esac

if [ -f examples_${project}.ttl ]
then
    rm examples_${project}.ttl
fi

if which riot
then
  cat examples/prefixes.ttl examples/$project/*.ttl | riot --syntax=turtle --formatted=turtle > examples_${project}.ttl
else
  rapper -q -i turtle <(cat examples/prefixes.ttl examples/${project}/*.ttl) -o turtle  > examples_${project}.ttl
fi
