/*******************************************************************************
 * Copyright (c) 2007, 2015 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.internal.custom.scrolledcompositekit;

import static org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil.getId;
import static org.eclipse.rap.rwt.internal.protocol.RemoteObjectFactory.getRemoteObject;
import static org.eclipse.rap.rwt.testfixture.internal.TestMessage.getParent;
import static org.eclipse.rap.rwt.testfixture.internal.TestMessage.getStyles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.eclipse.rap.json.JsonArray;
import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.json.JsonValue;
import org.eclipse.rap.rwt.internal.lifecycle.RemoteAdapter;
import org.eclipse.rap.rwt.internal.lifecycle.WidgetUtil;
import org.eclipse.rap.rwt.internal.protocol.Operation.CreateOperation;
import org.eclipse.rap.rwt.internal.remote.RemoteObjectRegistry;
import org.eclipse.rap.rwt.remote.OperationHandler;
import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.rap.rwt.testfixture.internal.TestMessage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.widgets.controlkit.ControlLCATestUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ScrolledCompositeLCA_Test {

 private static final String PROP_SHOW_FOCUSED_CONTROL = "showFocusedControl";

  private Display display;
  private Shell shell;
  private ScrolledComposite sc;
  private ScrollBar hScroll;
  private ScrollBar vScroll;
  private ScrolledCompositeLCA lca;

  @Before
  public void setUp() {
    Fixture.setUp();
    display = new Display();
    shell = new Shell( display );
    sc = new ScrolledComposite( shell, SWT.H_SCROLL | SWT.V_SCROLL );
    hScroll = sc.getHorizontalBar();
    vScroll = sc.getVerticalBar();
    lca = new ScrolledCompositeLCA();
  }

  @After
  public void tearDown() {
    Fixture.tearDown();
  }

  @Test
  public void testCommonControlProperties() throws IOException {
    ControlLCATestUtil.testCommonControlProperties( sc );
  }

  @Test
  public void testPreserveValues() {
    RemoteAdapter adapter = WidgetUtil.getAdapter( sc );
    assertEquals( null, adapter.getPreserved( PROP_SHOW_FOCUSED_CONTROL ) );
    hScroll.setSelection( 23 );
    vScroll.setSelection( 42 );
    sc.setShowFocusedControl( true );
    assertEquals( 23, hScroll.getSelection() );
    assertEquals( 42, vScroll.getSelection() );
    Rectangle rectangle = new Rectangle( 12, 30, 20, 40 );
    sc.setBounds( rectangle );
    Fixture.markInitialized( display );
    Fixture.preserveWidgets();
    assertEquals( Boolean.TRUE, adapter.getPreserved( PROP_SHOW_FOCUSED_CONTROL ) );
  }

  @Test
  public void testReadData_ScrollBarsSelectionEvent() {
    sc.setContent( new Composite( sc, SWT.NONE ) );
    SelectionListener selectionListener = mock( SelectionListener.class );
    hScroll.addSelectionListener( selectionListener );
    vScroll.addSelectionListener( selectionListener );

    Fixture.fakeNotifyOperation( getId( hScroll ), "Selection", null );
    Fixture.fakeNotifyOperation( getId( vScroll ), "Selection", null );
    Fixture.readDataAndProcessAction( sc );

    verify( selectionListener, times( 2 ) ).widgetSelected( any( SelectionEvent.class ) );
  }

  @Test
  public void testRenderCreate() throws IOException {
    lca.renderInitialization( sc );

    TestMessage message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( sc );
    assertEquals( "rwt.widgets.ScrolledComposite", operation.getType() );
    List<String> styles = getStyles( operation );
    assertTrue( styles.contains( "H_SCROLL" ) );
    assertTrue( styles.contains( "V_SCROLL" ) );
  }

  @Test
  public void testRenderInitialization_setsOperationHandler() throws IOException {
    String id = getId( sc );
    lca.renderInitialization( sc );

    OperationHandler handler = RemoteObjectRegistry.getInstance().get( id ).getHandler();
    assertTrue( handler instanceof ScrolledCompositeOperationHandler );
  }

  @Test
  public void testReadData_usesOperationHandler() {
    ScrolledCompositeOperationHandler handler = spy( new ScrolledCompositeOperationHandler( sc ) );
    getRemoteObject( getId( sc ) ).setHandler( handler );

    Fixture.fakeNotifyOperation( getId( sc ), "Help", new JsonObject() );
    lca.readData( sc );

    verify( handler ).handleNotifyHelp( sc, new JsonObject() );
  }

  @Test
  public void testRenderParent() throws IOException {
    lca.renderInitialization( sc );

    TestMessage message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( sc );
    assertEquals( getId( sc.getParent() ), getParent( operation ) );
  }

  @Test
  public void testRenderInitialContent() throws IOException {
    lca.render( sc );

    TestMessage message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( sc );
    assertFalse( operation.getProperties().names().contains( "content" ) );
  }

  @Test
  public void testRenderContent() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    String contentId = WidgetUtil.getId( content );

    sc.setContent( content );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( contentId, message.findSetProperty( sc, "content" ).asString() );
  }

  @Test
  public void testRenderContentUnchanged() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );

    sc.setContent( content );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( sc, "content" ) );
  }

  @Test
  public void testRenderInitialOrigin() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    sc.setContent( content );

    lca.render( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( hScroll, "selection" ) );
    assertNull( message.findSetOperation( vScroll, "selection" ) );
  }

  @Test
  public void testRenderOrigin() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    sc.setContent( content );

    sc.setOrigin( 1, 2 );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    JsonArray expected = JsonArray.readFrom( "[ 1, 2 ]" );
    assertEquals( expected, message.findSetProperty( sc, "origin" ) );
  }

  @Test
  public void testRenderOrigin_SetByScrollbar() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    sc.setContent( content );

    hScroll.setSelection( 1 );
    vScroll.setSelection( 2 );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    JsonArray expected = JsonArray.readFrom( "[ 1, 2 ]" );
    assertEquals( expected, message.findSetProperty( sc, "origin" ) );
  }

  @Test
  public void testRenderOriginUnchanged() throws IOException {
    Composite content = new Composite( sc, SWT.NONE );
    sc.setContent( content );
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );

    sc.setOrigin( 1, 2 );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( hScroll, "selection" ) );
    assertNull( message.findSetOperation( vScroll, "selection" ) );
  }

  @Test
  public void testRenderInitialShowFocusedControl() throws IOException {
    lca.render( sc );

    TestMessage message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( sc );
    assertFalse( operation.getProperties().names().contains( "showFocusedControl" ) );
  }

  @Test
  public void testRenderShowFocusedControl() throws IOException {
    sc.setShowFocusedControl( true );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.TRUE, message.findSetProperty( sc, "showFocusedControl" ) );
  }

  @Test
  public void testRenderShowFocusedControlUnchanged() throws IOException {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );

    sc.setShowFocusedControl( true );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( sc, "showFocusedControl" ) );
  }

  @Test
  public void testRenderAddScrollBarsSelectionListener_Horizontal() throws Exception {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( hScroll );
    Fixture.preserveWidgets();

    hScroll.addSelectionListener( new SelectionAdapter() { } );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.TRUE, message.findListenProperty( hScroll, "Selection" ) );
  }

  @Test
  public void testRenderRemoveScrollBarsSelectionListener_Horizontal() throws Exception {
    SelectionListener listener = new SelectionAdapter() { };
    hScroll.addSelectionListener( listener );
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( hScroll );
    Fixture.preserveWidgets();

    hScroll.removeSelectionListener( listener );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.FALSE, message.findListenProperty( hScroll, "Selection" ) );
  }

  @Test
  public void testRenderScrollBarsSelectionListenerUnchanged_Horizontal() throws Exception {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( hScroll );
    Fixture.preserveWidgets();

    hScroll.addSelectionListener( new SelectionAdapter() { } );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( hScroll, "Selection" ) );
  }

  @Test
  public void testRenderAddScrollBarsSelectionListener_Vertical() throws Exception {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( vScroll );
    Fixture.preserveWidgets();

    vScroll.addSelectionListener( new SelectionAdapter() { } );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.TRUE, message.findListenProperty( vScroll, "Selection" ) );
  }

  @Test
  public void testRenderRemoveScrollBarsSelectionListener_Vertical() throws Exception {
    SelectionListener listener = new SelectionAdapter() { };
    vScroll.addSelectionListener( listener );
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( vScroll );
    Fixture.preserveWidgets();

    vScroll.removeSelectionListener( listener );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.FALSE, message.findListenProperty( vScroll, "Selection" ) );
  }

  @Test
  public void testRenderScrollBarsSelectionListenerUnchanged_Vertical() throws Exception {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( vScroll );
    Fixture.preserveWidgets();

    vScroll.addSelectionListener( new SelectionAdapter() { } );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( vScroll, "Selection" ) );
  }

  @Test
  public void testRenderInitialScrollBarsVisible() throws IOException {
    lca.render( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( hScroll, "visibility" ) );
    assertNull( message.findSetOperation( vScroll, "visibility" ) );
  }

  @Test
  public void testRenderScrollBarsVisible_Horizontal() throws IOException {
    hScroll.setVisible( true );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertEquals( JsonValue.TRUE, message.findSetProperty( hScroll, "visibility" ) );
    assertNull( message.findSetOperation( vScroll, "visibility" ) );
  }

  @Test
  public void testRenderScrollBarsVisible_Vertical() throws IOException {
    vScroll.setVisible( true );
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( hScroll, "visibility" ) );
    assertEquals( JsonValue.TRUE, message.findSetProperty( vScroll, "visibility" ) );
  }

  @Test
  public void testRenderScrollBarsVisibleUnchanged() throws IOException {
    Fixture.markInitialized( display );
    Fixture.markInitialized( sc );
    Fixture.markInitialized( hScroll );
    Fixture.markInitialized( vScroll );

    hScroll.setVisible( false );
    vScroll.setVisible( false );
    Fixture.preserveWidgets();
    lca.renderChanges( sc );

    TestMessage message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( hScroll, "visibility" ) );
    assertNull( message.findSetOperation( vScroll, "visibility" ) );
  }

}
