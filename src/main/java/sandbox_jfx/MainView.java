package sandbox_jfx;

import org.controlsfx.control.PropertySheet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class MainView {
	@FXML
	public Region root;

	@FXML
	public ImageView image;

	@FXML
	public ComboBox<GeometryItem> geometries;

	@FXML
	public ColorPicker bgColor;

	@FXML
	public CheckBox showStats;

	@FXML
	public Slider rotX;

	@FXML
	public Slider rotY;

	@FXML
	private TitledPane matParamsPane;

	@FXML
	public BorderPane editorHost;

	@FXML
	public TextArea log;
	@FXML
	public TextArea vertex_glsl;
	@FXML
	public TextArea fragment_glsl;
	@FXML
	public Button refresh;

	public final PropertySheet matParams = new PropertySheet();

	@FXML
	public void initialize() {
		Pane p = (Pane)image.getParent();
		image.fitHeightProperty().bind(p.heightProperty());
		image.fitWidthProperty().bind(p.widthProperty());
		image.setFocusTraversable(true); // to receive KeyEvent
		//image.setFitWidth(0);
		//image.setFitHeight(0);
		ObservableList<GeometryItem> geometriesItems = FXCollections.observableArrayList(
				GeometryItem.newCube("cube")
				, GeometryItem.newSphere("sphere")
				, GeometryItem.newQuad("quad")
		);
		geometries.setItems(geometriesItems);

		matParamsPane.setContent(matParams);
	}

}