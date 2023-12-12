package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

record StønadsstatistikkUttakPeriode(@NotNull LocalDate fom, @NotNull LocalDate tom,
                                     PeriodeType type,
                                     StønadsstatistikkVedtak.StønadskontoType stønadskontoType, // hvilken konta man tar ut fra
                                     @NotNull StønadsstatistikkVedtak.RettighetType rettighetType,
                                     Forklaring forklaring,
                                     boolean erUtbetaling, // Skal utbetales for perioden
                                     int virkedager,
                                     @NotNull StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager trekkdager,
                                     @Valid Gradering gradering, // Perioden er gradert
                                     BigDecimal samtidigUttakProsent ) {




    enum Forklaring {
        UTSETTELSE_FERIE,
        UTSETTELSE_ARBEID,
        UTSETTELSE_INNLEGGELSE,
        UTSETTELSE_BARNINNLAGT,
        UTSETTELSE_SYKDOM,
        UTSETTELSE_HVOVELSE,
        UTSETTELSE_NAVTILTAK,
        OVERFØRING_ANNEN_PART_SYKDOM,
        OVERFØRING_ANNEN_PART_INNLAGT,
        OVERFØRING_ALENEOMSORG,
        OVERFØRING_BARE_SØKER_RETT,

        // Far fellesperiode/foreldrepenger
        AKTIVITETSKRAV_ARBEID,
        AKTIVITETSKRAV_UTDANNING,
        AKTIVITETSKRAV_ARBEIDUTDANNING,
        AKTIVITETSKRAV_SYKDOM,
        AKTIVITETSKRAV_INNLEGGELSE,
        AKTIVITETSKRAV_INTRODUKSJONSPROGRAM,
        AKTIVITETSKRAV_KVALIFISERINGSPROGRAM,
        MINSTERETT,
        FLERBARNSDAGER,
        //Samtidig
        SAMTIDIG_MØDREKVOTE,

        AVSLAG_ANNET,
        AVSLAG_AKTIVITETSKRAV,
        AVSLAG_SØKNADSFRIST,
        AVSLAG_IKKE_SØKT,
        AVSLAG_UTSETTELSE,
        AVSLAG_UTSETTELSE_TILBAKE_I_TID,
        AVSLAG_PLEIEPENGER,
        AVSLAG_STARTET_NY_STØNADSPERIODE,
        AVSLAG_BARNETS_ALDER,
        AVSLAG_VILKÅR
    }

    enum AktivitetType {
        ARBEIDSTAKER, FRILANS, NÆRING
    }

    record Gradering(AktivitetType aktivitetType, BigDecimal arbeidsprosent) {}

    enum PeriodeType {
        UTTAK, UTSETTELSE, AVSLAG
    }
}
