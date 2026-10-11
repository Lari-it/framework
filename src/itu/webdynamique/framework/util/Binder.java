package itu.webdynamique.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Binder {

    public static Object[] bindParams(Method methode, HttpServletRequest request) throws Exception {
        Parameter[] params = methode.getParameters();
        Object[] valeurs = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String nomParam = params[i].getName();
            Class<?> type = params[i].getType();

            if (isSimple(type)) {
                valeurs[i] = convertValue(request.getParameter(nomParam), type);
            } else {
                valeurs[i] = bindObject(request, nomParam, type);
            }
        }
        return valeurs;
    }

    private static Object bindObject(HttpServletRequest request, String nomParam, Class<?> type)
            throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        Map<String, String[]> paramMap = request.getParameterMap();
        String prefixe = nomParam + ".";

        for (String cle : paramMap.keySet()) {
            if (cle.startsWith(prefixe)) {
                String chemin = cle.substring(prefixe.length());
                String valeur = paramMap.get(cle)[0];
                setByPath(instance, chemin, valeur);
            }
        }
        return instance;
    }

    private static void setByPath(Object racine, String chemin, String valeur) throws Exception {
        String[] etapes = chemin.split("\\.");
        Object courant = racine;
        Field fieldCourant = null;
        Object parentCourant = null;
        int indexCourant = -1;

        for (int i = 0; i < etapes.length; i++) {
            String etape = etapes[i];
            boolean derniere = (i == etapes.length - 1);

            if (etape.contains("[")) {
                String nomField = etape.substring(0, etape.indexOf('['));
                int index = Integer.parseInt(etape.substring(etape.indexOf('[') + 1, etape.indexOf(']')));

                Field field = getField(courant.getClass(), nomField);
                field.setAccessible(true);

                List liste = (List) field.get(courant);
                if (liste == null) {
                    liste = new ArrayList();
                    field.set(courant, liste);
                }
                while (liste.size() <= index) {
                    liste.add(null);
                }

                if (derniere) {
                    Type genericType = field.getGenericType();
                    Class<?> elementType = getElementType(genericType);
                    liste.set(index, convertValue(valeur, elementType));
                } else {
                    if (liste.get(index) == null) {
                        Type genericType = field.getGenericType();
                        Class<?> elementType = getElementType(genericType);
                        liste.set(index, elementType.getDeclaredConstructor().newInstance());
                    }
                    parentCourant = courant;
                    fieldCourant = field;
                    indexCourant = index;
                    courant = liste.get(index);
                }

            } else {
                Field field = getField(courant.getClass(), etape);
                field.setAccessible(true);

                if (derniere) {
                    Object valeurConvertie = convertValue(valeur, field.getType());
                    if (valeurConvertie != null) {
                        field.set(courant, valeurConvertie);
                    }
                } else {
                    Object enfant = field.get(courant);
                    if (enfant == null) {
                        if (List.class.isAssignableFrom(field.getType())) {
                            enfant = new ArrayList();
                        } else {
                            enfant = field.getType().getDeclaredConstructor().newInstance();
                        }
                        field.set(courant, enfant);
                    }
                    courant = enfant;
                }
            }
        }
    }

    private static Field getField(Class<?> clazz, String nom) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(nom);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(nom + " introuvable dans " + clazz.getName());
    }

    private static Class<?> getElementType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class) {
                return (Class<?>) arg;
            }
            if (arg instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) arg).getRawType();
            }
        }
        return String.class;
    }

    public static boolean isSimple(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class;
    }

    public static Object convertValue(String valeur, Class<?> type) {
        if (valeur == null || valeur.isEmpty())
            return null;
        if (type == String.class)
            return valeur;
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(valeur);
        if (type == long.class || type == Long.class)
            return Long.parseLong(valeur);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(valeur);
        if (type == float.class || type == Float.class)
            return Float.parseFloat(valeur);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(valeur);
        return null;
    }
}