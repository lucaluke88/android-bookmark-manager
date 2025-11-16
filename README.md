# Bookmark Manager

Un'applicazione Android per gestire i segnalibri del browser con supporto per import/export CSV, ricerca e Material Design 3 con modalità AMOLED.

## Caratteristiche

- ✅ **Gestione Segnalibri**: Aggiungi, modifica ed elimina segnalibri con titolo e URL
- ✅ **Ricerca**: Cerca segnalibri per titolo o URL in tempo reale
- ✅ **Import/Export CSV**: Importa ed esporta i tuoi segnalibri in formato CSV
- ✅ **Material Design 3**: Interfaccia moderna con Material Design 3
- ✅ **Modalità AMOLED**: Supporto nativo per dark mode ottimizzata per schermi AMOLED
- ✅ **Compatibilità**: Supporta Android 19 (API level 19, Android 4.4 KitKat) e versioni successive

## Tecnologie Utilizzate

- **Kotlin**: Linguaggio di programmazione
- **Room Database**: Persistenza dati locale
- **Material Design 3**: Libreria UI moderna
- **Coroutines & Flow**: Programmazione asincrona
- **ViewBinding**: Binding delle view
- **MVVM Architecture**: Pattern architetturale

## Struttura del Progetto

```
app/
├── src/main/
│   ├── java/com/bookmarkmanager/
│   │   ├── data/              # Room entities, DAO, Database
│   │   ├── repository/         # Repository pattern
│   │   ├── viewmodel/          # ViewModels
│   │   ├── adapter/            # RecyclerView adapters
│   │   ├── util/               # Utility classes (CSV)
│   │   └── MainActivity.kt     # Main activity
│   ├── res/
│   │   ├── layout/             # XML layouts
│   │   ├── values/             # Strings, colors, themes
│   │   └── values-night/       # Dark theme resources
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Come Compilare

1. Apri il progetto in Android Studio
2. Sincronizza i file Gradle
3. Compila ed esegui l'app su un dispositivo o emulatore

## Requisiti

- Android Studio Hedgehog o superiore
- Min SDK: 19 (Android 4.4 KitKat) - Nota: le librerie moderne (Material Design 3, Lifecycle) richiedono almeno API 19
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20+

## Funzionalità Dettagliate

### Gestione Segnalibri
- Aggiungi nuovi segnalibri con titolo e URL
- Modifica segnalibri esistenti
- Elimina segnalibri con conferma

### Ricerca
- Ricerca in tempo reale mentre digiti
- Cerca sia nel titolo che nell'URL
- Case-insensitive

### Import/Export CSV
- Formato CSV standard con header "Title,URL"
- Supporto per quote escaping
- Compatibile con altri gestori di segnalibri

### Tema AMOLED
- Sfondo nero puro (#000000) in modalità dark
- Ottimizzato per schermi AMOLED per risparmiare batteria
- Transizione automatica tra light e dark mode

## Licenza

Questo progetto è open source e disponibile per uso personale e commerciale.

