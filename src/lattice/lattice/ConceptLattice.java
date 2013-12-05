package lattice.lattice;

/*
 * ConceptLattice.java
 *
 * last update on December 2013
 *
 */
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import lattice.dgraph.DAGraph;
import lattice.dgraph.DGraph;
import lattice.dgraph.Edge;
import lattice.dgraph.Node;
/**
 * This class extends class <code>Lattice</code> to provide specific methods 
 * to manipulate both a concept lattice or a closed set lattice.
 * <p>
 * This class provides methods implementing classical operation on a concept lattice:
 * join and meet reduction, concepts sets reduction, ...
 * <p>
 * This class also provides two static method generating a concept lattice:
 * methods <code>diagramLattice</code> and <code>completeLattice</code> both computes
 * the closed set lattice of a given closure system.
 * The firt one computes the hasse diagram of the closed set lattice
 * by invoking  method <code>immediateSuccessors</code>. This method implements  an
 * adaptation of the well-known Bordat algorithm that also
 * computes the dependance graph of the lattice where at once the minimal generators and the canonical
 * direct basis of the lattice are encoded.
 * The second static method computes the transitively closure of the lattice
 * as the inclusion relation defined on all the closures
 * generated by method <code>allClosures</code> that implements
 * the well-known Wille algorithm.
 * 
 * <p>
 * <img src="..\..\..\images\lgpl.png" height="20" alt="lgpl"/>
 * Copyright 2010 Karell Bertet<p>
 * This file is part of lattice.
 * lattice is free package: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with lattice.  If not, see <a href="http://www.gnu.org/licenses/" target="_blank">license</a>
 *
 * @author Karell Bertet
 * @version 2010
 */
public class ConceptLattice extends Lattice {

	/* ------------- CONSTRUCTORS ------------------ */	
	/** Constructs this component with an empty set of nodes.*/     
	public ConceptLattice () { 
		super ();
		}
  	/** Constructs this component with the specified set of concepts,
	* and empty treemap of successors and predecessors 
	* @param S the set of nodes **/	
   public ConceptLattice (TreeSet<Concept> S) {
		super ((TreeSet)S);
		}	
	/**  Constructs this component as a shallow copy of the specified lattice.<p>
	* Concept lattice property is checked for the specified lattice.
	* When not verified, this component is constructed with an empty set of nodes. 
	* @param L the lattice to be copied */
	public ConceptLattice (Lattice L) {	
		super (L);
		if (!this.isConceptLattice()) { 
			super.nodes=new TreeSet<Node>(); 
			this.succ=new TreeMap<Node,TreeSet<Edge>>();
			this.pred=new TreeMap<Node,TreeSet<Edge>>();
			}
		}
	/* ------------- OVERLAPPING METHODS ------------------ */						
   /** Adds the specified node to the set of node of this component. 
	* In the case where content of this node is not a concept, 
	* the node will not be added */
    public boolean addNode (Node n) {
	 	if (n instanceof Concept)
			return super.addNode(n); 
		else return false;
	}
   /** Adds the specified edge to this component: 
	* <code>to</code> is added as a successor of <code>from</code>.
	* If the cases where specified nodes don't belongs to the node set, 
	* and where nodes don't contains concept as content, 
	* then the edge will not be added.
	* @param from the node origine of the edge
	* @param to the node destination of the edge **/	 
   public boolean addEdge (Node from, Node to) {
		if ((to instanceof Concept) && (from instanceof Concept))
			return super.addEdge(from,to);
		else return false;
	}
	/* ------------- CONCEPT LATTICE CHEKING METHOD ------------------ */						
 
 	/** Check if nodes of this component are concepts */
	public boolean containsConcepts () {
		for (Node n : this.getNodes())
			if (!(n instanceof Concept)) return false;
		return true;	
	} 
   /** Check if this component is a lattice whose nodes are concepts
	*/	
   public boolean isConceptLattice () {
		if (!this.isLattice()) return false;
		if (!this.containsConcepts()) return false;
		return true;
	}
    /** Check if this component is a lattice whose nodes are concepts with non null set A
	*/
   public boolean containsAllSetA () {
        if (!this.containsConcepts()) return false;
        for (Node n : this.getNodes())
            if (!((Concept)n).hasSetA()) return false;
        return true;
   }
   /** Check if this component is a lattice whose nodes are concepts with non null set A
	*/
   public boolean containsAllSetB () {
        if (!this.containsConcepts()) return false;
        for (Node n : this.getNodes())
            if (!((Concept)n).hasSetB()) return false;
        return true;
   }

