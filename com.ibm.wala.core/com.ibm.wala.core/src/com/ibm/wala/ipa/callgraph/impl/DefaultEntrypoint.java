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

package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * An entrypoint whose parameter types are the declared types.
 * 
 * @author sfink
 */
public class DefaultEntrypoint extends Entrypoint {
  private final TypeReference[][] paramTypes;
  private final ClassHierarchy cha;

  public DefaultEntrypoint(IMethod method, ClassHierarchy cha) {
    super(method);
    this.cha = cha;
    paramTypes = makeParameterTypes(method);
    if (Assertions.verifyAssertions) {
      Assertions._assert(paramTypes != null, method.toString());
    }
  }

  public DefaultEntrypoint(MethodReference method, ClassHierarchy cha) {
    super(method, cha);
    this.cha = cha;
    paramTypes = makeParameterTypes(getMethod());
    if (Assertions.verifyAssertions) {
      Assertions._assert(paramTypes != null, method.toString());
    }
  }

  /**
   * @param method
   */
  protected TypeReference[][] makeParameterTypes(IMethod method) {
    TypeReference[][] result = new TypeReference[method.getNumberOfParameters()][];
    for (int i = 0; i < result.length; i++) {
      result[i] = makeParameterTypes(method, i);
    }

    return result;
  }

  protected TypeReference[] makeParameterTypes(IMethod method, int i) {
    return new TypeReference[] { method.getParameterType(i)};
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.Entrypoint#getParameterTypes(int)
   */
  public TypeReference[] getParameterTypes(int i) {
    return paramTypes[i];
  }

  public void setParameterTypes(int i, TypeReference[] types) {
    paramTypes[i] = types;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.Entrypoint#getNumberOfParameters()
   */
  public int getNumberOfParameters() {
    return paramTypes.length;
  }

  public ClassHierarchy getCha() {
    return cha;
  }
}
