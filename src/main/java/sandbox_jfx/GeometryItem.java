package sandbox_jfx;

import lombok.Data;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

@Data
public class GeometryItem {
	public final String label;
	public final Geometry geometry;

	@Override
	public String toString() {
		return label;
	}

	public static GeometryItem newSphere(String label) {
        Sphere sphMesh = new Sphere(32, 32, 2.5f);
        sphMesh.setTextureMode(Sphere.TextureMode.Projected);
        sphMesh.updateGeometry(32, 32, 2.5f, false, false);
        TangentBinormalGenerator.generate(sphMesh);
        Geometry sphere = new Geometry(label, sphMesh);
        //sphere.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_X));
        return new GeometryItem(label, sphere);
	}

	public static GeometryItem newCube(String label) {
        Box boxMesh = new Box(1.75f, 1.75f, 1.75f);
        TangentBinormalGenerator.generate(boxMesh);
        Geometry box = new Geometry(label, boxMesh);
        box.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.DEG_TO_RAD * 30, Vector3f.UNIT_X).multLocal(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y)));
        return new GeometryItem(label, box);
	}

	public static GeometryItem newQuad(String label) {
        Quad quadMesh = new Quad(4.5f, 4.5f);
        TangentBinormalGenerator.generate(quadMesh);
        Geometry quad = new Geometry(label, quadMesh);
        quad.setLocalTranslation(new Vector3f(-2.25f, -2.25f, 0));
        return new GeometryItem(label, quad);
	}
}