    /** Returns a copy of this component composed of a copy of each concept and each edge **/
    public ConceptLattice copy() {
        ConceptLattice CL = new ConceptLattice();
        TreeMap<Concept,Concept> copy = new TreeMap<Concept,Concept> ();
        for (Node n : this.getNodes()) {
            Concept c = (Concept) n;
            Concept c2 = c.copy();
            copy.put(c,c2);
            CL.addNode(c2);
        }
        for (Edge ed : this.getEdges()) 
            CL.addEdge (new Edge (copy.get(ed.from()), copy.get(ed.to()), ed.content()));
        return CL;
    }


   	/* ------------- SET A AND SET B HANDLING METHOD ------------------ */

   /** Replace set A in each concept of the lattice with the null value **/
   public boolean removeAllSetA () {
        if (!this.containsConcepts()) return false;
        for (Node n : this.getNodes()) {
             Concept c = (Concept) n;
             c.putSetA(null);
         }
        return true;
   }
   /** Replace set B in each concept of the lattice with the null value **/
   public boolean removeAllSetB () {
        if (!this.containsConcepts()) return false;
        for (Node n : this.getNodes()) {
             Concept c = (Concept) n;
             c.putSetB(null);
        }
        return true;
   }
   /** Replace null set A in each join irreducible concept with a set containing node ident **/
   public boolean initializeSetAForJoin () {
        if (!this.containsConcepts()) return false;
        TreeSet<Node> JoinIrr = this.joinIrreducibles ();
        for (Node n : this.getNodes()) {
            Concept c = (Concept) n;
            if (!c.hasSetA() && JoinIrr.contains(c)) {
                ComparableSet X = new ComparableSet();
                X.add(new Integer(c.ident));
                c.putSetA(X);
                }
            }
        return true;
   }
    /** Replace null set B in each meet irreducible concept with a set containing node ident **/
   public boolean initializeSetBForMeet () {
        if (!this.containsConcepts()) return false;
        TreeSet<Node> MeetIrr = this.meetIrreducibles ();
        for (Node n : this.getNodes()) {
             Concept c = (Concept) n;
             if (!c.hasSetB() && MeetIrr.contains(c)) {
                ComparableSet X = new ComparableSet();
                X.add(new Integer(c.ident));
                c.putSetB(X);
             }
         }
        return true;
   }

/* --------------- INCLUSION REDUCTION METHODS ------------ */

	/** Replaces, if not empty, set A of each concept with the difference between itself
     * and set A of its predecessors ;
     * Then replaces, if not empty, set B of each concept by
	 * the difference between itself and set B of its successors
     **/
	public boolean makeInclusionReduction () {
        if (!this.containsConcepts()) return false;
        boolean setA = this.containsAllSetA();
        boolean setB = this.containsAllSetB();
        if (!setA && !setB) return false;
        // makes setA inclusion reduction
        if (setA) {
            // computation of an inverse topological sort
            this.transpose();
            ArrayList<Node> sort = this.topologicalSort();
            this.transpose();
            // reduction of set A
            for (Node to : sort) {
                Concept cto  = (Concept)to;
                for (Node from : this.getNodesPred(to))  {
                    Concept cfrom = (Concept)from;
                    cto.getSetA().removeAll(cfrom.getSetA());
                }
            }
        }
        // makes setB inclusion reduction
        if (setB) {
            // computation of a topological sort
            ArrayList<Node> sort = this.topologicalSort();
            // reduction of set B
            for (Node to : sort)	{
                Concept cto  = (Concept)to;
                for (Node from : this.getNodesSucc(to))  {
                    Concept cfrom = (Concept)from;
                    cto.getSetB().removeAll(cfrom.getSetB());
                }
            }
        }
        return true;
	}

