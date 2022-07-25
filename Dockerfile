# available at https://github.com/evbruno/dockerfiles
FROM educhaos/scala-openjdk11:2.13

WORKDIR /app

COPY . /app/

ENTRYPOINT sbt run