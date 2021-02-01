package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;

public class OpphørUtilTest {

    Saksnummer saksnummer = new Saksnummer("100000000");

    ØkonomiKodeKlassifik FPATORD = ØkonomiKodeKlassifik.fraKode("FPATORD");
    ØkonomiKodeKlassifik FPATAL = ØkonomiKodeKlassifik.fraKode("FPATAL");

    LocalDate fom = LocalDate.of(2020, 1, 1);
    LocalDate tom = LocalDate.of(2020, 2, 1);

    Long HENVISNING_1 = 1L;
    Long HENVISNING_2 = 2L;

    @Test
    public void skal_kunne_håndtere_flere_feriepenge_koder_gjennom_flere_år() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        var maien = LocalDate.of(2020,5,1);
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        lagOppdragslinje(oppdrag1, ØkonomiKodeKlassifik.FPATFER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, ØkonomiKodeKlassifik.FPATFER, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, ØkonomiKodeKlassifik.FPREFAGFER_IOP, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 103, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, ØkonomiKodeKlassifik.FPREFAGFER_IOP, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 104, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, ØkonomiKodeKlassifik.FPSVREFAGFER_IOP, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 105, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.ENDR, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(LocalDateTime.now());
        lagOppdragslinje(oppdrag2, ØkonomiKodeKlassifik.FPATFER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 106, HENVISNING_1, fagsystemId * 1000 + 101, fagsystemId);
        lagOppdragslinje(oppdrag2, ØkonomiKodeKlassifik.FPATFER, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 107, HENVISNING_1, fagsystemId * 1000 + 103, fagsystemId);

        Oppdrag110 oppdrag3 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.ENDR, fagsystemId);
        oppdrag3.setOpprettetTidspunkt(LocalDateTime.now().plusDays(1));
        lagOpphørslinje(oppdrag3, ØkonomiKodeKlassifik.FPATFER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 106, maien, HENVISNING_2, fagsystemId * 1000 + 101, fagsystemId);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2, oppdrag3));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsAll(Arrays.asList(ØkonomiKodeKlassifik.FPREFAGFER_IOP, ØkonomiKodeKlassifik.FPSVREFAGFER_IOP, ØkonomiKodeKlassifik.FPATFER));
    }

    @Test
    public void skal_si_at_iverksatte_linjer_er_opphørbare() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag110.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag110, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag110, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag110));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD, FPATAL);
    }

    @Test
    public void skal_si_at_allrede_opphørt_klassekode_ikke_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATAL, fom, tom, fagsystemId * 1000 + 102, fom, HENVISNING_2, null, fagsystemId);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    @Test
    public void skal_si_at_klassekode_som_ikke_er_fullstendig_opphørt_fortsatt_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());
        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATORD, fom, tom, fagsystemId * 1000 + 101, fom.plusDays(1), HENVISNING_2, null, fagsystemId);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    private Oppdragslinje150 lagOpphørslinje(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                             LocalDate opphørsdatao, Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
                .medKodeEndringLinje(ØkonomiKodeEndringLinje.ENDR.name())
                .medKodeStatusLinje(ØkonomiKodeStatusLinje.OPPH.name())
                .medDatoStatusFom(opphørsdatao)
                .build();
    }

    private Oppdragslinje150 lagOppdragslinje(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                              Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
                .build();
    }

    private Oppdragslinje150.Builder lagOppdragslinjeBuilder(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom,
                                                             Long delytelseId, Long henvisning, Long refDelyteseId, Long refFagsystemId) {
        return Oppdragslinje150.builder()
                .medKodeEndringLinje(ØkonomiKodeEndringLinje.NY.name())
                .medFradragTillegg(TfradragTillegg.T.value())
                .medKodeKlassifik(klassekode.getKodeKlassifik())
                .medVedtakFomOgTom(fom, tom)
                .medSats(1L)
                .medTypeSats(ØkonomiTypeSats.DAG.name())
                .medBrukKjoreplan("N")
                .medSaksbehId("Z11111")
                .medHenvisning(henvisning)
                .medOppdrag110(oppdrag110)
                .medDelytelseId(delytelseId)
                .medRefDelytelseId(refDelyteseId)
                .medRefFagsystemId(refFagsystemId);
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, ØkonomiKodeFagområde fagområde, ØkonomiKodeEndring status, long fagsystemId) {
        return Oppdrag110.builder()
                .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
                .medKodeFagomrade(fagområde.name())
                .medKodeEndring(status.name())
                .medFagSystemId(fagsystemId)
                .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
                .medOppdragGjelderId("11111111111")
                .medDatoOppdragGjelderFom(LocalDate.of(2020, 1, 1))
                .medSaksbehId("Z111111")
                .medNøkkelAvstemming("foo")
                .medOppdragskontroll(oppdragskontroll)
                .medAvstemming115(Avstemming115.builder()
                        .medNokkelAvstemming("foo")
                        .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
                        .medTidspnktMelding("nå")
                        .build())
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
