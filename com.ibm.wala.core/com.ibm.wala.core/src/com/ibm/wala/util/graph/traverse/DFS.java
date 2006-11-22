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
package com.ibm.wala.util.graph.traverse;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * utilities related to depth-first search.
 * 
 * @author Stephen Fink
 */
public class DFS {

  /**
   * Perform a DFS starting with a particular node and return the set of all
   * nodes visited.
   * 
   * @param C
   *          collection of nodes to start from
   * @param filter
   *          only traverse nodes that need this filter
   */
  @SuppressWarnings("serial")
  public static <T> Collection<T> getReachableNodes(final Graph<T> G, Collection<? extends T> C, final Filter filter) {
    Iterator<T> dfs = new SlowDFSFinishTimeIterator<T>(G, C.iterator()) {

      @SuppressWarnings("unchecked")
      protected Iterator<T> getConnected(T n) {
        return new FilterIterator<T>((Iterator<T>) G.getSuccNodes(n), filter);
      }
    };
    return new Iterator2Collection<T>(dfs);
  }

  /**
   * Perform a DFS starting with a particular node set and return the set of all
   * nodes visited.
   * 
   * @param G
   *          the graph containing n
   * @return Set
   */
  public static <T> Set<T> getReachableNodes(Graph<T> G, Collection<? extends T> C) {
    HashSet<T> result = HashSetFactory.make();
    Iterator<T> dfs = iterateFinishTime(G, C.iterator());
    while (dfs.hasNext()) {
      result.add(dfs.next());
    }
    return result;
  }

  // need to comment this out to avoid ambiguous type failure from javac
  // /**
  // * Perform a DFS starting with a particular node and return the set of all
  // * nodes visited.
  // *
  // * @param G
  // * the graph containing n
  // * @param n
  // * @return Set
  // */
  // public static <T> Set<T> getReachableNodes(Graph<T> G, T n) {
  // HashSet<T> result = HashSetFactory.make();
  // Iterator<T> dfs = iterateFinishTime(G, new NonNullSingletonIterator<T>(n));
  // while (dfs.hasNext()) {
  // result.add(dfs.next());
  // }
  // return result;
  // }

  /**
   * Perform a DFS and return the set of all nodes visited.
   * 
   * @param G
   *          the graph containing n
   * @return Set
   */
  public static <T> Set<T> getReachableNodes(Graph<T> G) {
    HashSet<T> result = HashSetFactory.make();
    Iterator<T> dfs = iterateFinishTime(G);
    while (dfs.hasNext()) {
      result.add(dfs.next());
    }
    return result;
  }

  /**
   * Perform a DFS of a graph starting with a specified node and return a sorted
   * list of nodes. The nodes are sorted by depth first order.
   * 
   * @param G
   *          a graph
   * @param n
   *          the initial node
   * @return a sorted set of nodes in the graph in depth first order
   */
  public static <T> SortedSet<T> sortByDepthFirstOrder(Graph<T> G, T n) {
    Map<T, Integer> order = HashMapFactory.make();
    TreeSet<T> result = new TreeSet<T>(new DFSComparator<T>(order));

    Iterator<T> dfs = iterateFinishTime(G, new NonNullSingletonIterator<T>(n));
    int i = 0;
    while (dfs.hasNext()) {
      T nxt = dfs.next();
      order.put(nxt, new Integer(i++));
      result.add(nxt);
    }
    return result;
  }

  /**
   * Comparator class to order the nodes in the DFS according to the depth first
   * order
   */
  static class DFSComparator<T> implements Comparator<T> {
    private Map<T, Integer> order;

    DFSComparator(Map<T, Integer> order) {
      this.order = order;
    }

    public int compare(T o1, T o2) {
      // throws an exception if either argument is not a Node object
      if (o1 == o2) {
        return 0;
      }
      Integer t1 = (Integer) order.get(o1);
      Integer t2 = (Integer) order.get(o2);
      // throws an exception if either node has not been ordered
      return (t1.intValue() - t2.intValue());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    Assertions.UNREACHABLE("define a custom hash code to avoid non-determinism");
    return 0;
  }

  /**
   * @param G
   * @return iterator of nodes of G in order of DFS discover time
   */
  public static <T> DFSDiscoverTimeIterator iterateDiscoverTime(Graph<T> G) {
    if (G instanceof NumberedGraph) {
      return new NumberedDFSDiscoverTimeIterator<T>((NumberedGraph<T>) G);
    } else {
      return new SlowDFSDiscoverTimeIterator<T>(G);
    }
  }

  /**
   * @param G
   * @param roots
   *          roots of traversal, in order to visit in outermost loop of DFS
   * @return iterator of nodes of G in order of DFS discover time
   */
  public static <T> DFSDiscoverTimeIterator iterateDiscoverTime(Graph<T> G, Iterator<T> roots) {
    if (G instanceof NumberedGraph) {
      return new NumberedDFSDiscoverTimeIterator<T>((NumberedGraph<T>) G, roots);
    } else {
      return new SlowDFSDiscoverTimeIterator<T>(G, roots);
    }
  }

  /**
   * @param G
   * @param N
   *          root of traversal
   * @return iterator of nodes of G in order of DFS discover time
   */
  public static <T> DFSDiscoverTimeIterator iterateDiscoverTime(Graph<T> G, T N) {
    if (G instanceof NumberedGraph) {
      return new NumberedDFSDiscoverTimeIterator<T>((NumberedGraph<T>) G, N);
    } else {
      return new SlowDFSDiscoverTimeIterator<T>(G, N);
    }
  }

  /**
   * @param G
   * @return iterator of nodes of G in order of DFS finish time
   */
  public static <T> DFSFinishTimeIterator<T> iterateFinishTime(Graph<T> G) {
    if (G instanceof NumberedGraph) {
      return new NumberedDFSFinishTimeIterator<T>((NumberedGraph<T>) G);
    } else {
      return new SlowDFSFinishTimeIterator<T>(G);
    }
  }

  /**
   * @param G
   * @param ie
   *          roots of traversal, in order to visit in outermost loop of DFS
   * @return iterator of nodes of G in order of DFS finish time
   */
  public static <T> DFSFinishTimeIterator<T> iterateFinishTime(Graph<T> G, Iterator<? extends T> ie) {
    if (G instanceof NumberedGraph) {
      return new NumberedDFSFinishTimeIterator<T>((NumberedGraph<T>) G, ie);
    } else {
      return new SlowDFSFinishTimeIterator<T>(G, ie);
    }
  }

  // need to comment out to avoid ambiguous type error from javac
  // /**
  // * @param G
  // * @param n
  // * @return iterator of nodes of G in order of DFS finish time
  // */
  // public static <T> DFSFinishTimeIterator<T> iterateFinishTime(Graph<T> G, T
  // n) {
  // if (G instanceof NumberedGraph) {
  // return new NumberedDFSFinishTimeIterator<T>((NumberedGraph<T>) G, n);
  // } else {
  // return new SlowDFSFinishTimeIterator<T>(G, n);
  // }
  // }
}
