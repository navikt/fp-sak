package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingBehandlingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.ANKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering.ANKE_AVVIS;
import static org.mockito.Mockito.when;

/**
 * Default test scenario builder for Anke Engangssøknad. Kan opprettes for gitt
 * standard Scenario Engangssøknad
 * <p>
 * Oppretter en avsluttet behandling ved hjelp av Scenario Builder.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public class ScenarioAnkeEngangsstønad {

    private AbstractTestScenario<?> abstractTestScenario;
    private AnkeVurdering ankeVurdering;
    private Behandling ankeBehandling;
    private BehandlingStegType startSteg;

    private ScenarioAnkeEngangsstønad() {
    }

    public static ScenarioAnkeEngangsstønad forAvvistAnke(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario, ANKE_AVVIS);
    }

    private ScenarioAnkeEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, AnkeVurdering ankeVurdering) {
        this.abstractTestScenario = abstractTestScenario;

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        this.startSteg = ANKE;

        this.ankeVurdering = ankeVurdering;

        this.startSteg = BehandlingStegType.FORESLÅ_VEDTAK;

        return this;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {
        if (ankeBehandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        abstractTestScenario.buildAvsluttet(repositoryProvider);
        return buildAnke(repositoryProvider);
    }

    private Behandling buildAnke(BehandlingRepositoryProvider repositoryProvider) {
        var fagsak = abstractTestScenario.getFagsak();

        // oppprett og lagre behandling
        var builder = Behandling.forAnke(fagsak);

        ankeBehandling = builder.build();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(ankeBehandling);
        behandlingRepository.lagre(ankeBehandling, lås);
        if (ankeVurdering != null) {
            Behandlingsresultat.builder().medBehandlingResultatType(
                    AnkeVurderingBehandlingResultat.tolkBehandlingResultatType(ankeVurdering, AnkeVurderingOmgjør.ANKE_TIL_GUNST))
                    .buildFor(ankeBehandling);
        } else {
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT)
                    .buildFor(ankeBehandling);
        }

        ankeBehandling.getAksjonspunkter().forEach(punkt -> AksjonspunktTestSupport.setTilUtført(punkt, "Test"));

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(ankeBehandling, startSteg);
        }

        return ankeBehandling;
    }

    public BehandlingRepository mockBehandlingRepository() {
        var behandlingRepository = abstractTestScenario.mockBehandlingRepository();
        when(behandlingRepository.hentBehandling(ankeBehandling.getId())).thenReturn(ankeBehandling);
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return abstractTestScenario.mockBehandlingRepositoryProvider();
    }

    public Behandling lagMocked() {
        // pga det ikke går ann å flytte steg hvis mocket så settes startsteg til null
        startSteg = null;
        var repositoryProvider = abstractTestScenario.mockBehandlingRepositoryProvider();
        lagre(repositoryProvider);
        ankeBehandling.setId(AbstractTestScenario.nyId());
        return ankeBehandling;
    }

}
