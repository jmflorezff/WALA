/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/                                                                         
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.fixedpoint.impl.AbstractFixedPointSystem;
import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.AbstractStatement;
import com.ibm.wala.fixedpoint.impl.GeneralStatement;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixedpoint.impl.UnaryStatement;
import com.ibm.wala.fixpoint.IFixedPointStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.collections.SmallMap;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.intset.BasicNonNegativeIntRelation;
import com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * A dataflow graph implementation specialized for propagation-based pointer
 * analysis
 * 
 * @author sfink
 */
public class PropagationGraph extends AbstractFixedPointSystem {

  private final static boolean DEBUG = false;

  private final static boolean VERBOSE = false;

  /**
   * Track nodes (PointsToSet Variables and AbstractEquations)
   */
  private final NumberedNodeManager<INodeWithNumber> nodeManager = new DelegatingNumberedNodeManager<INodeWithNumber>();

  /**
   * Track edges (equations) that are not represented implicitly
   */
  private final EdgeManager<INodeWithNumber> edgeManager = new SparseNumberedEdgeManager<INodeWithNumber>(nodeManager, 2, BasicNonNegativeIntRelation.SIMPLE);

  private final DelegateGraph delegateGraph = new DelegateGraph();

  private final HashSet<AbstractStatement> delegateStatements = new HashSet<AbstractStatement>();

  /**
   * special representation for implicitly represented unary equations. This is
   * a map from UnaryOperator -> IBinaryNonNegativeIntRelation.
   * 
   * for UnaryOperator op, let R be implicitMap.get(op) then (i,j) \in R implies
   * i op j is an equation in the graph
   * 
   */
  private final SmallMap<UnaryOperator,IBinaryNonNegativeIntRelation> implicitUnaryMap = new SmallMap<UnaryOperator,IBinaryNonNegativeIntRelation>();

  /**
   * The inverse of relations in the implicit map
   * 
   * for UnaryOperator op, let R be invImplicitMap.get(op) then (i,j) \in R
   * implies j op i is an equation in the graph
   */
  private final SmallMap<UnaryOperator, IBinaryNonNegativeIntRelation> invImplicitUnaryMap = new SmallMap<UnaryOperator,IBinaryNonNegativeIntRelation>();

  /**
   * Number of implicit unary equations registered
   */
  private int implicitUnaryCount = 0;

  /**
   * @param m
   * @param key
   * @return a relation in map m corresponding to a key
   */
  private IBinaryNonNegativeIntRelation findOrCreateRelation(Map<UnaryOperator, IBinaryNonNegativeIntRelation> m, UnaryOperator key) {
    IBinaryNonNegativeIntRelation result = m.get(key);
    if (result == null) {
      result = makeRelation((AbstractOperator) key);
      m.put(key, result);
    }
    return result;
  }

  /**
   * @return a Relation object to track implicit equations using the operator
   */
  private IBinaryNonNegativeIntRelation makeRelation(AbstractOperator op) {
    byte[] implementation = null;
    if (op instanceof AssignOperator) {
      // lots of assignments.
      implementation = new byte[] { BasicNonNegativeIntRelation.SIMPLE_SPACE_STINGY,
          BasicNonNegativeIntRelation.SIMPLE_SPACE_STINGY };
    } else {
      // assume sparse assignments with any other operator.
      implementation = new byte[] { BasicNonNegativeIntRelation.SIMPLE_SPACE_STINGY };
    }
    return new BasicNonNegativeIntRelation(implementation, BasicNonNegativeIntRelation.SIMPLE);
  }

  /**
   * @author sfink
   * 
   * A graph which tracks explicit equations.
   * 
   * use this with care ...
   */
  private class DelegateGraph extends AbstractNumberedGraph<INodeWithNumber> {

    private int equationCount = 0;

    private int varCount = 0;

    public void addNode(INodeWithNumber o) {
      Assertions.UNREACHABLE("Don't call me");
    }

