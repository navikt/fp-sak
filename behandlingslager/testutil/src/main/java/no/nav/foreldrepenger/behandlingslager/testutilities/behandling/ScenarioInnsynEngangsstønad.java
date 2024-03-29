package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_INNSYN;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public class ScenarioInnsynEngangsstønad {

    public static ScenarioInnsynEngangsstønad innsyn(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioInnsynEngangsstønad().setup(abstractTestScenario);
    }

    private final Map<AksjonspunktDefinisjon, BehandlingStegType> opprettedeAksjonspunktDefinisjoner = new EnumMap<>(AksjonspunktDefinisjon.class);

    private AbstractTestScenario<?> abstractTestScenario;

    private Behandling behandling;
    private BehandlingStegType startSteg;

    private ScenarioInnsynEngangsstønad() {
    }

    private ScenarioInnsynEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario) {
        this.abstractTestScenario = abstractTestScenario;

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        this.startSteg = BehandlingStegType.VURDER_INNSYN;

        this.opprettedeAksjonspunktDefinisjoner.put(VURDER_INNSYN, BehandlingStegType.VURDER_INNSYN);
        return this;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        abstractTestScenario.buildAvsluttet(repositoryProvider);
        return buildInnsyn(repositoryProvider);
    }

    private Behandling buildInnsyn(BehandlingRepositoryProvider repositoryProvider) {
        var fagsak = abstractTestScenario.getFagsak();

        // oppprett og lagre behandling
        var builder = Behandling.nyBehandlingFor(fagsak, BehandlingType.INNSYN);

        behandling = builder.build();

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT)
                .buildFor(behandling);

        opprettedeAksjonspunktDefinisjoner.forEach(
                (apDef, stegType) -> AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, apDef, stegType));

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, startSteg);
        }
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        return behandling;
    }

    public BehandlingRepository mockBehandlingRepository() {
        var behandlingRepository = abstractTestScenario.mockBehandlingRepository();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return abstractTestScenario.mockBehandlingRepositoryProvider();
    }

    public Behandling lagMocked() {
        // pga det ikke går ann å flytte steg hvis mocket så settes startsteg til null
        startSteg = null;
        lagre(abstractTestScenario.mockBehandlingRepositoryProvider());
        behandling.setId(AbstractTestScenario.nyId());
        return behandling;
    }

    public Fagsak getFagsak() {
        return abstractTestScenario.getFagsak();
    }

}
