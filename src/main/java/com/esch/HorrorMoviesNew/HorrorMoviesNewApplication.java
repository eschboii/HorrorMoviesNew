package com.esch.HorrorMoviesNew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
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
		return movies;
	}

	@PostMapping("/review")
	public String addReview(@RequestParam int movieId, @RequestParam String review) {
		return null;
	}

	@GetMapping("/reviews")
	public List<String> getReviews(@RequestParam int movieId) {
		List<String> reviews = new ArrayList<>();
		return reviews;
	}

	@GetMapping("/poster")
	public ResponseEntity<byte[]> getMoviePoster(@RequestParam String filename) throws IOException {
		return ResponseEntity.ok(null);
	}
}

