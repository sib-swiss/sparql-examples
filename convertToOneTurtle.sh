#!/usr/bin/bash

# To load the examples into a SPARQL endpoint they should be concatenated into one example file. Use the script `convertIntoOneTurtle.sh` provide the project name with a `-p` parameter
# This expects the Jena tools to be available in your $PATH. e.g. `export PATH="$JENA_HOME/bin:$PATH"`
# ./convertToOneTurtle.sh -p uniprot

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
            ln -s UniProt "${project}"
        fi
        ;;
    uniprot)
        project="uniprot";
        pattern="UniProt";;
    nextprot)
        pattern="neXtProt";;
    bgee)
        pattern="Bgee";;
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
