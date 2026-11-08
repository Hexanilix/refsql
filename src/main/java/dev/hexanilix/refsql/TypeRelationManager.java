package dev.hexanilix.refsql;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TypeRelationManager {

    private final Map<Class<?>, ClassSqlRelationDefinition<?>> type_relation_definitions = new HashMap<>();

    public <T> void addRelationDefinition(Class<T> clazz, ClassSqlRelationDefinition<T> definition) {
        this.type_relation_definitions.put(clazz, definition);
    }

    public <T> ClassSqlRelationDefinition<T> get(Class<T> clazz) {
        return (ClassSqlRelationDefinition<T>) this.type_relation_definitions.get(clazz);
    }

    public  <Obj> Object convert(@NotNull Obj obj) {
        ClassSqlRelationDefinition<Obj> conv = (ClassSqlRelationDefinition<Obj>) get(obj.getClass());
        return conv != null ? conv.convertToSql(obj) : obj;
    }

}
