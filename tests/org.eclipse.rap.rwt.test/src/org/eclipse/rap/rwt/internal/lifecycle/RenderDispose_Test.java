/*******************************************************************************
 * Copyright (c) 2002-2006 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/

package org.eclipse.rap.rwt.internal.lifecycle;

import java.io.IOException;
import junit.framework.TestCase;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.RWTFixture;
import org.eclipse.rap.rwt.events.*;
import org.eclipse.rap.rwt.internal.engine.PhaseListenerRegistry;
import org.eclipse.rap.rwt.lifecycle.*;
import org.eclipse.rap.rwt.widgets.*;
import com.w4t.Fixture;
import com.w4t.engine.lifecycle.*;
import com.w4t.engine.requests.RequestParams;
import com.w4t.util.browser.Ie6;

public class RenderDispose_Test extends TestCase {

  private final PreserveWidgetsPhaseListener preserveWidgetsPhaseListener
    = new PreserveWidgetsPhaseListener();

  public void testDispose() throws IOException {
    RWTLifeCycle lifeCycle = new RWTLifeCycle();
    // set up the test widget hierarchy
    Display display = new Display();
    Composite shell = new Shell( display , RWT.NONE );
    final Button button = new Button( shell, RWT.PUSH );
    String displayId = DisplayUtil.getAdapter( display ).getId();
    // first rendering: html document that contains the javaScript 'application'
    RWTFixture.fakeNewRequest();
    lifeCycle.execute();
    // second rendering: initial markup that constructs the above created
    // widget hierarchy (display, shell and button)
    RWTFixture.fakeNewRequest();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    lifeCycle.execute();
    // dispose of the button
    button.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( final SelectionEvent event ) {
        button.dispose();
      }
    } );
    RWTFixture.fakeNewRequest();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    String buttonId = WidgetUtil.getId( button );
    Fixture.fakeRequestParam( JSConst.EVENT_WIDGET_SELECTED, buttonId );
    lifeCycle.execute();
    String expected 
        = "org.eclipse.rap.rwt.EventUtil.suspendEventHandling();"
        + "var wm = org.eclipse.rap.rwt.WidgetManager.getInstance();"
        + "wm.dispose( \"w3\" );"
        + "qx.ui.core.Widget.flushGlobalQueues();"
        + "org.eclipse.rap.rwt.EventUtil.resumeEventHandling();";
    assertEquals( expected, Fixture.getAllMarkup() );
  }

  public void testDisposeNotYetInitialized() throws IOException {
    // set up the test widget hierarchy
    Display display = new Display();
    final Composite shell = new Shell( display , RWT.NONE );
    String displayId = DisplayUtil.getAdapter( display ).getId();
    // first rendering: html document that contains the javaScript 'application'
    RWTLifeCycle lifeCycle = new RWTLifeCycle();
    lifeCycle.execute();
    // second rendering: initial markup that constructs the above created
    // widget hierarchy (display, shell and button)
    RWTFixture.fakeNewRequest();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    lifeCycle.execute();
    // create and dispose of the button
    RWTFixture.fakeNewRequest();
    Fixture.fakeRequestParam( RequestParams.UIROOT, displayId );
    lifeCycle.addPhaseListener( new PhaseListener() {

      private static final long serialVersionUID = 1L;

      public void beforePhase( final PhaseEvent event ) {
        Button button = new Button( shell, RWT.PUSH );
        button.dispose();
      }

      public void afterPhase( final PhaseEvent event ) {
      }

      public PhaseId getPhaseId() {
        return PhaseId.RENDER;
      }
    } );
    lifeCycle.execute();
    String expected
      =   "org.eclipse.rap.rwt.EventUtil.suspendEventHandling();"
        + "qx.ui.core.Widget.flushGlobalQueues();"
        + "org.eclipse.rap.rwt.EventUtil.resumeEventHandling();";
    assertEquals( expected, Fixture.getAllMarkup() );
  }

  protected void setUp() throws Exception {
    RWTFixture.setUp();
    Fixture.fakeResponseWriter();
    Fixture.fakeBrowser( new Ie6( true, true ) );
    PhaseListenerRegistry.add( preserveWidgetsPhaseListener );
  }

  protected void tearDown() throws Exception {
    PhaseListenerRegistry.clear();
    RWTFixture.tearDown();
  }
}
