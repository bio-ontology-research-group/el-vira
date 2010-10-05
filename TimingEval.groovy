package de.bioonto.elvira

import java.util.*
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

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  i longOpt:'input', 'input file', args:1, required:true
  r longOpt:'reasoner', 'reasoner to use (0 for Pellet, 1 for Hermit, 2 for Fact++, Default: 0)',args:1
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

def diri = new File(opt.i) // infile
//def diri = IRI.create(args[0]) // infile

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
} else if (opt.r == "2") {
  println "Using FaCT++ reasoner"
  reasonerFactory = new FaCTPlusPlusReasonerFactory()
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
Calendar now = Calendar.getInstance()
def start = now.getTimeInMillis()
println reasoner.getUnsatisfiableClasses()
now = Calendar.getInstance()
def stop = now.getTimeInMillis()
def elapsed = stop-start
println "Elapsed time: "+elapsed+"ms"
