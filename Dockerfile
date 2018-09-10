FROM clojure:lein

WORKDIR /app

COPY project.clj .
RUN lein deps

COPY ./src ./src
COPY ./test ./test

RUN lein uberjar

CMD ["sh", "-c", "java -jar target/connector-1.0.0-SNAPSHOT-standalone.jar --config ${CONFIG_FILE} --password ${CERTS_FILE_PASSWORD} ${CERTS_FILE}"]
