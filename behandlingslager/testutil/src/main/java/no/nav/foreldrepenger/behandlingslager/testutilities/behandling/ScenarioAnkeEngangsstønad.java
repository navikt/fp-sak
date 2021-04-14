package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.ANKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering.ANKE_AVVIS;
import static no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering.ANKE_OMGJOER;
import static no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE;
import static no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

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

    private Map<AksjonspunktDefinisjon, BehandlingStegType> opprettedeAksjonspunktDefinisjoner = new HashMap<>();
    private Map<AksjonspunktDefinisjon, BehandlingStegType> utførteAksjonspunktDefinisjoner = new HashMap<>();
    private AbstractTestScenario<?> abstractTestScenario;
    private AnkeVurdering ankeVurdering;
    private String behandlendeEnhet;
    private Behandling ankeBehandling;
    private BehandlingStegType startSteg;
    private AnkeVurderingResultatEntitet.Builder vurderingResultat = AnkeVurderingResultatEntitet.builder();
    private BehandlingVedtak behandlingVedtak;

    private ScenarioAnkeEngangsstønad() {
    }

    public static ScenarioAnkeEngangsstønad forStadfestet(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario, ANKE_STADFESTE_YTELSESVEDTAK);
    }

    public static ScenarioAnkeEngangsstønad forAvvistAnke(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario, ANKE_AVVIS);
    }

    public static ScenarioAnkeEngangsstønad forOpphevOgHjemsende(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario, ANKE_OPPHEVE_OG_HJEMSENDE);
    }

    public static ScenarioAnkeEngangsstønad forOmgjør(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario, ANKE_OMGJOER);
    }

    public static ScenarioAnkeEngangsstønad forUtenVurderingResultat(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioAnkeEngangsstønad().setup(abstractTestScenario).medBehandlingStegStart(ANKE);
    }

    private ScenarioAnkeEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario) {
        return setup(abstractTestScenario, ANKE, MANUELL_VURDERING_AV_ANKE);
    }

    private ScenarioAnkeEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, BehandlingStegType stegType,
            AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.abstractTestScenario = abstractTestScenario;

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        this.startSteg = stegType;

        this.opprettedeAksjonspunktDefinisjoner.put(aksjonspunktDefinisjon, stegType);
        return this;
    }

    private ScenarioAnkeEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, AnkeVurdering ankeVurdering) {
        setup(abstractTestScenario);
        this.ankeVurdering = ankeVurdering;

        this.opprettedeAksjonspunktDefinisjoner.remove(MANUELL_VURDERING_AV_ANKE);
        this.utførteAksjonspunktDefinisjoner.put(MANUELL_VURDERING_AV_ANKE, ANKE);

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        if (ankeVurdering.equals(AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK)) {
            this.startSteg = ANKE;
            this.opprettedeAksjonspunktDefinisjoner.put(MANUELL_VURDERING_AV_ANKE, ANKE);
        } else {
            this.startSteg = BehandlingStegType.FORESLÅ_VEDTAK;
            this.opprettedeAksjonspunktDefinisjoner.put(FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        }

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

        if (behandlendeEnhet != null) {
            builder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }
        ankeBehandling = builder.build();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(ankeBehandling);
        behandlingRepository.lagre(ankeBehandling, lås);
        if (ankeVurdering != null) {
            Behandlingsresultat.builder().medBehandlingResultatType(
                    BehandlingResultatType.tolkBehandlingResultatType(ankeVurdering))
                    .buildFor(ankeBehandling);
        } else {
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT)
                    .buildFor(ankeBehandling);
        }

        utførteAksjonspunktDefinisjoner.forEach((apDef, stegType) -> AksjonspunktTestSupport.leggTilAksjonspunkt(ankeBehandling, apDef, stegType));

        ankeBehandling.getAksjonspunkter().forEach(punkt -> AksjonspunktTestSupport.setTilUtført(punkt, "Test"));

        opprettedeAksjonspunktDefinisjoner.forEach((apDef, stegType) -> AksjonspunktTestSupport.leggTilAksjonspunkt(ankeBehandling, apDef, stegType));

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(ankeBehandling, startSteg);
        }

        return ankeBehandling;
    }

    public ScenarioAnkeEngangsstønad medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak ankeOmgjørÅrsak) {
        vurderingResultat.medAnkeOmgjørÅrsak(ankeOmgjørÅrsak);
        return this;
    }

    public ScenarioAnkeEngangsstønad medAnkeVurderingOmgjør(AnkeVurderingOmgjør ankeVurderingOmgjør) {
        vurderingResultat.medAnkeVurderingOmgjør(ankeVurderingOmgjør);
        return this;
    }

    public ScenarioAnkeEngangsstønad medBegrunnelse(String begrunnelse) {
        vurderingResultat.medBegrunnelse(begrunnelse);
        return this;
    }

    public ScenarioAnkeEngangsstønad medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return this;
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
        // Whitebox.setInternalState(ankeBehandling, "id", AbstractTestScenario.nyId());
        return ankeBehandling;
    }

    public Fagsak getFagsak() {
        return abstractTestScenario.getFagsak();
    }

    public ScenarioAnkeEngangsstønad medAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        opprettedeAksjonspunktDefinisjoner.put(apDef, stegType);
        return this;
    }

    public ScenarioAnkeEngangsstønad medUtførtAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        utførteAksjonspunktDefinisjoner.put(apDef, stegType);
        return this;
    }

    public ScenarioAnkeEngangsstønad medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return this;
    }
}
