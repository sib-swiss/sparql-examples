#!/usr/bin/bash



project="uniprot"
while getopts anuhrsgmcbop: option; do
    case "$option" in
        a) project="all";;
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
pattern="$project"
case "$project" in
    trembl)
        ;&
    swiss-prot)
        if [ ! -L "${project}" ]
        then
            ln -s uniprot "${project}"
        fi
        ;;
    all)
        pattern='*';;
esac

if [ -f examples_${project}.ttl ]
then
    rm examples_${project}.ttl
fi

if which riot
then
  cat examples/prefixes.ttl examples/$pattern/*.ttl | riot --syntax=turtle --formatted=turtle > examples_${project}.ttl
else
  rapper -q -i turtle <(cat examples/prefixes.ttl examples/${pattern}/*.ttl) -o turtle  > examples_${project}.ttl
fi
