package io.intelliflow.service;

import java.time.LocalDate;
import java.util.*;

public class ClassProperties {

    public static Class findClass(String dataType){
        switch (dataType) {
            case "Integer":
                return Integer.class;
            case "Character":
                return Character.class;
            case "Float":
                return Float.class;
            case "Long":
                return Long.class;
            case "DateTime":
                return Date.class;
            case "List":
                return List.class;
            case "ArrayList":
                return ArrayList.class;
            case "Map":
                return Map.class;
            case "HashMap":
                return HashMap.class;
            case "LinkedList":
                return LinkedList.class;
            case "Set":
                return Set.class;
            case "Deque":
                return Deque.class;
            case "Boolean":
                return Boolean.class;
            case "Byte":
                return Byte.class;
            case "Date":
                return LocalDate.class;
            case "Double":
                return Double.class;

            default:
                return String.class;
        }
    }
}
