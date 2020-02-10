package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollManagerFactoryDagYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.SjekkOmDetFinnesTilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.endring.OppdragskontrollEndring;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.førstegangsoppdrag.OppdragskontrollFørstegang;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OppdragskontrollOpphør;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OpprettOpphørIEndringsoppdrag;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class OppdragskontrollManagerFactoryTest {

    private BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjenesteFP = mock(BehandlingTilOppdragMapperTjeneste.class);
    private OppdragskontrollOpphør oppdragskontrollOpphørFP;
    private OppdragskontrollEndring oppdragskontrollEndringFP;
    private OppdragskontrollFørstegang oppdragskontrollFørstegangFP;
    private OpprettOpphørIEndringsoppdrag opprettOpphørIEndringsoppdragBruker;
    private OppdragskontrollManagerFactoryDagYtelse oppdragskontrollManagerFactory;
    private SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelseMock = mock(SjekkOmDetFinnesTilkjentYtelse.class);
    private Behandling originalBehandling;
    private Behandling revurdering;

    @Before
    public void setup() {
        oppdragskontrollFørstegangFP = new OppdragskontrollFørstegang(behandlingTilOppdragMapperTjenesteFP);
        oppdragskontrollOpphørFP = new OppdragskontrollOpphør(behandlingTilOppdragMapperTjenesteFP);
        opprettOpphørIEndringsoppdragBruker = new OpprettOpphørIEndringsoppdrag(oppdragskontrollOpphørFP);
        oppdragskontrollEndringFP = new OppdragskontrollEndring(behandlingTilOppdragMapperTjenesteFP, opprettOpphørIEndringsoppdragBruker);
        oppdragskontrollManagerFactory = new OppdragskontrollManagerFactoryDagYtelse(
            oppdragskontrollFørstegangFP,
            oppdragskontrollEndringFP,
            oppdragskontrollOpphørFP,
            sjekkOmDetFinnesTilkjentYtelseMock);
        originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                    .medOriginalBehandling(originalBehandling))
            .build();
        Whitebox.setInternalState(revurdering, "id", 256L);
    }

    /**
     * Forrige behandling TY: Ja
     * Denne behandling TY: Ja
     * Tidligere oppdrag: Ja
     * Beslutningsvedtak: Nei
     */
    @Test
    public void skal_sende_endring_når_finnes_TY_i_denne_og_forrige() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(revurdering))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.ANNEN_ENDRING);

        // Act
        Optional<OppdragskontrollManager> resultatOpt = oppdragskontrollManagerFactory.getManager(revurdering, true);

        // Assert
        assertThat(resultatOpt).hasValueSatisfying(oppdragskontrollManager ->
            assertThat(oppdragskontrollManager).isSameAs(oppdragskontrollEndringFP));
    }


    /**
     * Forrige behandling TY: Nei
     * Denne behandling TY: Ja
     * Tidligere oppdrag: Ja
     */
    @Test
    public void skal_sende_endring_når_finnes_TY_i_denne_og_oppdrag_fra_før() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(revurdering))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.ANNEN_ENDRING);

        // Act
        Optional<OppdragskontrollManager> resultatOpt = oppdragskontrollManagerFactory.getManager(revurdering, true);

        // Assert
        assertThat(resultatOpt).hasValueSatisfying(oppdragskontrollManager ->
            assertThat(oppdragskontrollManager).isSameAs(oppdragskontrollEndringFP));
    }

    /**
     * Forrige behandling TY: N/A
     * Denne behandling TY: Ja
     * Tidligere oppdrag: Nei
     */
    @Test
    public void skal_sende_førstegang_når_finnes_TY_i_denne_og_ikke_fra_før() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(originalBehandling))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.ENDRET_FRA_TOM);

        // Act
        Optional<OppdragskontrollManager> resultatOpt = oppdragskontrollManagerFactory.getManager(originalBehandling, false);

        // Assert
        assertThat(resultatOpt).hasValueSatisfying(oppdragskontrollManager ->
            assertThat(oppdragskontrollManager).isSameAs(oppdragskontrollFørstegangFP));
    }

    /**
     * Forrige behandling TY: Ja
     * Denne behandling TY: Nei
     * Tidligere oppdrag: Ja
     */
    @Test
    public void skal_sende_opphør_når_ingen_TY_i_denne_og_forrige_har_TY() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(revurdering))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.ENDRET_TIL_TOM);

        // Assert
        Optional<OppdragskontrollManager> resultatOpt = oppdragskontrollManagerFactory.getManager(revurdering, true);

        // Assert
        assertThat(resultatOpt).hasValueSatisfying(oppdragskontrollManager ->
            assertThat(oppdragskontrollManager).isSameAs(oppdragskontrollOpphørFP));
    }

    /**
     * Forrige behandling TY: Nei
     * Denne behandling TY: Nei
     * Tidligere oppdrag: Nei
     */
    @Test
    public void skal_ikke_sende_når_ingen_TY_i_denne_forrige_eller_tidligere() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(revurdering))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.INGEN_ENDRING);

        // Act
        Optional<OppdragskontrollManager> resultatRevurdering = oppdragskontrollManagerFactory.getManager(revurdering, false);

        // Assert
        assertThat(resultatRevurdering).isEmpty();
    }

    /**
     * Forrige behandling TY: Nei
     * Denne behandling TY: Nei
     * Tidligere oppdrag: Ja
     */
    @Test
    public void skal_ikke_sende_når_ingen_TY_i_denne_eller_forrige_men_finnes_oppdrag_fra_før() {
        // Arrange
        when(sjekkOmDetFinnesTilkjentYtelseMock.tilkjentYtelseDiffMotForrige(revurdering))
            .thenReturn(SjekkOmDetFinnesTilkjentYtelse.TilkjentYtelseDiff.INGEN_ENDRING);

        // Act
        Optional<OppdragskontrollManager> resultat = oppdragskontrollManagerFactory.getManager(revurdering, true);

        // Assert
        assertThat(resultat).isEmpty();
    }
}
