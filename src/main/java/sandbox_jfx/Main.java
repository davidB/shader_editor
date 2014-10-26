package sandbox_jfx;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.controlsfx.control.PopOver;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;


import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import jfxtras.scene.control.window.CloseIcon;
import jfxtras.scene.control.window.MinimizeIcon;
import jfxtras.scene.control.window.Window;
import lombok.RequiredArgsConstructor;
import rx.Observable;


import com.jme3.asset.DesktopAssetManager;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3x.jfx.FxPlatformExecutor;
import com.jme3x.jfx.injfx.JmeForImageView;


import eu.mihosoft.vrl.workflow.Connector;
import eu.mihosoft.vrl.workflow.FlowFactory;
import eu.mihosoft.vrl.workflow.VFlow;
import eu.mihosoft.vrl.workflow.VNode;
import eu.mihosoft.vrl.workflow.fx.FXFlowNodeSkin;
import eu.mihosoft.vrl.workflow.fx.FXSkinFactory;
import eu.mihosoft.vrl.workflow.fx.ScalableContentPane;
import eu.mihosoft.vrl.workflow.fx.VCanvas;

public class Main extends Application {
	final static String WIP_PATH = "wip";
	final static String SHADER_VERT_PATH = WIP_PATH + "/shader.vert";
	final static String SHADER_FRAG_PATH = WIP_PATH + "/shader.frag";


	public static void main(String[] args) {
		launch(args);
	}

	public static MainView mainController() throws Exception {
		final FXMLLoader fxmlLoader = new FXMLLoader();
		final URL location = Thread.currentThread().getContextClassLoader().getResource(MainView.class.getCanonicalName().replace('.', '/') + ".fxml");
		fxmlLoader.setLocation(location);
		//final ResourceBundle defaultRessources = fxmlLoader.getResources();
		//fxmlLoader.setResources(this.addCustomRessources(defaultRessources));
		fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
		fxmlLoader.load(location.openStream());
		return fxmlLoader.getController();
	}

