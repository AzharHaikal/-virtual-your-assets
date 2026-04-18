# Build App
mvn clean install

# Run SonnarQube
mvn clean verify sonar:sonar "-Dsonar.projectKey=VirtualYourAsset" "-Dsonar.host.url=http://localhost:9000" "-Dsonar.login=sqp_553310e85e96f72a7615470fa73b4ac003916479"