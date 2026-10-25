package itu.webdynamique.framework;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Binder {

    public static Object bindParams(Parameter[] params, HttpServletRequest request) throws Exception {
        return bindParams(params, request, null);
    }

    public static Object bindParams(Parameter[] params, HttpServletRequest request, Session session) throws Exception {
        if (params.length == 0) {
            return null;
        }

        if (params.length == 1) {
            Parameter param = params[0];
            Class<?> type = param.getType();
            String paramName = param.getName();

            if (type == Session.class) {
                return session;
            }
            
            else if (isSimpleType(type)) {
                return convertSimpleType(paramName, type, request);
            }
            
            else if (type == MapFile.class) {
                return createMapFile(paramName, request);
            }
            
            else {
                Object instance = type.getDeclaredConstructor().newInstance();
                bindObjectFields(instance, request);
                return instance;
            }
        }

        
        Object[] values = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Class<?> type = param.getType();
            String paramName = param.getName();

            if (type == Session.class) {
                values[i] = session;
            } else if (isSimpleType(type)) {
                values[i] = convertSimpleType(paramName, type, request);
            } else if (type == MapFile.class) {
                values[i] = createMapFile(paramName, request);
            } else {
                Object instance = type.getDeclaredConstructor().newInstance();
                bindObjectFields(instance, request);
                values[i] = instance;
            }
        }
        return values;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class ||
               type == int.class || type == Integer.class ||
               type == double.class || type == Double.class ||
               type == long.class || type == Long.class ||
               type == float.class || type == Float.class ||
               type == boolean.class || type == Boolean.class;
    }

    private static Object convertSimpleType(String paramName, Class<?> type, HttpServletRequest request) {
        String valeurBrute = request.getParameter(paramName);
        
        if (type == String.class) {
            return valeurBrute;
        } else if (type == int.class || type == Integer.class) {
            return valeurBrute != null ? Integer.parseInt(valeurBrute) : 0;
        } else if (type == double.class || type == Double.class) {
            return valeurBrute != null ? Double.parseDouble(valeurBrute) : 0.0;
        } else if (type == long.class || type == Long.class) {
            return valeurBrute != null ? Long.parseLong(valeurBrute) : 0L;
        } else if (type == float.class || type == Float.class) {
            return valeurBrute != null ? Float.parseFloat(valeurBrute) : 0.0f;
        } else if (type == boolean.class || type == Boolean.class) {
            return valeurBrute != null ? Boolean.parseBoolean(valeurBrute) : false;
        }
        return null;
    }

    private static MapFile createMapFile(String paramName, HttpServletRequest request) throws Exception {
        try {
            Part part = request.getPart(paramName);
            if (part == null || part.getSize() == 0) {
                return null;
            }

            String fileName = part.getSubmittedFileName();
            String nom = fileName;
            String extension = "";
            
            if (fileName != null && fileName.contains(".")) {
                int lastDot = fileName.lastIndexOf(".");
                nom = fileName.substring(0, lastDot);
                extension = fileName.substring(lastDot + 1);
            }

            byte[] binaire = readInputStream(part.getInputStream());

            MapFile mapFile = new MapFile(nom, extension, binaire);
            System.out.println("[Binder] MapFile créé: " + mapFile);
            return mapFile;
        } catch (Exception e) {
            System.out.println("[Binder] Erreur lors de la création du MapFile pour " + paramName + ": " + e.getMessage());
            return null;
        }
    }

    private static byte[] readInputStream(InputStream inputStream) throws IOException {
        List<Byte> byteList = new ArrayList<>();
        int b;
        while ((b = inputStream.read()) != -1) {
            byteList.add((byte) b);
        }
        byte[] result = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;
    }

    private static void bindObjectFields(Object instance, HttpServletRequest request) throws Exception {
        Class<?> clazz = instance.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            
            if (isSimpleType(fieldType)) {
                Object value = convertSimpleType(fieldName, fieldType, request);
                if (value != null) {
                    field.set(instance, value);
                }
            }
            
            else if (fieldType == MapFile.class) {
                MapFile mapFile = createMapFile(fieldName, request);
                if (mapFile != null) {
                    field.set(instance, mapFile);
                }
            }
           
            else {
                
            }
        }
    }

    public static void setByPath(Object target, String path, Object value) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Object current = target;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Field field = current.getClass().getDeclaredField(part);
            field.setAccessible(true);
            
            Object nested = field.get(current);
            if (nested == null) {
                nested = field.getType().getDeclaredConstructor().newInstance();
                field.set(current, nested);
            }
            current = nested;
        }

        String lastPart = parts[parts.length - 1];
        Field lastField = current.getClass().getDeclaredField(lastPart);
        lastField.setAccessible(true);

        

        if (value instanceof MapFile) {
            lastField.set(current, value);
        }
        
        else if (isSimpleType(lastField.getType())) {
            if (value != null) {
                lastField.set(current, convertValue(value, lastField.getType()));
            }
        }
        
        else {
            lastField.set(current, value);
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetType == float.class || targetType == Float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }
        
        return value;
    }
}
