package com.esch.HorrorMoviesNew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootApplication
@RestController
@RequestMapping("/movies")
@CrossOrigin(origins = "*")
public class HorrorMoviesNewApplication {

    private final DataSource dataSource;

    public HorrorMoviesNewApplication(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeDatabase();
    }

    public static void main(String[] args) {
        SpringApplication.run(HorrorMoviesNewApplication.class, args);
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS horror_movies (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), release_year INT, director VARCHAR(255), poster VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS reviews (id INT AUTO_INCREMENT PRIMARY KEY, movie_id INT, review TEXT, FOREIGN KEY(movie_id) REFERENCES horror_movies(id))");
            stmt.execute("INSERT INTO horror_movies (title, release_year, director, poster) VALUES ('The Exorcist', 1973, 'William Friedkin', 'exorcist.jpg'), ('Halloween', 1978, 'John Carpenter', 'halloween.jpg'), ('A Nightmare on Elm Street', 1984, 'Wes Craven', 'elm_street.jpg'), ('The Shining', 1980, 'Stanley Kubrick', 'shining.jpg')");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping
    public List<Map<String, Object>> getMovies(@RequestParam(value = "search", required = false) String search) {
        List<Map<String, Object>> movies = new ArrayList<>();

        String qry = "SELECT * FROM horror_movies";

        if (search != null && !search.isBlank()) {
            qry = "SELECT * FROM horror_movies WHERE "
                    + "title LIKE '%" + search + "%' OR "
                    + "director LIKE '%" + search + "%' OR "
                    + "CAST(release_year AS VARCHAR) LIKE '%" + search + "%'";
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(qry)) {

            while (rs.next()) {
                Map<String, Object> movie = new HashMap<>();
                movie.put("id", rs.getInt("id"));
                movie.put("title", rs.getString("title"));
                movie.put("release_year", rs.getInt("release_year"));
                movie.put("director", rs.getString("director"));
                movie.put("poster", rs.getString("poster"));
                movies.add(movie);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    // A01 - Broken Access Control (Ingen åtkomstkontroll)
    // Applikationen saknar autentisering och auktorisering, så vem som helst kan lägga till eller ändra reviews.
    // Använd Postman för att lägga till en review utan autentisering:
    // http://localhost:8080/movies/review?movieId=1 OR 1=1&review=Hello

    // A08 - Cross-Site Scripting (XSS)
    // Applikationen kontrollerar inte användarinmatning i review-fältet, vilket gör att det går att injicera skadlig
    // JavaScript-kod.
    // Använd Postman för att injicera skript:
    // http://localhost:8080/movies/review?movieId=1&review=<script>alert("XSS");</script>
    @PostMapping("/review")
    public String addReview(@RequestParam String movieId, @RequestParam String review) {
        String qry = "INSERT INTO reviews (movie_id, review) VALUES (" + movieId + ", '" + review + "')";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(qry);
            return "Review added: " + review;

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to add review";
        }
    }

    // A03 - SQL Injection (Direkt användning av användarens input i SQL-frågor)
    // Applikationen använder direkt användarinmatning i SQL-frågor utan validering vilket gör det går att injicera SQL-kommandon och manipulera databasen.
    // Använd Postman för att injicera skadlig SQL och ta bort tabellen 'reviews':
    // http://localhost:8080/movies/review?movieId=1&review='); DROP TABLE reviews; --
    @GetMapping("/reviews")
    public List<String> getReviews(@RequestParam int movieId) {
        List<String> reviews = new ArrayList<>();

        String qry = "SELECT review FROM reviews WHERE movie_id = " + movieId;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(qry)) {

            while (rs.next()) {
                reviews.add(rs.getString("review"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return reviews;
    }

    // A04 - Path Traversal (Felaktig hantering av filvägar)
    // Applikationen tillåter användare att begära filer genom att skicka filsökvägar som innehåller "..". Med det går
    // det att komma åt filer utanför den avsedda katalogen såsom systemfiler.
    // Använd Postman för att läsa känsliga systemfiler:
    // http://localhost:8080/movies/poster?filename=../../../../Windows/system.ini

    // A05 - Sensitive File Exposure (Exponering av känsliga filer)
    // Applikationen tillåter användare att begära filer som ligger utanför den avsedda katalogen. Detta innebär att
    // applikationen kan visa känsliga filer som konfigurationsfiler eller andra känsliga dokument som databasanvändare
    // och lösenord.

    // Använd Postman för att få tillgång till interna konfigurationsfiler:
    // http://localhost:8080/movies/poster?filename=/src/main/resources/application.properties
    @GetMapping("/poster")
    public ResponseEntity<byte[]> getMoviePoster(@RequestParam String filename) throws IOException {
        File baseDir = new File(".").getCanonicalFile();
        File file;

        if (filename.startsWith("/") || filename.startsWith("..") || filename.contains("/")) {
            file = new File(baseDir, filename).getCanonicalFile();
        } else {
            file = new File("posters", filename);
        }

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] imgBytes = Files.readAllBytes(file.toPath());
        String contentType = Files.probeContentType(file.toPath());

        if (contentType == null) {
            contentType = "text/plain";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(imgBytes);
    }
}