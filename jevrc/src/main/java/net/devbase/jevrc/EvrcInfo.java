package net.devbase.jevrc;

import java.util.HashMap;
import java.util.Map;

import net.devbase.jevrc.tlv.Utils;

import org.json.simple.JSONObject;

public class EvrcInfo {

    static final Map<String , String> DOCUMENT_TAGS = new HashMap<String , String>() {{
        put("71.9F33", "issuing_state");
        put("71.9F35", "competent_authority");
        put("71.9F36", "issuing_authority");
        put("71.9F38", "unambiguous_number");
        put("71.8E", "issuing_date");
        put("71.8D", "expiry_date");
        put("72.C9", "serial_number");
    }};

    static final Map<String , String> VEHICLE_TAGS = new HashMap<String , String>() {{
        put("71.82", "first_registration_date");
        put("72.C5", "production_year");
        put("71.A3.87", "make");
        put("71.A3.88", "type");
        put("71.A3.89", "commercial_description");
        put("71.8A", "id_number");
        put("71.81", "registration_number");
        put("71.A5.91", "max_net_power");
        put("71.A5.90", "engine_capacity");
        put("71.A5.92", "fuel_type");
        put("71.93", "power_weight_ratio");
        put("71.8C", "mass");
        put("71.A4.8B", "max_permissible_laden_mass");
        put("71.8F", "type_approval_number");
        put("71.A6.94", "seats_number");
        put("71.A6.95", "standing_places_number");
        put("72.A5.9E", "engine_id_number");
        put("72.99", "axies_number");
        put("72.98", "vehicle_category");
        put("72.9F24", "color");
        put("72.C1", "owner_change_restriction");
        put("72.C4", "load");        
    }};
    
    static final Map<String , String> PERSONAL_TAGS = new HashMap<String , String>() {{
        put("72.C2", "owner_personalno");
        put("71.A1.A2.83", "owner_legal_name");
        put("71.A1.A2.84", "owner_first_name");
        put("71.A1.A2.85", "owner_address");
        put("72.C3", "user_personalno");
        put("72.A1.A9.83", "user_legal_name");
        put("72.A1.A9.84", "user_first_name");
        put("72.A1.A9.85", "user_address");
    }};

    private Map<String, String> documentData = new HashMap<String, String>();
    private Map<String, String> vehicleData = new HashMap<String, String>();
    private Map<String, String> personalData = new HashMap<String, String>();
    
    public EvrcInfo() {
    
    }
    
    private void addDataFromDict(Map<String, String> tags, Map<String, byte[]> dict, Map<String, String> data) {
        for (Map.Entry<String, String> e : tags.entrySet()) {
            if (dict.containsKey(e.getKey())) {
                byte[] value_raw = dict.get(e.getKey());
                String value_str = Utils.bytes2UTF8String(value_raw);
                data.put(e.getValue(), value_str);
            }
        }
    }
    
    public void addPersonalData(Map<String, byte[]> dict) {
        addDataFromDict(PERSONAL_TAGS, dict, personalData);
    }

    public void addVehicleData(Map<String, byte[]> dict) {
        addDataFromDict(VEHICLE_TAGS, dict, vehicleData);
    }

    public void addDocumentData(Map<String, byte[]> dict) {
        addDataFromDict(DOCUMENT_TAGS, dict, documentData);
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Document data\n");
        for (Map.Entry<String, String> field : documentData.entrySet()) {
            out.append(String.format("%s: %s\n", field.getKey(), field.getValue()));
        }
        out.append("\nVehicle data\n");
        for (Map.Entry<String, String> field : vehicleData.entrySet()) {
            out.append(String.format("%s: %s\n", field.getKey(), field.getValue()));
        }
        out.append("\nPersonal data\n");
        for (Map.Entry<String, String> field : personalData.entrySet()) {
            out.append(String.format("%s: %s\n", field.getKey(), field.getValue()));
        }
        return out.toString();
    }
    
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        
        JSONObject document = new JSONObject();
        JSONObject personal = new JSONObject();
        JSONObject vehicle = new JSONObject();
        
        for (Map.Entry<String, String> field : vehicleData.entrySet()) {
            vehicle.put(field.getKey(), field.getValue());
        }
        for (Map.Entry<String, String> field : personalData.entrySet()) {
            personal.put(field.getKey(), field.getValue());
        }
        for (Map.Entry<String, String> field : documentData.entrySet()) {
            document.put(field.getKey(), field.getValue());
        }
        
        obj.put("personal", personal);
        obj.put("vehicle", vehicle);
        obj.put("document", document);

        return obj;
    }    
}
