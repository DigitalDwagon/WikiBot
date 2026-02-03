FROM gradle:jdk21 AS build
COPY --chown=gradle:gradle ./src /home/gradle/src
COPY --chown=gradle:gradle ./gradle /home/gradle/gradle
COPY --chown=gradle:gradle ./build.gradle.kts /home/gradle
COPY --chown=gradle:gradle ./settings.gradle.kts /home/gradle
WORKDIR /home/gradle
RUN gradle build --no-daemon

FROM eclipse-temurin:21-jammy
RUN mkdir /wikibot
COPY --from=build /home/gradle/build/libs/*shadow.jar /wikibot/wikibot.jar
WORKDIR /wikibot

RUN apt update
RUN apt install -y python3 python3-pip zstd p7zip-full

# These installations have to run separately, or else pip will try to resolve the dependency conflicts and use very old versions
RUN pip install --no-cache-dir --upgrade dokuWikiDumper
RUN pip install --no-cache-dir --upgrade pukiWikiDumper
RUN pip install --no-cache-dir --upgrade wikiteam3

ENV PYTHONUNBUFFERED=true
ENTRYPOINT ["java", "-jar", "/wikibot/wikibot.jar"]
