package de.bioonto.elvira

import java.util.logging.Logger
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.*
import de.tudresden.inf.lat.jcel.owlapi.main.*
import org.semanticweb.owlapi.profiles.*
import org.semanticweb.owlapi.util.*
import com.clarkparsia.pellet.owlapiv3.*
import org.mindswap.pellet.KnowledgeBase
import org.mindswap.pellet.expressivity.*
import org.mindswap.pellet.*
import org.semanticweb.HermiT.Reasoner
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory
import de.tudresden.inf.lat.jcel.owlapi.main.*
import de.tudresden.inf.lat.cel.owlapi.*

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  i longOpt:'input', 'input file', args:1, required:true
  o longOpt:'output', 'output file',args:1, required:true
  r longOpt:'reasoner', 'reasoner to use (0 for Pellet, 1 for Hermit, 2 for JCel, 3 for CEL, Default: 0)',args:1
  v longOpt:'verbose', 'prints progress of OWL reasoning'
}
def opt = cli.parse(args)
if( !opt ) {
  //  cli.usage()
  return
}
if( opt.h ) {
    cli.usage()
    return
}

def prof = null
if (opt.p) {
  println "Pellet-compatible conversion"
  prof = new PelletOWL2ELProfile()
} else {
  println "Standard conversion"
  prof = new OWL2ELProfile()
}

def diri = new File(opt.i) // infile
//def diri = IRI.create(args[0]) // infile
File outfile = new File(opt.o) // outfile

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
manager.addIRIMapper(new NonMappingOntologyIRIMapper())
manager.setSilentMissingImportsHandling(true)

OWLOntology ont = manager.loadOntologyFromOntologyDocument(diri)


OWLDataFactory fac = manager.getOWLDataFactory()

OWLReasonerFactory reasonerFactory = null

OWLOntology infOnt = manager.createOntology()

if (opt.r == "0" || !opt.r) {
  println "Using Pellet reasoner"
  reasonerFactory = new PelletReasonerFactory()
} else if (opt.r == "1") {
  println "Using Hermit reasoner"
  reasonerFactory = new Reasoner.ReasonerFactory()
}
//JcelReasoner reasoner = new JcelReasoner(ont)

// /* Copy every asserted axiom which is in EL to the new ontology. This is
//    necessary to retain annotation axioms, which are not inferred by the
//    reasoner and would otherwise be lost. */
def report = prof.checkOntology(ont)
println "Input ontology is in EL: "+report.isInProfile()
def viol = report.getViolations()
def ignoreSet = new TreeSet()
viol.each { 
  if (it.getAxiom()!=null) {
    ignoreSet.add(it.getAxiom())
  }
}
def s = ont.getAxioms()
s.each {
  if (
    (! ignoreSet.contains(it))
  ) {
    manager.addAxiom(infOnt,it)
  }
}

/* Use reasoner to add inferred axioms */
ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
OWLReasoner reasoner = null

if (! ((opt.r == "2") || (opt.r == "3"))) {
  if (opt.v) { // verbose
    reasoner = reasonerFactory.createNonBufferingReasoner(ont, config)
  } else {
    reasoner = reasonerFactory.createNonBufferingReasoner(ont)
  }
} else if (opt.r == "2") {
  reasoner = new JcelReasoner(ont)
} else {
  reasoner = new CelReasoner(ont)
}

List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>()
gens.add(new InferredSubClassAxiomGenerator())
gens.add(new InferredDisjointClassesAxiomGenerator())
gens.add(new InferredEquivalentClassAxiomGenerator())
gens.add(new InferredEquivalentObjectPropertyAxiomGenerator())
gens.add(new InferredClassAssertionAxiomGenerator())
gens.add(new InferredInverseObjectPropertiesAxiomGenerator())
gens.add(new InferredObjectPropertyCharacteristicAxiomGenerator())
gens.add(new InferredPropertyAssertionGenerator())
gens.add(new InferredSubObjectPropertyAxiomGenerator())

InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, gens)
iog.addGenerator(new InferredDisjointClassesAxiomGenerator())
println iog.getAxiomGenerators()
iog.fillOntology(manager, infOnt)
manager.saveOntology(infOnt, IRI.create(outfile.toURI()))
