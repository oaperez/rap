/*******************************************************************************
 * Copyright (c) 2008, 2011 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.events;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.rwt.Fixture;
import org.eclipse.rwt.internal.lifecycle.DisplayUtil;
import org.eclipse.rwt.internal.lifecycle.JSConst;
import org.eclipse.rwt.internal.service.RequestParams;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;


public class MouseEvent_Test extends TestCase {

  private static final String MOUSE_DOWN = "mouseDown|";
  private static final String MOUSE_UP = "mouseUp|";
  private static final String MOUSE_DOUBLE_CLICK = "mouseDoubleClick|";

  private String log;
  private Display display;
  private Shell shell;

  protected void setUp() throws Exception {
    Fixture.setUp();
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    display = new Display();
    shell = new Shell( display );
    log = "";
  }

  protected void tearDown() throws Exception {
    display.dispose();
    Fixture.tearDown();
  }

  public void testCopyFieldsFromUntypedEvent() {
    final List log = new ArrayList();
    Button button = new Button( shell, SWT.PUSH );
    button.addMouseListener( new MouseAdapter() {
      public void mouseDown( final MouseEvent event ) {
        log.add( event );
      }
    } );
    Object data = new Object();
    Event event = new Event();
    event.data = data;
    event.button = 2;
    event.x = 10;
    event.y = 20;
    event.stateMask = 23;
    event.time = 4711;
    button.notifyListeners( SWT.MouseDown, event );
    MouseEvent mouseEvent = ( MouseEvent )log.get( 0 );
    assertSame( button, mouseEvent.getSource() );
    assertSame( button, mouseEvent.widget );
    assertSame( display, mouseEvent.display );
    assertSame( data, mouseEvent.data );
    assertEquals( 10, mouseEvent.x );
    assertEquals( 20, mouseEvent.y );
    assertEquals( 2, mouseEvent.button );
    assertEquals( 23, mouseEvent.stateMask );
    assertEquals( 4711, mouseEvent.time );
    assertEquals( SWT.MouseDown, mouseEvent.getID() );
  }

  public void testAddRemoveListener() {
    MouseListener listener = new MouseListener() {
      public void mouseDoubleClick( MouseEvent e ) {
        log += MOUSE_DOUBLE_CLICK;
      }
      public void mouseDown( MouseEvent e ) {
        log += MOUSE_DOWN;
      }
      public void mouseUp( MouseEvent e ) {
        log += MOUSE_UP;
      }
    };
    MouseEvent.addListener( shell, listener );

    MouseEvent event;
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOWN );
    event.processEvent();
    event = new MouseEvent( shell, MouseEvent.MOUSE_UP );
    event.processEvent();
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOUBLE_CLICK );
    event.processEvent();
    assertEquals( MOUSE_DOWN + MOUSE_UP + MOUSE_DOUBLE_CLICK, log );

    log = "";
    MouseEvent.removeListener( shell, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOWN );
    event.processEvent();
    assertEquals( "", log );
  }

  public void testAddRemoveUntypedListener() {
    final java.util.List log = new ArrayList();
    Listener listener = new Listener() {
      public void handleEvent( Event event ) {
        log.add( event );
      }
    };
    // MouseDown
    shell.addListener( SWT.MouseDown, listener );
    MouseEvent event;
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOWN );
    event.processEvent();
    Event firedEvent = ( Event )log.get( 0 );
    assertEquals( SWT.MouseDown, firedEvent.type );
    log.clear();
    shell.removeListener( SWT.MouseDown, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOWN );
    event.processEvent();
    assertEquals( 0, log.size() );
    // MouseUp
    shell.addListener( SWT.MouseUp, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_UP );
    event.processEvent();
    firedEvent = ( Event )log.get( 0 );
    assertEquals( SWT.MouseUp, firedEvent.type );
    log.clear();
    shell.removeListener( SWT.MouseUp, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_UP );
    event.processEvent();
    assertEquals( 0, log.size() );
    // MouseDoubleCLick
    shell.addListener( SWT.MouseDoubleClick, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOUBLE_CLICK );
    event.processEvent();
    firedEvent = ( Event )log.get( 0 );
    assertEquals( SWT.MouseDoubleClick, firedEvent.type );
    log.clear();
    shell.removeListener( SWT.MouseDoubleClick, listener );
    event = new MouseEvent( shell, MouseEvent.MOUSE_DOUBLE_CLICK );
    event.processEvent();
    assertEquals( 0, log.size() );
  }

  public void testTypedMouseEventOrder() {
    final java.util.List events = new ArrayList();
    shell.setLocation( 100, 100 );
    shell.open();
    shell.addMouseListener( new MouseListener() {
      public void mouseDoubleClick( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseDown( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseUp( final MouseEvent event ) {
        events.add( event );
      }
    } );
    String displayId = DisplayUtil.getId( display );
    String shellId = WidgetUtil.getId( shell );
    int shellX = shell.getLocation().x;
    int shellY = shell.getLocation().y;
    // Simulate request that sends a mouseDown + mouseUp sequence
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseUpRequest( shellId, shellX + 30, shellY + 30 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 2, events.size() );
    MouseEvent mouseEvent = ( ( MouseEvent )events.get( 0 ) );
    assertEquals( MouseEvent.MOUSE_DOWN, mouseEvent.getID() );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    mouseEvent = ( ( MouseEvent )events.get( 1 ) );
    assertEquals( MouseEvent.MOUSE_UP, mouseEvent.getID() );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    assertTrue( ( mouseEvent.stateMask & SWT.BUTTON1 ) != 0 );
    // Simulate request that sends a mouseDown + mouseUp + dblClick sequence
    events.clear();
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseUpRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseDoubleClickRequest( shellId, shellX + 30, shellY + 30 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 3, events.size() );
    mouseEvent = ( ( MouseEvent )events.get( 0 ) );
    assertEquals( MouseEvent.MOUSE_DOWN, mouseEvent.getID() );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    assertTrue( ( mouseEvent.stateMask & SWT.BUTTON1 ) != 0 );
    mouseEvent = ( ( MouseEvent )events.get( 1 ) );
    assertEquals( MouseEvent.MOUSE_DOUBLE_CLICK, mouseEvent.getID() );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    assertTrue( ( mouseEvent.stateMask & SWT.BUTTON1 ) != 0 );
    mouseEvent = ( ( MouseEvent )events.get( 2 ) );
    assertEquals( MouseEvent.MOUSE_UP, mouseEvent.getID() );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    assertTrue( ( mouseEvent.stateMask & SWT.BUTTON1 ) != 0 );
  }

  public void testUntypedMouseEventOrder() {
    final java.util.List events = new ArrayList();
    shell.setLocation( 100, 100 );
    shell.open();
    shell.addListener( SWT.MouseDown, new Listener() {
      public void handleEvent( final Event event ) {
        events.add( event );
      }
    } );
    shell.addListener( SWT.MouseUp, new Listener() {
      public void handleEvent( final Event event ) {
        events.add( event );
      }
    } );
    shell.addListener( SWT.MouseDoubleClick, new Listener() {
      public void handleEvent( final Event event ) {
        events.add( event );
      }
    } );
    String displayId = DisplayUtil.getId( display );
    String shellId = WidgetUtil.getId( shell );
    int shellX = shell.getLocation().x;
    int shellY = shell.getLocation().y;
    // Simulate request that sends a mouseDown + mouseUp sequence
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseUpRequest( shellId, shellX + 30, shellY + 30 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 2, events.size() );
    Event mouseEvent = ( ( Event )events.get( 0 ) );
    assertEquals( SWT.MouseDown, mouseEvent.type );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    mouseEvent = ( ( Event )events.get( 1 ) );
    assertEquals( SWT.MouseUp, mouseEvent.type );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    // Simulate request that sends a mouseDown + mouseUp + dblClick sequence
    events.clear();
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseUpRequest( shellId, shellX + 30, shellY + 30 );
    fakeMouseDoubleClickRequest( shellId, shellX + 30, shellY + 30 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 3, events.size() );
    mouseEvent = ( ( Event )events.get( 0 ) );
    assertEquals( SWT.MouseDown, mouseEvent.type );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    mouseEvent = ( ( Event )events.get( 1 ) );
    assertEquals( SWT.MouseDoubleClick, mouseEvent.type );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
    mouseEvent = ( ( Event )events.get( 2 ) );
    assertEquals( SWT.MouseUp, mouseEvent.type );
    assertSame( shell, mouseEvent.widget );
    assertEquals( 1, mouseEvent.button );
    assertEquals( 28, mouseEvent.x );
    assertEquals( 28, mouseEvent.y );
  }

  public void testNoMouseEventOutsideClientArea() {
    final java.util.List events = new ArrayList();
    Menu menuBar = new Menu( shell, SWT.BAR );
    shell.setMenuBar( menuBar );
    shell.setLocation( 100, 100 );
    shell.open();
    shell.addMouseListener( new MouseListener() {
      public void mouseDoubleClick( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseDown( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseUp( final MouseEvent event ) {
        events.add( event );
      }
    } );
    String displayId = DisplayUtil.getId( display );
    String shellId = WidgetUtil.getId( shell );
    int shellX = shell.getLocation().x;
    int shellY = shell.getLocation().y;
    // Simulate request that sends a mouseDown + mouseUp on shell border
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 1, shellY + 1 );
    fakeMouseUpRequest( shellId, shellX + 1, shellY + 1 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 2, shell.getBorderWidth() );
    assertEquals( 0, events.size() );
    events.clear();
    // Simulate request that sends a mouseDown + mouseUp on shell titlebar
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 10, shellY + 10 );
    fakeMouseUpRequest( shellId, shellX + 10, shellY + 10 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 0, events.size() );
    events.clear();
    // Simulate request that sends a mouseDown + mouseUp on shell menubar
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( shellId, shellX + 24, shellY + 24 );
    fakeMouseUpRequest( shellId, shellX + 24, shellY + 24 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 0, events.size() );
  }

  public void testNoMouseEventOnScrollBars() {
    final java.util.List events = new ArrayList();
    Table table = new Table( shell, SWT.NONE );
    table.setSize( 100, 100 );
    for( int i = 0; i < 50; i++ ) {
      new TableItem( table, SWT.NONE);
    }
    table.addMouseListener( new MouseListener() {
      public void mouseDoubleClick( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseDown( final MouseEvent event ) {
        events.add( event );
      }
      public void mouseUp( final MouseEvent event ) {
        events.add( event );
      }
    } );
    assertEquals( new Rectangle( 0, 0, 85, 100 ), table.getClientArea() );
    String displayId = DisplayUtil.getId( display );
    String tableId = WidgetUtil.getId( table );
    // Simulate request that sends a mouseDown + mouseUp on scrollbar
    Fixture.fakeResponseWriter();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    fakeMouseDownRequest( tableId, 90, 10 );
    fakeMouseUpRequest( tableId, 90, 10 );
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 0, events.size() );
  }

  private static void fakeMouseDoubleClickRequest( final String shellId,
                                                   final int x,
                                                   final int y )
  {
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK, shellId );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_BUTTON, "1" );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_X,
                              String.valueOf( x ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_Y,
                              String.valueOf( y ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_TIME, "0" );
  }

  private static void fakeMouseUpRequest( final String shellId,
                                          final int x,
                                          final int y )
  {
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_UP, shellId );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_UP_BUTTON, "1" );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_UP_X, String.valueOf( x ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_UP_Y, String.valueOf( y ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_UP_TIME, "0" );
  }

  private static void fakeMouseDownRequest( final String shellId,
                                            final int x,
                                            final int y )
  {
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOWN, shellId );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOWN_BUTTON, "1" );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOWN_X, String.valueOf( x ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOWN_Y, String.valueOf( y ) );
    Fixture.fakeRequestParam( JSConst.EVENT_MOUSE_DOWN_TIME, "0" );
  }
}
