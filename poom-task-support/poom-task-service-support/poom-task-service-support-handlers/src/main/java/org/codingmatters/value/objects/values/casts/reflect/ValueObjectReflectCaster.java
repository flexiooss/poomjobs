package org.codingmatters.value.objects.values.casts.reflect;

import org.codingmatters.value.objects.values.casts.ValueObjectCaster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ValueObjectReflectCaster<From, To> implements ValueObjectCaster<From, To> {

    private Method toMap;
    private Method fromMap;
    private Method build;

    public ValueObjectReflectCaster(Class<From> fromClass, Class<To> toClass) throws ValueObjectUncastableException {
        try {
            this.toMap = fromClass.getMethod("toMap");
            this.fromMap = toClass.getMethod("fromMap", Map.class);
            for (Class<?> declaredClass : toClass.getDeclaredClasses()) {
                if (declaredClass.getSimpleName().equals("Builder")) {
                    this.build = declaredClass.getMethod("build");
                }
            }
        } catch (NoSuchMethodException e) {
            throw new ValueObjectUncastableException("cannot cast " + fromClass.getSimpleName() + " to " + toClass.getSimpleName(), e);
        }
    }

    @Override
    public To cast(From from) throws ValueObjectCastException{
        if(from == null) return null;
        /*
        To.fromMap(from.toMap()).build()
         */
        Object map;
        try {
            map = this.toMap.invoke(from);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ValueObjectCastException("error converting value to map", e);
        }
        Object builder;
        try {
            builder = this.fromMap.invoke(null, map);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ValueObjectCastException("error getting builder from map", e);
        }
        To casted;
        try {
            casted = (To) this.build.invoke(builder);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new ValueObjectCastException("error building value", e);
        }

        return casted;
    }
}
