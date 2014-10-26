package sandbox_jfx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
/**
 * see http://pixelduke.wordpress.com/2012/09/16/zooming-inside-a-scrollpane/
 * see https://gist.github.com/RupprechJo/7455537
 * @author dwayne
 */
public class ZoomableScrollPane extends Pane {
	Node content;
	Group zoomGroup = new Group();
	public Scale scaleTransform = new Scale(1, 1, 0, 0);
	public Translate translateTransform = new Translate(0, 0, 0);
	public final DoubleProperty zoomFactor = new SimpleDoubleProperty(1);

	public ZoomableScrollPane() {
	    zoomGroup.getTransforms().add(scaleTransform);
	    zoomGroup.getTransforms().add(translateTransform);

		zoomFactor.addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				scaleTransform.setX(newValue.doubleValue());
				scaleTransform.setY(newValue.doubleValue());
				requestLayout();
			}
		});
	}

	public ZoomableScrollPane(Node content) {
		this();
		setContent0(content);
	}

	public void setContent0(Node content) {
		this.content = content;
	    Group contentGroup = new Group();
	    contentGroup.getChildren().add(zoomGroup);
	    zoomGroup.getChildren().add(content);
	    super.getChildren().add(contentGroup);
	}

//	protected void layoutChildren() {
//		Pos pos = Pos.TOP_LEFT;
//		double width = getWidth();
//		double height = getHeight();
//		double top = getInsets().getTop();
//		double right = getInsets().getRight();
//		double left = getInsets().getLeft();
//		double bottom = getInsets().getBottom();
//		double contentWidth = (width - left - right)/zoomFactor.get();
//		double contentHeight = (height - top - bottom)/zoomFactor.get();
//		layoutInArea(content, left, top,
//				contentWidth, contentHeight,
//				0, null,
//				pos.getHpos(),
//				pos.getVpos());
//	}
}