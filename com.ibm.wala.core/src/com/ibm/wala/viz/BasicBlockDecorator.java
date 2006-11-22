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
package com.ibm.wala.viz;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.graph.NodeDecorator;

public abstract class BasicBlockDecorator implements NodeDecorator {

  private CGNode currentNode;
  
  public BasicBlockDecorator() {
    super();
  }

  /**
   * @return Returns the currentNode.
   */
  public CGNode getCurrentNode() {
    return currentNode;
  }

  /**
   * @param currentNode The currentNode to set.
   */
  public void setCurrentNode(CGNode currentNode) {
    this.currentNode = currentNode;
  }
}