	@Override
	public void start(Stage stage) throws Exception {
		MainView controller = mainController();

		JmeForImageView jme = new JmeForImageView();
		jme.bind(controller.image);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		      public void handle(WindowEvent e){
				jme.stop(true);
		      }
		});
		Observable<ChaseCamera> fcam = Observable.from(jme.enqueue((jmeApp) -> {
			jmeApp.getAssetManager().registerLocator(WIP_PATH, WipLocator.class);
			Node prev = new Node("preview");
			jmeApp.getRootNode().attachChild(prev);
			ChaseCamera ccam = new ChaseCamera(jmeApp.getCamera()) {
			    public void setEnabled(boolean enabled) {
			        this.enabled = enabled;
			        this.canRotate = enabled;
			    }
			};
			prev.addControl(ccam);
			return ccam;
		}));

		//TODO delay after the last update
		controller.fragment_glsl.textProperty().addListener((ov, o, n) -> {
			System.err.println("change frag");
			WipLocator.setContent(SHADER_FRAG_PATH, n);
		});
		controller.fragment_glsl.textProperty().set(read("Shaders/Test.frag"));

		//TODO delay after the last update
		controller.vertex_glsl.textProperty().addListener((ov, o, n) -> {
			WipLocator.setContent(SHADER_VERT_PATH, n);
		});
		controller.vertex_glsl.textProperty().set(read("Shaders/Test.vert"));

		controller.refresh.setOnAction((evt) -> {
			refreshPreview(controller, jme);
		});

		controller.geometries.valueProperty().addListener((ov, o, n) -> {
			refreshPreview(controller, jme);
		});
		controller.geometries.setValue(controller.geometries.getItems().get(0));

		fcam.subscribe((cam) -> {
			try {
				final Vector3f lastPosition = new Vector3f();
				cam.setEnabled(true);
				cam.setRotationSpeed(0.1f);
				cam.setMaxVerticalRotation(FastMath.TWO_PI);
				//cam.setDragToRotate(true); // <-- NPE because inputMnager is null
				javafx.scene.Node evtReceiver = (javafx.scene.Node) controller.image;
				evtReceiver.getParent().setMouseTransparent(false);
				evtReceiver.setFocusTraversable(false);
				evtReceiver.addEventHandler(MouseEvent.ANY, (e) -> {
					//System.err.println("mouseEvent " + e);
					if (MouseEvent.MOUSE_ENTERED.equals(e.getEventType())) {
						controller.image.requestFocus(); // ?
						e.consume();
					} else if (MouseEvent.MOUSE_PRESSED.equals(e.getEventType())  /*&& dragModeActiveProperty.get()*/) {
						lastPosition.x = (float)e.getX();
						lastPosition.y = (float)e.getY();
						e.consume();
					} else if (MouseEvent.MOUSE_DRAGGED.equals(e.getEventType())) {
						float deltaX = (float) e.getX() - lastPosition.x;
						float deltaY = (float) e.getY() - lastPosition.y;
						cam.onAnalog(ChaseCamera.ChaseCamMoveRight, deltaX, 0);
						cam.onAnalog(ChaseCamera.ChaseCamUp, deltaY, 0);
						lastPosition.x = (float)e.getX();
						lastPosition.y = (float)e.getY();
						e.consume();
					}
				});
				evtReceiver.addEventHandler(KeyEvent.KEY_PRESSED, (e) -> {
					System.err.println("keyEvent " + e);
					if (KeyCode.A == e.getCode()) {
						cam.onAnalog(ChaseCamera.ChaseCamZoomIn, 0.2f, 0);
						e.consume();
					} else if (KeyCode.Q == e.getCode()) {
						cam.onAnalog(ChaseCamera.ChaseCamZoomOut, 0.2f, 0);
						e.consume();
					}
				});
			} catch(Exception exc) {
				exc.printStackTrace();
			}
		});

		controller.bgColor.valueProperty().addListener((ov, o, n) -> {
			jme.enqueue((app) -> {
				app.getViewPort().setBackgroundColor(new ColorRGBA((float)n.getRed(), (float)n.getGreen(), (float)n.getBlue(), (float)n.getOpacity()));
				return null;
			});
		});
		controller.bgColor.setValue(Color.LIGHTGRAY);

		controller.showStats.selectedProperty().addListener((ov, o, n) -> {
			jme.enqueue((app) -> {
				app.setDisplayStatView(n);
				app.setDisplayFps(n);
				return null;
			});
		});
		controller.showStats.setSelected(!controller.showStats.isSelected());

		controller.rotX.valueProperty().addListener((ov, o, n) -> jme.enqueue((app) -> {
			Spatial spatial = app.getRootNode().getChild("preview");
			Quaternion quat = new Quaternion();
			quat.fromAngles(n.floatValue() * FastMath.DEG_TO_RAD, ((float)controller.rotY.getValue()) * FastMath.DEG_TO_RAD, 0);
			spatial.setLocalRotation(quat);
			return null;
		}));
		controller.rotY.valueProperty().addListener((ov, o, n) -> jme.enqueue((app) -> {
			Spatial spatial = app.getRootNode().getChild("preview");
			Quaternion quat = new Quaternion();
			quat.fromAngles(((float)controller.rotX.getValue()) * FastMath.DEG_TO_RAD, n.floatValue() * FastMath.DEG_TO_RAD, 0);
			spatial.setLocalRotation(quat);
			return null;
		}));

		controller.editorHost.centerProperty().set(makeWorkflow());
		Scene scene = new Scene(controller.root, 600, 400);

