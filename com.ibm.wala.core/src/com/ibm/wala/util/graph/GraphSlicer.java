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
package com.ibm.wala.util.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.warnings.WalaException;

/**
 */
public class GraphSlicer {

  /*
   * (non-Javadoc)
   */
  public static <T> Collection<T> slice(Graph<T> g, Filter f) throws WalaException {

    HashSet<T> roots = new HashSet<T>();
    for (Iterator<? extends T> it = g.iterateNodes(); it.hasNext();) {
      T o = it.next();
      if (f.accepts(o)) {
        roots.add(o);
      }
    }

    Set<T> result = DFS.getReachableNodes(GraphInverter.invert(g), roots);

    return result;

  }

  /*
   * (non-Javadoc)
   * 
   */
  public static <T> Graph<T> prune(final Graph<T> g, final Filter f) {

    final NodeManager<T> n = new NodeManager<T>() {

      int nodeCount = -1;

      public Iterator<T> iterateNodes() {
        return new FilterIterator<T>(g.iterateNodes(), f);
      }

      public int getNumberOfNodes() {
        if (nodeCount == -1) {
          nodeCount = IteratorUtil.count(iterateNodes());
        }
        return nodeCount;
      }

      public void addNode(T n) {
        Assertions.UNREACHABLE();
      }

      public void removeNode(T n) {
        Assertions.UNREACHABLE();
      }

      public boolean containsNode(T N) {
        return f.accepts(N) && g.containsNode(N);
      }

    };
    final EdgeManager<T> e = new EdgeManager<T>() {

      public Iterator<T> getPredNodes(T N) {
        return new FilterIterator<T>(g.getPredNodes(N), f);
      }

      public int getPredNodeCount(T N) {
        return IteratorUtil.count(getPredNodes(N));
      }

      public Iterator<T> getSuccNodes(T N) {
        return new FilterIterator<T>(g.getSuccNodes(N), f);
      }

      public int getSuccNodeCount(T N) {
        return IteratorUtil.count(getSuccNodes(N));
      }

      public void addEdge(T src, T dst) {
        Assertions.UNREACHABLE();
      }

      public void removeEdge(T src, T dst) {
        Assertions.UNREACHABLE();
      }

      public void removeAllIncidentEdges(T node) {
        Assertions.UNREACHABLE();
      }

      public void removeIncomingEdges(T node) {
        Assertions.UNREACHABLE();
      }

      public void removeOutgoingEdges(T node) {
        Assertions.UNREACHABLE();
      }

      public boolean hasEdge(T src, T dst) {
        return g.hasEdge(src, dst) && f.accepts(src) && f.accepts(dst);
      }

    };
    AbstractGraph<T> output = new AbstractGraph<T>() {

      protected NodeManager<T> getNodeManager() {
        return n;
      }

      protected EdgeManager<T> getEdgeManager() {
        return e;
      }

    };

    return output;
  }
} 
