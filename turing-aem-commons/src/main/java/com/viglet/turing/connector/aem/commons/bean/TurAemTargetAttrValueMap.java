package com.viglet.turing.connector.aem.commons.bean;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemTargetAttrValueMap extends HashMap<String, TurMultiValue> {

    public void addWithSingleValue(String attributeName, String value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Date value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Boolean value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Long value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Double value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Float value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.singleItem(value, override), override);
    }

    public void addWithStringCollectionValue(String attributeName, List<String> value,
            boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, new TurMultiValue(value, override), override);
    }

    public void addWithDateCollectionValue(String attributeName, List<Date> value,
            boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, TurMultiValue.fromDateCollection(value, override), override);
    }

    public void addWithSingleValue(String attributeName, Integer value, boolean override) {
        if (value == null) {
            return;
        }
        TurMultiValue newValue = TurMultiValue.singleItem(value, override);
        addOrMerge(attributeName, newValue, override);
    }

    public void addWithSingleValue(String attributeName, TurMultiValue value, boolean override) {
        if (value == null) {
            return;
        }
        addOrMerge(attributeName, value, override);
    }

    /**
     * Internal helper to add or merge TurMultiValue based on override flag.
     */
    private void addOrMerge(String attributeName, TurMultiValue value, boolean override) {
        if (override || !this.containsKey(attributeName)) {
            this.put(attributeName, value);
        } else {
            this.get(attributeName).addAll(value);
        }
    }

    public void merge(TurAemTargetAttrValueMap fromMap) {
        fromMap.keySet().forEach(fromKey -> {
            if (this.containsKey(fromKey)) {
                if (fromMap.get(fromKey).isOverride()) {
                    this.put(fromKey, fromMap.get(fromKey));
                } else {
                    this.get(fromKey).addAll(fromMap.get(fromKey));
                }
            } else {
                this.put(fromKey, fromMap.get(fromKey));
            }
        });
    }

    public static TurAemTargetAttrValueMap singleItem(TurAemTargetAttrValue turCmsTargetAttrValue) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.put(turCmsTargetAttrValue.getTargetAttrName(),
                turCmsTargetAttrValue.getMultiValue());
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, List<String> value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithStringCollectionValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, TurMultiValue value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, String value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Date value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Boolean value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Integer value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Double value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Float value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName, Long value,
            boolean override) {
        TurAemTargetAttrValueMap turCmsTargetAttrValueMap = new TurAemTargetAttrValueMap();
        turCmsTargetAttrValueMap.addWithSingleValue(attributeName, value, override);
        return turCmsTargetAttrValueMap;
    }

    public static TurAemTargetAttrValueMap singleItem(String attributeName,
            TurMultiValue turMultiValue) {
        return TurAemTargetAttrValueMap
                .singleItem(new TurAemTargetAttrValue(attributeName, turMultiValue));
    }


    public static TurAemTargetAttrValueMap singleItem(TurAemTargetAttr turAemTargetAttr,
            boolean override) {
        return TurAemTargetAttrValueMap.singleItem(turAemTargetAttr.getName(),
                turAemTargetAttr.getTextValue(), override);
    }
}
