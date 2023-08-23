package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OppdragKjedeTest {

    private final LocalDate nå = LocalDate.now();
    private final Periode p1 = Periode.of(nå, nå.plusDays(5));
    private final Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    private final Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    @Test
    void skal_konvertere_enkel_kjede_til_ytelse() {
        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).medRefDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medRefDelytelseId(DelytelseId.parse("FOO001002")).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Satsen.dagsats(1000)),
            new YtelsePeriode(p2, Satsen.dagsats(2000)),
            new YtelsePeriode(p3, Satsen.dagsats(1100))
        );
    }

    @Test
    void skal_konvertere_kjede_med_opphør_til_ytelse() {

        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).medRefDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medRefDelytelseId(DelytelseId.parse("FOO001002")).build())

            //opphør fra start av periode 2
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medOpphørFomDato(p2.getFom()).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Satsen.dagsats(1000))
        );
    }

    @Test
    void skal_konvertere_kjede_med_opphør_inne_i_linje_til_ytelse() {
        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).medRefDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medRefDelytelseId(DelytelseId.parse("FOO001002")).build())

            //opphør fra inne i periode 2
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medOpphørFomDato(p2.getFom().plusDays(3)).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Satsen.dagsats(1000)),
            new YtelsePeriode(Periode.of(p2.getFom(), p2.getFom().plusDays(2)), Satsen.dagsats(2000))
        );
    }

    @Test
    void identisk_tidsperiode_skal_overskrive_tidligere_verdi_i_samme_tidsperiode() {
        //metoden brukes typisk bare for feriepenger

        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).medRefDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medRefDelytelseId(DelytelseId.parse("FOO001002")).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Satsen.dagsats(1100))
        );
    }

    @Test
    void skal_ha_at_oppdragslinjer_implisitt_opphører_det_som_har_senere_periode() {
        //metoden brukes typisk bare for feriepenger

        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).medRefDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO001003")).medRefDelytelseId(DelytelseId.parse("FOO001002")).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Satsen.dagsats(1100))
        );
    }

    @Test
    void skal_kreve_at_oppdragslinjer_i_kjeden_peker_på_hverandre() {
        var linje1 = OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build();
        var linje2 = OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).build();
        var kjede = OppdragKjede.builder().medOppdragslinje(linje1);
        var exception = assertThrows(IllegalArgumentException.class, () -> kjede.medOppdragslinje(linje2));

        assertThat(exception.getMessage()).isEqualTo("Oppdragslinje med delytelseId FOO001002 er ikke først i kjeden, og må referere til forrige oppdragslinje (delytelseId FOO001001)");
    }

    @Test
    void skal_ikke_kreve_at_oppdragslinje_peker_på_forrige_når_alt_tidligere_er_opphørt() {
        var kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO001001")).medOpphørFomDato(p1.getFom()).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Satsen.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO001002")).build())
            .build();

        var perioder = kjede.tilYtelse().getPerioder();
        Assertions.assertThat(perioder).hasSize(1);
        Assertions.assertThat(perioder.get(0).getPeriode()).isEqualTo(p2);
        Assertions.assertThat(perioder.get(0).getSats()).isEqualTo(Satsen.dagsats(2000));
    }
}
