import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

abstract class ParsedElement {
    enum Option {
        STR,
        DUP,
        REF,
        PROP,
        WARN,
        MISS,
        MULTI,
    }
    static class Inconsistency {
        String first;
        String second;
        EnumSet<Option> opt;
        public Inconsistency(String first, String second, EnumSet<Option> opt) {
            this.first = first;
            this.second = second;
            this.opt = opt;
        }
    }

    int index;
    String id;
    protected List<Inconsistency> inconsistencies =
            new ArrayList<>();
    public ParsedElement(int index, String id) {
        this.index = index;
        this.id = id;
    }
    public String reportInconsistencies() {
        if (inconsistencies.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ELEMENT ");
        sb.append(id);
        sb.append(" ");
        sb.append(index);
        sb.append(": ");
        for (var inc : inconsistencies) {
            sb.append(inc.first);
            sb.append("=");
            if (inc.second != null) {
                String enclosing = inc.opt.contains(Option.STR) ? "'" : "";
                sb.append(enclosing);
                sb.append(inc.second);
                sb.append(enclosing);
            } else {
                sb.append("null");
            }
            if (inc.opt.contains(Option.DUP)) {
                sb.append(" (DUPLICATE)");
            }
            if (inc.opt.contains(Option.REF)) {
                sb.append(" (REFERENCE)");
            }
            if (inc.opt.contains(Option.PROP))  {
                sb.append(" (PROPAGATED)");
            }
            if (inc.opt.contains(Option.WARN)) {
                sb.append(" (WARNING)");
            }
            if (inc.opt.contains(Option.MISS)) {
                sb.append(" (MISS)");
            }
            if (inc.opt.contains(Option.MULTI)) {
                sb.append(" (MULTIPLE)");
            }
            sb.append(";");
        }
        sb.append("\n");
        return sb.toString();
    }
}

// actor element in actors.xml
class Actor extends ParsedElement {
    public static final String HEADER = "sa";

    String stageName;
    Integer dob;
    String id;
    boolean schemaConsistent = true;

