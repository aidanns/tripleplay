//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011-2012, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.particle;

import java.util.ArrayList;
import java.util.List;

import react.Signal;
import react.Slot;

import playn.core.Image;
import static playn.core.PlayN.graphics;

import tripleplay.particle.Emitter;

/**
 * Manages all particle emitters.
 */
public class Particles
{
    /**
     * Creates an emitter that supports up to {@code maxParticles} particles at any one time.
     *
     * @param image the image to use for each particle.
     */
    public Emitter createEmitter (int maxParticles, Image image) {
        final Emitter emitter = new Emitter(this, maxParticles, image);
        emitter._conn = _onUpdate.connect(new Slot<Now>() { public void onEmit (Now now) {
            emitter.update(now.time, now.dt);
        }});
        graphics().rootLayer().add(emitter.layer);
        return emitter;
    }

    /**
     * Updates all registered emitters. Call this from {@link Game#update} or similar.
     *
     * @param delta the time that has elapsed since the last frame, in milliseconds.
     */
    public void update (float delta) {
        _onUpdate.emit(_now.update(delta));
    }

    float now () {
        return _now.time;
    }

    protected static class Now {
        public float time;
        public float dt;
        public Now update (float delta) {
            dt = delta/1000;
            time += dt;
            return this;
        }
    }

    protected final Now _now = new Now();
    protected final Signal<Now> _onUpdate = Signal.create();
    protected final ParticleShader _shader = new ParticleShader(graphics().ctx());
}
