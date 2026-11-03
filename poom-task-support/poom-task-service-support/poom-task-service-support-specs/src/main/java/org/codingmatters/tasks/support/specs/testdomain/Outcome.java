package org.codingmatters.tasks.support.specs.testdomain;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Issue d'un appel à un handler, lue sur la réponse générée (statusNNN() non nul) : code HTTP et payload
 * éventuel. Seule la couche domaine de test manipule les codes ; les steps parlent d'acceptation/refus.
 */
public record Outcome(int status, Optional<Object> payload) {

    public static Outcome of(Object response) {
        for (Method method : publicType(response).getMethods()) {
            if (method.getName().matches("status\\d{3}") && method.getParameterCount() == 0) {
                Object statusValue = invoke(method, response);
                if (statusValue != null) {
                    return new Outcome(Integer.parseInt(method.getName().substring(6)), payloadOf(statusValue));
                }
            }
        }
        throw new IllegalStateException("réponse sans statut : " + response);
    }

    public boolean accepted() {
        return this.status >= 200 && this.status < 300;
    }

    /** Réponse partielle (206) : d'autres éléments restent à lister. */
    public boolean partial() {
        return this.status == 206;
    }

    public boolean notFound() {
        return this.status == 404;
    }

    public boolean badRequest() {
        return this.status == 400;
    }

    /** La collection ne se parcourt pas (405). */
    public boolean notAllowed() {
        return this.status == 405;
    }

    /** La page demandée n'est pas une plage valide (416). */
    public boolean rangeNotSatisfiable() {
        return this.status == 416;
    }

    public boolean unexpectedError() {
        return this.status == 500;
    }

    @SuppressWarnings("unchecked")
    public <T> T payloadAs(Class<T> type) {
        return (T) this.payload.filter(type::isInstance)
                .orElseThrow(() -> new IllegalStateException("pas de " + type.getSimpleName() + " dans la réponse " + this.status));
    }

    private static Optional<Object> payloadOf(Object statusValue) {
        try {
            return Optional.ofNullable(publicType(statusValue).getMethod("payload").invoke(statusValue));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Les valeurs générées sont des *Impl package-private : on passe par leur interface publique. */
    private static Class<?> publicType(Object value) {
        Class<?>[] interfaces = value.getClass().getInterfaces();
        return interfaces.length > 0 ? interfaces[0] : value.getClass();
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
