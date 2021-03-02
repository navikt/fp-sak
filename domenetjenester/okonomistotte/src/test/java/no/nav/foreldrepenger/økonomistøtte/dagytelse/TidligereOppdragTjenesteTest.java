package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.ForrigeOppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;

class TidligereOppdragTjenesteTest {

    Saksnummer saksnummer = new Saksnummer("999999999");

    Long HENVISNING_1 = 1111111L;
    Long HENVISNING_2 = 2222222L;

    @Test
    public void skal_kunne_håndtere_ferepenger_på_samme_kodeklasifikk_gjennom_flere_år() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 999999999L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_ARBEIDSTAKER, LocalDate.of(2019, 10, 17), LocalDate.of(2019, 11, 6), fagsystemId*1000 + 100, HENVISNING_1, null, null);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_ARBEIDSTAKER, LocalDate.of(2019, 11, 7), LocalDate.of(2019, 12, 18), fagsystemId*1000 + 101, HENVISNING_1, fagsystemId*1000 + 100, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_ARBEIDSTAKER, LocalDate.of(2019, 12, 19), LocalDate.of(2020, 2, 19), fagsystemId*1000 + 102, HENVISNING_1, fagsystemId*1000 + 101, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_ARBEIDSTAKER, LocalDate.of(2020, 2, 20), LocalDate.of(2020, 6, 10), fagsystemId*1000 + 103, HENVISNING_1, fagsystemId*1000 + 102, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FERIEPENGER_BRUKER, LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31), fagsystemId*1000 + 104, HENVISNING_1, null, null);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FERIEPENGER_BRUKER, LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31), fagsystemId*1000 + 105, HENVISNING_1, null, null);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.ENDR, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(LocalDateTime.now());
        lagOpphørslinje(oppdrag2, KodeKlassifik.FPF_ARBEIDSTAKER, LocalDate.of(2020, 2, 20), LocalDate.of(2020, 6, 10), fagsystemId*1000 + 103, LocalDate.of(2019, 10, 17), HENVISNING_2, null, null);
        lagOpphørslinje(oppdrag2, KodeKlassifik.FERIEPENGER_BRUKER, LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31), fagsystemId*1000 + 104, LocalDate.of(2020, 5, 1), HENVISNING_2, null, null);
        lagOpphørslinje(oppdrag2, KodeKlassifik.FERIEPENGER_BRUKER, LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31), fagsystemId*1000 + 105, LocalDate.of(2021, 5, 1), HENVISNING_2, null, null);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_DAGPENGER, LocalDate.of(2019, 10, 17), LocalDate.of(2019, 10, 25), fagsystemId*1000 + 106, HENVISNING_2, null, null);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_DAGPENGER, LocalDate.of(2019, 10, 28), LocalDate.of(2019, 12, 6), fagsystemId*1000 + 107, HENVISNING_2, fagsystemId*1000 + 106, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_DAGPENGER, LocalDate.of(2019, 12, 7), LocalDate.of(2020, 2, 7), fagsystemId*1000 + 108, HENVISNING_2, fagsystemId*1000 + 107, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_DAGPENGER, LocalDate.of(2020, 2, 10), LocalDate.of(2020, 5, 31), fagsystemId*1000 + 109, HENVISNING_2, fagsystemId*1000 + 108, fagsystemId);

        ForrigeOppdragInput forrigeOppdragInput = new ForrigeOppdragInput(Arrays.asList(oppdrag1, oppdrag2), null);

        OppdragInput oppdragInput = OppdragInput.builder()
            .medForrigeOppdragInput(forrigeOppdragInput)
            .medPersonIdent(PersonIdent.fra("12121299999"))
            .medFagsakYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medForenkletTilkjentYtelse(TilkjentYtelse.builder().medEndringsdato(LocalDate.of(2019, 10, 17)).build())
            .build();

        var oppdragslinje150s = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(oppdragInput, false);

        Assertions.assertThat(oppdragslinje150s).isNotEmpty();
        Assertions.assertThat(oppdragslinje150s.size()).isEqualTo(5);
    }

    private Oppdragslinje150 lagOpphørslinje(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                             LocalDate opphørsdatao, Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
            .medKodeEndringLinje(KodeEndringLinje.ENDRING)
            .medKodeStatusLinje(KodeStatusLinje.OPPHØR)
            .medDatoStatusFom(opphørsdatao)
            .build();
    }

    private Oppdragslinje150 lagOppdragslinje(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                              Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
            .build();
    }

    private Oppdragslinje150.Builder lagOppdragslinjeBuilder(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom,
                                                             Long delytelseId, Long henvisning, Long refDelyteseId, Long refFagsystemId) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medKodeKlassifik(klassekode)
            .medVedtakFomOgTom(fom, tom)
            .medSats(Sats.på(1L))
            .medTypeSats(TypeSats.DAGLIG)
            .medOppdrag110(oppdrag110)
            .medDelytelseId(delytelseId)
            .medUtbetalesTilId("11111111111")
            .medRefDelytelseId(refDelyteseId)
            .medRefFagsystemId(refFagsystemId);
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, ØkonomiKodeFagområde fagområde, ØkonomiKodeEndring status, long fagsystemId) {
        return Oppdrag110.builder()
            .medKodeFagomrade(fagområde.name())
            .medKodeEndring(status.name())
            .medFagSystemId(fagsystemId)
            .medOppdragGjelderId("11111111111")
            .medSaksbehId("Z111111")
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming(Avstemming.ny())
            .build();
    }

    private Oppdragskontroll lagOppdragskontroll(Saksnummer saksnummer) {
        return Oppdragskontroll.builder()
            .medBehandlingId(1L)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(false)
            .medProsessTaskId(-1L)
            .build();
    }

}