//		if (Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW)) {
//			Application.setUserAgentStylesheet(this.getClass().getResource("modena-custom.css").toExternalForm());
//		}
		scene.getStylesheets().add(this.getClass().getResource("gui.css").toExternalForm());
		//makeInternalWindow((Pane)controller.root);
		stage.setTitle("Shader Editor");
		stage.setScene(scene);
		stage.show();
		//makeSubWindow();
	}

	//TODO add trottle to avoid to overload
	void refreshPreview(MainView controller, JmeForImageView jme) {
		final Geometry geom = controller.geometries.valueProperty().get().geometry;
		jme.enqueue((jmeApp) -> {
			((DesktopAssetManager)jmeApp.getAssetManager()).clearCache();

			Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
			mat.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
			mat.getParams().stream().forEach((v) -> System.out.printf("m n: %s, v: %s, t: %s\n", v.getName(), v.getValue(), v.getVarType()));
			mat.getMaterialDef().getMaterialParams().stream().forEach((v) -> System.out.printf("md n: %s, v: %s, t: %s\n", v.getName(), v.getValue(), v.getVarType()));
			//geom.setMaterial(mat);

			TechniqueDef technique = new TechniqueDef(null);
			technique.setShaderFile(SHADER_VERT_PATH, SHADER_FRAG_PATH, Caps.GLSL110.name(), Caps.GLSL110.name());
			technique.addWorldParam("WorldViewProjectionMatrix");
			//technique.addWorldParam(name);
			technique.getWorldBindings().stream().forEach((v) -> System.out.printf("td n: %s\n", v));
			MaterialDef def = new MaterialDef(jmeApp.getAssetManager(), "matdef0");
			def.addTechniqueDef(technique);
			Material mat2 = new Material(def);
			geom.setMaterial(mat2);

			//TODO use an intermediate structure with other into and a way to load/save from MaterialDef/Material MatParams.
//			Material mat0 = new Material(mat.getMaterialDef());
//			mat.getMaterialDef().getMaterialParams().stream().forEach((v) -> {
//				mat0.setParam(v.getName(), v.getVarType(), mat.getParam(v.getName()));
//			});
//			geom.setMaterial(mat0);
			FxPlatformExecutor.runOnFxApplication(()-> {
				List<PropertySheet.Item> items = mat.getMaterialDef().getMaterialParams().stream().map((v) -> asPropertySheetItem(v, mat)).sorted((o1, o2) -> o1.getName().compareTo(o2.getName())).collect(Collectors.toList());
				controller.matParams.getItems().setAll(items);
			});

			Node host = (Node)jmeApp.getRootNode().getChild("preview");
			host.detachAllChildren();
			host.attachChild(geom);
			return host;
		});
	}

	String read(String path) throws Exception {
		String b = "";
		try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			b = reader.lines().reduce("", (acc, l) ->  acc + l + "\n");
		}
		return b;
	}

	void makeSubWindow() {
		Stage stage = new Stage();
		stage.setScene(new Scene(new Group(new Text(10,10, "my second window"))));
		stage.show();
	}

	void makeInternalWindow(Pane parent) {
        Window windowOne = new Window("Window Title 1");
        windowOne.setId("window-control-1");
        windowOne.getLeftIcons().addAll(
        	new MinimizeIcon(windowOne)
        	,new CloseIcon(windowOne)
        );
        windowOne.setPrefSize(600, 400);
        windowOne.setLayoutX(100);
        windowOne.setLayoutY(100);

        parent.getChildren().add(windowOne);
	}

	javafx.scene.Node makeWorkflow() {
        // create scalable root pane
//		VCanvas canvas = new VCanvas();

		Pane root = new Pane();
		ZoomableScrollPane canvas = new ZoomableScrollPane(root);
		//canvas.setOnScroll(new EventHandler<ScrollEvent>() {
		canvas.addEventHandler(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				double inc = (event.getDeltaY() / event.getMultiplierY()) * 0.1;
				//canvas.zoomFactor.set(canvas.zoomFactor.doubleValue() + inc);
				Scale scale = canvas.scaleTransform;
				//Translate t = canvas.translateTransform;
				//scale.setPivotX(event.getX() /* scale.getX()*/ + t.getX());
				//scale.setPivotY(event.getY() /* scale.getY()*/ + t.getY());
				//t.setX(t.getX() - (event.getX() * inc) / (scale.getX() + inc));
				//t.setY(t.getY() - (event.getY() * inc) / (scale.getY() + inc));

				scale.setX(scale.getX() + inc);
				scale.setY(scale.getY() + inc);
				canvas.requestLayout();
			}
		});
		// allow the label to be dragged around.
		final Delta dragDelta = new Delta();
		canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
		  @Override public void handle(MouseEvent mouseEvent) {
		    // record a delta distance for the drag and drop operation.
		    dragDelta.x = mouseEvent.getX();
		    dragDelta.y = mouseEvent.getY();
		    canvas.setCursor(Cursor.MOVE);
		  }
		});
		canvas.setOnMouseReleased(new EventHandler<MouseEvent>() {
		  @Override public void handle(MouseEvent mouseEvent) {
		    canvas.setCursor(Cursor.DEFAULT);
		  }
		});
		canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
		  @Override public void handle(MouseEvent mouseEvent) {
				//canvas.translateTransform.setX(canvas.translateTransform.getX() + inc );
			  	Scale scale = canvas.scaleTransform;
				Translate t = canvas.translateTransform;
				t.setX(t.getX() + (mouseEvent.getX() - dragDelta.x) / scale.getX());
				t.setY(t.getY() + (mouseEvent.getY() - dragDelta.y) / scale.getY());
				dragDelta.x = mouseEvent.getX();
			    dragDelta.y = mouseEvent.getY();
				canvas.requestLayout();
		  }
		});
		canvas.setOnMouseEntered(new EventHandler<MouseEvent>() {
		  @Override public void handle(MouseEvent mouseEvent) {
		    canvas.setCursor(Cursor.DEFAULT);
		  }
		});


        // define background style
        canvas.setStyle("-fx-background-color: linear-gradient(to bottom, rgb(10,32,60), rgb(42,52,120));");

        // create a new flow object
        VFlow flow = FlowFactory.newFlow();


        // create skin factory for flow visualization
        FXSkinFactory fXSkinFactory = new FXSkinFactory(root);

        // generate the ui for the flow
        flow.setSkinFactories(fXSkinFactory);
		PopOver popOver = new PopOver();
		flow.getNodes().addListener(new ListChangeListener<VNode>(){
			@Override
			public void onChanged(ListChangeListener.Change<? extends VNode> c) {
				while (c.next()) {
					if (c.wasAdded()){
						for (VNode vn : c.getAddedSubList()) {
							flow.getNodeSkinsById(vn.getId())
							.stream()
							.filter(s -> s instanceof FXFlowNodeSkin)
							.forEach(s -> {
								//TODO Store registration
								((FXFlowNodeSkin) s).connectorsProperty.addListener(new MapChangeListener<Connector,Shape>(){
									@Override
									public void onChanged(MapChangeListener.Change<? extends Connector, ? extends Shape> change) {
										if (change.wasAdded()) {
											Connector c = change.getKey();
											Shape cn =  change.getValueAdded();
											//TODO Store registration
											cn.addEventHandler(MouseEvent.MOUSE_ENTERED, (e) -> {
										    	((Label)popOver.getContentNode()).setText(change.getKey().getLocalId() + " : " + c.getType());
										    	popOver.show((javafx.scene.Node)e.getSource(), -10);
											});
											cn.addEventHandler(MouseEvent.MOUSE_EXITED, (e) -> {
										    	popOver.hide();
											});
										} else if (change.wasRemoved()) {
											//TODO remove registration
										}
									}
								});
							});
						}
					} else if (c.wasRemoved()) {
						//TODO remove registration
					}
				}
			}
		});
