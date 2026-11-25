package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.table.schema.column.TableColumn;

public record ColumnMapping<T extends TableColumn>(T column, ColumnReader reader, Class<?> clazz) {}
