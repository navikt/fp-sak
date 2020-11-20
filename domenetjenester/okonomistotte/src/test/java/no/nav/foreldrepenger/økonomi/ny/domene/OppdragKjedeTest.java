package no.nav.foreldrepenger.økonomi.ny.domene;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class OppdragKjedeTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    @Test
    public void skal_konvertere_enkel_kjede_til_ytelse() {
        OppdragKjede kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).medRefDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medRefDelytelseId(DelytelseId.parse("FOO-1-2")).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Sats.dagsats(1000)),
            new YtelsePeriode(p2, Sats.dagsats(2000)),
            new YtelsePeriode(p3, Sats.dagsats(1100))
        );
    }

    @Test
    public void skal_konvertere_kjede_med_opphør_til_ytelse() {

        OppdragKjede kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).medRefDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medRefDelytelseId(DelytelseId.parse("FOO-1-2")).build())

            //opphør fra start av periode 2
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medOpphørFomDato(p2.getFom()).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Sats.dagsats(1000))
        );
    }

    @Test
    public void skal_konvertere_kjede_med_opphør_inne_i_linje_til_ytelse() {

        OppdragKjede kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).medRefDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medRefDelytelseId(DelytelseId.parse("FOO-1-2")).build())

            //opphør fra inne i periode 2
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p3).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medOpphørFomDato(p2.getFom().plusDays(3)).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Sats.dagsats(1000)),
            new YtelsePeriode(Periode.of(p2.getFom(), p2.getFom().plusDays(2)), Sats.dagsats(2000))
        );
    }

    @Test
    public void identisk_tidsperiode_skal_overskrive_tidligere_verdi_i_samme_tidsperiode() {
        //metoden brukes typisk bare for feriepenger

        OppdragKjede kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).medRefDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1100)).medDelytelseId(DelytelseId.parse("FOO-1-3")).medRefDelytelseId(DelytelseId.parse("FOO-1-2")).build())
            .build();

        assertThat(kjede.tilYtelse().getPerioder()).containsExactly(
            new YtelsePeriode(p1, Sats.dagsats(1100))
        );
    }

    @Test
    public void skal_kreve_at_oppdragslinjer_i_kjeden_peker_på_hverandre() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).build())
            .build());

        assertThat(exception.getMessage()).isEqualTo("Oppdragslinje med delytelseId FOO-1-2 er ikke først i kjeden, og må referere til forrige oppdragslinje (delytelseId FOO-1-1)");
    }

    @Test
    public void skal_ikke_kreve_at_oppdragslinje_peker_på_forrige_når_alt_tidligere_er_opphørt() {
        OppdragKjede kjede = OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dagsats(1000)).medDelytelseId(DelytelseId.parse("FOO-1-1")).medOpphørFomDato(p1.getFom()).build())
            .medOppdragslinje(OppdragLinje.builder().medPeriode(p2).medSats(Sats.dagsats(2000)).medDelytelseId(DelytelseId.parse("FOO-1-2")).build())
            .build();

        List<YtelsePeriode> perioder = kjede.tilYtelse().getPerioder();
        Assertions.assertThat(perioder).hasSize(1);
        Assertions.assertThat(perioder.get(0).getPeriode()).isEqualTo(p2);
        Assertions.assertThat(perioder.get(0).getSats()).isEqualTo(Sats.dagsats(2000));
    }
}
