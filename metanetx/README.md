Examples extracted from rdf.metantex.org

```js
var examplesh2=
document.querySelectorAll('#mnx_view  ol > li');
for (var i=0;i< examplesh2.length;i++) {
    let q=examplesh2[i].getElementsByTagName('button')[0].attributes['onclick'].nodeValue; 
    console.log("prefix ex: <https://sparql.metanetx.org/.well-known/sparql-examples/>\nprefix sh: <http://www.w3.org/ns/shacl#>\nprefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nprefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\nprefix schema: <https://schema.org/>\nex:"+(i+1)+"\na sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;\n    sh:prefixes _:sparql_examples_prefixes, _:metanetx_sparql_examples_prefixes ;\n    schema:target <https://rdf.metanetx.org/sparql/> ;\n    rdfs:comment '''"+examplesh2[i].firstChild.data.trim()+"''' ; \n  sh:select '''"+q.substring(40, q.length -1).trim().replaceAll("\\n","\n")+"'''.");
}
```


