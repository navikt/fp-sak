package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import static no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik.FERIEPENGER_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik.FPF_ARBEIDSTAKER;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.FagsystemId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;

public class EksisterendeOppdragMapperTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Saksnummer saksnummer = new Saksnummer("1");

    String frnBruker = "12345678901";

    DelytelseId delytelseId1 = DelytelseId.parse("1100100");
    DelytelseId delytelseId2 = delytelseId1.neste();
    DelytelseId delytelseId3 = delytelseId2.neste();
    DelytelseId delytelseId4 = delytelseId3.neste();
    DelytelseId delytelseId5 = delytelseId4.neste();

    @Test
    public void skal_mappe_eksisterende_oppdrag() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll();
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(150), delytelseId1);

        Map<KjedeNøkkel, OppdragKjede> kjeder = EksisterendeOppdragMapper.tilKjeder(Arrays.asList(oppdragskontroll));
        KjedeNøkkel kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(kjeder.keySet()).containsOnly(kjedeNøkkel);
        OppdragKjede kjede = kjeder.get(kjedeNøkkel);
        Assertions.assertThat(kjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(150)).medRefDelytelseId(delytelseId1)
                .build());
    }

    @Test
    public void skal_mappe_brukket_kjede_til_to_kjeder() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll();
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(150), null); // denne peker ikke til forrige, slik den egentlig skal

        Map<KjedeNøkkel, OppdragKjede> kjeder = EksisterendeOppdragMapper.tilKjeder(Arrays.asList(oppdragskontroll));
        KjedeNøkkel kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        KjedeNøkkel kjedeNøkkelKnektKjede = KjedeNøkkel.builder(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER).medKnektKjedeDel(1).build();
        Assertions.assertThat(kjeder.keySet()).containsOnly(kjedeNøkkel, kjedeNøkkelKnektKjede);
        Assertions.assertThat(kjeder.get(kjedeNøkkel).getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build());
        Assertions.assertThat(kjeder.get(kjedeNøkkelKnektKjede).getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(150)).build());
    }

    @Test
    public void skal_mappe_kjede_med_opphør_og_fortsettelse_etter_opphør() {
        LocalDate opphørsdato = p1.getFom().plusDays(2);

        Oppdragskontroll oppdragskontroll = lagOppdragskontroll();
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        Oppdragslinje150 linje1 = lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        Oppdragslinje150 linje2 = lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(101), DelytelseId.parse(Long.toString(linje1.getDelytelseId())));
        lagOpphørslinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(101), opphørsdato);
        lagOrdinærLinje(oppdrag110, delytelseId3, p3, Satsen.dagsats(100), DelytelseId.parse(Long.toString(linje2.getDelytelseId())));

        Map<KjedeNøkkel, OppdragKjede> kjeder = EksisterendeOppdragMapper.tilKjeder(Arrays.asList(oppdragskontroll));
        KjedeNøkkel kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(kjeder.keySet()).containsOnly(kjedeNøkkel);
        OppdragKjede kjede = kjeder.get(kjedeNøkkel);
        Assertions.assertThat(kjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(101)).medRefDelytelseId(linje1.getDelytelseId()).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(101)).medOpphørFomDato(opphørsdato).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId3).medPeriode(p3).medSats(Satsen.dagsats(100)).medRefDelytelseId(linje2.getDelytelseId()).build()
        );
    }

    @Test
    void skal_mappe_feriepenger_som_er_opphørt_og_så_gjeninnført() {
        Periode mai = Periode.of(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

        Oppdragskontroll oppdragskontroll = lagOppdragskontroll();
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOppdragslinje150(oppdrag110, delytelseId1, mai, Satsen.engang(1000), null, null, FERIEPENGER_BRUKER);
        lagOppdragslinje150(oppdrag110, delytelseId1, mai, Satsen.engang(1000), null, mai.getFom(), FERIEPENGER_BRUKER);
        lagOppdragslinje150(oppdrag110, delytelseId2, mai, Satsen.engang(1000), null, null, FERIEPENGER_BRUKER);

        Map<KjedeNøkkel, OppdragKjede> kjeder = EksisterendeOppdragMapper.tilKjeder(Arrays.asList(oppdragskontroll));
        KjedeNøkkel kjedeNøkkel = KjedeNøkkel.lag(FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2019);
        KjedeNøkkel kjedeNøkkel2 = kjedeNøkkel.forNesteKnekteKjededel();
        Assertions.assertThat(kjeder.keySet()).containsOnly(kjedeNøkkel, kjedeNøkkel2);
        OppdragKjede aktivKjede = kjeder.get(kjedeNøkkel);
        Assertions.assertThat(aktivKjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(mai).medSats(Satsen.engang(1000)).build()
        );
        OppdragKjede opphørtKjede = kjeder.get(kjedeNøkkel2);
        Assertions.assertThat(opphørtKjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(mai).medSats(Satsen.engang(1000)).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(mai).medSats(Satsen.engang(1000)).medOpphørFomDato(mai.getFom()).build()
        );
    }

    private Oppdragskontroll lagOppdragskontroll() {
        return Oppdragskontroll.builder()
            .medBehandlingId(1L)
            .medProsessTaskId(1000L)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(true)
            .build();
    }

    private Oppdragslinje150 lagOpphørslinje(Oppdrag110 oppdrag110, DelytelseId delytelseId, Periode p, Satsen sats, LocalDate opphørFomDato) {
        return lagOppdragslinje150(oppdrag110, delytelseId, p, sats, null, opphørFomDato, FPF_ARBEIDSTAKER);
    }

    private Oppdragslinje150 lagOrdinærLinje(Oppdrag110 oppdrag110, DelytelseId delytelseId, Periode p, Satsen sats, DelytelseId refDelytelseId) {
        return lagOppdragslinje150(oppdrag110, delytelseId, p, sats, refDelytelseId, null, FPF_ARBEIDSTAKER);
    }

    private Oppdragslinje150 lagOppdragslinje150(Oppdrag110 oppdrag110, DelytelseId delytelseId, Periode p, Satsen sats, DelytelseId refDelytelseId,
                                                 LocalDate opphørFomDato, KodeKlassifik kodeKlassifik) {
        return Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medDelytelseId(Long.parseLong(delytelseId.toString()))
            .medKodeKlassifik(kodeKlassifik)
            .medVedtakFomOgTom(p.getFom(), p.getTom())
            .medSats(no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats.på(sats.getSats()))
            .medTypeSats(TypeSats.fraKode(sats.getSatsType().getKode()))
            .medDatoStatusFom(opphørFomDato)
            .medKodeStatusLinje(opphørFomDato != null ? KodeStatusLinje.OPPHØR : null)
            .medKodeEndringLinje(opphørFomDato != null ? KodeEndringLinje.ENDRING : KodeEndringLinje.NY)
            .medRefDelytelseId(refDelytelseId != null ? Long.parseLong(refDelytelseId.toString()) : null)
            .medRefFagsystemId(refDelytelseId != null ? Long.parseLong(refDelytelseId.getFagsystemId().toString()) : null)
            .build();
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, FagsystemId fagsystemId) {
        return Oppdrag110.builder()
            .medKodeEndring("NY")
            .medKodeFagomrade("FP")
            .medUtbetFrekvens("MND")
            .medOppdragGjelderId(frnBruker)
            .medDatoOppdragGjelderFom(LocalDate.MIN)
            .medSaksbehId("Z100000")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .medFagSystemId(Long.parseLong(fagsystemId.toString()))
            .build();
    }
}
