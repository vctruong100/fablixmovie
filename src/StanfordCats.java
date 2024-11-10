import java.util.*;
import java.util.stream.Collectors;
import static java.util.Map.entry;

/*
 * Cats mapping according to
 * http://infolab.stanford.edu/pub/movies/doc.html
 *
 * See sections 3.13 and 4.4
 */
class CatsMapping {
    private static final Map<String, String> map = Map.ofEntries(
            // 3.13
            entry("ctxx", "Uncategorized"),
            entry("actn", "Violence"),
            entry("camp", "Camp"),
            entry("comd", "Comedy"),
            entry("disa", "Disaster"),
            entry("epic", "Epic"),
            entry("horr", "Horror"),
            entry("noir", "Black"),
            entry("scfi", "Science Fiction"),
            entry("west", "Western"),
            entry("advt", "Adventure"),
            entry("cart", "Cartoon"),
            entry("docu", "Documentary"),
            entry("faml", "Family"),
            entry("musc", "Musical"),
            entry("porn", "Pornography"),
            entry("surl", "Surreal"),
            entry("dram", "Drama"),
            entry("hist", "History"),
            entry("avga", "Avant Garde"),
            entry("cnr", "Cops and Robbers"),
            entry("myst", "Mystery"),
            entry("susp", "Thriller"),
            entry("romt", "Romantic"),

            // 4.4
            entry("s.f.", "Science Fiction"),
            entry("biop", "Biographical Picture"),
            entry("tv", "TV show"),
            entry("tvs", "TV series"),
            entry("tvm", "TV miniseries")

            // misc
    );
    public static List<String> collect(String cat) {
        List<String> cats = new ArrayList<>();
        // remove mapped entries in string
        // add mapped entries to cats
        String cannedCat = Arrays.stream(cat.split(" "))
                .filter(c -> !c.isEmpty())
                .filter(c -> {
                    if (map.containsKey(c.toLowerCase())) {
                        cats.add(map.get(c.toLowerCase()));
                        return false;
                    }
                    return true;
                }).collect(Collectors.joining(" "));
        // add canned cat to list of cats
        // if it's not empty
        // also capitalize the first letter
        cannedCat = cannedCat.trim();
        if (!cannedCat.isEmpty()) {
            cats.add(cannedCat.substring(0, 1).toUpperCase()
                    + cannedCat.substring(1));
        }
        return cats;
    }
}