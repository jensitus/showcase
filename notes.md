## Build: Manuelle Schritte

### Test für Azure Repository
```sh
docker build . -t phactum.azurecr.io/insurance-showcase-frontend --platform linux/x86_64
```

### Test für lokale 
```sh
docker build . -t insurance-showcase-frontend
```

## Run: 

"foreground": 
für background option "-d"
```sh
docker run --rm -it -p 4200:4200 insurance-showcase-frontend
```

