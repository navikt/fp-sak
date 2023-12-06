package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

record StønadsstatistikkUttakPeriode(@NotNull LocalDate fom, @NotNull LocalDate tom,
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
        SAMTIDIG_MØDREKVOTE
    }

    enum AktivitetType {
        ARBEIDSTAKER, FRILANS, NÆRING
    }

    record Gradering(AktivitetType aktivitetType, BigDecimal arbeidsprosent) {}
}
