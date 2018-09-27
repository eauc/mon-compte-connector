FROM clojure:lein

WORKDIR /app

COPY project.clj .
RUN lein deps

COPY ./src ./src
COPY ./test ./test
COPY ./.git ./.git

RUN lein uberjar

CMD ["sh", "-c", "java -jar target/connector-*-standalone.jar --config ${CONFIG_FILE} --password ${CERTS_FILE_PASSWORD} ${CERTS_FILE}"]
