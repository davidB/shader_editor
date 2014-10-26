package sandbox_jfx;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;

public class WipLocator implements AssetLocator {
    private final static Map<String, String> assets = new HashMap<>();

	public static void setContent(String path, final String content) {
		assets.put(path, content);
	}

	@Override
	public void setRootPath(String rootPath) {
	}

	@Override
	public AssetInfo locate(AssetManager manager, AssetKey key) {
		final String content = assets.get(key.getName());
		return (content == null) ?
			null
			: new AssetInfo(manager, key) {
				@Override
				public InputStream openStream() {
					System.err.println("load :" + key);
					try {
						return new ByteArrayInputStream(content.getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						return new ByteArrayInputStream(content.getBytes());
					}
				}
			};
	}

}