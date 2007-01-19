/**
 * 
 */
package org.eclipse.rap.demo.controls;

import org.eclipse.rap.rwt.layout.RowData;
import org.eclipse.rap.rwt.layout.RowLayout;
import org.eclipse.rap.rwt.widgets.*;

public class ListTab extends ExampleTab {

  private List list;

  public ListTab( TabFolder folder ) {
    super( folder, "List" );
  }

  void createStyleControls( ) {
    createStyleButton( "BORDER" );
    createVisibilityButton();
    createEnablementButton();
    createFontChooser();
  }

  void createExampleControls( Composite top ) {
    top.setLayout( new RowLayout() );
    int style = getStyle();
    list = new List( top, style );
    list.setLayoutData( new RowData(200, 200) );
    
    for( int i = 1; i <= 25; i++ ) {
      list.add( "Item " + i );
    }
    registerControl( list );
  }

}
