package org.codingmatters.value.objects.values.casts;

public interface ValueObjectCaster<From, To> {

    To cast(From from) throws ValueObjectCastException;

    class ValueObjectCastException extends Exception{
        public ValueObjectCastException(String message) {
            super(message);
        }

        public ValueObjectCastException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class ValueObjectUncastableException extends Exception {
        public ValueObjectUncastableException(String message) {
            super(message);
        }

        public ValueObjectUncastableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
