package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

record StønadsstatistikkUttakPeriode(@NotNull LocalDate fom,
                                     @NotNull LocalDate tom,
                                     PeriodeType type,
                                     StønadsstatistikkVedtak.StønadskontoType stønadskontoType, // hvilken konta man tar ut fra
                                     @NotNull StønadsstatistikkVedtak.RettighetType rettighetType,
                                     Forklaring forklaring,
                                     LocalDate søknadsdato,
                                     boolean erUtbetaling, // Skal utbetales for perioden
                                     int virkedager,
                                     @NotNull StønadsstatistikkVedtak.ForeldrepengerRettigheter.Trekkdager trekkdager,
                                     @Valid Gradering gradering, // Perioden er gradert
                                     BigDecimal samtidigUttakProsent) {




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
        //Samtidig 150% regel
        SAMTIDIG_MØDREKVOTE,

        AVSLAG_ANNET,
        AVSLAG_AKTIVITETSKRAV, //14-13
        AVSLAG_SØKNADSFRIST, //22-13
        AVSLAG_IKKE_SØKT, //14-10, 14-14, 14-9-6
        AVSLAG_UTSETTELSE, //14-11
        AVSLAG_UTSETTELSE_TILBAKE_I_TID, //14-11
        AVSLAG_PLEIEPENGER, //14-10 a
        AVSLAG_STØNADSPERIODE_UTLØPT, //14-10-3
        AVSLAG_VILKÅR //14-2, 14-5, 14-6
    }

    enum AktivitetType {
        ARBEIDSTAKER, FRILANS, NÆRING
    }

    record Gradering(AktivitetType aktivitetType, BigDecimal arbeidsprosent) {}

    enum PeriodeType {
        UTTAK, UTSETTELSE, AVSLAG
    }
}