/*
        flow.getNodeSkinsById(n1.getId()).stream()
		.filter(s -> s instanceof FXFlowNodeSkin)
		.forEach(s -> {
			javafx.scene.Node cn = ((FXFlowNodeSkin) s).getConnectorNodeByReference(c);
			Tooltip t = new Tooltip(c.getLocalId() + " : " + c.getType());
			t.getStyleClass().add("ttip");
			Tooltip.install(cn, t);

			cn.addEventFilter(MouseEvent.MOUSE_ENTERED, (e) -> {
		    	((Label)popOver.getContentNode()).setText(c.getLocalId() + " : " + c.getType());
		    	popOver.show((javafx.scene.Node)e.getSource());
			});
			cn.addEventFilter(MouseEvent.MOUSE_EXITED, (e) -> {
		    	//popOver.hide();
			});
		})
		;
*/
        // make it visible
        flow.setVisible(true);

        // add two nodes to the flow
        VNode n1 = flow.newNode();
        VNode n2 = flow.newNode();

        // specify input & output capabilities...

        // ... for node 1
        Connector c = n1.addInput("data");
        n1.addOutput("data");

        // ... for node 2
        n2.addInput("data");
        n2.addOutput("data");

        //return new ScrollPane(canvas);
        return canvas;
	}
	//TODO support range for Number
	//TODO allow to define if Vector3/4 is for vector or color
	PropertySheet.Item asPropertySheetItem(MatParam def, Material values) {
		switch(def.getVarType()){
				case Boolean : return new PropertySheetItem4MatParam(def, values, Boolean.class, Optional.of(PropertyEditor4Boolean.class));
				case Vector4 :
					if (def.getName().toLowerCase().indexOf("color") > -1) {
						//return new PropertySheetItem4MatParam(mp, Vector4f.class, Optional.of(PropertyEditor4Color.class));
						return new PropertySheetItem4MatParam(def, values, ColorRGBA.class, Optional.of(PropertyEditor4Color.class));
					}
//				case Float: return Float.class;
//				case Int: return Integer.class;
//				case Texture2D: return File.class;
//				case Vector2: return Vector2f.class;
//				case Vector3: return Vector3f.class;
//				case Vector4: return Vector4f.class;
		}
		return new PropertySheetItem4MatParam(def, values, Object.class, Optional.empty());
	}

	public static class PropertyEditor4Boolean extends AbstractPropertyEditor<Boolean, ComboBox<Boolean>> {
		public PropertyEditor4Boolean(PropertySheet.Item property) {
			super(property, new ComboBox<Boolean>());
			getEditor().setItems(FXCollections.observableArrayList(true, false, null));
		}

        @Override protected ObservableValue<Boolean> getObservableValue() {
            return getEditor().getSelectionModel().selectedItemProperty();
            //return "undef".equals(v) ? null : Boolean.parseBoolean(v);
        }

        @Override public void setValue(Boolean value) {
            getEditor().getSelectionModel().select(value);
        }
	}

	public static class PropertyEditor4Color implements PropertyEditor<ColorRGBA> {
    	private final ColorPicker editor = new ColorPicker();

		public PropertyEditor4Color(PropertySheet.Item property) {
			setValue((ColorRGBA)property.getValue());
			editor.valueProperty().addListener((ov,o,n) -> property.setValue(getValue()));
		}

        public void setValue(ColorRGBA value) {
        	if (value == null) {
        		editor.setValue(null);
        	} else {
        		editor.setValue(new Color(value.r, value.g, value.b, value.a));
        	}
        }

		@Override
		public ColorRGBA getValue() {
			Color n = editor.getValue();
			return (n == null) ? null : new ColorRGBA((float)n.getRed(), (float)n.getGreen(), (float)n.getBlue(), (float)n.getOpacity());
		}

		@Override
		public javafx.scene.Node getEditor() {
			return editor;
		}
	}
