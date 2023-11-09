package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import static no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik.FERIEPENGER_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik.FPF_ARBEIDSTAKER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.FagsystemId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.SatsType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;

class EksisterendeOppdragMapperTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Saksnummer saksnummer = new Saksnummer("12345");

    String frnBruker = "12345678901";

    DelytelseId delytelseId1 = DelytelseId.parse("1100100");
    DelytelseId delytelseId2 = delytelseId1.neste();
    DelytelseId delytelseId3 = delytelseId2.neste();

    @Test
    void skal_mappe_eksisterende_oppdrag() {
        var oppdragskontroll = lagOppdragskontroll();
        var oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(150), delytelseId1);

        var kjeder = EksisterendeOppdragMapper.tilKjeder(Arrays.asList(oppdragskontroll));
        var kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        assertThat(kjeder).containsOnlyKeys(kjedeNøkkel);
        var kjede = kjeder.get(kjedeNøkkel);
        assertThat(kjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(150)).medRefDelytelseId(delytelseId1)
                .build());
    }

    @Test
    void skal_mappe_brukket_kjede_til_to_kjeder() {
        var oppdragskontroll = lagOppdragskontroll();
        var oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(150), null); // denne peker ikke til forrige, slik den egentlig skal

        var kjeder = EksisterendeOppdragMapper.tilKjeder(List.of(oppdragskontroll));
        var kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var kjedeNøkkelKnektKjede = KjedeNøkkel.builder(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER).medKnektKjedeDel(1).build();
        assertThat(kjeder).containsOnlyKeys(kjedeNøkkel, kjedeNøkkelKnektKjede);
        assertThat(kjeder.get(kjedeNøkkel).getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build());
        assertThat(kjeder.get(kjedeNøkkelKnektKjede).getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(150)).build());
    }

    @Test
    void skal_mappe_kjede_med_opphør_og_fortsettelse_etter_opphør() {
        var opphørsdato = p1.getFom().plusDays(2);

        var oppdragskontroll = lagOppdragskontroll();
        var oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        var linje1 = lagOrdinærLinje(oppdrag110, delytelseId1, p1, Satsen.dagsats(100), null);
        var linje2 = lagOrdinærLinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(101), DelytelseId.parse(Long.toString(linje1.getDelytelseId())));
        lagOpphørslinje(oppdrag110, delytelseId2, p2, Satsen.dagsats(101), opphørsdato);
        lagOrdinærLinje(oppdrag110, delytelseId3, p3, Satsen.dagsats(100), DelytelseId.parse(Long.toString(linje2.getDelytelseId())));

        var kjeder = EksisterendeOppdragMapper.tilKjeder(List.of(oppdragskontroll));
        var kjedeNøkkel = KjedeNøkkel.lag(FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        assertThat(kjeder).containsOnlyKeys(kjedeNøkkel);
        var kjede = kjeder.get(kjedeNøkkel);
        assertThat(kjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId1).medPeriode(p1).medSats(Satsen.dagsats(100)).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(101)).medRefDelytelseId(linje1.getDelytelseId()).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(p2).medSats(Satsen.dagsats(101)).medOpphørFomDato(opphørsdato).build(),
            OppdragLinje.builder().medDelytelseId(delytelseId3).medPeriode(p3).medSats(Satsen.dagsats(100)).medRefDelytelseId(linje2.getDelytelseId()).build()
        );
    }

    @Test
    void skal_mappe_feriepenger_som_er_opphørt_og_så_gjeninnført() {
        var mai = Periode.of(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

        var oppdragskontroll = lagOppdragskontroll();
        var oppdrag110 = lagOppdrag110(oppdragskontroll, FagsystemId.parse(saksnummer.getVerdi() + "100"));
        lagOppdragslinje150(oppdrag110, delytelseId1, mai, Satsen.engang(1000), null, null, FERIEPENGER_BRUKER);
        lagOppdragslinje150(oppdrag110, delytelseId1, mai, Satsen.engang(1000), null, mai.getFom(), FERIEPENGER_BRUKER);
        lagOppdragslinje150(oppdrag110, delytelseId2, mai, Satsen.engang(1000), null, null, FERIEPENGER_BRUKER);

        var kjeder = EksisterendeOppdragMapper.tilKjeder(List.of(oppdragskontroll));
        var kjedeNøkkel = KjedeNøkkel.lag(FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2019);
        var kjedeNøkkel2 = kjedeNøkkel.forNesteKnekteKjededel();
        assertThat(kjeder).containsOnlyKeys(kjedeNøkkel, kjedeNøkkel2);
        var aktivKjede = kjeder.get(kjedeNøkkel);
        assertThat(aktivKjede.getOppdragslinjer()).containsExactly(
            OppdragLinje.builder().medDelytelseId(delytelseId2).medPeriode(mai).medSats(Satsen.engang(1000)).build()
        );
        var opphørtKjede = kjeder.get(kjedeNøkkel2);
        assertThat(opphørtKjede.getOppdragslinjer()).containsExactly(
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
            .medSats(Sats.på(sats.getSats()))
            .medTypeSats(mapFraSatsType(sats.getSatsType()))
            .medDatoStatusFom(opphørFomDato)
            .medKodeStatusLinje(opphørFomDato != null ? KodeStatusLinje.OPPH : null)
            .medKodeEndringLinje(opphørFomDato != null ? KodeEndringLinje.ENDR : KodeEndringLinje.NY)
            .medRefDelytelseId(refDelytelseId != null ? Long.parseLong(refDelytelseId.toString()) : null)
            .medRefFagsystemId(refDelytelseId != null ? Long.parseLong(refDelytelseId.getFagsystemId().toString()) : null)
            .build();
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, FagsystemId fagsystemId) {
        return Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(KodeFagområde.FP)
            .medOppdragGjelderId(frnBruker)
            .medSaksbehId("Z100000")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .medFagSystemId(Long.parseLong(fagsystemId.toString()))
            .build();
    }

    private TypeSats mapFraSatsType(SatsType satsType) {
        return switch (satsType) {
            case DAG -> TypeSats.DAG;
            case ENG -> TypeSats.ENG;
        };
    }
}
