/*******************************************************************************
 * Copyright (c) 2011 EclipseSource and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.scrollbarkit;

import org.eclipse.rwt.internal.theme.AbstractThemeAdapter;
import org.eclipse.rwt.internal.theme.WidgetMatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ScrollBar;

public class ScrollBarThemeAdapter extends AbstractThemeAdapter {
  
  protected void configureMatcher( WidgetMatcher matcher ) {
    matcher.addStyle( "HORIZONTAL", SWT.HORIZONTAL );
    matcher.addStyle( "VERTICAL", SWT.VERTICAL );
  }

  public int getScrollBarWidth( final ScrollBar scrollbar ) {
    return getCssDimension( "ScrollBar", "width", scrollbar );
  }
  
}
