:: call mvn spring-boot:run -am -pl turing-app -Dskip.npm
call mvn spring-boot:run -pl turing-app -Dskip.npm -Dspring-boot.run.arguments="--turing.mongodb.enabled=true --logging.config=classpath:logback-spring-mongo.xml"