	/** Replaces set A of each join irreducible node by
	* the difference between itself and set A of the unique predecessor.
	* Others closed sets are replaced by an emptyset **/
	public boolean makeIrreduciblesReduction () {
        // make inclusion reduction
        if (this.makeInclusionReduction()) {
        	// check if not set A reduced concepts are join irreducibles
            // and if not set B reduced concepts are meet irreducibles
            TreeSet<Node> JoinIrr = this.joinIrreducibles ();
            TreeSet<Node> MeetIrr = this.meetIrreducibles ();
            for (Node n : this.getNodes()) {
                Concept c = (Concept)n;
                if (c.hasSetA() && !c.getSetA().isEmpty() && !JoinIrr.contains(c))
                    c.putSetA(new ComparableSet());
                if (c.hasSetB() && !c.getSetB().isEmpty() && !MeetIrr.contains(c))
                    c.putSetB(new ComparableSet());
            }
        }
        return true; 
    }

    /** Returns a lattice where edges are valuated by the difference between
     * set A of two adjacent concepts */
	public boolean makeEdgeValuation () {
        if (!this.containsConcepts()) return false;
		for (Node n1 : this.getNodes())
            for (Edge ed : this.getEdgesSucc(n1))
             if (!ed.hasContent()) {
                 Node n2 = ed.to();
                 TreeSet diff = new TreeSet();
                 diff.addAll (((Concept)n2).getSetA());
                 diff.removeAll(((Concept)n1).getSetA());
                 ed.setContent(diff);
             }
       return true;
    }


    /* --------------- LATTICE GENERATION METHODS ------------ */

	/** Returns a lattice where join irreducibles node's content
	* is replaced by the first element of set A.
	* Other nodes are replaced by a new comparable.
	*/
	public Lattice getJoinReduction () {
        if (!this.containsConcepts()) return null;
        if (!this.containsAllSetA()) return null;
		Lattice L = new Lattice ();
		//ConceptLattice CSL = new ConceptLattice (this);
        ConceptLattice CSL = this.copy();
		CSL.makeIrreduciblesReduction ();
        TreeSet<Node> JoinIrr = CSL.joinIrreducibles ();
		// addition to L of a comparable issued from each reduced closed set
		TreeMap<Node,Node> reduced = new TreeMap<Node,Node>();
		for (Node n : CSL.getNodes()) {
            Concept c = (Concept) n;
            Node nred;
			if (c.hasSetA() && JoinIrr.contains(n))
				nred = new Node(c.getSetA().first());
            else
                nred = new Node();
            reduced.put(n, nred);
        }
        // addtion of nodes to L
		for (Node n : CSL.getNodes())
			L.addNode(reduced.get(n));
		// addtion of edges to L
		for (Node from : CSL.getNodes())
			for (Node to : CSL.getNodesSucc(from))
				L.addEdge (reduced.get(from),reduced.get(to));
		return L;
	}

