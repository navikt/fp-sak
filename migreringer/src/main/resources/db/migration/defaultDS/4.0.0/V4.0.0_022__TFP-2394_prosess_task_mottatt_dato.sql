INSERT INTO PROSESS_TASK_TYPE (KODE,
                               NAVN,
                               FEIL_MAKS_FORSOEK,
                               FEIL_SEK_MELLOM_FORSOEK,
                               FEILHANDTERING_ALGORITME,
                               BESKRIVELSE,
                               CRON_EXPRESSION)
VALUES ('oppdater.yf.soknad.mottattdato', 'Mottatt dato for søknadsperioder', 1, 10, 'DEFAULT',
        'Mottatt dato for søknadsperioder der det ikke finnes', '');
