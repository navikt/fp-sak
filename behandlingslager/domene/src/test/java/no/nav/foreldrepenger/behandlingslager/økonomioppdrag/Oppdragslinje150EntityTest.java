package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class Oppdragslinje150EntityTest {
    private Oppdragslinje150.Builder oppdragslinje150Builder;
    private Oppdragslinje150 oppdragslinje150;
    private Oppdragslinje150 oppdragslinje150_2;

    private static final KodeEndringLinje KODEENDRINGLINJE = KodeEndringLinje.NY;
    private static final LocalDate DATOSTATUSFOM = LocalDate.now().minusDays(15);
    private static final String VEDTAKID = "457";
    private static final Long DELYTELSEID = 300L;
    private static final KodeKlassifik KODEKLASSIFIK = KodeKlassifik.FPF_ARBEIDSTAKER;
    private static final LocalDate DATOVEDTAKFOM = LocalDate.now().minusDays(10);
    private static final LocalDate DATOVEDTAKTOM = LocalDate.now().minusDays(8);
    private static final Sats SATS = Sats.på(50000);
    private static final TypeSats TYPESATS = TypeSats.ENG;
    private static final String SAKSBEHID = "Z1236524";
    private static final String UTBETALESTILID = "456";
    private static final Long REFFAGSYSTEMID = 678L;
    private static final Long REFDELYTELSEID = 789L;
    private static final KodeFagområde KODEFAGOMRADE = KodeFagområde.REFUTG;
    private static final Long FAGSYSTEMID = 250L;
    private static final String OPPDRAGGJELDERID = "1";
    private static final Saksnummer SAKSID = new Saksnummer("700");
    private static final Boolean VENTERKVITTERING = true;
    private static final Long TASKID = 52L;
    private static final Long BEHANDLINGID = 321L;

    @BeforeEach
    void setup() {
        oppdragslinje150Builder = Oppdragslinje150.builder();
        oppdragslinje150 = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        oppdragslinje150 = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(KODEENDRINGLINJE);
        assertThat(oppdragslinje150.getDatoStatusFom()).isEqualTo(DATOSTATUSFOM);
        assertThat(oppdragslinje150.getVedtakId()).isEqualTo(VEDTAKID);
        assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(DELYTELSEID);
        assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KODEKLASSIFIK);
        assertThat(oppdragslinje150.getDatoVedtakFom()).isEqualTo(DATOVEDTAKFOM);
        assertThat(oppdragslinje150.getDatoVedtakTom()).isEqualTo(DATOVEDTAKTOM);
        assertThat(oppdragslinje150.getSats()).isEqualTo(SATS);
        assertThat(oppdragslinje150.getTypeSats()).isEqualTo(TYPESATS);
        assertThat(oppdragslinje150.getUtbetalesTilId()).isEqualTo(UTBETALESTILID);
        assertThat(oppdragslinje150.getRefFagsystemId()).isEqualTo(REFFAGSYSTEMID);
        assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(REFDELYTELSEID);

    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {
        // mangler kodeEndringLinje
        try {
            oppdragslinje150Builder.build();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("kodeEndringLinje");
        }

        // mangler kodeKlassifik
        oppdragslinje150Builder.medKodeEndringLinje(KODEENDRINGLINJE);
        try {
            oppdragslinje150Builder.build();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("kodeKlassifik");
        }

        // mangler datoVedtakFom
        oppdragslinje150Builder.medKodeKlassifik(KODEKLASSIFIK);
        try {
            oppdragslinje150Builder.build();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("vedtakPeriode");
        }

        // mangler sats
        oppdragslinje150Builder.medVedtakFomOgTom(DATOVEDTAKFOM, DATOVEDTAKTOM);
        try {
            oppdragslinje150Builder.build();
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("sats");
        }

    }

    @Test
    void skal_håndtere_null_this_feilKlasse_i_equals() {
        oppdragslinje150 = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdragslinje150)
            .isNotNull()
            .isNotEqualTo("blabla");
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        oppdragslinje150Builder = lagBuilderMedPaakrevdeFelter();
        oppdragslinje150 = oppdragslinje150Builder.build();
        oppdragslinje150_2 = oppdragslinje150Builder.build();

        assertThat(oppdragslinje150).isEqualTo(oppdragslinje150_2);
        assertThat(oppdragslinje150_2).isEqualTo(oppdragslinje150);

        oppdragslinje150_2 = oppdragslinje150Builder.medKodeKlassifik(KodeKlassifik.FERIEPENGER_BRUKER).build();
        assertThat(oppdragslinje150).isNotEqualTo(oppdragslinje150_2);
        assertThat(oppdragslinje150_2).isNotEqualTo(oppdragslinje150);
    }

    @Test
    void skal_bruke_KodeKlassifik_i_equalsOgHashCode() {
        oppdragslinje150Builder = lagBuilderMedPaakrevdeFelter();
        oppdragslinje150 = oppdragslinje150Builder.build();
        oppdragslinje150Builder.medKodeKlassifik(KodeKlassifik.FERIEPENGER_BRUKER);
        oppdragslinje150_2 = oppdragslinje150Builder.build();

        assertThat(oppdragslinje150).isNotEqualTo(oppdragslinje150_2);
        assertThat(oppdragslinje150.hashCode()).isNotEqualTo(oppdragslinje150_2.hashCode());

    }

    @Test
    void skal_bruke_KodeEndringLinje_i_equalsOgHashCode() {
        oppdragslinje150Builder = lagBuilderMedPaakrevdeFelter();
        oppdragslinje150 = oppdragslinje150Builder.build();
        oppdragslinje150Builder.medKodeEndringLinje(KodeEndringLinje.ENDR);
        oppdragslinje150_2 = oppdragslinje150Builder.build();

        assertThat(oppdragslinje150).isNotEqualTo(oppdragslinje150_2);
        assertThat(oppdragslinje150.hashCode()).isNotEqualTo(oppdragslinje150_2.hashCode());

    }

    @Test
    void skal_bruke_KodeStatusLinje_i_equalsOgHashCode() {
        oppdragslinje150Builder = lagBuilderMedPaakrevdeFelter();
        oppdragslinje150 = oppdragslinje150Builder.build();
        oppdragslinje150Builder.medKodeStatusLinje(KodeStatusLinje.OPPH);
        oppdragslinje150_2 = oppdragslinje150Builder.build();

        assertThat(oppdragslinje150).isNotEqualTo(oppdragslinje150_2);
        assertThat(oppdragslinje150.hashCode()).isNotEqualTo(oppdragslinje150_2.hashCode());

    }

    private Oppdragslinje150.Builder lagBuilderMedPaakrevdeFelter() {

        return Oppdragslinje150.builder()
                .medKodeEndringLinje(KODEENDRINGLINJE)
                .medDatoStatusFom(DATOSTATUSFOM)
                .medVedtakId(VEDTAKID)
                .medDelytelseId(DELYTELSEID)
                .medKodeKlassifik(KODEKLASSIFIK)
                .medVedtakFomOgTom(DATOVEDTAKFOM, DATOVEDTAKTOM)
                .medSats(SATS)
                .medTypeSats(TYPESATS)
                .medUtbetalesTilId(UTBETALESTILID)
                .medRefFagsystemId(REFFAGSYSTEMID)
                .medRefDelytelseId(REFDELYTELSEID)
                .medOppdrag110(lagOppdrag110MedPaakrevdeFelter().build());

    }

    private Oppdrag110.Builder lagOppdrag110MedPaakrevdeFelter() {
        return Oppdrag110.builder()
                .medKodeEndring(KodeEndring.NY)
                .medKodeFagomrade(KODEFAGOMRADE)
                .medFagSystemId(FAGSYSTEMID)
                .medOppdragGjelderId(OPPDRAGGJELDERID)
                .medSaksbehId(SAKSBEHID)
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(lagOppdragskontrollMedPaakrevdeFelter().build());
    }

    private Oppdragskontroll.Builder lagOppdragskontrollMedPaakrevdeFelter() {
        return Oppdragskontroll.builder()
                .medBehandlingId(BEHANDLINGID)
                .medSaksnummer(SAKSID)
                .medVenterKvittering(VENTERKVITTERING)
                .medProsessTaskId(TASKID);
    }
}
