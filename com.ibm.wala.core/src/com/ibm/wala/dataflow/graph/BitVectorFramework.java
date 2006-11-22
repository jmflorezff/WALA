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
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * 
 * a basic implementation of the dataflow framework
 * 
 * @author sfink
 */
public class BitVectorFramework<T,L> extends BasicFramework<T> {

  private final OrdinalSetMapping<L> latticeValues;

  public BitVectorFramework(Graph<T> flowGraph, ITransferFunctionProvider<T> transferFunctionProvider, OrdinalSetMapping<L> latticeValues) {
    super(flowGraph,transferFunctionProvider);
    this.latticeValues = latticeValues;
  } 

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getLatticeValues()
   */
  public OrdinalSetMapping<L> getLatticeValues() {
    return latticeValues;
  }

}