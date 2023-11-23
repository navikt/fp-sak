package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

class StønadsstatistikkVedtakPeriode {

    private LocalDate fom;
    private LocalDate tom;
    private int virkedager;
    private PeriodeAktivitet aktivitet;

    //nøkkel org identifikator, aktivitetsstatus, inntektskategori

    private BigDecimal arbeidsprosent; //Ut i fra 100%??

    // private List<TilkjentAndel> utbetalingsgrader // mottakere??

    private StønadsstatistikkVedtak.StønadskontoType stønadskontoType; // hvilken konta man tar ut fra

    private boolean gradering; //Perioden er gradert
    private boolean gradertAktivitet; //Gradert aktivitet i perioden
    private boolean samtidigUttak;

    private StønadsstatistikkVedtak.RettighetType rettighetType;


    private BigDecimal trekkdager;
    private BigDecimal utbetalingsgrad;
    private Forklaring forklaring;

    // Oppdragsinfo / linje - hva slags tekniske attributter må dere få fra oss - se på oppdrag-dvh-fp-v2.xsd
    // matching med tilkjent ytelse - hvordan gjørese dette se på ytelse-fp-v2.xsd

    // uttak-svp vs uttak-fp kan vi sende det likt

    enum Forklaring {
        UTSETTELSE_FERIE,
        UTSETTELSE_ARBEID,
        UTSETTELSE_INNLEGGELSE,
        UTSETTELSE_BARNINNLAGT,
        UTSETTELSE_SYKDOM,
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

    record PeriodeAktivitet(Type aktivitetType, String identifikator) {
        enum Type {
            ARBEIDSTAKER, FRILANS, SELVSTENDIG_NÆRINGSDRIVENDE
        }
    }
}
