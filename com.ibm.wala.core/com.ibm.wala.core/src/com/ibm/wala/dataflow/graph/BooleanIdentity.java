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

import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.IVariable;


/**
 * Operator OUT = IN
 */
public class BooleanIdentity extends UnaryOperator {
  
  private final static BooleanIdentity SINGLETON = new BooleanIdentity();
  
  public static BooleanIdentity instance() {
    return SINGLETON;
  }
  
  private  BooleanIdentity() {
  }

  /* (non-Javadoc)
   */
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BooleanVariable L = (BooleanVariable) lhs;
    BooleanVariable R = (BooleanVariable) rhs;

    if (L.sameValue(R)) {
      return NOT_CHANGED;
    } else {
      L.copyState(R);
      return CHANGED;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Id ";
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  public int hashCode() {
    return 9802;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return (o instanceof BooleanIdentity);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.fixpoint.UnaryOperator#isIdentity()
   */
  public boolean isIdentity() {
    return true;
  }
}