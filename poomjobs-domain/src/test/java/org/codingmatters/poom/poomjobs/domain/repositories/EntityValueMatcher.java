package org.codingmatters.poom.poomjobs.domain.repositories;

import org.codingmatters.poom.servives.domain.entities.Entity;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.function.Predicate;

/**
 * Created by nelt on 6/6/17.
 */
public class EntityValueMatcher<V> extends TypeSafeMatcher<Entity<V>> {

    static public <V> EntityValueMatcher<V> valueMatches(Predicate<V> predicate) {
        return new EntityValueMatcher<>(predicate);
    }

    private final Predicate<V> predicate;

    private EntityValueMatcher(Predicate<V> predicate) {
        this.predicate = predicate;
    }

    @Override
    protected boolean matchesSafely(Entity<V> item) {
        return this.predicate.test(item.value());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("doesn't match predicate");
    }
}
