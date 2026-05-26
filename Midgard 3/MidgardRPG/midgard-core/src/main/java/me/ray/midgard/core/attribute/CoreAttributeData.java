package me.ray.midgard.core.attribute;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.ray.midgard.core.profile.ModuleData;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonAdapter(CoreAttributeData.Adapter.class)
public class CoreAttributeData implements ModuleData {

    private final Map<String, AttributeInstance> instances = new ConcurrentHashMap<>();

    public CoreAttributeData() {
    }

    public AttributeInstance getInstance(Attribute attribute) {
        return instances.computeIfAbsent(attribute.getId(), id -> new AttributeInstance(attribute));
    }
    
    public AttributeInstance getInstance(String attributeId) {
        return AttributeRegistry.getInstance().getAttribute(attributeId)
                .map(this::getInstance)
                .orElse(null);
    }

    public Map<String, AttributeInstance> getInstances() {
        return instances;
    }

    public static class Adapter extends TypeAdapter<CoreAttributeData> {
        @Override
        public void write(JsonWriter out, CoreAttributeData value) throws IOException {
            out.beginObject();
            for (Map.Entry<String, AttributeInstance> entry : value.instances.entrySet()) {
                out.name(entry.getKey()).value(entry.getValue().getBaseValue());
            }
            out.endObject();
        }

        @Override
        public CoreAttributeData read(JsonReader in) throws IOException {
            CoreAttributeData data = new CoreAttributeData();
            in.beginObject();
            while (in.hasNext()) {
                String id = in.nextName();
                double baseValue = in.nextDouble();
                
                Attribute attribute = AttributeRegistry.getInstance().getAttribute(id).orElse(null);
                if (attribute != null) {
                    AttributeInstance instance = new AttributeInstance(attribute);
                    instance.setBaseValue(baseValue);
                    data.instances.put(id, instance);
                }
            }
            in.endObject();
            return data;
        }
    }
}
