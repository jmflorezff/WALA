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

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * @author sfink
 *
 */
public abstract class AbstractPointerAnalysis implements PointerAnalysis {

  /**
   * graph representation of pointer-analysis results
   */
  private HeapGraph heapGraph;
  /**
   * Governing call graph.
   */
  private final CallGraph cg;

  /**
   * bijection from InstanceKey <=>Integer
   */
  protected final MutableMapping<InstanceKey> instanceKeys;
  
  protected AbstractPointerAnalysis(CallGraph cg, MutableMapping<InstanceKey> instanceKeys) {
    this.cg = cg;
    this.instanceKeys = instanceKeys;
  }

  public HeapGraph getHeapGraph() {
    if (heapGraph == null) {
      heapGraph = new BasicHeapGraph(this, cg);
    }
    return heapGraph;
  }

  /**
   * @return Returns the callgraph.
   */
  protected CallGraph getCallGraph() {
    return cg;
  }

  public Collection<InstanceKey> getInstanceKeys() {
    return Collections.unmodifiableCollection(instanceKeys.getObjects());
  }

  public OrdinalSetMapping<InstanceKey> getInstanceKeyMapping() {
    return instanceKeys;
  }

}
