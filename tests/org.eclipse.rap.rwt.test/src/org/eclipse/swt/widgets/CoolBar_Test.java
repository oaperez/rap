/*******************************************************************************
 * Copyright (c) 2002, 2014 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.widgets;

import static org.eclipse.rap.rwt.testfixture.internal.SerializationTestUtil.serializeAndDeserialize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.rap.rwt.internal.lifecycle.PhaseId;
import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class CoolBar_Test {

  private Display display;
  private Shell shell;

  @Before
  public void setUp() {
    Fixture.setUp();
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    display = new Display();
    shell = new Shell( display );
  }

  @After
  public void tearDown() {
    Fixture.tearDown();
  }

  @Test
  public void testHierarchy() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );

    assertTrue( Composite.class.isAssignableFrom( bar.getClass() ) );
    assertSame( shell, bar.getParent() );
    assertSame( display, bar.getDisplay() );

    CoolItem item = new CoolItem( bar, SWT.NONE );
    assertEquals( 1, bar.getItemCount() );
    assertSame( display, item.getDisplay() );
    assertSame( bar, item.getParent() );
  }

  @Test
  public void testItems() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    assertEquals( 0, bar.getItemCount() );
    assertTrue( Arrays.equals( new CoolItem[ 0 ], bar.getItems() ) );

    CoolItem item = new CoolItem( bar, SWT.NONE );
    assertEquals( 1, bar.getItemCount() );
    assertSame( item, bar.getItems()[ 0 ] );
    assertSame( item, bar.getItem( 0 ) );
    assertEquals( 0, bar.indexOf( item ) );

    CoolBar anotherBar = new CoolBar( shell, SWT.NONE );
    CoolItem anotherItem = new CoolItem( anotherBar, SWT.NONE );
    assertEquals( -1, bar.indexOf( anotherItem ) );
  }

  @Test
  public void testIndexOnCreation() {
    CoolBar coolBar = new CoolBar( shell, SWT.NONE );
    CoolItem coolItem = new CoolItem( coolBar, SWT.NONE );
    coolItem.setText( "1" );
    assertSame( coolItem, coolBar.getItem( 0 ) );
    CoolItem coolItem2 = new CoolItem( coolBar, SWT.NONE, 0 );
    coolItem2.setText( "2" );
    assertSame( coolItem2, coolBar.getItem( 0 ) );
  }

  @Test
  public void testStyle() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    assertEquals( SWT.NO_FOCUS | SWT.HORIZONTAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.NO_FOCUS );
    assertEquals( SWT.NO_FOCUS | SWT.HORIZONTAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.H_SCROLL );
    assertEquals( SWT.NO_FOCUS | SWT.HORIZONTAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.FLAT );
    assertEquals( SWT.NO_FOCUS | SWT.FLAT | SWT.HORIZONTAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.VERTICAL );
    assertEquals( SWT.NO_FOCUS | SWT.VERTICAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.VERTICAL | SWT.FLAT );
    assertEquals( SWT.NO_FOCUS | SWT.VERTICAL | SWT.FLAT | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.HORIZONTAL );
    assertEquals( SWT.NO_FOCUS | SWT.HORIZONTAL | SWT.LEFT_TO_RIGHT, bar.getStyle() );

    bar = new CoolBar( shell, SWT.HORIZONTAL | SWT.FLAT );
    assertEquals( SWT.NO_FOCUS | SWT.HORIZONTAL | SWT.FLAT | SWT.LEFT_TO_RIGHT, bar.getStyle() );
  }

  @Test
  public void testIndexOf() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    CoolItem item = new CoolItem( bar, SWT.NONE );
    assertEquals( 0, bar.indexOf( item ) );

    item.dispose();
    try {
      bar.indexOf( item );
      fail( "indexOf must not answer for a disposed item" );
    } catch( IllegalArgumentException e ) {
      // expected
    }
    try {
      bar.indexOf( null );
      fail( "indexOf must not answer for null item" );
    } catch( IllegalArgumentException e ) {
      // expected
    }
  }

  @Test
  public void testDispose() {
    final java.util.List<Object> log = new ArrayList<Object>();
    DisposeListener disposeListener = new DisposeListener() {
      public void widgetDisposed( DisposeEvent event ) {
        log.add( event.getSource() );
      }
    };
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    bar.addDisposeListener( disposeListener );
    CoolItem item1 = new CoolItem( bar, SWT.NONE );
    item1.addDisposeListener( disposeListener );
    CoolItem item2 = new CoolItem( bar, SWT.NONE );
    item2.addDisposeListener( disposeListener );

    item1.dispose();
    assertTrue( item1.isDisposed() );
    assertEquals( 1, bar.getItemCount() );

    bar.dispose();
    assertTrue( bar.isDisposed() );
    assertTrue( item2.isDisposed() );

    assertSame( item1, log.get( 0 ) );
    assertSame( item2, log.get( 1 ) );
    assertSame( bar, log.get( 2 ) );
  }

  @Test
  public void testLocked() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    assertFalse( bar.getLocked() );
    bar.setLocked( true );
    assertTrue( bar.getLocked() );
  }

  @Test
  public void testItemOrder() {
    CoolBar bar = new CoolBar( shell, SWT.NONE );
    new CoolItem( bar, SWT.NONE );
    new CoolItem( bar, SWT.NONE );

    // Test initial itemOrder -> matches the order in which the items are added
    assertEquals( 0, bar.getItemOrder()[ 0 ] );
    assertEquals( 1, bar.getItemOrder()[ 1 ] );

    // Test setItemOrder with legal arguments
    bar.setItemOrder( new int[] { 1, 0 } );
    assertEquals( 0, bar.getItemOrder()[ 1 ] );
    assertEquals( 1, bar.getItemOrder()[ 0 ] );

    // Test setItemOrder with illegal arguments
    int[] expectedItemOrder = bar.getItemOrder();
    try {
      bar.setItemOrder( null );
      fail( "setItemOrder must not allow null-argument" );
    } catch( IllegalArgumentException e ) {
      // Ensure that nothing that itemOrder hasn't changed
      assertArrayEquals( expectedItemOrder, bar.getItemOrder() );
    }
    try {
      bar.setItemOrder( new int[] { 0, 5 } );
      fail( "setItemOrder must not allow argument with indics out of range" );
    } catch( IllegalArgumentException e ) {
      // Ensure that nothing that itemOrder hasn't changed
      assertArrayEquals( expectedItemOrder, bar.getItemOrder() );
    }
    try {
      bar.setItemOrder( new int[] { 0, 0 } );
      fail( "setItemOrder must not allow argument with duplicate indices" );
    } catch( IllegalArgumentException e ) {
      // Ensure that nothing that itemOrder hasn't changed
      assertArrayEquals( expectedItemOrder, bar.getItemOrder() );
    }
    try {
      bar.setItemOrder( new int[] { 1 } );
      String msg
        = "setItemOrder must not allow argument whose length doesn't match "
        + "the number of items";
      fail( msg );
    } catch( IllegalArgumentException e ) {
      // Ensure that nothing that itemOrder hasn't changed
      assertArrayEquals( expectedItemOrder, bar.getItemOrder() );
    }
  }

  @Test
  public void testComputeSize() {
    CoolBar coolBar = new CoolBar( shell, SWT.HORIZONTAL );
    Point expected = new Point( 0, 0 );
    assertEquals( expected, coolBar.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );

    createItem( coolBar );
    expected = new Point( 178, 30 );
    assertEquals( expected, coolBar.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );

    coolBar = new CoolBar( shell, SWT.VERTICAL );
    createItem( coolBar );
    expected = new Point( 168, 40 );
    assertEquals( expected, coolBar.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );

    coolBar = new CoolBar( shell, SWT.FLAT );
    createItem( coolBar );
    expected = new Point( 178, 30 );
    assertEquals( expected, coolBar.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );

    expected = new Point( 100, 100 );
    assertEquals( expected, coolBar.computeSize( 100, 100 ) );
  }

  @Test
  public void testDisposeWithFontDisposeInDisposeListener() {
    CoolBar coolBar = new CoolBar( shell, SWT.NONE );
    new CoolItem( coolBar, SWT.NONE );
    new CoolItem( coolBar, SWT.NONE );
    final Font font = new Font( display, "font-name", 10, SWT.NORMAL );
    coolBar.setFont( font );
    coolBar.addDisposeListener( new DisposeListener() {
      public void widgetDisposed( DisposeEvent event ) {
        font.dispose();
      }
    } );
    coolBar.dispose();
  }

  @Test
  public void testIsSerializable() throws Exception {
    CoolBar coolBar = new CoolBar( shell, SWT.NONE );
    new CoolItem( coolBar, SWT.NONE );

    CoolBar deserializedCoolBar = serializeAndDeserialize( coolBar );

    assertEquals( 1, deserializedCoolBar.getItemCount() );
  }

  private static CoolItem createItem( CoolBar coolBar ) {
    ToolBar toolBar = new ToolBar( coolBar, SWT.FLAT );
    for( int i = 0; i < 3; i++ ) {
      ToolItem item = new ToolItem( toolBar, SWT.PUSH );
      item.setText( "item " + i );
    }
    toolBar.pack();
    Point size = toolBar.getSize();
    CoolItem item = new CoolItem( coolBar, SWT.NONE );
    item.setControl( toolBar );
    Point preferred = item.computeSize( size.x, size.y );
    item.setPreferredSize( preferred );
    return item;
  }

}
