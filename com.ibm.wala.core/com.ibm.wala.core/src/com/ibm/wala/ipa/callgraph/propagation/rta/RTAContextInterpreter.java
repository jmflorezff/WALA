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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * This object will analyze a method in a context and return information
 * needed for RTA.
 * 
 * @author sfink
 */
public interface RTAContextInterpreter {
  /**
   * Does this object understand the given method?
   * The caller had better check this before inquiring on other properties.
   */
  public boolean understands(CGNode node);
  /**
   * @return an Iterator of the types that may be allocated by a given
   * method in a given context.
   */
  public abstract Iterator<NewSiteReference> iterateNewSites(CGNode node, WarningSet warnings);
  /**
   * @return an Iterator of the call statements that may execute
   * in a given method for a given context
   */
  public abstract Iterator<CallSiteReference> iterateCallSites(CGNode node, WarningSet warnings);

  /**
   * @return iterator of FieldReference
   */
  public Iterator iterateFieldsRead(CGNode node, WarningSet warnings); 

  /**
   * @return iterator of FieldReference
   */
  public Iterator iterateFieldsWritten(CGNode node, WarningSet warnings); 

  /**
   * record that the "factory" method of a node should be interpreted to allocate a 
   * particular klass.
   * 
   * TODO: this is a little ugly, is there a better place to move this?
   * 
   * @param node
   * @param klass
   * @return true iff a NEW type was recorded, false if the type was previously recorded.
   */
  public boolean recordFactoryType(CGNode node, IClass klass);
  
  /**
   * Bind this object to a new object to track warnings
   * @param newWarnings
   */
  public void setWarnings(WarningSet newWarnings);
}