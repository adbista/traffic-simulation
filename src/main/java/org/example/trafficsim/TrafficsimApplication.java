package org.example.trafficsim;

import org.example.trafficsim.app.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrafficsimApplication {

	public static void main(String[] args) throws Exception {
		if (args.length == 2) {
			Main.main(args);
			return;
		}
		SpringApplication.run(TrafficsimApplication.class, args);
	}

}
