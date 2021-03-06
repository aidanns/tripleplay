package tripleplay.flump;

import pythagoras.f.IPoint;
import pythagoras.f.Point;

import playn.core.Image;
import playn.core.ImageLayer;
import playn.core.Json;
import playn.core.PlayN;

public class Texture
    implements Instance
{
    protected Texture (Symbol symbol) {
        _layer = PlayN.graphics().createImageLayer(symbol.region);
        _layer.setOrigin(-symbol.offset.x(), -symbol.offset.y());
    }

    @Override public ImageLayer layer () {
        return _layer;
    }

    public static class Symbol
        implements tripleplay.flump.Symbol
    {
        public final Image.Region region;
        public final IPoint offset;

        protected Symbol (Json.Object json, Image atlas) {
            _name = json.getString("symbol");
            offset = KeyframeData.getPoint(json, "offset", 0, 0);

            Json.TypedArray<Float> rect = json.getArray("rect", Float.class);
            region = atlas.subImage(rect.get(0), rect.get(1), rect.get(2), rect.get(3));
        }

        @Override public String name () {
            return _name;
        }

        @Override public Texture createInstance () {
            return new Texture(this);
        }

        protected String _name;
    }

    protected ImageLayer _layer;
}