    /**
     * 
     * @param eq
     */
    public void addEquation(AbstractStatement eq) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(!containsStatement(eq));
      }
      equationCount++;
      super.addNode(eq);
    }

    /**
     * @param v
     */
    public void addVariable(IVariable v) {
      if (!containsVariable(v)) {
        varCount++;
        super.addNode(v);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    protected NodeManager<INodeWithNumber> getNodeManager() {
      return nodeManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    protected EdgeManager<INodeWithNumber> getEdgeManager() {
      return edgeManager;
    }

    protected int getEquationCount() {
      return equationCount;
    }

    protected int getVarCount() {
      return varCount;
    }

  }

  /**
   * @param eq
   */
  public void addStatement(GeneralStatement eq) {
    IVariable lhs = eq.getLHS();
    delegateGraph.addEquation(eq);
    delegateStatements.add(eq);
    if (lhs != null) {
      delegateGraph.addVariable(lhs);
      delegateGraph.addEdge(eq, lhs);
    }
    for (int i = 0; i < eq.getRHS().length; i++) {
      IVariable v = eq.getRHS()[i];
      if (v != null) {
        delegateGraph.addVariable(v);
        delegateGraph.addEdge(v, eq);
      }
    }
  }

  /**
   * @param eq
   */
  public void addStatement(UnaryStatement eq) {
    if (useImplicitRepresentation(eq)) {
      addImplicitStatement(eq);
    } else {
      IVariable lhs = eq.getLHS();
      IVariable rhs = eq.getRightHandSide();
      delegateGraph.addEquation(eq);
      delegateStatements.add(eq);
      if (lhs != null) {
        delegateGraph.addVariable(lhs);
        delegateGraph.addEdge(eq, lhs);
      }
      delegateGraph.addVariable(rhs);
      delegateGraph.addEdge(rhs, eq);
    }
  }
 
  /**
   * @return true iff this equation should be represented implicitly in this
   *         data structure
   */
  private boolean useImplicitRepresentation(IFixedPointStatement s) {
    AbstractStatement eq = (AbstractStatement) s;
    AbstractOperator op = eq.getOperator();
    return (op instanceof AssignOperator || op instanceof PropagationCallGraphBuilder.FilterOperator);
  }


  public void removeVariable(PointsToSetVariable p) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(getNumberOfStatementsThatDef(p) == 0);
      Assertions._assert(getNumberOfStatementsThatUse(p) == 0);
    }
    delegateGraph.removeNode(p);
  }
  
  private void addImplicitStatement(UnaryStatement eq) {
    if (DEBUG) {
      Trace.println("addImplicitStatement " + eq);
    }
    delegateGraph.addVariable(eq.getLHS());
    delegateGraph.addVariable(eq.getRightHandSide());
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    if (DEBUG) {
      Trace.println("lhs rhs " + lhs + " " + rhs);
    }
    IBinaryNonNegativeIntRelation R = findOrCreateRelation(implicitUnaryMap, (UnaryOperator) eq.getOperator());
    boolean b = R.add(lhs, rhs);
    if (b) {
      implicitUnaryCount++;
      IBinaryNonNegativeIntRelation iR = findOrCreateRelation(invImplicitUnaryMap, (UnaryOperator) eq.getOperator());
      iR.add(rhs, lhs);
    }
  }
  
  private void removeImplicitStatement(UnaryStatement eq) {
    if (DEBUG) {
      Trace.println("removeImplicitStatement " + eq);
    }
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    if (DEBUG) {
      Trace.println("lhs rhs " + lhs + " " + rhs);
    }
    IBinaryNonNegativeIntRelation R = findOrCreateRelation(implicitUnaryMap, (UnaryOperator) eq.getOperator());
    R.remove(lhs,rhs);
    IBinaryNonNegativeIntRelation iR = findOrCreateRelation(invImplicitUnaryMap, (UnaryOperator) eq.getOperator());
    iR.remove(rhs,lhs);
    implicitUnaryCount--;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    Assertions.UNREACHABLE();
    return false;
  }

  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatements() {
    Iterator<AbstractStatement> it = new FilterIterator(delegateGraph.iterateNodes(), new Filter() {
      public boolean accepts(Object x) {
        return x instanceof AbstractStatement;
      }
    });
    return new CompoundIterator<AbstractStatement>(it, new GlobalImplicitIterator());
  }

  /**
   * @author sfink
   * 
   * Iterator of implicit equations that use a particular variable.
   */
  private final class ImplicitUseIterator implements Iterator<AbstractStatement> {

    final IVariable use;

    final IntIterator defs;

    final UnaryOperator op;

    ImplicitUseIterator(UnaryOperator op, IVariable use, IntSet defs) {
      this.op = op;
      this.use = use;
      this.defs = defs.intIterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return defs.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    public AbstractStatement next() {
      int l = defs.next();
      IVariable lhs = (IVariable) delegateGraph.getNode(l);
      UnaryStatement temp = op.makeEquation(lhs, use);
      if (DEBUG) {
        Trace.print("XX Return temp: " + temp);
        Trace.println("lhs rhs " + l + " " + use.getGraphNodeId());
      }
      return temp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /**
   * @author sfink
   * 
   * Iterator of implicit equations that def a particular variable.
   */
  private final class ImplicitDefIterator implements Iterator<AbstractStatement> {

    final IVariable def;

    final IntIterator uses;

    final UnaryOperator op;

    ImplicitDefIterator(UnaryOperator op, IntSet uses, IVariable def) {
      this.op = op;
      this.def = def;
      this.uses = uses.intIterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return uses.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    public AbstractStatement next() {
      int r = uses.next();
      IVariable rhs = (IVariable) delegateGraph.getNode(r);
      UnaryStatement temp = op.makeEquation(def, rhs);
      if (DEBUG) {
        Trace.print("YY Return temp: " + temp);
      }
      return temp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /**
   * @author sfink
   * 
   * Iterator of all implicit equations
   */
  private class GlobalImplicitIterator implements Iterator<AbstractStatement> {

    private final Iterator outerKeyDelegate = implicitUnaryMap.keySet().iterator();

    private Iterator innerDelegate;

    private UnaryOperator currentOperator;

    GlobalImplicitIterator() {
      advanceOuter();
    }

    /**
     * advance to the next operator
     */
    private void advanceOuter() {
      innerDelegate = null;
      while (outerKeyDelegate.hasNext()) {
        currentOperator = (UnaryOperator) outerKeyDelegate.next();
        IBinaryNonNegativeIntRelation R = implicitUnaryMap.get(currentOperator);
        Iterator it = R.iterator();
        if (it.hasNext()) {
          innerDelegate = it;
          return;
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return innerDelegate != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    public AbstractStatement next() {
      IntPair p = (IntPair) innerDelegate.next();
      int lhs = p.getX();
      int rhs = p.getY();
      UnaryStatement result = currentOperator.makeEquation((IVariable) delegateGraph.getNode(lhs), (IVariable) delegateGraph
          .getNode(rhs));
      if (!innerDelegate.hasNext()) {
        advanceOuter();
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    Assertions.UNREACHABLE();
    return 0;
  }

  /**
   * @param eq
   */
  public void removeStatement(IFixedPointStatement eq) {
    if (useImplicitRepresentation(eq)) {
      removeImplicitStatement((UnaryStatement)eq);
    } else {
      delegateStatements.remove(eq);
      delegateGraph.removeNodeAndEdges(eq);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#reorder()
   */
  public void reorder() {
    VariableGraphView graph = new VariableGraphView();

    Iterator<IVariable> finishTime = DFS.iterateFinishTime(graph);
    Iterator<IVariable> rev = new ReverseIterator<IVariable>(finishTime);
    Graph<IVariable> G_T = GraphInverter.invert(graph);
    Iterator<IVariable> order = DFS.iterateFinishTime(G_T, rev);
    int number = 0;
    while (order.hasNext()) {
      Object elt = order.next();
      if (elt instanceof IVariable) {
        IVariable v = (IVariable) elt;
        v.setOrderNumber(number++);
      }
    }
  }

  /**
   * @author sfink
   * 
   * A graph of just the variables in the system. v1 -> v2 iff there exists
   * equation e s.t. e uses v1 and e defs v2.
   * 
   * Note that this graph trickily and fragilely reuses the nodeManager from the
   * delegateGraph, above. This will work ok as long as every variable is
   * inserted in the delegateGraph.
   */
  private class VariableGraphView extends AbstractNumberedGraph<IVariable> {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
     */
    public void removeNodeAndEdges(IVariable N) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
     */
    public Iterator<IVariable> iterateNodes() {
      return getVariables();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
     */
    public int getNumberOfNodes() {
      return delegateGraph.getVarCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
     */
    public void addNode(IVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
     */
    public void removeNode(IVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
     */
    public boolean containsNode(IVariable N) {
      Assertions.UNREACHABLE();
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
     */
    public Iterator<IVariable> getPredNodes(IVariable N) {
      IVariable v = (IVariable) N;
      final Iterator eqs = getStatementsThatDef(v);
      return new Iterator<IVariable>() {
        Iterator<IVariable> inner;

        public boolean hasNext() {
          return eqs.hasNext() || (inner != null);
        }

        @SuppressWarnings("unchecked")
        public IVariable next() {
          if (inner != null) {
            IVariable result = inner.next();
            if (!inner.hasNext()) {
              inner = null;
            }
            return result;
          } else {
            AbstractStatement eq = (AbstractStatement) eqs.next();
            if (useImplicitRepresentation(eq)) {
              return ((UnaryStatement) eq).getRightHandSide();
            } else {
              inner = (Iterator<IVariable>) delegateGraph.getPredNodes(eq);
              return next();
            }
          }
        }

        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
     */
    public int getPredNodeCount(INodeWithNumber N) {
      IVariable v = (IVariable) N;
      int result = 0;
      for (Iterator eqs = getStatementsThatDef(v); eqs.hasNext();) {
        AbstractStatement eq = (AbstractStatement) eqs.next();
        if (useImplicitRepresentation(eq)) {
          result++;
        } else {
          result += delegateGraph.getPredNodeCount(N);
        }
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    public Iterator<IVariable> getSuccNodes(IVariable N) {
      IVariable v = (IVariable) N;
      final Iterator eqs = getStatementsThatUse(v);
      return new Iterator<IVariable>() {
        IVariable nextResult;
        {
          advance();
        }

        public boolean hasNext() {
          return nextResult != null;
        }

        public IVariable next() {
          IVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = (AbstractStatement) eqs.next();
            nextResult = eq.getLHS();
          }
        }

        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
     */
    public int getSuccNodeCount(IVariable v) {
      int result = 0;
      for (Iterator eqs = getStatementsThatUse(v); eqs.hasNext();) {
        AbstractStatement eq = (AbstractStatement) eqs.next();
        IVariable lhs = eq.getLHS();
        if (lhs != null) {
          result++;
        }
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
     *      java.lang.Object)
     */
    public void addEdge(IVariable src, IVariable dst) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
     */
    public void removeAllIncidentEdges(IVariable node) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    @SuppressWarnings("unchecked")
    protected NodeManager getNodeManager() {
      return nodeManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    @SuppressWarnings("unchecked")
    protected EdgeManager getEdgeManager() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
      return null;
    }

  }

  /*
   * (non-Javadoc)
   */
  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatementsThatUse(IVariable v) {
    int number = v.getGraphNodeId();
    if (number == -1) {
      return EmptyIterator.instance();
    }
    Iterator<AbstractStatement> result = (Iterator<AbstractStatement>) delegateGraph.getSuccNodes(v);
    for (int i = 0; i < invImplicitUnaryMap.size(); i++) {
      UnaryOperator op = (UnaryOperator) invImplicitUnaryMap.getKey(i);
      IBinaryNonNegativeIntRelation R = (IBinaryNonNegativeIntRelation) invImplicitUnaryMap.getValue(i);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result = new CompoundIterator<AbstractStatement>(new ImplicitUseIterator(op, v, s), result);
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getStatementsThatDef(com.ibm.wala.dataflow.fixpoint.IVariable)
   */
  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatementsThatDef(IVariable v) {
    int number = v.getGraphNodeId();
    if (number == -1) {
      return EmptyIterator.instance();
    }
    Iterator<AbstractStatement> result = (Iterator<AbstractStatement>) delegateGraph.getPredNodes(v);
    for (int i = 0; i < implicitUnaryMap.size(); i++) {
      UnaryOperator op = (UnaryOperator) implicitUnaryMap.getKey(i);
      IBinaryNonNegativeIntRelation R = (IBinaryNonNegativeIntRelation) implicitUnaryMap.getValue(i);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result = new CompoundIterator<AbstractStatement>(new ImplicitDefIterator(op, s, v), result);
      }
    }

    return result;
  }

  /**
   * Note that this implementation consults the implicit relation for each and
   * every operator cached. This will be inefficient if there are many implicit
   * operators.
   * 
   */
  public int getNumberOfStatementsThatUse(IVariable v) {
    int number = v.getGraphNodeId();
    if (number == -1) {
      return 0;
    }
    int result = delegateGraph.getSuccNodeCount(v);
    for (Iterator it = invImplicitUnaryMap.keySet().iterator(); it.hasNext();) {
      UnaryOperator op = (UnaryOperator) it.next();
      IBinaryNonNegativeIntRelation R = invImplicitUnaryMap.get(op);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result += s.size();
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getNumberOfStatementsThatUse(com.ibm.wala.dataflow.fixpoint.AbstractVariable)
   */
  public int getNumberOfStatementsThatDef(IVariable v) {
    int number = v.getGraphNodeId();
    if (number == -1) {
      return 0;
    }
    int result = delegateGraph.getPredNodeCount(v);
    for (Iterator it = implicitUnaryMap.keySet().iterator(); it.hasNext();) {
      UnaryOperator op = (UnaryOperator) it.next();
      IBinaryNonNegativeIntRelation R = implicitUnaryMap.get(op);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result += s.size();
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getVariables()
   */
  @SuppressWarnings("unchecked")
  public Iterator<IVariable> getVariables() {
    Iterator<IVariable> it = new FilterIterator(delegateGraph.iterateNodes(), new Filter() {
      public boolean accepts(Object x) {
        return x instanceof IVariable;
      }
    });
    return it;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public void performVerboseAction() {
    if (VERBOSE) {
      Trace.println("stats for " + getClass());
      Trace.println("number of variables: " + delegateGraph.getVarCount());
      Trace.println("implicit equations: " + (implicitUnaryCount));
      Trace.println("explicit equations: " + delegateGraph.getEquationCount());
      Trace.println("implicit map:");
      int count = 0;
      int totalBytes = 0;
      for (Iterator it = implicitUnaryMap.entrySet().iterator(); it.hasNext();) {
        count++;
        Map.Entry e = (Map.Entry) it.next();
        IBinaryNonNegativeIntRelation R = (IBinaryNonNegativeIntRelation) e.getValue();
        Trace.println("entry " + count);
        R.performVerboseAction();
        HeapTracer.Result result = HeapTracer.traceHeap(Collections.singleton(R), false);
        totalBytes += result.getTotalSize();
      }
      Trace.println("bytes in implicit map: " + totalBytes);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#containsStatement(com.ibm.wala.dataflow.fixpoint.AbstractStatement)
   */
  public boolean containsStatement(IFixedPointStatement eq) {
    if (useImplicitRepresentation(eq)) {
      UnaryStatement ueq = (UnaryStatement) eq;
      return containsImplicitStatement(ueq);
    } else {
      return delegateStatements.contains(eq);
    }
  }

  /**
   * @param eq
   * @return true iff the graph already contains this equation
   */
  private boolean containsImplicitStatement(UnaryStatement eq) {
    if (!containsVariable(eq.getLHS())) {
      return false;
    }
    if (!containsVariable(eq.getRightHandSide())) {
      return false;
    }
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    UnaryOperator op = (UnaryOperator) eq.getOperator();
    IBinaryNonNegativeIntRelation R = implicitUnaryMap.get(op);
    if (R != null) {
      return R.contains(lhs, rhs);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#containsVariable(com.ibm.wala.dataflow.fixpoint.IVariable)
   */
  public boolean containsVariable(IVariable v) {
    return delegateGraph.containsNode(v);
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void addStatement(IFixedPointStatement statement) {
    if (statement instanceof UnaryStatement) {
      addStatement((UnaryStatement) statement);
    } else if (statement instanceof GeneralStatement) {
      addStatement((GeneralStatement) statement);
    } else {
      Assertions.UNREACHABLE("unexpected: " + statement.getClass());
    }
  }


  /**
   * A graph of just the variables in the system. v1 -> v2 iff there exists an
   * Assingment equation e s.t. e uses v1 and e defs v2.
   * 
   */
  public NumberedGraph<IVariable> getAssignmentGraph() {
    return new FilteredConstraintGraphView() {

      boolean isInteresting(AbstractStatement eq) {
        return eq instanceof AssignEquation;
      }
    };
  }

  /**
   * A graph of just the variables in the system. v1 -> v2 iff there exists an
   * Assingnment or Filter equation e s.t. e uses v1 and e defs v2.
   * 
   */
  public Graph<IVariable> getFilterAssignmentGraph() {
    return new FilteredConstraintGraphView() {

      boolean isInteresting(AbstractStatement eq) {
        return eq instanceof AssignEquation || eq.getOperator() instanceof PropagationCallGraphBuilder.FilterOperator;
      }
    };
  }

  /**
   * A graph of just the variables in the system. v1 -> v2 that are related by
   * def-use with "interesting" operators
   * 
   */
  private abstract class FilteredConstraintGraphView extends AbstractNumberedGraph<IVariable> {

    abstract boolean isInteresting(AbstractStatement eq);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
     */
    public void removeNodeAndEdges(IVariable N) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
     */
    public Iterator<IVariable> iterateNodes() {
      return getVariables();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
     */
    public int getNumberOfNodes() {
      return delegateGraph.getVarCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
     */
    public void addNode(IVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
     */
    public void removeNode(IVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
     */
    public boolean containsNode(IVariable N) {
      Assertions.UNREACHABLE();
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
     */
    public Iterator<? extends IVariable> getPredNodes(IVariable N) {
      IVariable v = (IVariable) N;
      final Iterator eqs = getStatementsThatDef(v);
      return new Iterator<IVariable>() {
        IVariable nextResult;
        {
          advance();
        }

        public boolean hasNext() {
          return nextResult != null;
        }

        public IVariable next() {
          IVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = (AbstractStatement) eqs.next();
            if (isInteresting(eq)) {
              nextResult = ((UnaryStatement) eq).getRightHandSide();
            }
          }
        }

        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
     */
    public int getPredNodeCount(IVariable N) {
      IVariable v = (IVariable) N;
      int result = 0;
      for (Iterator eqs = getStatementsThatDef(v); eqs.hasNext();) {
        AbstractStatement eq = (AbstractStatement) eqs.next();
        if (isInteresting(eq)) {
          result++;
        }
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    public Iterator<IVariable> getSuccNodes(IVariable N) {
      IVariable v = (IVariable) N;
      final Iterator eqs = getStatementsThatUse(v);
      return new Iterator<IVariable>() {
        IVariable nextResult;
        {
          advance();
        }

        public boolean hasNext() {
          return nextResult != null;
        }

        public IVariable next() {
          IVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = (AbstractStatement) eqs.next();
            if (isInteresting(eq)) {
              nextResult = ((UnaryStatement) eq).getLHS();
            }
          }
        }

        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
     */
    public int getSuccNodeCount(IVariable N) {
      IVariable v = (IVariable) N;
      int result = 0;
      for (Iterator eqs = getStatementsThatUse(v); eqs.hasNext();) {
        AbstractStatement eq = (AbstractStatement) eqs.next();
        if (isInteresting(eq)) {
          result++;
        }
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
     *      java.lang.Object)
     */
    public void addEdge(IVariable src, IVariable dst) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
     */
    public void removeAllIncidentEdges(IVariable node) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    @SuppressWarnings("unchecked")
    protected NodeManager getNodeManager() {
      return nodeManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    protected EdgeManager<IVariable> getEdgeManager() {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public String spaceReport() {
    StringBuffer result = new StringBuffer("PropagationGraph\n");
    result.append("ImplicitEdges:" + countImplicitEdges() + "\n");
    // for (Iterator it = implicitUnaryMap.values().iterator(); it.hasNext(); )
    // {
    // result.append(it.next() + "\n");
    // }
    return result.toString();
  }

  private int countImplicitEdges() {
    int result = 0;
    for (Iterator it = new GlobalImplicitIterator(); it.hasNext();) {
      it.next();
      result++;
    }
    return result;
  }

}
