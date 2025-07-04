package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.FagsystemId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;

class OppdragKjedeFactoryTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Periode p2del1 = Periode.of(p2.getFom(), p2.getFom());
    Periode p2del2 = Periode.of(p2.getFom().plusDays(1), p2.getTom());

    Periode mai = Periode.of(nå.withMonth(5).withDayOfMonth(1), nå.withMonth(5).withDayOfMonth(31));

    FagsystemId fagsystemId = FagsystemId.parse("FOO-1");


    @Test
    void skal_ikke_lage_kjede_når_førstegangsvedtak_er_tomt() {
        var tidligereOppdrag = OppdragKjede.builder().build();
        var nyYtelse = Ytelse.builder().build();
        var factory = OppdragKjedeFactory.lagForNyMottaker(fagsystemId);
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);
        assertThat(resultat).isNull();
    }

    @Test
    void skal_kjede_sammen_perioder_i_førstegangsvedtak() {
        var tidligereOppdrag = OppdragKjede.builder().build();
        var nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();

        var factory = OppdragKjedeFactory.lagForNyMottaker(fagsystemId);
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        var linjer = resultat.getOppdragslinjer();
        assertThat(linjer).hasSize(3);
        assertLik(linjer.get(0), p1, Satsen.dagsats(1000), fagsystemId, null);
        assertLik(linjer.get(1), p2, Satsen.dagsats(1100), fagsystemId, linjer.get(0).getDelytelseId());
        assertLik(linjer.get(2), p3, Satsen.dagsats(1200), fagsystemId, linjer.get(1).getDelytelseId());
    }

    @Test
    void skal_søtte_fullstendig_opphør() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Satsen.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Satsen.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Satsen.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder().build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-3"));
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), p3, Satsen.dagsats(1100), fagsystemId, p1.getFom());
    }

    @Test
    void skal_støtte_opphør_fra_spesifikk_dato() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Satsen.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Satsen.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Satsen.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2del1, Satsen.dagsats(2000)))
            .build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-3"));
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), p3, Satsen.dagsats(1100), fagsystemId,
            p2del1.getTom().plusDays(1));
    }

    @Test
    @Disabled("Feiler på mandager")
    void skal_støtte_fotsettelse_etter_opphør() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Satsen.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Satsen.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2)
                .medSats(Satsen.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medOpphørFomDato(p1.getFom())
                .build())
            .build();
        var nyYtelse = Ytelse.builder().leggTilPeriode(new YtelsePeriode(p2del1, Satsen.dagsats(2000))).build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-2"));
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertLik(resultat.getOppdragslinjer().get(0), p2del1, Satsen.dagsats(2000), fagsystemId,
            DelytelseId.parse("FOO-1-2"));
    }

    @Test
    void skal_støtte_endring_fra_spesifikk_dato_inne_i_periode_fra_tilkjent_ytelse() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p1)
                .medSats(Satsen.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2del1)
                .medSats(Satsen.dagsats(1500))
                .medDelytelseId(DelytelseId.parse("FOO-1-2"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p2del2)
                .medSats(Satsen.dagsats(2000))
                .medDelytelseId(DelytelseId.parse("FOO-1-3"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-2"))
                .build())
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(p3)
                .medSats(Satsen.dagsats(1100))
                .medDelytelseId(DelytelseId.parse("FOO-1-4"))
                .medRefDelytelseId(DelytelseId.parse("FOO-1-3"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1500)))
            .build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-4"));
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        var linjer = resultat.getOppdragslinjer();
        assertThat(linjer).hasSize(2);
        assertOpphørslinje(linjer.get(0), p3, Satsen.dagsats(1100), fagsystemId, p2.getFom().plusDays(1));
        assertLik(linjer.get(1), p2del2, Satsen.dagsats(1500), fagsystemId, linjer.get(0).getDelytelseId());
    }

    @Test
    void skal_opphøre_fra_starten_og_sende_alle_perioder_når_det_legges_til_en_tidligere_periode() {
        var eksisterendeKjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .medPeriode(p2)
                .medSats(Satsen.dagsats(1000))
                .build())
            .build();

        var nyttVedtak = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1000)))
            .build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        var linjer = factory.lagOppdragskjedeForYtelse(eksisterendeKjede, nyttVedtak)
            .getOppdragslinjer();
        assertThat(linjer).hasSize(3);
        var opphørsdato = p2.getFom();
        assertOpphørslinje(linjer.get(0), p2, Satsen.dagsats(1000), fagsystemId, opphørsdato);
        assertLik(linjer.get(1), p1, Satsen.dagsats(1000), fagsystemId, linjer.get(0).getDelytelseId());
        assertLik(linjer.get(2), p2, Satsen.dagsats(1000), fagsystemId, linjer.get(1).getDelytelseId());
    }

    @Test
    void skal_søtte_fullstendig_opphør_av_feriepenger() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(mai)
                .medSats(Satsen.engang(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder().build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        var resultat = factory.lagOppdragskjedeForFeriepenger(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertOpphørslinje(resultat.getOppdragslinjer().get(0), mai, Satsen.engang(1000), fagsystemId, mai.getFom());
    }

    @Test
    void skal_ikke_bruke_opphørslinje_men_bare_overskrive_forrige_periode_ved_endring_i_feriepenger() {
        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(mai)
                .medSats(Satsen.engang(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder().leggTilPeriode(new YtelsePeriode(mai, Satsen.engang(1001))).build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        var resultat = factory.lagOppdragskjedeForFeriepenger(tidligereOppdrag, nyYtelse);

        // ved opphør skal siste linje sendes på nytt, med opphørsdato til første dato det skal opphøres fra
        assertThat(resultat.getOppdragslinjer()).hasSize(1);
        assertLik(resultat.getOppdragslinjer().get(0), mai, Satsen.engang(1001), fagsystemId,
            DelytelseId.parse("FOO-1-1"));
    }

    @Test
    void skal_støtte_å_splitte_periode() {
        var dag1 = LocalDate.now();
        var dag2 = dag1.plusDays(1);
        var dag3 = dag1.plusDays(2);
        var helePeriode = Periode.of(dag1, dag3);

        var tidligereOppdrag = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder()
                .medPeriode(helePeriode)
                .medSats(Satsen.dagsats(1000))
                .medDelytelseId(DelytelseId.parse("FOO-1-1"))
                .build())
            .build();
        var nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(dag1, dag1), Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(dag3, dag3), Satsen.dagsats(1000)))
            .build();

        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("FOO-1-1"));
        var resultat = factory.lagOppdragskjedeForYtelse(tidligereOppdrag, nyYtelse);
        assertThat(resultat.getEndringsdato()).isEqualTo(dag2);
        var linjer = resultat.getOppdragslinjer();
        assertThat(linjer.get(0).erOpphørslinje()).isTrue();
        assertThat(linjer.get(0).getPeriode()).isEqualTo(helePeriode);
        assertThat(linjer.get(0).getOpphørFomDato()).isEqualTo(dag2);
        assertThat(linjer.get(0).getSats()).isEqualTo(Satsen.dagsats(1000));
        assertThat(linjer.get(0).getDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-1"));

        assertThat(linjer.get(1).erOpphørslinje()).isFalse();
        assertThat(linjer.get(1).getPeriode()).isEqualTo(Periode.of(dag3, dag3));
        assertThat(linjer.get(1).getSats()).isEqualTo(Satsen.dagsats(1000));
        assertThat(linjer.get(1).getDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-2"));
        assertThat(linjer.get(1).getRefDelytelseId()).isEqualTo(DelytelseId.parse("FOO-1-1"));
    }

    @Test
    void skal_håndtere_at_første_dato_i_kjeden_ikke_er_i_første_linje() {
        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("02.03.2020-05.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-100")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("06.03.2020-06.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-101")).medRefDelytelseId(DelytelseId.parse("x-1-100")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("06.03.2020-06.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-101")).medOpphørFomDato(LocalDate.of(2020, 3, 2)).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("01.03.2020-01.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-103")).medRefDelytelseId(DelytelseId.parse("x-1-101")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("02.03.2020-02.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-104")).medRefDelytelseId(DelytelseId.parse("x-1-103")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p("05.03.2020-06.03.2020")).medSats(Satsen.dagsats(1)).medDelytelseId(DelytelseId.parse("x-1-105")).medRefDelytelseId(DelytelseId.parse("x-1-104")).build())
            .build();

        var nyYtelse = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p("01.03.2020-01.03.2020"), Satsen.dagsats(2)))
            .leggTilPeriode(new YtelsePeriode(p("02.03.2020-05.03.2020"), Satsen.dagsats(2)))
            .leggTilPeriode(new YtelsePeriode(p("06.03.2020-06.03.2020"), Satsen.dagsats(2)))
            .build();


        var factory = OppdragKjedeFactory.lagForEksisterendeMottaker(DelytelseId.parse("x-1-105"));
        var resultat = factory.lagOppdragskjedeFraFellesEndringsdato(kjede, nyYtelse, false, LocalDate.of(2020, 3, 1));

        var endelig = kjede.leggTil(resultat);
        assertThat(endelig.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p("01.03.2020-01.03.2020"), Satsen.dagsats(2)),
            new YtelsePeriode(p("02.03.2020-05.03.2020"), Satsen.dagsats(2)),
            new YtelsePeriode(p("06.03.2020-06.03.2020"), Satsen.dagsats(2))
        );

        //TODO lag test som sjekker oppdragslinjene direkte
    }

    private static Periode p(String tekst) {
        var deler = tekst.split("-");
        var pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return Periode.of(LocalDate.parse(deler[0], pattern), LocalDate.parse(deler[1], pattern));
    }


    public void assertLik(OppdragLinje linje,
                          Periode p,
                          Satsen sats,
                          FagsystemId fagsystemId,
                          DelytelseId refDelytelseId) {
        assertThat(linje.getPeriode()).isEqualTo(p);
        assertThat(linje.getSats()).isEqualTo(sats);
        assertThat(linje.getUtbetalingsgrad()).isNull();
        assertThat(linje.getDelytelseId().getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(linje.getRefDelytelseId()).isEqualTo(refDelytelseId);
        assertThat(linje.getOpphørFomDato()).isNull();
    }

    public void assertOpphørslinje(OppdragLinje linje,
                                   Periode p,
                                   Satsen sats,
                                   FagsystemId fagsystemId,
                                   LocalDate opphørsdato) {
        assertThat(linje.getPeriode()).isEqualTo(p);
        assertThat(linje.getSats()).isEqualTo(sats);
        assertThat(linje.getUtbetalingsgrad()).isNull();
        assertThat(linje.getDelytelseId().getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(linje.getRefDelytelseId()).isNull();
        assertThat(linje.getOpphørFomDato()).isEqualTo(opphørsdato);
    }

}
