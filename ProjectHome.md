El Vira uses OWL reasoning to convert ontologies from any OWL expressivity into [OWL EL, OWL  QL or OWL RL](http://www.w3.org/TR/owl2-profiles/).

EL, QL and RL are subsets of OWL in which decisions for most reasoning tasks can be decided in polynomial time. Reasoners for these profiles therefore allow inferences over much larger ontologies and knowledge bases than general OWL reasoners.

El Vira supports conversion of OWL to OWL EL, QL and RL, while providing the flexibility to remove various language features (such as datatype or annotation properties) so that reasoners like Pellet recognize the ontologies as EL.

A repository of OBO ontologies converted to EL using EL Vira is available at http://bioonto.dcs.aber.ac.uk/el-ont, in RL at http://bioonto.dcs.aber.ac.uk/rl-ont and in QL at http://bioonto.dcs.aber.ac.uk/ql-ont.

El Vira implements an ontology modularization approach that restricts language expressivity without changing the signature of an ontology. It uses an OWL reasoner to generate the deductive closure of an OWL theory and selects statements in the desired language expressivity from the deductive closure. Therefore, some inferences (in particular taxonomic inferences) are maintained in the EL Vira approach to ontology modularization.

El Vira is published in Bioinformatics:

> Hoehndorf, Robert, Dumontier, Michel, Oellrich, Anika, Wimalaratne, Sarala, Rebholz-Schuhmann, Dietrich, Schofield, Paul, and Gkoutos, Georgios V. (2011). [A common layer of interoperability for biomedical ontologies based on OWL EL](http://www.ncbi.nlm.nih.gov/pubmed/21343142). Bioinformatics, 27(7):1001-8.