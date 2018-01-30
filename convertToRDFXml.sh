#!/usr/bin/bash
riot --formatted=rdfxml *.ttl | xz > examples.rdf.xz

