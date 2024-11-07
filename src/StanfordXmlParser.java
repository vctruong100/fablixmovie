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

abstract class ParsedElement {
    enum Option {
        STR,
        NUM,
        DUP,
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
        } else {
            fid = fElement.getTextContent().trim();
        }

        // a
        if (aElement == null) {
            inconsistencies.add(new Inconsistency(
                    "a", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
        } else {
            actor = aElement.getTextContent().trim();
            if (actor.isEmpty()) {
                inconsistencies.add(new Inconsistency(
                        "a", actor,
                        EnumSet.of(Option.STR)));
                schemaConsistent = false;
            }
        }

        // check foreign key constraints on fid
        // indicate inconsistencies with a MISS
        if (!films.containsKey(fid)) {
            inconsistencies.add(new Inconsistency(
                    "f", fid,
                    EnumSet.of(Option.STR, Option.MISS)));
            schemaConsistent = false;
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
        }

        // if actor does not exist, add a new actor with dob=null
        // the actor is only added if schema is consistent
        if (!actors.containsKey(actor)) {
            if (schemaConsistent) {
                actors.put(actor, new ArrayList<>());
                actors.get(actor).add(new Actor(actor, null));
            }
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
            return fid.equals(c.fid) && actor.equals(c.actor);
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

    List<String> cats = new ArrayList<>();
    String director;
    String fid;
    String title;
    Integer year;
    boolean schemaConsistent = true;

    public Film(int index, Element element, Map<String, Film> films) {
        super(index, element.getTagName());
        // Parse film element based on main.dtd
        Element fidElement = (Element) element.getElementsByTagName("fid")
                .item(0);
        Element titleElement = (Element) element.getElementsByTagName("t")
                .item(0);
        Element yearElement = (Element) element.getElementsByTagName("year")
                .item(0);
        NodeList catNodeList = element.getElementsByTagName("cat");

        // fid
        if (fidElement == null) {
            inconsistencies.add(new Inconsistency(
                    "fid", null,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
        } else {
            fid = fidElement.getTextContent().trim();
            if (fid.isEmpty()) {
                inconsistencies.add(new Inconsistency(
                        "fid", fid,
                        EnumSet.of(Option.STR)));
                schemaConsistent = false;
            } else if (films.containsKey(fid)) {
                inconsistencies.add(new Inconsistency(
                        "fid", fid,
                        EnumSet.of(Option.STR, Option.DUP)));
                schemaConsistent = false; // no duplicates
            }
        }

        // title
        title = titleElement.getTextContent().trim();
        if (title.isEmpty()) {
            inconsistencies.add(new Inconsistency(
                    "title", title,
                    EnumSet.of(Option.STR)));
            schemaConsistent = false; // assume empty title is null
        }

        // year
        try {
            year = Integer.parseInt(yearElement.getTextContent().trim());
        } catch (Exception e) {
            inconsistencies.add(new Inconsistency(
                    "year", yearElement.getTextContent().trim(),
                    EnumSet.of(Option.STR)));
            schemaConsistent = false;
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

    public StanfordXmlParser(Path path) throws Exception {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        Class.forName("com.mysql.jdbc.Driver").newInstance();
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

        // insert step
        insertCats();
        insertActors();
        insertFilms();
        insertCasts();

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
            StringBuilder buffer = new StringBuilder();

            if (filmNodeList.getLength() == 0) {
                continue; // no films
            }

            if (directorNameElement == null) {
                // Inconsistency: missing dirname tag; director is null
                buffer.append("SKIP film ");
                buffer.append(elementCnt);
                if (filmNodeList.getLength() > 1) {
                    buffer.append("-");
                    buffer.append(elementCnt + filmNodeList.getLength() - 1);
                }
                buffer.append(": dirname=null;\n");
                elementCnt += filmNodeList.getLength();
            } else {
                // Each film in this node list shall be associated
                // with the dirname text as the director
                for (int j = 0; j < filmNodeList.getLength(); j++) {
                    Element filmElement = (Element) filmNodeList.item(j);
                    Film film = new Film(elementCnt++, filmElement, films);
                    film.director = directorNameElement.getTextContent();
                    if (film.schemaConsistent) {
                        // no schema inconsistencies
                        // add to films using fid as key
                        films.put(film.fid, film);
                    }
                    buffer.append(film.reportInconsistencies());
                    cats.addAll(film.cats);
                }
            }
            if (buffer.length() > 0) {
                inconsistentMainsBuf.append(buffer);
            }
        }
    }

    private void insertCats() throws SQLException {
        // cats map to genres table
        String existsQuery = "select * from genres where name = ?";
        String insertQuery = "insert into genres(name) values (?)";
        for (String cat : cats) {
            PreparedStatement existsStatement = conn.prepareStatement(existsQuery);
            PreparedStatement insertStatement = conn.prepareStatement(insertQuery);
            ResultSet existsRs;

            // only insert if it doesn't already exist
            existsStatement.setString(1, cat);
            existsRs = existsStatement.executeQuery();
            if (!existsRs.next()) {
                insertStatement.setString(1, cat);
                insertStatement.executeUpdate();
            }
            existsStatement.close();
            insertStatement.close();
        }
    }

    private void insertActors() throws SQLException {
        // actors map to stars table
        String existsQuery = "select * from stars where name = ? and birthYear = ?";
        String insertQuery = "insert into stars(id, name, birthYear) values (?, ?, ?)";
        for (var val : actors.values()) {
            for (Actor actor : val) {
                PreparedStatement existsStatement = conn.prepareStatement(existsQuery);
                PreparedStatement insertStatement = conn.prepareStatement(insertQuery);
                ResultSet existsRs;

                // only insert if it doesn't already exist
                existsStatement.setString(1, actor.stageName);
                if (actor.dob != null) {
                    existsStatement.setInt(2, actor.dob);
                } else {
                    existsStatement.setNull(2, Types.INTEGER);
                }

                existsRs = existsStatement.executeQuery();
                if (!existsRs.next()) {
                    insertStatement.setString(1, actor.id);
                    insertStatement.setString(2, actor.stageName);
                    if (actor.dob != null) {
                        insertStatement.setInt(3, actor.dob);
                    } else {
                        insertStatement.setNull(3, Types.INTEGER);
                    }
                    insertStatement.executeUpdate();
                } else {
                    // if it exists, use the id in the database
                    String oldId = actor.id;
                    actor.id = existsRs.getString("id");

                    inconsistentInsertsBuf.append("REPLACE actor id='");
                    inconsistentInsertsBuf.append(oldId);
                    inconsistentInsertsBuf.append("' -> id='");
                    inconsistentInsertsBuf.append(actor.id);
                    inconsistentInsertsBuf.append("' (EXISTS)\n");
                }
                existsStatement.close();
                insertStatement.close();
            }
        }
    }

    private void insertCasts() throws SQLException {
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

    private void insertFilms() throws SQLException {
        // films map to movies table
        // cats map to genre_in_movies table
        String existsQuery = "select * from movies where title = ? and year = ? and director = ?";
        String insertQuery = "insert into movies(id, title, year, director) values (?, ?, ?, ?)";
        String genreIdQuery = "select id from genres where name = ?";
        String relExistsQuery = "select * from genres_in_movies where movieId = ? and genreId = ?";
        String relInsertQuery = "insert into genres_in_movies(movieId, genreId) values (?, ?)";
        String priceInsertQuery = "insert into prices(movieId, price) "
                + "values (?, truncate(rand() * 99.00 + 1.00, 2))";
        for (Film film : films.values()) {
            PreparedStatement existsStatement = conn.prepareStatement(existsQuery);
            PreparedStatement insertStatement = conn.prepareStatement(insertQuery);
            PreparedStatement priceInsertStatement = conn.prepareStatement(priceInsertQuery);
            ResultSet existsRs;

            // prepend a unique header to avoid any
            // potential id conflicts with the database
            film.fid = Film.HEADER + film.fid;

            // only insert if it doesn't already exist
            existsStatement.setString(1, film.title);
            existsStatement.setInt(2, film.year);
            existsStatement.setString(3, film.director);
            existsRs = existsStatement.executeQuery();

            if (!existsRs.next()) {
                insertStatement.setString(1, film.fid);
                insertStatement.setString(2, film.title);
                insertStatement.setInt(3, film.year);
                insertStatement.setString(4, film.director);
                insertStatement.executeUpdate();

                priceInsertStatement.setString(1, film.fid);
                priceInsertStatement.executeUpdate();
            } else {
                // if it exists, use the id in the database
                String oldId = film.fid;
                film.fid = existsRs.getString("id");

                inconsistentInsertsBuf.append("REPLACE film fid='");
                inconsistentInsertsBuf.append(oldId);
                inconsistentInsertsBuf.append("' -> fid='");
                inconsistentInsertsBuf.append(film.fid);
                inconsistentInsertsBuf.append("' (EXISTS)\n");
            }
            existsStatement.close();
            insertStatement.close();
            priceInsertStatement.close();

            // insert all cat relationships
            for (String cat : film.cats) {
                int genreId;
                PreparedStatement genreIdStatement = conn.prepareStatement(genreIdQuery);
                PreparedStatement relExistsStatement = conn.prepareStatement(relExistsQuery);
                PreparedStatement relInsertStatement = conn.prepareStatement(relInsertQuery);
                ResultSet genreIdRs;
                ResultSet relExistsRs;

                // get genre id for cat
                genreIdStatement.setString(1, cat);
                genreIdRs = genreIdStatement.executeQuery();
                if (!genreIdRs.next()) {
                    genreIdStatement.close();
                    relExistsStatement.close();
                    relInsertStatement.close();
                    continue; // ??? missing cat
                }
                genreId = genreIdRs.getInt("id");

                // only insert if it doesn't already exist
                relExistsStatement.setString(1, film.fid);
                relExistsStatement.setInt(2, genreId);
                relExistsRs = relExistsStatement.executeQuery();
                if (!relExistsRs.next()) {
                    relInsertStatement.setString(1, film.fid);
                    relInsertStatement.setInt(2, genreId);
                    relInsertStatement.executeUpdate();
                }
                genreIdStatement.close();
                relExistsStatement.close();
                relInsertStatement.close();
            }
        }
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
        } catch(Exception e) {
            System.out.println("Error: StanfordXmlParser encountered a fatal error");
            e.printStackTrace();
            System.exit(1);
        } finally {
            elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Elapsed time: " + elapsed / 1000.0 + "s");
        }
    }
}
