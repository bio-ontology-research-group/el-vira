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

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  i longOpt:'input', 'input file', args:1, required:true
  o longOpt:'output', 'output file',args:1, required:true
  p longOpt:'pellet-compliant', 'ignore nominals and datatype properties to provide support for Pellet (and other) EL reasoners'
  a longOpt:'noanno', 'exclude annotation properties (sometimes required for Pellet compliance)'
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

manager.saveOntology(infOnt, IRI.create(outfile.toURI()))


