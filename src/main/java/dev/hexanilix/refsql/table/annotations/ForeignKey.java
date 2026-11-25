package dev.hexanilix.refsql.table.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

    /**
     * The Java class representing the referenced table.
     */
    Class<?> target();

    /**
     * The table name, this overrides the table name derived from the target class
     */
    String table();

    /**
     * The referenced field/property name.
     * Defaults to the target's primary key.
     */
    String field() default "id";

    OnDelete onDelete() default OnDelete.NO_ACTION;

    OnUpdate onUpdate() default OnUpdate.NO_ACTION;

    enum OnDelete {
        NO_ACTION,
        RESTRICT,
        CASCADE,
        SET_NULL,
        SET_DEFAULT
    }

    enum OnUpdate {
        NO_ACTION,
        RESTRICT,
        CASCADE,
        SET_NULL,
        SET_DEFAULT
    }
}