package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyrOmsorgOgRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.OverstyrOmsorgOgRettOppdaterer;
import no.nav.vedtak.exception.FunksjonellException;

@CdiDbAwareTest
class OverstyrOmsorgOgRettOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    private OverstyrOmsorgOgRettOppdaterer oppdaterer;

    @BeforeEach
    void setup() {
        oppdaterer = new OverstyrOmsorgOgRettOppdaterer(ytelseFordelingTjeneste, mock(HistorikkinnslagRepository.class));
    }

    @Test
    void skal_oppdatere_ytelses_fordeling_ved_overstyring_av_rett() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var dto = new OverstyrOmsorgOgRettDto("begrunnelse", RettighetType.BARE_SØKER_RETT);

        // Act
        oppdaterer.håndterOverstyring(dto, behandling, kontekst(behandling));

        // Assert
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        assertThat(yfa.getOverstyrtRettighet()).contains(RettighetType.BARE_SØKER_RETT);
    }


    @Test
    void skal_overstyre_eksisterende_avklaring_selv_om_opprinnelig_avklaring_er_det_samme_som_ny_overstyring() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var dto = new OverstyrOmsorgOgRettDto("begrunnelse", RettighetType.BARE_SØKER_RETT);

        // Act
        oppdaterer.håndterOverstyring(dto, behandling, kontekst(behandling));

        // Assert
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        assertThat(yfa.getOverstyrtRettighet()).contains(RettighetType.BARE_SØKER_RETT);
    }


    @Test
    void skal_ikke_oppdatere_ytelsesfordeling_hvis_eksisterende_rett_er_det_samme_som_en_overstyrer() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOverstyrtRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .lagre(repositoryProvider);

        var dto = new OverstyrOmsorgOgRettDto("begrunnelse", RettighetType.BARE_SØKER_RETT);
        var kontekst = kontekst(behandling);

        // Act
        assertThrows(FunksjonellException.class, () -> oppdaterer.håndterOverstyring(dto, behandling, kontekst));
    }

    private BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(),
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
    }
}