//	public static class PropertyEditor4Color extends AbstractPropertyEditor<Vector4f, ColorPicker> {
//		public PropertyEditor4Color(PropertySheet.Item property) {
//			super(property, new ColorPicker());
//			setValue((Vector4f)property.getValue());
//		}
//
//        @Override protected ObservableValue<Vector4f> getObservableValue() {
//        	SimpleObjectProperty<Vector4f> pvalue = new SimpleObjectProperty<Vector4f>();
//			pvalue.bind(Bindings.createObjectBinding(()-> {
//            	Color c = getEditor().getValue();
//            	return (c == null) ? null : new Vector4f((float)c.getRed(), (float)c.getGreen(), (float)c.getBlue(), (float)c.getOpacity());
//            }, getEditor().valueProperty()));
//            return pvalue;
//        }
//
//        @Override public void setValue(Vector4f value) {
//        	if (value == null) {
//        		getEditor().setValue(null);
//        	} else {
//        		getEditor().setValue(new Color(value.x, value.y, value.z, value.w));
//        	}
//        }
//	}
}

@RequiredArgsConstructor
class PropertySheetItem4MatParam implements PropertySheet.Item {
	public final MatParam def;
	public final Material values;
	public final Class<?> type;
	public final Optional<Class<? extends PropertyEditor>> editor;

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public String getCategory() {
		return "";
	}

	@Override
	public String getName() {
		return def.getName();
	}

	@Override
	public String getDescription() {
		return String.format("%s : %s", def.getName(), def.getVarType());
	}

	@Override
	public Object getValue() {
		MatParam mp = values.getParam(def.getName());
		return (mp == null) ? null : mp.getValue();
	}

	@Override
	public void setValue(Object value) {
		System.err.printf("update material : %s %s\n", def.getName(), value);
		if (value == null) {
			values.clearParam(def.getName());
		} else {
			values.setParam(def.getName(), def.getVarType(), value);
		}
	}

	public Optional<Class<? extends PropertyEditor>> getPropertyEditorClass() {
		return editor;
    }
};

class Delta { double x, y; }