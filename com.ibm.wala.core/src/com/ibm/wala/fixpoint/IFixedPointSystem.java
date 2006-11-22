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
package com.ibm.wala.fixpoint;

import java.util.Iterator;

/**
 * Represents a set of {@link IFixedPointStatement}s to be solved by a
 * {@link IFixedPointSolver}
 */
public interface IFixedPointSystem {
  /**
   * adds a {@link IFixedPointSystemListener}to the list of listeners to which
   * changes to this {@link IFixedPointSystem}should be reported.
   */
  void addListener(IFixedPointSystemListener l);

  /**
   * removes a {@link IFixedPointSystemListener}to the list of listeners to
   * which changes to this {@link IFixedPointSystem}should be reported.
   */
  void removeListener(IFixedPointSystemListener l);

  /**
   * removes a given statement
   */
  void removeStatement(IFixedPointStatement statement);

  /**
   * Add a statement to the system
   */
  public void addStatement(IFixedPointStatement statement);

  /**
   * Return an Iterator of the {@link IFixedPointStatement}s in this system
   * 
   * @return Iterator <Constraint>
   */
  public Iterator getStatements();

  /**
   * Return an Iterator of the variables in this graph
   * 
   * @return Iterator <IVariable>
   */
  public Iterator getVariables();

  /**
   * @param s
   * @return true iff this system already contains an equation that is equal()
   *         to s
   */
  boolean containsStatement(IFixedPointStatement s);

  /**
   * @param v
   * @return true iff this system already contains a variable that is equal() to
   *         v.
   */
  boolean containsVariable(IVariable v);

  /**
   * @param v
   * @return Iterator <statement>, the statements that use the variable
   */
  Iterator getStatementsThatUse(IVariable v);

  /**
   * @param v
   * @return Iterator <statement>, the statements that def the variable
   */
  Iterator getStatementsThatDef(IVariable v);

  int getNumberOfStatementsThatUse(IVariable v);

  int getNumberOfStatementsThatDef(IVariable v);

  /**
   * reorder the statements in this system
   */
  void reorder();

}