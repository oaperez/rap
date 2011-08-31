/*******************************************************************************
 * Copyright (c) 2002, 2011 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.tableitemkit;

import java.io.IOException;

import org.eclipse.rwt.internal.util.EncodingUtil;
import org.eclipse.rwt.lifecycle.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.internal.widgets.*;
import org.eclipse.swt.internal.widgets.tablekit.TableLCAUtil;
import org.eclipse.swt.widgets.*;


public final class TableItemLCA extends AbstractWidgetLCA {

  private static interface IRenderRunnable {
    void run() throws IOException;
  }

  static final String PROP_TEXTS = "texts";
  static final String PROP_IMAGES = "images";
  static final String PROP_CHECKED = "checked";
  static final String PROP_GRAYED = "grayed";
  static final String PROP_INDEX = "index";
  static final String PROP_SELECTED = "selected";
  static final String PROP_BACKGROUND = "background";
  static final String PROP_FOREGROUND = "foreground";
  static final String PROP_FONT = "font";
  static final String PROP_CELL_BACKGROUNDS = "cellBackgrounds";
  static final String PROP_CELL_FOREGROUNDS = "cellForegrounds";
  static final String PROP_CELL_FONTS = "cellFonts";
  static final String PROP_CACHED = "cached";
  static final String PROP_VARIANT = "variant";

  public void preserveValues( Widget widget ) {
    TableItem item = ( TableItem )widget;
    Table table = item.getParent();
    int index = item.getParent().indexOf( item );
    IWidgetAdapter adapter = WidgetUtil.getAdapter( item );
    // don't resolve items unintentionally
    if( isCached( table, index ) ) {
      ItemLCAUtil.preserve( item );
      if( ( table.getStyle() & SWT.CHECK ) != 0 ) {
        adapter.preserve( PROP_CHECKED, Boolean.valueOf( item.getChecked() ) );
        adapter.preserve( PROP_GRAYED, Boolean.valueOf( item.getGrayed() ) );
      }
      adapter.preserve( PROP_TEXTS, getTexts( item ) );
      adapter.preserve( PROP_IMAGES, getImages( item ) );
      adapter.preserve( PROP_INDEX, new Integer( index ) );
      adapter.preserve( PROP_SELECTED, Boolean.valueOf( isSelected( table, index ) ) );
      Object itemAdapter = item.getAdapter( ITableItemAdapter.class );
      ITableItemAdapter tableItemAdapter = ( ITableItemAdapter )itemAdapter;
      adapter.preserve( PROP_BACKGROUND, tableItemAdapter.getUserBackground() );
      adapter.preserve( PROP_FOREGROUND, tableItemAdapter.getUserForeground() );
      adapter.preserve( PROP_FONT, tableItemAdapter.getUserFont() );
      adapter.preserve( PROP_CELL_BACKGROUNDS, tableItemAdapter.getCellBackgrounds() );
      adapter.preserve( PROP_CELL_FOREGROUNDS, tableItemAdapter.getCellForegrounds() );
      adapter.preserve( PROP_CELL_FONTS, tableItemAdapter.getCellFonts() );
      adapter.preserve( PROP_VARIANT, WidgetUtil.getVariant( widget ) );
    }
    adapter.preserve( PROP_CACHED, Boolean.valueOf( isCached( table, index ) ) );
  }

  public void readData( Widget widget ) {
    TableItem item = ( TableItem )widget;
    readChecked( item );
  }

  public void renderInitialization( final Widget widget ) throws IOException {
    TableItem tableItem = ( TableItem )widget;
    JSWriter writer = JSWriter.getWriterFor( widget );
    Table parent = tableItem.getParent();
    Integer index  = new Integer( tableItem.getParent().indexOf( tableItem ) );
    Object[] args = new Object[] { parent, index, WidgetUtil.getId( widget ) };
    writer.callStatic( "org.eclipse.rwt.widgets.TreeItem.createItem", args );
  }

  public void renderChanges( Widget widget ) throws IOException {
    final TableItem item = ( TableItem )widget;
    if( wasCleared( item ) ) {
      writeClear( item );
      writeSelection( item );
    } else {
      Table table = item.getParent();
      if( isCached( table, table.indexOf( item ) ) ) {
        preservingInitialized( item, new IRenderRunnable() {
          public void run() throws IOException {
            // items that were uncached and are now cached (materialized) are
            // handled as if they were just created (initialized = false)
            if( !wasCached( item ) ) {
              setInitialized( item, false );
            }
            writeChanges( item );
          }
        } );
      }
    }
  }

  /* (intentionally not JavaDoc'ed)
   * The client-side representation of a TableItem is not a qooxdoo widget.
   * Therefore the standard mechanism for dispoing of a widget is not used.
   */
  public void renderDispose( Widget widget ) throws IOException {
    TableItem item = ( TableItem )widget;
    if( !isParentDisposed( item ) ) {
      JSWriter writer = JSWriter.getWriterFor( item );
      writer.call( "dispose", null );
    }
  }

  //////////////////
  // ReadData helper

  private void readChecked( final TableItem item ) {
    String value = WidgetLCAUtil.readPropertyValue( item, "checked" );
    if( value != null ) {
      item.setChecked( Boolean.valueOf( value ).booleanValue() );
    }
  }

  ///////////////////////
  // RenderChanges helper

  private static void writeChanges( final TableItem item ) throws IOException {
    writeTexts( item );
    writeImages( item );
    writeBackground( item );
    writeForeground( item );
    writeFont( item );
    writeCellBackgrounds( item );
    writeCellForegrounds( item );
    writeCellFonts( item );
    writeChecked( item );
    writeGrayed( item );
    writeSelection( item );
    writeVariant( item );
    if( isVisible( item ) ) {
      Table table = item.getParent();
      TableLCAUtil.hasAlignmentChanged( table );
      hasIndexChanged( item );
    }
    writeFocused( item );
  }

  private static void writeClear( final TableItem item ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( item );
    writer.call( "clear", null );
  }

  private static boolean writeTexts( final TableItem item ) throws IOException {
    String[] texts = getTexts( item );
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_TEXTS, texts );
    if( result ) {
      transformTexts( item, texts );
      JSWriter writer = JSWriter.getWriterFor( item );
      writer.set( "texts", new Object[] { texts } );
    }
    return result;
  }

  private static void transformTexts( TableItem item, String[] texts ) {
    for( int i = 0; i < texts.length; i++ ) {
      if( isRichTextEnabled( item ) && RichTextParser.isRichText( texts[ i ] ) ) {
        RichTextToHtmlTransformer transformer = new RichTextToHtmlTransformer( item );
        RichTextParser richTextParser = new RichTextParser( transformer );
        richTextParser.parse( texts[ i ] );
        texts[ i ] = transformer.getHtml();
      } else {
        texts[ i ] = WidgetLCAUtil.escapeText( item.getText( i ), false );
        texts[ i ] = EncodingUtil.replaceWhiteSpaces( texts[ i ] );
      }
    }
  }
  
  private static boolean writeImages( TableItem item ) throws IOException {
    Image[] images = getImages( item );
    Image[] defValue = new Image[ images.length ];
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_IMAGES, images, defValue );
    if( result ) {
      JSWriter writer = JSWriter.getWriterFor( item );
      String[] imagePaths = new String[ images.length ];
      for( int i = 0; i < imagePaths.length; i++ ) {
        imagePaths[ i ] = ImageFactory.getImagePath( images[ i ] );
      }
      writer.set( "images", new Object[] { imagePaths } );
    }
    return result;
  }

  private static boolean writeBackground( final TableItem item ) throws IOException {
    ITableItemAdapter adapter = ( ITableItemAdapter )item.getAdapter( ITableItemAdapter.class );
    Color background = adapter.getUserBackground();
    JSWriter writer = JSWriter.getWriterFor( item );
    return writer.set( PROP_BACKGROUND, "background", background, null );
  }

  private static boolean writeForeground( final TableItem item ) throws IOException {
    Object adapter = item.getAdapter( ITableItemAdapter.class );
    ITableItemAdapter tableItemAdapter = ( ITableItemAdapter )adapter;
    Color foreground = tableItemAdapter.getUserForeground();
    JSWriter writer = JSWriter.getWriterFor( item );
    return writer.set( PROP_FOREGROUND, "foreground", foreground, null );
  }

  private static boolean writeFont( final TableItem item ) throws IOException {
    Object adapter = item.getAdapter( ITableItemAdapter.class );
    ITableItemAdapter tableItemAdapter = ( ITableItemAdapter )adapter;
    Font font = tableItemAdapter.getUserFont();
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_FONT, font, null );
    if( result ) {
      JSWriter writer = JSWriter.getWriterFor( item );
      String fontCss = font != null ? toCss( font ) : null;
      writer.set( "font", fontCss );
    }
    return result;
  }

  private static boolean writeCellBackgrounds( TableItem item ) throws IOException {
    ITableItemAdapter adapter = ( ITableItemAdapter )item.getAdapter( ITableItemAdapter.class );
    Color[] backgrounds = adapter.getCellBackgrounds();
    // default values are null
    Color[] defValue = new Color[ getColumnCount( item ) ];
    JSWriter writer = JSWriter.getWriterFor( item );
    return writer.set( PROP_CELL_BACKGROUNDS, "cellBackgrounds", backgrounds, defValue );
  }

  private static boolean writeCellForegrounds( TableItem item ) throws IOException {
    ITableItemAdapter adapter = ( ITableItemAdapter )item.getAdapter( ITableItemAdapter.class );
    Color[] foregrounds = adapter.getCellForegrounds();
    // default values are null
    Color[] defValue = new Color[ getColumnCount( item ) ];
    JSWriter writer = JSWriter.getWriterFor( item );
    return writer.set( PROP_CELL_FOREGROUNDS, "cellForegrounds", foregrounds, defValue );
  }

  private static boolean writeCellFonts( final TableItem item ) throws IOException {
    Object adapter = item.getAdapter( ITableItemAdapter.class );
    ITableItemAdapter tableItemAdapter = ( ITableItemAdapter )adapter;
    Font[] fonts = tableItemAdapter.getCellFonts();
    // default values are null
    Font[] defValue = new Font[ fonts.length ];
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_CELL_FONTS, fonts, defValue );
    if( result ) {
      String[] css = new String[ fonts.length ];
      for( int i = 0; i < fonts.length; i++ ) {
        css[ i ] = fonts[ i ] != null ? toCss( fonts[ i ] ) : null;
      }
      JSWriter writer = JSWriter.getWriterFor( item );
      writer.set( "cellFonts", new Object[] { css } );
    }
    return result;
  }

  private static boolean writeChecked( final TableItem item ) throws IOException {
    boolean result;
    if( ( item.getParent().getStyle() & SWT.CHECK ) != 0 ) {
      JSWriter writer = JSWriter.getWriterFor( item );
      Boolean newValue = Boolean.valueOf( item.getChecked() );
      result = writer.set( PROP_CHECKED, "checked", newValue, Boolean.FALSE );
    } else {
      result = false;
    }
    return result;
  }

  private static boolean writeGrayed( final TableItem item ) throws IOException {
    boolean result;
    if( ( item.getParent().getStyle() & SWT.CHECK ) != 0 ) {
      JSWriter writer = JSWriter.getWriterFor( item );
      Boolean newValue = Boolean.valueOf( item.getGrayed() );
      result = writer.set( PROP_GRAYED, "grayed", newValue, Boolean.FALSE );
    } else {
      result = false;
    }
    return result;
  }

  private static boolean writeSelection( final TableItem item ) throws IOException {
    Boolean newValue = Boolean.valueOf( isSelected( item ) );
    Boolean defValue = Boolean.FALSE;
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_SELECTED, newValue, defValue );
    if( result ) {
      JSWriter writer = JSWriter.getWriterFor( item.getParent() );
      String jsFunction = isSelected( item ) ? "selectItem" : "deselectItem";
      writer.call( jsFunction, new Object[]{ item } );
    }
    return result;
  }

  // TODO [rh] check if necessary to honor focusIndex == -1, would mean to
  //      call jsTable.setFocusIndex( -1 ) in TableLCA
  private static void writeFocused( final TableItem item ) throws IOException {
    if( TableLCAUtil.hasFocusIndexChanged( item.getParent() ) && isFocused( item ) ) {
      JSWriter writer = JSWriter.getWriterFor( item.getParent() );
      writer.set( "focusItem", new Object[]{ item } );
    }
  }

  private static boolean writeVariant( final TableItem item ) throws IOException {
    JSWriter writer = JSWriter.getWriterFor( item );
    String variant = WidgetUtil.getVariant( item );
    boolean result = WidgetLCAUtil.hasChanged( item, PROP_VARIANT, variant, null );
    if( result ) {
      Object[] args = new Object[] { "variant_" + variant };
      writer.set( "variant", args );
    }
    return result;
  }

  private static String toCss( Font font ) {
    StringBuffer result = new StringBuffer();
    FontData fontData = FontUtil.getData( font );
    if( ( fontData.getStyle() & SWT.ITALIC ) != 0 ) {
      result.append( "italic " );
    }
    if( ( fontData.getStyle() & SWT.BOLD ) != 0 ) {
      result.append( "bold " );
    }
    result.append( fontData.getHeight() );
    result.append( "px " );
    // TODO [rh] preliminary: low budget font-name-escaping
    String escapedName = fontData.getName().replaceAll( "\"", "" );
    result.append( escapedName );
    return result.toString();
  }

  private static boolean hasIndexChanged( TableItem item ) {
    int index = item.getParent().indexOf( item );
    return WidgetLCAUtil.hasChanged( item, PROP_INDEX, new Integer( index ) );
  }

  //////////////////////
  // Item data accessors

  static String[] getTexts( final TableItem item ) {
    int columnCount = getColumnCount( item );
    String[] result = new String[ columnCount ];
    for( int i = 0; i < columnCount; i++ ) {
      result[ i ] = item.getText( i );
    }
    return result;
  }

  static Image[] getImages( final TableItem item ) {
    int columnCount = getColumnCount( item );
    Image[] result = new Image[ columnCount ];
    for( int i = 0; i < columnCount; i++ ) {
      result[ i ] = item.getImage( i );
    }
    return result;
  }

  private static int getColumnCount( final TableItem item ) {
    return Math.max( 1, item.getParent().getColumnCount() );
  }

  private static boolean isSelected( final TableItem item ) {
    Table table = item.getParent();
    int index = table.indexOf( item );
    return isSelected( table, index );
  }

  private static boolean isSelected( final Table table, final int itemIndex ) {
    return itemIndex != -1 && table.isSelected( itemIndex );
  }

  private static boolean isFocused( final TableItem item ) {
    int focusIndex = getTableAdapter( item ).getFocusIndex();
    return focusIndex != -1 && item == item.getParent().getItem( focusIndex );
  }

  private static boolean isVisible( final TableItem item ) {
    return getTableAdapter( item ).isItemVisible( item );
  }

  private static boolean wasCleared( final TableItem item ) {
    Table table = item.getParent();
    boolean cached = isCached( table, table.indexOf( item ) );
    boolean wasCached = wasCached( item );
    return !cached && wasCached;
  }

  private static boolean isCached( final Table table, final int index ) {
    ITableAdapter adapter = ( ITableAdapter )table.getAdapter( ITableAdapter.class );
    return !adapter.isItemVirtual( index );
  }

  private static boolean wasCached( final TableItem item ) {
    boolean wasCached;
    IWidgetAdapter adapter = WidgetUtil.getAdapter( item );
    if( adapter.isInitialized() ) {
      Boolean preserved = ( Boolean )adapter.getPreserved( PROP_CACHED );
      wasCached = Boolean.TRUE.equals( preserved );
    } else {
      wasCached = true;
    }
    return wasCached;
  }

  //////////////////
  // helping methods

  private static ITableAdapter getTableAdapter( final TableItem item ) {
    return ( ITableAdapter )item.getParent().getAdapter( ITableAdapter.class );
  }

  private static void preservingInitialized( TableItem item, IRenderRunnable runnable )
    throws IOException
  {
    boolean initialized = WidgetUtil.getAdapter( item ).isInitialized();
    runnable.run();
    setInitialized( item, initialized );
  }

  private static void setInitialized( TableItem item, boolean initialized ) {
    WidgetAdapter adapter = ( WidgetAdapter )item.getAdapter( IWidgetAdapter.class );
    adapter.setInitialized( initialized );
  }

  private static boolean isRichTextEnabled( TableItem item ) {
    ITableItemAdapter adapter = ( ITableItemAdapter )item.getAdapter( ITableItemAdapter.class );
    return adapter.isRichTextEnabled();
  }

  private boolean isParentDisposed( TableItem item ) {
    ITableItemAdapter adapter = ( ITableItemAdapter )item.getAdapter( ITableItemAdapter.class );
    return adapter.isParentDisposed();
  }
}