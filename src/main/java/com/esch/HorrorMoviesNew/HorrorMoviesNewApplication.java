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
import java.nio.file.Path;
import java.nio.file.Paths;
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

        if(search != null && !search.isBlank()){
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

    @PostMapping("/review")
    public String addReview(@RequestParam int movieId, @RequestParam String review) {
        return review;
    }

    @GetMapping("/reviews")
    public List<String> getReviews(@RequestParam int movieId) {
        List<String> reviews = new ArrayList<>();
        return reviews;
    }

    @GetMapping("/poster")
    public ResponseEntity<byte[]> getMoviePoster(@RequestParam String filename) throws IOException {
        Path imgPath = Paths.get("posters", filename);
        File file = imgPath.toFile();

        if(!file.exists()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] imgBytes = Files.readAllBytes(imgPath);
        String contentType = Files.probeContentType(imgPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(imgBytes);
    }
}

