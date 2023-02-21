package org.codingmatters.value.objects.values.casts.reflect;

import org.codingmatters.value.objects.demo.books.Book;
import org.codingmatters.value.objects.demo.books.Person;
import org.codingmatters.value.objects.values.casts.ValueObjectCaster;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class ValueObjectReflectCasterTest {

    private final ValueObjectReflectCaster<Book, Person> caster = new ValueObjectReflectCaster<>(Book.class, Person.class);

    public ValueObjectReflectCasterTest() throws ValueObjectCaster.ValueObjectUncastableException {
    }

    @Test
    public void whenNull__thenCastedToNull() throws Exception {
        assertThat(this.caster.cast(null), is(nullValue()));
    }

    @Test
    public void given__when__then() throws Exception {
        assertThat(this.caster.cast(Book.builder().name("the name").build()), is(Person.builder().name("the name").build()));
    }
}