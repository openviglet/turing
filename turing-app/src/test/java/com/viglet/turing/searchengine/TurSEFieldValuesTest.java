package com.viglet.turing.searchengine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;

class TurSEFieldValuesTest {

    @Test
    void toTextShouldReturnEmptyForNull() {
        assertEquals("", TurSEFieldValues.toText(null));
    }

    @Test
    void toTextShouldReturnTrimmedString() {
        assertEquals("hello", TurSEFieldValues.toText("  hello  "));
    }

    @Test
    void toTextShouldHandleLong() {
        assertEquals("12345", TurSEFieldValues.toText(12345L));
    }

    @Test
    void toTextShouldHandleDate() {
        Date date = new Date(0L); // epoch
        String result = TurSEFieldValues.toText(date);
        assertEquals("1970-01-01T00:00:00Z", result);
    }

    @Test
    void toTextShouldHandleArrayList() {
        ArrayList<String> list = new ArrayList<>();
        list.add("  first  ");
        list.add("second");
        assertEquals("first", TurSEFieldValues.toText(list));
    }

    @Test
    void toTextShouldReturnEmptyForEmptyArrayList() {
        assertEquals("", TurSEFieldValues.toText(new ArrayList<>()));
    }

    @Test
    void toTextShouldHandleObjectArray() {
        Object[] arr = new Object[]{"  value  "};
        assertEquals("value", TurSEFieldValues.toText(arr));
    }

    @Test
    void toTextShouldHandleLongInObjectArray() {
        Object[] arr = new Object[]{42L};
        assertEquals("42", TurSEFieldValues.toText(arr));
    }

    @Test
    void toTextShouldHandleInteger() {
        assertEquals("42", TurSEFieldValues.toText(42));
    }

    @Test
    void toTextShouldHandleArrayListWithLong() {
        ArrayList<Long> list = new ArrayList<>();
        list.add(999L);
        assertEquals("999", TurSEFieldValues.toText(list));
    }

    @Test
    void toTextShouldHandleArrayListWithDate() {
        ArrayList<Date> list = new ArrayList<>();
        list.add(new Date(0L));
        assertEquals("1970-01-01T00:00:00Z", TurSEFieldValues.toText(list));
    }

    @Test
    void toTextShouldHandleArrayListWithNull() {
        ArrayList<Object> list = new ArrayList<>();
        list.add(null);
        assertEquals("", TurSEFieldValues.toText(list));
    }
}
