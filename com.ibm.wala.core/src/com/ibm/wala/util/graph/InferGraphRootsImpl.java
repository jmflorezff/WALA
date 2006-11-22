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

import com.ibm.wala.util.warnings.WalaException;

/**
 *  TODO: Move this somewhere.
 */
public class InferGraphRootsImpl {

  /*
   * (non-Javadoc)
   * 
   */
  public static <T> Collection<T> inferRoots(Graph<T> g) throws WalaException {
    HashSet<T> s = new HashSet<T>();
    for (Iterator<? extends T> it = g.iterateNodes(); it.hasNext();) {
      T node = it.next();
      if (g.getPredNodeCount(node) == 0) {
        s.add(node);
      }
    }
    return s;
  }
} // InferGraphRootsImpl
