package de.bioonto.elvira

import java.util.logging.Logger
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.*
import de.tudresden.inf.lat.jcel.owlapi.main.*
import org.semanticweb.owlapi.profiles.*
import org.semanticweb.owlapi.util.*
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory
import org.mindswap.pellet.KnowledgeBase
import org.mindswap.pellet.expressivity.*
import org.mindswap.pellet.*
import org.semanticweb.HermiT.Reasoner
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory
import org.semanticweb.owlapi.io.*

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  i longOpt:'input', 'input file', args:1, required:true
  o longOpt:'output', 'output file',args:1, required:true
  r longOpt:'reasoner', 'reasoner to use (0 for Pellet, 1 for Hermit, 2 for Fact++, Default: 0)',args:1
  p longOpt:'pellet-compliant', 'ignore nominals and datatype properties to provide support for Pellet (and other) EL reasoners'
  v longOpt:'verbose', 'verbose output: prints progress of OWL reasoning'
  d longOpt:'disjoints', 'include inferred disjointness axioms (may take a long time)'
  a longOpt:'noanno', 'exclude annotation properties (sometimes required for Pellet compliance)'
  t longOpt:'transitivity', 'infer object property characteristics'
  s longOpt:'select-profile', '0 for EL (default), 1 for QL, 2 for RL',args:1
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
if (!opt.s || opt.s == "0") {
  if (opt.p) {
    println "Conversion to OWL EL Profile (Pellet-compliant)"
    prof = new PelletOWL2ELProfile()
  } else {
    println "Conversion to OWL EL Profile"
    prof = new OWL2ELProfile()
  }
} else if (opt.s == "1") {
  println "Conversion to OWL QL Profile"
  prof = new OWL2QLProfile()
} else if (opt.s == "2") {
  println "Conversion to OWL RL Profile"
  prof = new OWL2RLProfile()
} else {
  println "Invalid parameter s"
  return
}

def diri = new File(opt.i) // infile
//def diri = IRI.create(args[0]) // infile
File outfile = new File(opt.o) // outfile

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
manager.addIRIMapper(new NonMappingOntologyIRIMapper())
manager.setSilentMissingImportsHandling(true)

OWLOntology ont = manager.loadOntologyFromOntologyDocument(diri)

OWLOntologyID oldOntologyID = ont.getOntologyID()
OWLOntologyID newOntologyID = new OWLOntologyID(oldOntologyID.getOntologyIRI(),IRI.create("http://el-vira.googlecode.com"))

OWLDataFactory fac = manager.getOWLDataFactory()

OWLReasonerFactory reasonerFactory = null

OWLOntology infOnt = manager.createOntology()

if (opt.r == "0" || !opt.r) {
  println "Using Pellet reasoner"
  reasonerFactory = PelletReasonerFactory.getInstance()
} else if (opt.r == "1") {
  println "Using Hermit reasoner"
  reasonerFactory = new Reasoner.ReasonerFactory()
} else if (opt.r == "2") {
  println "Using FaCT++ reasoner"
  reasonerFactory = new FaCTPlusPlusReasonerFactory()
}


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
if (!opt.a) {
  def s = ont.getAxioms()
  s.each {
    if (
      (! ignoreSet.contains(it))
    ) {
      manager.addAxiom(infOnt,it)
    }
  }
}
/* Use reasoner to add inferred axioms */
ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
OWLReasoner reasoner = null

if (opt.v) { // verbose
  reasoner = reasonerFactory.createNonBufferingReasoner(ont, config)
} else {
  reasoner = reasonerFactory.createNonBufferingReasoner(ont)
}

List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>()
gens.add(new InferredSubClassAxiomGenerator())
gens.add(new InferredDisjointClassesAxiomGenerator())
gens.add(new InferredEquivalentClassAxiomGenerator())
gens.add(new InferredEquivalentObjectPropertyAxiomGenerator())
gens.add(new InferredClassAssertionAxiomGenerator())
gens.add(new InferredInverseObjectPropertiesAxiomGenerator())
gens.add(new InferredPropertyAssertionGenerator())
gens.add(new InferredSubObjectPropertyAxiomGenerator())

InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner)
if (opt.d) {
  println "Adding Disjointness axioms"
  iog.addGenerator(new InferredDisjointClassesAxiomGenerator())
}
if (!opt.t) {
 iog.getAxiomGenerators().each {
 if (it.getLabel() == "Object property characteristics") {
 iog.removeGenerator(it)
  }
 }
}

iog.fillOntology(manager, infOnt)

File tempFile = File.createTempFile("elvira",".owl")

manager.saveOntology(infOnt, IRI.create(tempFile))
manager.removeOntology(ont)
manager.removeOntology(infOnt)

infOnt = manager.loadOntologyFromOntologyDocument(IRI.create(tempFile))

report = prof.checkOntology(infOnt)
viol = report.getViolations()
ignoreSet = new TreeSet()
viol.each { 
  ignoreSet.add(it.getAxiom())
}


OWLOntology ont2 = manager.createOntology(newOntologyID)

s = infOnt.getAxioms()
// copy axiom, provided that it does not have to be ignored and if pellet-compliance is on, then it must not be a datapropertyassertion
s.each { 
  if (
    ((! ignoreSet.contains(it))
     &&
     ((it.getAxiomType()!=AxiomType.DATA_PROPERTY_ASSERTION)||!opt.p)
    )
  ) {
    manager.addAxiom(ont2,it)
  }
}

File tempFile2 = File.createTempFile("elvira-stage2",".owl")

manager.saveOntology(ont2, IRI.create(tempFile2))
manager.removeOntology(ont2)

ont2 = manager.loadOntologyFromOntologyDocument(IRI.create(tempFile2))

report = prof.checkOntology(ont2)
println "Output ontology is in EL: "+report.isInProfile()

// ont2.getAxioms(AxiomType.ANNOTATION_ASSERTION).each {
//   println it
// }


//manager.setOntologyDocumentIRI(ont2,newOntologyID.getOntologyIRI())
//println ont2.getOntologyID()
manager.saveOntology(ont2, IRI.create(outfile.toURI()))