	/** Returns a lattice where meet irreducibles node's content  
	* is replaced by the first element of set B. 
	* Other nodes are replaced by a new comparable.
	*/
	public Lattice getMeetReduction () {
        if (!this.containsConcepts()) return null;
        if (!this.containsAllSetB()) return null;
		Lattice L = new Lattice ();
		if (!this.containsConcepts()) return L;			
		//ConceptLattice CSL = new ConceptLattice (this);
        ConceptLattice CSL = this.copy();
        CSL.makeIrreduciblesReduction ();
		TreeSet<Node> MeetIrr = CSL.meetIrreducibles ();		
		// addition to L of a comparable issued from each reduced closed set
		TreeMap<Node,Node> reduced = new TreeMap<Node,Node>();
		for (Node n : CSL.getNodes()) {
            Concept c = (Concept)n;
            Node nred;
			if (c.hasSetB() && MeetIrr.contains(n))
				nred = new Node(c.getSetB().first());
            else
                nred = new Node();
            reduced.put(n, nred);
		}
		for (Node n : CSL.getNodes())
			L.addNode(reduced.get(n));
		// addtion of edges to L
		for (Node from : CSL.getNodes()) 
			for (Node to : CSL.getNodesSucc(from))
				L.addEdge (reduced.get(from),reduced.get(to));	
		return L;
	}	
	/** Returns a lattice where each join irreducible concept
	* is replaced by a node containing the first element of set A,
	* and each meet irreducible concept is replaced by a node contining the first element of set B.
    * A concept that is at once join and meet irreducible is replaced by
    * a node containing the first element of set A and the first element of set B in a set.
	* Other nodes are replaced by an empty node.
	*/
	public Lattice getIrreduciblesReduction () {
		Lattice L = new Lattice ();
		if (!this.containsConcepts()) return L;			
		//ConceptLattice CSL = new ConceptLattice (this);
        ConceptLattice CSL = this.copy();
        CSL.makeIrreduciblesReduction ();
		TreeSet<Node> JoinIrr = CSL.joinIrreducibles ();		
		TreeSet<Node> MeetIrr = CSL.meetIrreducibles ();		
		// addition to L of a comparable issued from each reduced closed set
		TreeMap<Node,Node> reduced = new TreeMap<Node,Node>();
		for (Node n : CSL.getNodes()) {
            Concept c = (Concept) n;
			// create a new Node with two indexed elements: the first of set A and the first of set B 
			if (c.hasSetA() && c.hasSetB() && MeetIrr.contains(c) && JoinIrr.contains(c)) {
				TreeSet<Comparable> content = new TreeSet<Comparable>();
				content.add(c.getSetA().first());
				content.add(c.getSetB().first());
				Node nred = new Node(content);
				reduced.put(n, nred);
			}			
			// create a new Node with the first element of set A
			else if (c.hasSetA() && JoinIrr.contains(n)) {
				Node nred = new Node(((Concept)n).getSetA().first());
				reduced.put(n, nred);
			}
			// create a new Node with the first element of set A			
			else if (c.hasSetB() && MeetIrr.contains(n)) {
				Node nred = new Node(((Concept)n).getSetB().first());
				reduced.put(n, nred);
			}
			else
				reduced.put(n, new Node());
        }
        // addtion of nodes to L
		for (Node n : CSL.getNodes())
			L.addNode(reduced.get(n));
		// addtion of edges to L	
		for (Node from : CSL.getNodes()) 
			for (Node to : CSL.getNodesSucc(from))
				L.addEdge (reduced.get(from),reduced.get(to));	
		return L;
	}

	/* -------- STATIC CLOSEDSET LATTICE GENERATION FROM AN IS OR A CONTEXT ------------------ */

  	/** Generates and returns the complete (i.e. transitively closed) closed set lattice of the
     * specified closure system, that can be an implicational system (IS) or a context.<p>
     * <p>
     * The lattice is generated using the well-known Next Closure algorithm.
     * All closures are first generated using the method:
     * <code> Vector<Concept> allClosures </code>
     * that implements the well-known Next Closure algorithm.
     * Then, all concepts are ordered by inclusion.
     * <p>
     * @param init a closure system (an IS or a Context)
	 */
        public static ConceptLattice completeLattice (ClosureSystem init) {
            ConceptLattice L = new ConceptLattice();
            // compute all the closed set with allClosures
            Vector<Concept> allclosure = init.allClosures();
            for (Concept cl : allclosure)
    			L.addNode(cl);

            // an edge corresponds to an inclusion between two closed sets
        	for (Node from : L.getNodes())
            	for (Node to : L.getNodes())
                	if (((Concept)to).containsAllInA(((Concept)from).getSetA()))
                       L.addEdge(from,to);            
            // Hasse diagram is computed
        return L;
    }

