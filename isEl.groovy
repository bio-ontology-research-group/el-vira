import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.*
import com.clarkparsia.pellet.owlapiv3.*
import org.mindswap.pellet.KnowledgeBase
import org.mindswap.pellet.expressivity.*
import org.mindswap.pellet.*
import org.semanticweb.owlapi.profiles.*

def infile = new File(args[0])

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
OWLOntology ont = manager.loadOntologyFromOntologyDocument(infile)
OWLDataFactory fac = manager.getOWLDataFactory()

OWL2ELProfile prof = new OWL2ELProfile()
def report1 = prof.checkOntology(ont)
println "Input ontology is in EL (OWLAPI): "+report1.isInProfile()


PelletReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance()
OWLReasoner reasoner = reasonerFactory.createReasoner(ont)

def kb = reasoner.getKB()
def expressivity = kb.getExpressivity()
println "Input ontology is in EL (Pellet): " + expressivity.isEL()

