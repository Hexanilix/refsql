
import java.util.ArrayList;
import java.util.List;

public class ConstructorClassConverterConstructorGenerator {

    private static String createConstructor(int a) {
        StringBuilder sb = new StringBuilder();

        sb.append("public <");

        List<String> generics = new ArrayList<>();

        for (int i = 1; i <= a; i++) {
            generics.add("A" + i);
        }

        sb.append(String.join(", ", generics));

        sb.append(">\n");

        sb.append("ConstructorClassConverter(\n")
                .append("        ClassTableSchema<T> schema,\n")
                .append("        Constructor")
                .append(a)
                .append("<T")
                .append(generics.size() > 0 ? ", " : "")
                .append(String.join(", ", generics))
                .append("> reference\n")
                .append(") { this(schema, extractConstructor(reference)); }\n");

        return sb.toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 30; i++) {
            System.out.print(createConstructor(i));
        }
    }
}
