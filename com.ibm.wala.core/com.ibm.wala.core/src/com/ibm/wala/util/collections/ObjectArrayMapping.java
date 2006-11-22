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
package com.ibm.wala.util.collections;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 *
 * A bit set mapping based on an immutable object array.
 * This is not terribly efficient, but is useful for prototyping.
 * 
 * @author sfink
 */
public class ObjectArrayMapping<T> implements OrdinalSetMapping<T> {

  private T[] array;
  /**
   * A mapping from object to Integer
   */
  private HashMap<T,Integer> map = HashMapFactory.make();

  /**
   * Constructor for ObjectArrayMapping.
   */
  public ObjectArrayMapping(final T[] array) {
    this.array = array;
    for (int i = 0; i < array.length; i++) {
      map.put(array[i], new Integer(i));
    }
  }


  /* (non-Javadoc)
   */
  public T getMappedObject(int n) {
    return array[n];
  }


  /* (non-Javadoc)
   */
  public int getMappedIndex(Object o) {
    if (Assertions.verifyAssertions) {
      if (map.get(o) == null) {
        Assertions.UNREACHABLE("unmapped object " + o);
      }
    }
    return map.get(o).intValue();
  }

  public boolean hasMappedIndex(Object o) {
    return map.get(o) != null;
  }


  /* (non-Javadoc)
   */
  public int getMappingSize() {
    return array.length;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.intset.OrdinalSetMapping#iterator()
   */
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.intset.OrdinalSetMapping#makeSingleton(int)
   */
  public OrdinalSet makeSingleton(int i) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }


  public int add(Object o) {
    Assertions.UNREACHABLE();
    return 0;
  }
}
