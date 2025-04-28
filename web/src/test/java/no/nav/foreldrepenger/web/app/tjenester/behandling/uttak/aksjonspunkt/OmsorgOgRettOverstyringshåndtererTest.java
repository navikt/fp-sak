package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyrOmsorgOgRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.OmsorgOgRettOverstyringshåndterer;

@CdiDbAwareTest
class OmsorgOgRettOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    @Inject
    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;

    private OmsorgOgRettOverstyringshåndterer oppdaterer;

    @BeforeEach
    void setup() {
        oppdaterer = new OmsorgOgRettOverstyringshåndterer(mock(HistorikkinnslagRepository.class), faktaOmsorgRettTjeneste);
    }

    @Test
    void skal_oppdatere_ytelses_fordeling_ved_overstyring_av_rett() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var dto = new OverstyrOmsorgOgRettDto("begrunnelse", Rettighetstype.BARE_FAR_RETT);

        // Act
        oppdaterer.håndterOverstyring(dto, BehandlingReferanse.fra(behandling));

        // Assert
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        assertThat(yfa.getOverstyrtRettighetstype()).contains(Rettighetstype.BARE_FAR_RETT);
    }


    @Test
    void skal_overstyre_eksisterende_avklaring_selv_om_opprinnelig_avklaring_er_det_samme_som_ny_overstyring() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var dto = new OverstyrOmsorgOgRettDto("begrunnelse", Rettighetstype.BARE_MOR_RETT);

        // Act
        oppdaterer.håndterOverstyring(dto, BehandlingReferanse.fra(behandling));

        // Assert
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        assertThat(yfa.getOverstyrtRettighetstype()).contains(Rettighetstype.BARE_MOR_RETT);
    }
}
