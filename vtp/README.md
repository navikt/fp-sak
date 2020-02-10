# Hack for å omgåes MQ når man kjører mot VTP.

## For å laste ned klientene utenfor NK:
```
$http_proxy=socks5h://127.0.0.1:5000 vtp/getmqclients.sh
```

## Bygging av docker image for å teste på egen laptop
Dette er ikke mulig denne saken må testet fra sikker sone. Først bygge applikasjonen, så kjøre
docker build kommandoen med CURL som downloadscript. Dette fordi WGET ikke støtter socks5 ut av 
boksen og CURL ikke er installert på utviklerimaget. Tungvint, men da slipper vi å endre pipeline.
```bash
mvnk -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install

docker build -t fpsak . --build-arg DOWNLOAD_SCRIPT=getmqclients-curl.sh
```

## Kjøre opp FPSAK med VTP-vennlige biblioteker
Dette gjøres med å sette environment-variabel $EXTRA_CLASS_PATH med mappen der artifaktene som
overstyrer MQ ligger. 
```yaml
      - EXTRA_CLASS_PATH=:vtp-lib/*
```

