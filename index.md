# SIB SPARQL examples

We collect the SPARQL examples in different formats,
for different projects of the SIB Swiss Institute of Bioinformatics where we
have a public SPARQL endpoint.

In this github pages we have a HTML rendering for all them.

{% assign active_resources = site.data.resources | where_exp: "r", "r.category != 'deprecated'" -%}
<ul class="resource-grid">
{%- for r in active_resources %}
  <li class="category-{{ r.category }}"><a href="./examples/{{ r.path }}/">{{ r.name }}</a></li>
{%- endfor %}
</ul>

{% assign used_categories = active_resources | map: "category" | uniq -%}
<ul class="resource-grid-legend">
{%- for slug in used_categories %}
  <li class="resource-grid-legend__item category-{{ slug }}">{{ site.data.categories[slug].label }}</li>
{%- endfor %}
</ul>

We also collect [some basic statistics on the different SPARQL features in use](./examples/algebra-statistics.md)

{% assign deprecated_resources = site.data.resources | where_exp: "r", "r.category == 'deprecated'" -%}
{% if deprecated_resources.size > 0 %}
## Deprecated resources

These SPARQL endpoints are no longer actively maintained; their examples are kept here for reference.

<ul class="resource-grid resource-grid--deprecated">
{%- for r in deprecated_resources %}
  <li class="category-{{ r.category }}"><a href="./examples/{{ r.path }}/">{{ r.name }}</a></li>
{%- endfor %}
</ul>
{% endif %}