    public Actor(int index, Element element, Map<String, List<Actor>> actors) {
        super(index, element.getTagName());
        // Parse actor element based on actor.dtd
        Element stageNameElement = (Element) element.getElementsByTagName("stagename")
                .item(0);
        Element dobElement = (Element) element.getElementsByTagName("dob")
                .item(0);

        // id: randomly generated with header 'sf'
        id = generateRandomActorId();

        // dob
        // note: dob=null is schema consistent
        if (dobElement != null) {
            // assume inconsistent dob as null
            // assume empty dob is null
            String dobText = dobElement.getTextContent().trim();
            if (!dobText.isEmpty()) {
                try {
                    dob = Integer.parseInt(dobText);
                } catch (Exception e) {
                    inconsistencies.add(new Inconsistency(
                            "dob", dobText,
                            EnumSet.of(Option.STR, Option.WARN)));
                }
            }
        }

        // stagename
        // note: stagename=null is not schema consistent
        if (stageNameElement == null) {
            inconsistencies.add(new Inconsistency(
                    "stagename", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
        } else {
            stageName = stageNameElement.getTextContent().trim();
            if (stageName.isEmpty()) {
                inconsistencies.add(new Inconsistency(
                        "stagename", "",
                        EnumSet.of(Option.STR)));
                schemaConsistent = false;
            } else if (actors.containsKey(stageName)) {
                // duplicate if and only if (stageName, ) dob matches
                boolean matches = actors.get(stageName).stream()
                        .anyMatch(a -> Objects.equals(dob, a.dob));
                if (matches) {
                    inconsistencies.add(new Inconsistency(
                            "stagename", stageName,
                            EnumSet.of(Option.STR, Option.DUP)));
                    inconsistencies.add(new Inconsistency(
                            "dob", (Objects.toString(dob)),
                            EnumSet.of(Option.STR, Option.DUP)));
                    schemaConsistent = false; // no duplicates
                }
            }
        }
    }

    public Actor(String stageName, Integer dob) {
        super(-1, ""); // do not care
        this.stageName = stageName;
        this.dob = dob;
        this.id = generateRandomActorId();
    }

    private static String generateRandomActorId() {
        // Randomly generate a 10-char id
        // The first 2 chars are always 'sf' (the header)
        Random rng = new Random();
        String charSet = "0123456789"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER);
        for (int i = 0; i < 8; i++) {
            int r = rng.nextInt(charSet.length());
            sb.append(charSet.charAt(r));
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Actor) {
            Actor a = (Actor) o;
            return Objects.equals(stageName, a.stageName)
                    && Objects.equals(dob, a.dob);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(stageName, dob);
    }

    public int parameterize(PreparedStatement st, int paramIndex)
            throws SQLException {
        st.setString(++paramIndex, id);
        st.setString(++paramIndex, stageName);
        if (dob != null) {
            st.setInt(++paramIndex, dob);
        } else {
            st.setNull(++paramIndex, Types.INTEGER);
        }
        return paramIndex;
    }
}

// m element in casts.xml
class Cast extends ParsedElement {
    String fid;
    String actor;
    boolean schemaConsistent = true;

    public Cast(int index,
                Element element,
                Map<String, List<Actor>> actors,
                Map<String, Film> films,
                Set<Cast> casts)
    {
        super(index, element.getTagName());
        // Parse m element based on cast.dtd
        Element fElement = (Element) element.getElementsByTagName("f")
                .item(0);
        Element aElement = (Element) element.getElementsByTagName("a")
                .item(0);

        // f
        if (fElement == null) {
            inconsistencies.add(new Inconsistency(
                    "f", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
            return;
        } else {
            fid = fElement.getTextContent().trim();
        }

        // a
        if (aElement == null) {
            inconsistencies.add(new Inconsistency(
                    "a", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
            return;
        } else {
            actor = aElement.getTextContent().trim();
            if (actor.isEmpty()) {
                inconsistencies.add(new Inconsistency(
                        "a", actor,
                        EnumSet.of(Option.STR)));
                schemaConsistent = false;
                return;
            }
        }

        // check foreign key constraints on fid
        // indicate inconsistencies with a MISS
        if (!films.containsKey(fid)) {
            inconsistencies.add(new Inconsistency(
                    "f", fid,
                    EnumSet.of(Option.STR, Option.MISS)));
            schemaConsistent = false;
            return;
        }

        // check if cast is a duplicate
        if (casts.contains(this)) {
            inconsistencies.add(new Inconsistency(
                    "f", fid,
                    EnumSet.of(Option.STR, Option.DUP)));
            inconsistencies.add(new Inconsistency(
                    "a", actor,
                    EnumSet.of(Option.STR, Option.DUP)));
            schemaConsistent = false;
            return;
        }

        // no matching actor
        // schema is still consistent if we add an actor
        // such that dob=null
        if (!actors.containsKey(actor)) {
            actors.put(actor, new ArrayList<>());
            actors.get(actor).add(new Actor(actor, null));
            inconsistencies.add(new Inconsistency(
                    "a", actor,
                    EnumSet.of(Option.STR, Option.MISS, Option.WARN)));
        } else {
            // possible warning if there exists multiple actors
            // since cast shall associate all actors of the same stage
            // name with the given movie (lack of temporal information)
            //
            // note: this is still schema consistent
            if (actors.get(actor).size() > 1) {
                inconsistencies.add(new Inconsistency(
                        "a", actor,
                        EnumSet.of(Option.STR, Option.WARN, Option.MULTI)));
            }
        }
    }

    public boolean equals(Object o) {
        // key equivalence for casts set
        if (this == o) return true;
        if (o instanceof Cast) {
            Cast c = (Cast) o;
            return Objects.equals(fid, c.fid)
                    && Objects.equals(actor, c.actor);
        }
        return false;
    }

    public int hashCode() {
        // hash equivalence for casts set
        return Objects.hash(fid, actor);
    }
}

// film element in mains.xml
class Film extends ParsedElement {
    public static final String HEADER = "sf";

    Set<String> cats = new TreeSet<>();
    String director;
    String fid;
    String title;
    Integer year;
    boolean schemaConsistent = true;

    public Film(int index,
                String director,
                Element element,
                Map<String, Film> films,
                Map<Integer, String> fids)
    {
        super(index, element.getTagName());
        int hashcode;
        // Parse film element based on main.dtd
        Element fidElement = (Element) element.getElementsByTagName("fid")
                .item(0);
        Element titleElement = (Element) element.getElementsByTagName("t")
                .item(0);
        Element yearElement = (Element) element.getElementsByTagName("year")
                .item(0);
        NodeList catNodeList = element.getElementsByTagName("cat");

        // director
        this.director = director;

        // fid
        if (fidElement == null) {
            inconsistencies.add(new Inconsistency(
                    "fid", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
            return;
        } else {
            fid = fidElement.getTextContent().trim();
            if (fid.isEmpty()) {
                inconsistencies.add(new Inconsistency(
                        "fid", fid,
                        EnumSet.of(Option.STR)));
                schemaConsistent = false;
                return;
            } else if (films.containsKey(fid)) {
                inconsistencies.add(new Inconsistency(
                        "fid", fid,
                        EnumSet.of(Option.STR, Option.DUP)));
                schemaConsistent = false; // no duplicates
                return;
            }
        }

        // title
        title = titleElement.getTextContent().trim();
        if (title.isEmpty()) {
            inconsistencies.add(new Inconsistency(
                    "title", title,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false; // assume empty title is null
            return;
        }

        // year
        try {
            year = Integer.parseInt(yearElement.getTextContent().trim());
        } catch (Exception e) {
            inconsistencies.add(new Inconsistency(
                    "year", yearElement.getTextContent().trim(),
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
            return;
        }

        // cats
        for (int i = 0; i < catNodeList.getLength(); i++) {
            Element catElement = (Element) catNodeList.item(i);
            String cat = catElement.getTextContent().trim();
            if (cat.isEmpty()) {
                // still schema consistent -- add warning
                inconsistencies.add(new Inconsistency(
                        "cat" + i, cat,
                        EnumSet.of(Option.STR, Option.WARN)));
                continue;
            }
            // map cats to their appropriate mappings if applicable
            cats.addAll(CatsMapping.collect(cat));
        }

        // check against duplicate by field equivalence
        hashcode = hashCode();
        if (fids.containsKey(hashcode)) {
            // insert cats from this film
            // to the referenced film
            // in case different cats have been defined
            String catsFmt;
            String fid2 = fids.get(hashcode);
            if (cats.isEmpty()) {
                catsFmt = "[]";
            } else {
                catsFmt = "['" + String.join("', '", cats) + "']";
                films.get(fid2).cats.addAll(cats);
            }

            // schema inconsistent due to duplication
            inconsistencies.add(new Inconsistency(
                    "fid", fid,
                    EnumSet.of(Option.STR)));
            inconsistencies.add(new Inconsistency(
                    "fid2", fid2,
                    EnumSet.of(Option.STR, Option.REF)));
            inconsistencies.add(new Inconsistency(
                    "title", title,
                    EnumSet.of(Option.STR)));
            inconsistencies.add(new Inconsistency(
                    "year", year.toString(),
                    EnumSet.of(Option.STR)));
            inconsistencies.add(new Inconsistency(
                    "director", director,
                    EnumSet.of(Option.STR)));
            inconsistencies.add(new Inconsistency(
                    "cats", catsFmt,
                    EnumSet.of(Option.PROP)));
            schemaConsistent = false;
        }
    }

    public boolean equals(Object o) {
        // key equivalence for casts set
        if (this == o) return true;
        if (o instanceof Film) {
            Film f = (Film) o;
            return Objects.equals(title, f.title)
                    && Objects.equals(year, f.year)
                    && Objects.equals(director, f.director);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(title, year, director);
    }

    public int parameterize(PreparedStatement st, int paramIndex)
            throws SQLException {
        st.setString(++paramIndex, fid);
        st.setString(++paramIndex, title);
        st.setInt(++paramIndex, year);
        st.setString(++paramIndex, director);
        return paramIndex;
    }
}

public class StanfordXmlParser {
    Connection conn;
    Document actorsDom;
    Document castsDom;
    Document mainsDom;
    Path cwd;
    Path inconsistencyPath;
    FileWriter inconsistencyWriter;

    StringBuilder inconsistentActorsBuf = new StringBuilder();
    StringBuilder inconsistentCastsBuf = new StringBuilder();
    StringBuilder inconsistentMainsBuf = new StringBuilder();
    StringBuilder inconsistentInsertsBuf = new StringBuilder();

    Set<String> cats = new TreeSet<>();
    Set<Cast> casts = new HashSet<>();
    Map<String, List<Actor>> actors = new TreeMap<>();
    Map<String, Film> films = new TreeMap<>();
    Map<Integer, String> fids = new HashMap<>();

    public StanfordXmlParser(Path path) throws Exception {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        // shared connection for single threads
        Class.forName("com.mysql.cj.jdbc.Driver")
                .getDeclaredConstructor().newInstance();
        conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
        conn.setAutoCommit(false);

        cwd = path;
        inconsistencyPath = cwd.resolve("inconsistencies.txt");
        inconsistencyWriter = new FileWriter(inconsistencyPath.toFile());
    }

    public void run() throws Exception {
        // extract step
        parseXmlFiles();

        // transform step
        parseActorsDocument();
        parseMainsDocument();
        parseCastsDocument();

        // load step
        loadCats();
        loadActors();
        loadFilms();
        loadCasts();

        outputParsedData(); // TEMP
    }

    public void close() throws Exception {
        // Commit and close connections
        conn.commit();
        conn.close();

        // Write inconsistent actors into report
        inconsistencyWriter.write("<!-- BEGIN actors.xml -->\n");
        inconsistencyWriter.write(inconsistentActorsBuf.toString());
        inconsistencyWriter.write("<!-- END actors.xml -->\n\n");

        // Write inconsistent casts into report
        inconsistencyWriter.write("<!-- BEGIN casts.xml -->\n");
        inconsistencyWriter.write(inconsistentCastsBuf.toString());
        inconsistencyWriter.write("<!-- END casts.xml -->\n\n");

        // Write inconsistent mains into report
        inconsistencyWriter.write("<!-- BEGIN mains.xml -->\n");
        inconsistencyWriter.write(inconsistentMainsBuf.toString());
        inconsistencyWriter.write("<!-- END mains.xml -->\n\n");

        // Write any corrected inconsistencies that may
        // appear during insertion into report
        inconsistencyWriter.write("<!-- BEGIN insertions -->\n");
        inconsistencyWriter.write(inconsistentInsertsBuf.toString());
        inconsistencyWriter.write("<!-- END insertions -->\n");

        // Flush the inconsistency report
        inconsistencyWriter.close();
    }

    private void parseXmlFiles() throws Exception {
        DocumentBuilderFactory dbFactory =
                DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            actorsDom = db.parse(cwd.resolve("actors63.xml")
                    .toFile());
            castsDom = db.parse(cwd.resolve("casts124.xml")
                    .toFile());
            mainsDom = db.parse(cwd.resolve("mains243.xml")
                    .toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Error: XML parse error");
            throw e;
        }
    }

    private void parseActorsDocument() {
        Element documentElement = actorsDom.getDocumentElement();
        NodeList nodeList = documentElement.getElementsByTagName("actor");

        int elementCnt = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            Actor actor = new Actor(elementCnt++, element, actors);
            if (actor.schemaConsistent) {
                // no schema inconsistencies
                // add to actors using stageName + dob as key
                if (!actors.containsKey(actor.stageName)) {
                    actors.put(actor.stageName, new ArrayList<>());
                }
                actors.get(actor.stageName).add(actor);
            }
            inconsistentActorsBuf.append(actor.reportInconsistencies());
        }
    }

    private void parseCastsDocument() {
        Element documentElement = castsDom.getDocumentElement();
        NodeList nodeList = documentElement.getElementsByTagName("m");

        int elementCnt = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            Cast cast = new Cast(elementCnt++, element, actors, films, casts);
            if (cast.schemaConsistent) {
                // no schema inconsistencies
                // add to casts using fid + actor as key
                casts.add(cast);
            }
            inconsistentCastsBuf.append(cast.reportInconsistencies());
        }
    }

    private void parseMainsDocument() {
        Element documentElement = mainsDom.getDocumentElement();
        NodeList nodeList = documentElement.getElementsByTagName("directorfilms");

        int elementCnt = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            Element directorElement = (Element) element
                    .getElementsByTagName("director").item(0);
            Element directorNameElement = (Element) directorElement
                    .getElementsByTagName("dirname").item(0);
            NodeList filmNodeList = element.getElementsByTagName("film");
            StringBuilder sb = new StringBuilder();
            String director = null;

            if (filmNodeList.getLength() == 0) {
                continue; // no films
            }

            if (directorNameElement != null) {
                director = directorNameElement.getTextContent().trim();
            }
            if (director == null || director.isEmpty()) {
                // Inconsistency: missing dirname tag; director is null
                sb.append("SKIP film ");
                sb.append(elementCnt);
                if (filmNodeList.getLength() > 1) {
                    sb.append("-");
                    sb.append(elementCnt + filmNodeList.getLength() - 1);
                }
                if (director == null) {
                    sb.append(": dirname=null;\n");
                } else {
                    sb.append(": dirname='';\n");
                }
                elementCnt += filmNodeList.getLength();
            } else {
                // Each film in this node list shall be associated
                // with the dirname text as the director
                for (int j = 0; j < filmNodeList.getLength(); j++) {
                    Element filmElement = (Element) filmNodeList.item(j);
                    Film film = new Film(elementCnt++, director, filmElement, films, fids);
                    if (film.schemaConsistent) {
                        // no schema inconsistencies
                        // add to films using fid as key
                        // also add fid to avoid possible duplicate entries by value
                        films.put(film.fid, film);
                        fids.put(film.hashCode(), film.fid);
                        cats.addAll(film.cats);
                    }
                    sb.append(film.reportInconsistencies());
                }
            }
            inconsistentMainsBuf.append(sb);
        }
    }

    private void loadCats() throws SQLException {
        // cats map to genres table
        // reduced to a single SQL call for efficiency
        StringBuilder insertSb = new StringBuilder();
        PreparedStatement insertStatement;
        int accum = 0;

        // this only inserts cats if they do not already exist
        insertSb.append("insert into genres(name) select name from (values ");
        insertSb.append("row(?),".repeat(cats.size()));
        insertSb.deleteCharAt(insertSb.length() - 1);
        insertSb.append(") as sf_xml_cats(name) ");
        insertSb.append("where name not in (select name from genres)");

        insertStatement = conn.prepareStatement(insertSb.toString());
        for (String cat : cats) {
            insertStatement.setString(++accum, cat);
        }
        insertStatement.executeUpdate();
        insertStatement.close();
    }

    private void loadActors() throws SQLException {
        // actors map to stars table
        // reduced to a few SQL queries for efficiency
        StringBuilder sbuf = new StringBuilder();
        StringBuilder selectSb = new StringBuilder();
        StringBuilder insertSb = new StringBuilder();
        PreparedStatement selectStatement;
        PreparedStatement insertStatement;
        ResultSet selectRs;
        Set<Actor> flattenedActors;
        int selectAccum = 0;
        int insertAccum = 0;

        // set of actors
        flattenedActors = actors.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        // single SQL query to detect duplicate actors
        selectSb.append("select sf_xml_actors.id, stageName, dob, (s.id) dupid from (values ");
        selectSb.append("row(?, ?, ?),".repeat(flattenedActors.size()));
        selectSb.deleteCharAt(selectSb.length() - 1);
        selectSb.append(") as sf_xml_actors(id, stageName, dob) ");
        selectSb.append("left join stars s on s.name=sf_xml_actors.stageName ");
        selectSb.append("and s.birthYear=sf_xml_actors.dob");
        selectStatement = conn.prepareStatement(selectSb.toString());
        for (Actor actor : flattenedActors) {
            selectAccum = actor.parameterize(selectStatement, selectAccum);
        }
        selectRs = selectStatement.executeQuery();
        while (selectRs.next()) {
            StringBuilder sb;
            String id;
            String stageName;
            String dupid = selectRs.getString("dupid");
            if (dupid != null) {
                // rectify inconsistency
                sb = new StringBuilder();
                id = selectRs.getString("id");
                stageName = selectRs.getString("stageName");
                sb.append("RECTIFY actor id='");
                sb.append(id);
                sb.append("' -> id='");
                sb.append(dupid);
                sb.append("' (EXISTS)\n");
                sbuf.append(sb);
                actors.get(stageName).removeIf(actor -> {
                   if (actor.id.equals(id)) {
                       flattenedActors.remove(actor);
                       return true;
                   }
                   return false;
                });
            }
        }

        // single SQL query to insert all remaining actors
        insertSb.append("insert into stars(id, name, birthYear) values ");
        insertSb.append("(?, ?, ?),".repeat(flattenedActors.size()));
        insertSb.deleteCharAt(insertSb.length() - 1);
        insertStatement = conn.prepareStatement(insertSb.toString());
        for (Actor actor : flattenedActors) {
            insertAccum = actor.parameterize(insertStatement, insertAccum);
            }
        insertStatement.executeUpdate();
        insertStatement.close();

        inconsistentInsertsBuf.append(sbuf);
    }

    private void loadCasts() throws SQLException {
        // casts map to stars_in_movies table
        String relExistsQuery = "select * from stars_in_movies where starId = ? and movieId = ?";
        String relInsertQuery = "insert into stars_in_movies(starId, movieId) values (?, ?)";
        for (Cast cast : casts) {
            // get the proper film id (which may have changed during insertion)
            String fid2 = films.get(cast.fid).fid;

            // insert relationship for all actors under the same stage name
            for (Actor actor : actors.get(cast.actor)) {
                PreparedStatement relExistsStatement = conn.prepareStatement(relExistsQuery);
                PreparedStatement relInsertStatement = conn.prepareStatement(relInsertQuery);
                ResultSet relExistsRs;

                // only insert if it doesn't already exist
                relExistsStatement.setString(1, actor.id);
                relExistsStatement.setString(2, fid2);
                relExistsRs = relExistsStatement.executeQuery();
                if (!relExistsRs.next()) {
                    relInsertStatement.setString(1, actor.id);
                    relInsertStatement.setString(2, fid2);
                    relInsertStatement.executeUpdate();
                }
                relExistsStatement.close();
                relInsertStatement.close();
            }
        }
    }

    private void loadFilms() throws SQLException {
        // films map to movies table
        // cats map to genre_in_movies table
        // reduced to a few SQL queries for efficiency
        StringBuilder sbuf = new StringBuilder();
        StringBuilder selectSb = new StringBuilder();
        StringBuilder insertSb = new StringBuilder();
        StringBuilder insertCatSb = new StringBuilder();
        StringBuilder insertPriceSb = new StringBuilder();
        PreparedStatement selectStatement;
        PreparedStatement insertStatement;
        PreparedStatement insertCatStatement;
        PreparedStatement insertPriceStatement;
        ResultSet selectRs;
        int selectAccum = 0;
        int insertAccum = 0;
        int insertCatAccum = 0;
        int insertPriceAccum = 0;

        // prepend unique header to avoid any
        // potential id conflicts with the existing database
        films.values().parallelStream().forEach(film -> {
           film.fid = Film.HEADER + film.fid;
        });

        /*
         * Single SQL query to possibly detect if a film is a
         * duplicate with a pre-existing film in the database
         */
        selectSb.append("select fid, sf_xml_films.title, sf_xml_films.year, ");
        selectSb.append("sf_xml_films.director, (m.id) dupid from (values ");
        selectSb.append("row(?, ?, ?, ?),".repeat(films.values().size()));
        selectSb.deleteCharAt(selectSb.length() - 1);
        selectSb.append(") as sf_xml_films(fid, title, year, director) ");
        selectSb.append("left join movies m on m.title=sf_xml_films.title ");
        selectSb.append("and m.year=sf_xml_films.year and m.director=sf_xml_films.director");
        selectStatement = conn.prepareStatement(selectSb.toString());
        for (Film film : films.values()) {
            selectAccum = film.parameterize(selectStatement, selectAccum);
        }
        selectRs = selectStatement.executeQuery();
        while (selectRs.next()) {
            StringBuilder sb;
            String fid;
            String dupid = selectRs.getString("dupid");
            if (dupid != null) {
                // rectify inconsistency
                sb = new StringBuilder();
                fid = selectRs.getString("fid");
                sb.append("RECTIFY film fid='");
                sb.append(fid);
                sb.append("' -> fid='");
                sb.append(dupid);
                sb.append("' (EXISTS)\n");
                sbuf.append(sb);
                films.remove(fid);
            }
        }
        selectStatement.close();

        /*
         * SQL queries to insert all movies, associate genres,
         * and generate random prices.
         * Each step is done in a single SQL query
         */

        // insert all movies
        insertSb.append("insert into movies(id, title, year, director) values ");
        insertSb.append("(?, ?, ?, ?),".repeat(films.values().size()));
        insertSb.deleteCharAt(insertSb.length() - 1);
        insertStatement = conn.prepareStatement(insertSb.toString());
        for (Film film : films.values()) {
            insertAccum = film.parameterize(insertStatement, insertAccum);
        }
        insertStatement.executeUpdate();
        insertStatement.close();

        // insert all relatable cats
        insertCatSb.append("insert into genres_in_movies(genreId, movieId) ");
        insertCatSb.append("select g.id, fid from (values ");
        insertCatSb.append("row(?, ?),".repeat(
                films.values().parallelStream()
                        .mapToInt(film -> film.cats.size())
                        .sum())
        );
        insertCatSb.deleteCharAt(insertCatSb.length() - 1);
        insertCatSb.append(") as sf_xml_relcats(fid, cat), genres g ");
        insertCatSb.append("where g.name=sf_xml_relcats.cat");
        insertCatStatement = conn.prepareStatement(insertCatSb.toString());
        for (Film film : films.values()) {
            for (String cat : film.cats) {
                insertCatStatement.setString(++insertCatAccum, film.fid);
                insertCatStatement.setString(++insertCatAccum, cat);
            }
        }
        insertCatStatement.executeUpdate();
        insertCatStatement.close();

        // generate random prices for each new film
        insertPriceSb.append("insert into prices(movieId, price) ");
        insertPriceSb.append("select fid, truncate(rand() * 99.00 + 1.00, 2) price from (values ");
        insertPriceSb.append("row(?),".repeat(films.values().size()));
        insertPriceSb.deleteCharAt(insertPriceSb.length() - 1);
        insertPriceSb.append(") as sf_xml_film(fid)");
        insertPriceStatement = conn.prepareStatement(insertPriceSb.toString());
        for (Film film : films.values()) {
            insertPriceStatement.setString(++insertPriceAccum, film.fid);
        }
        insertPriceStatement.executeUpdate();
        insertPriceStatement.close();

        inconsistentInsertsBuf.append(sbuf);
    }

    private void outputParsedData() {
        // outputs parsed data to files (REMOVE LATER)
        // the filenames will start with an underscore
        try (FileWriter catsFw = new FileWriter(cwd.resolve("_cats.txt").toFile());
             FileWriter actorsFw = new FileWriter(cwd.resolve("_actors.txt").toFile());
             FileWriter castsFw = new FileWriter(cwd.resolve("_casts.txt").toFile());
             FileWriter filmsFw = new FileWriter(cwd.resolve("_films.txt").toFile()))
        {
            catsFw.write(cats.size() + " cats\n");
            for (String cat : cats) {
                catsFw.write(cat);
                catsFw.write("\n");
            }

            actorsFw.write(actors.size() + " actors\n");
            for (var entry : actors.entrySet()) {
                List<Actor> actors = entry.getValue();
                for (var actor : actors) {
                    actorsFw.write("'"
                            + actor.stageName + '-' + actor.dob
                            + "' : ");
                    actorsFw.write(String.format(
                            "Actor( id='%s', stageName='%s', dob=",
                            actor.id,
                            actor.stageName));
                    if (actor.dob == null) {
                        actorsFw.write("null )\n");
                    } else {
                        actorsFw.write(actor.dob + " )\n");
                    }
                }
            }

            castsFw.write(casts.size() + " cast relationships\n");
            for (Cast cast : casts) {
                castsFw.write(String.format(
                        "Cast ( fid='%s', actor='%s' )\n",
                        cast.fid,
                        cast.actor));
            }

            filmsFw.write(films.size() + " films\n");
            for (var entry : films.entrySet()) {
                Film val = entry.getValue();
                filmsFw.write("'" + entry.getKey() + "' : ");
                filmsFw.write(String.format(
                        "Film( fid='%s', director='%s', year=%d, title='%s', cats=",
                        val.fid,
                        val.director,
                        val.year,
                        val.title));
                if (val.cats.isEmpty()) {
                    filmsFw.write("[] )\n");
                } else {
                    filmsFw.write(String.format("['%s'] )\n",
                            String.join("', '", val.cats)));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not output parsed stuff to temp files");
        }
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        long elapsed;

        Function<Path, String> canonicalize = p -> {
            try {
                return p.toFile().getCanonicalPath();
            } catch (Exception e) {
                return p.toFile().getPath();
            }
        };
        Path xmlPath = Paths.get("stanford-movies");

        // Takes in an argument that specifies
        // where the XML files are located
        // By default, the parser uses the path "./stanford-movies"
        if (args.length >= 1) {
            xmlPath = Paths.get(args[0]);
        }
        if (!xmlPath.toFile().isDirectory()) {
            System.out.println("Error: File at \""
                    + canonicalize.apply(xmlPath) + "\" does not exist");
            System.exit(1);
        }
        
        // Run Stanford XML parser with the
        // path argument (supplied or default)
        try {
            System.out.println("Running StanfordXmlParser using \""
                    + canonicalize.apply(xmlPath) + "\"");
            StanfordXmlParser parser = new StanfordXmlParser(xmlPath);
            parser.run();
            parser.close();
            System.out.println("Finished executing StanfordXmlParser");
            System.out.println("Inconsistency report written to \""
                    + canonicalize.apply(parser.inconsistencyPath) + "\"");
        } catch (Exception e) {
            System.out.println("Error: StanfordXmlParser encountered a fatal error");
            e.printStackTrace();
            System.exit(1);
        } finally {
            elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Elapsed time: " + elapsed / 1000.0 + "s");
        }
    }
}
