package beetlkhi.utils.xsd;

import java.util.List;
import java.util.Optional;

/**
 * Helper class that allows to filter XML elements based on their class
 */
public class ElementFilter {

    /**
     * Return all elements of a single element which is subclass of the provided class.
     * Helpful if the list is known to have only one such object
     *
     * @param objectList the list to filter
     * @param clazz      the desired Class Object such as 'MyType.class'
     * @param <T>        the Class parameter
     * @return Optionally, an object of the requested subtype
     */
    public static <T> Optional<T> getClass(List<Object> objectList, Class<T> clazz) {
        return objectList
                .stream()
                .filter(obj -> obj.getClass() == clazz)
                .map(clazz::cast)
                .findAny();
    }

}
