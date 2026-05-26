package me.ray.midgard.core.effect;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.ray.midgard.core.profile.ModuleData;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonAdapter(EffectData.Adapter.class)
public class EffectData implements ModuleData {

    // Use CopyOnWriteArrayList to avoid ConcurrentModificationException during iteration/ticking
    private final List<ActiveEffect> activeEffects = new CopyOnWriteArrayList<>();

    public List<ActiveEffect> getActiveEffects() {
        return activeEffects;
    }
    
    public void addEffect(ActiveEffect effect) {
        activeEffects.add(effect);
    }
    
    public void removeEffect(ActiveEffect effect) {
        activeEffects.remove(effect);
    }

    public static class Adapter extends TypeAdapter<EffectData> {
        @Override
        public void write(JsonWriter out, EffectData value) throws IOException {
            out.beginArray();
            for (ActiveEffect effect : value.activeEffects) {
                out.beginObject();
                out.name("id").value(effect.getEffect().getId());
                out.name("duration").value(effect.getRemainingDuration());
                out.name("applier").value(effect.getApplierId().toString());
                out.endObject();
            }
            out.endArray();
        }

        @Override
        public EffectData read(JsonReader in) throws IOException {
            EffectData data = new EffectData();
            in.beginArray();
            while (in.hasNext()) {
                in.beginObject();
                String id = null;
                long duration = 0;
                UUID applier = null;
                
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "id": id = in.nextString(); break;
                        case "duration": duration = in.nextLong(); break;
                        case "applier": applier = UUID.fromString(in.nextString()); break;
                        default: in.skipValue(); break;
                    }
                }
                in.endObject();
                
                if (id != null) {
                    StatusEffect effect = EffectRegistry.getInstance().getEffect(id).orElse(null);
                    if (effect != null) {
                        data.addEffect(new ActiveEffect(effect, duration, applier));
                    }
                }
            }
            in.endArray();
            return data;
        }
    }
}
