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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.debug.Assertions;

/**
 * a local pointer key that carries a type filter
 * 
 * @author sfink
 */
public class LocalPointerKeyWithFilter extends LocalPointerKey implements FilteredPointerKey {

  private final IClass typeFilter;

  /**
   * 
   */
  public LocalPointerKeyWithFilter(CGNode node, int valueNumber, IClass typeFilter) {
    super(node,valueNumber);
    if (Assertions.verifyAssertions) {
      Assertions._assert(typeFilter != null);
    }
    this.typeFilter = typeFilter;
  }


  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerKey#getTypeFilter()
   */
  public IClass getTypeFilter() {
    return typeFilter;
  }

}