  	/** Generates and returns the Hasse diagram of the closed set lattice of the 
     * specified closure system, that can be an implicational system (IS) or a context.<p>
     * <p>
    * The Hasse diagramm of the closed set lattice is
     * obtained by a recursively generation of immediate successors of a given closed set,
     * starting from the botom closed set. Implemented algorithm is an adaptation of Bordat's
     * algorithm where the dependance graph is computed while the lattice is generated.
	 * This treatment is performed in O(cCl|S|^3log g) where S is the initial set of elements,
     * c is the number of closed sets that could be exponential in the worst case,
     * Cl is the closure computation complexity
     * and g is the number of minimal generators of the lattice.
     * <p>
     * The dependance graph of the lattice is also computed while the lattice generation.
     * The dependance graph of a lattice encodes at once the minimal generators
     * and the canonical direct basis of the lattice .
     * <p>
     * @param init a closure system (an IS or a Context)
     */
    public static ConceptLattice diagramLattice (ClosureSystem init) {
        ConceptLattice L = new ConceptLattice();
        //if (Diagram) {
            // computes the dependance graph of the closure system
            // addition of nodes in the precedence graph
            L.dependanceGraph = new DGraph();
            for (Comparable c : init.getSet())
                L.dependanceGraph.addNode(new Node(c));
            // intialize the close set lattice with botom element
            Concept bot = new Concept (init.closure(new ComparableSet()), false);
            L.addNode(bot);
            // recursive genaration from the botom element with diagramLattice
            L.recursiveDiagramLattice(bot, init);
            // minimalisation of edge's content to get only inclusion-minimal valuation for each edge
            /**for (Edge ed : L.dependanceGraph.getEdges()) {
                TreeSet<ComparableSet> valEd = new TreeSet<ComparableSet>(((TreeSet<ComparableSet>)ed.content()));
                for (ComparableSet X1 : valEd)
                    for (ComparableSet X2 : valEd)
                        if (X1.containsAll(X2) && !X2.containsAll(X1))
                            ((TreeSet<ComparableSet>)ed.content()).remove(X1);
            }**/
        return L;
        }

	/** Returns the Hasse diagramme of the closed set lattice of the specified closure system
     * issued from the specified concept.

     * Immediate successors generation is an adaptation of Bordat's theorem
    * stating that there is a bijection
    * between minimal strongly connected component of the precedence subgraph issued
	* from the specified node, and its immediate successors.
	* <p>
	* This treatment is performed in O(cCl|S|^3log g) where S is the initial set of elements,
     * c is the number of closed sets that could be exponential in the worst case,
     * Cl is the closure computation complexity
     * and g is the number of minimal generators of the lattice.
	*/

	public void recursiveDiagramLattice (Concept n, ClosureSystem init) {
		Vector<TreeSet<Comparable>> immSucc = this.immediateSuccessors (n, init);
		for (TreeSet<Comparable> X : immSucc) {
            Concept c = new Concept(new TreeSet(X),false);
            Concept ns = (Concept) this.getNode(c);
            if (ns != null)  // when ns already exists, addition of a new edge                
				this.addEdge(n,ns);
			else { // when ns don't already exists, addition of a new node and recursive treatment
				this.addNode(c);
				this.addEdge (n,c);
				this.recursiveDiagramLattice (c, init);
			}
		}
	}

