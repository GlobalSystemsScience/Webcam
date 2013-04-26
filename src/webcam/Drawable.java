package webcam;

import java.awt.Rectangle;

import javax.swing.JComponent;

public interface Drawable {

	public JComponent getComponent();
	
	public Rectangle getDrawRectangle();

}
