package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class Oppdrag110EntityTest {
    private Oppdrag110.Builder oppdrag110Builder;
    private Oppdrag110 oppdrag110;
    private Oppdrag110 oppdrag110_2;

    private static final KodeEndring KODEENDRING = KodeEndring.ENDR;
    private static final KodeFagområde KODEFAGOMRADE = KodeFagområde.REFUTG;
    private static final Long FAGSYSTEMID = 250L;
    private static final String OPPDRAGGJELDERID = "1";
    private static final String SAKSBEHID = "Z1236525";
    private static final Long BEHANDLINGID = 321L;
    private static final Saksnummer SAKSID = new Saksnummer("700");
    private static final Boolean VENTERKVITTERING = true;
    private static final Long TASKID = 52L;
    private static final String FORVENTET_EXCEPTION = "forventet exception";

    @BeforeEach
    public void setup() {
        oppdrag110Builder = Oppdrag110.builder();
        oppdrag110 = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        oppdrag110 = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdrag110.getKodeEndring()).isEqualTo(KODEENDRING);
        assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KODEFAGOMRADE);
        assertThat(oppdrag110.getFagsystemId()).isEqualTo(FAGSYSTEMID);
        assertThat(oppdrag110.getOppdragGjelderId()).isEqualTo(OPPDRAGGJELDERID);
        assertThat(oppdrag110.getSaksbehId()).isEqualTo(SAKSBEHID);

    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {
        // mangler kodeEndring
        try {
            oppdrag110Builder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("kodeEndring");
        }

        // mangler kodeFagomrade
        oppdrag110Builder.medKodeEndring(KODEENDRING);
        try {
            oppdrag110Builder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("kodeFagomrade");
        }

        // mangler fagsystemId
        oppdrag110Builder.medKodeFagomrade(KODEFAGOMRADE);
        try {
            oppdrag110Builder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("fagsystemId");
        }

        // mangler fagsystemId
        oppdrag110Builder.medFagSystemId(FAGSYSTEMID);
        try {
            oppdrag110Builder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("oppdragGjelderId");
        }

        // mangler saksbehId
        oppdrag110Builder.medOppdragGjelderId(OPPDRAGGJELDERID);
        try {
            oppdrag110Builder.build();
            fail(FORVENTET_EXCEPTION);
        } catch (NullPointerException e) {
            assertThat(e.getMessage()).contains("saksbehId");
        }

    }

    @Test
    void skal_håndtere_null_this_feilKlasse_i_equals() {
        oppdrag110 = lagBuilderMedPaakrevdeFelter().build();

        assertThat(oppdrag110)
            .isNotNull()
            .isNotEqualTo("blabla");
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        oppdrag110Builder = lagBuilderMedPaakrevdeFelter();
        oppdrag110 = oppdrag110Builder.build();
        oppdrag110_2 = oppdrag110Builder.build();

        assertThat(oppdrag110).isEqualTo(oppdrag110_2);
        assertThat(oppdrag110_2).isEqualTo(oppdrag110);

        oppdrag110_2 = oppdrag110Builder.medKodeEndring(KodeEndring.UEND).build();
        assertThat(oppdrag110).isNotEqualTo(oppdrag110_2);
        assertThat(oppdrag110_2).isNotEqualTo(oppdrag110);
    }

    @Test
    void skal_bruke_KodeEndring_i_equalsOgHashCode() {
        oppdrag110Builder = lagBuilderMedPaakrevdeFelter();
        oppdrag110 = oppdrag110Builder.build();
        oppdrag110Builder.medKodeEndring(KodeEndring.UEND);
        oppdrag110_2 = oppdrag110Builder.build();

        assertThat(oppdrag110).isNotEqualTo(oppdrag110_2);
        assertThat(oppdrag110.hashCode()).isNotEqualTo(oppdrag110_2.hashCode());

    }

    @Test
    void skal_bruke_FagsystemId_i_equalsOgHashCode() {
        oppdrag110Builder = lagBuilderMedPaakrevdeFelter();
        oppdrag110 = oppdrag110Builder.build();
        oppdrag110Builder.medFagSystemId(251L);
        oppdrag110_2 = oppdrag110Builder.build();

        assertThat(oppdrag110).isNotEqualTo(oppdrag110_2);
        assertThat(oppdrag110.hashCode()).isNotEqualTo(oppdrag110_2.hashCode());

    }

    @Test
    void skal_bruke_SaksbehId_i_equalsOgHashCode() {
        oppdrag110Builder = lagBuilderMedPaakrevdeFelter();
        oppdrag110 = oppdrag110Builder.build();
        oppdrag110Builder.medFagSystemId(201L);
        oppdrag110_2 = oppdrag110Builder.build();

        assertThat(oppdrag110).isNotEqualTo(oppdrag110_2);
        assertThat(oppdrag110.hashCode()).isNotEqualTo(oppdrag110_2.hashCode());

    }

    private static Oppdrag110.Builder lagBuilderMedPaakrevdeFelter() {
        return Oppdrag110.builder()
                .medKodeEndring(KODEENDRING)
                .medKodeFagomrade(KODEFAGOMRADE)
                .medFagSystemId(FAGSYSTEMID)
                .medOppdragGjelderId(OPPDRAGGJELDERID)
                .medSaksbehId(SAKSBEHID)
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(lagOppdragskontrollMedPaakrevdeFelter().build());
    }

    private static Oppdragskontroll.Builder lagOppdragskontrollMedPaakrevdeFelter() {
        return Oppdragskontroll.builder()
                .medBehandlingId(BEHANDLINGID)
                .medSaksnummer(SAKSID)
                .medVenterKvittering(VENTERKVITTERING)
                .medProsessTaskId(TASKID);
    }
}