	/** Returns the list of immediate successors of a given node of the lattice. <p>
	* <p>
	* This treatment is an adaptation of Bordat's theorem stating that there is a bijection
    * between minimal strongly connected component of the precedence subgraph issued
	* from the specified node, and its immediate successors.
	* <p>
	* This treatment is performed in O(Cl|S|^3log g) where S is the initial set of elements,
     * Cl is the closure computation complexity
     * and g is the number of minimal generators of the lattice.
     * <p>
     * This treatment is recursively invoked by method recursiveDiagramlattice. In this case, the dependance graph
     * is initialized by method recursiveDiagramMethod, and updated by this method,
     * with addition some news edges and/or new valuations on existing edges.
     * When this treatment is not invoked by method recursiveDiagramLattice, then the dependance graph
     * is initialized, but it may be not complete. It is the case for example for on-line generation of the
     * concept lattice.
	*/
	public Vector<TreeSet<Comparable>> immediateSuccessors (Node n, ClosureSystem init) {
        // Initialization of the dependance graph when not initialized by method recursiveDiagramLattice
        if (this.dependanceGraph == null) {
            this.dependanceGraph = new DGraph();
            for (Comparable c : init.getSet())
                this.dependanceGraph.addNode(new Node(c));
        }
        // computes newVal, the subset to be used to valuate every new dependance relation
        // newVal = F\predecessors of F in the precedence graph of the closure system
        // For a non reduced closure system, the precedence graph is not acyclic,
        // and therefore strongly connected components have to be used.
		ComparableSet F = new ComparableSet (((Concept)n).getSetA());        
        DGraph prec = init.precedenceGraph();        
        DAGraph acyclPrec = prec.stronglyConnectedComponent();        
        ComparableSet newVal = new ComparableSet ();
        newVal.addAll(F);
        for (Object x : F)  {
            // computes nx, the strongly connected component containing x
            Node nx = null;
            for (Node cc : acyclPrec.getNodes()) {
                TreeSet<Node> CC = (TreeSet<Node>) cc.content;                
                for (Node y : CC)
                    if (x.equals(y.content))
                        nx=cc;
            }
            // computes the minorants of nx in the acyclic graph
            TreeSet<Node> ccMinNx = acyclPrec.minorants(nx);
            // removes from newVal every minorants of nx
            for (Node cc : ccMinNx) {
                TreeSet<Node> CC = (TreeSet<Node>) cc.content;
                for (Node y : CC)
                    newVal.remove(y.content);
            }
        }
        // computes the node belonging in S\F
        TreeSet<Node> N = new TreeSet<Node> ();        
        for (Node in : this.dependanceGraph.getNodes())
            if (!F.contains(in.content))
                N.add(in);
        // computes the dependance relation between nodes in S\F
        // and valuated this relation by the subset of S\F
        TreeSet<Edge> E = new TreeSet<Edge>();
        for (Node from : N)
            for (Node to : N)
               if (!from.equals(to)) {
                // check if from is in dependance relation with to
                // i.e. "from" belongs to the closure of "F+to"
                ComparableSet FPlusTo = new ComparableSet(F);
                FPlusTo.add(to.content);
                FPlusTo = new ComparableSet(init.closure(FPlusTo));
                if (FPlusTo.contains(from.content)) {
                    // there is a dependance relation between from and to
                    // search for an existing edge between from and to
                    Edge ed = this.dependanceGraph.getEdge(from, to);
                    if (ed==null) {
                        ed = new Edge (from,to,new TreeSet<ComparableSet>());
                        this.dependanceGraph.addEdge(ed);
                    }
                    E.add(ed);
                    // check if F is a minimal set closed for dependance relation between from and to
                    ((TreeSet<ComparableSet>)ed.content()).add(newVal);
                    TreeSet<ComparableSet> ValEd = new TreeSet<ComparableSet>((TreeSet<ComparableSet>)ed.content());
                        for (ComparableSet X1 : ValEd) {
                            if (X1.containsAll(newVal) && !newVal.containsAll(X1))
                                ((TreeSet<ComparableSet>)ed.content()).remove(X1);
                            if (!X1.containsAll(newVal) && newVal.containsAll(X1))
                                ((TreeSet<ComparableSet>)ed.content()).remove(newVal);
                        }
                  }
               }
        // computes the dependance subgraph of the closed set F as the reduction
        // of the dependance graph composed of nodes in S\A and edges of the dependance relation
        DGraph sub = this.dependanceGraph.subgraphByNodes(N);
        DGraph delta = sub.subgraphByEdges(E);
        // computes the sources of the CFC of the dependance subgraph
        // that corresponds to successors of the closed set F
		DAGraph CFC = delta.stronglyConnectedComponent ();
		TreeSet<Node> SCCmin = CFC.sinks();
        Vector<TreeSet<Comparable>> immSucc = new Vector<TreeSet<Comparable>>();
        for (Node n1 : SCCmin) {
			TreeSet s = new TreeSet(F);
			TreeSet<Node> toadd = (TreeSet<Node>)n1.content;
			for (Node n2 : toadd)
				s.add(n2.content);
			immSucc.add(s);
		}
       return immSucc;
	}
}// end of ConceptLattice
