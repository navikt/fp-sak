package no.nav.foreldrepenger.behandlingskontroll.testutilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.Whitebox;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {
    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private Behandling behandling;

    private Fagsak fagsak;
    private BehandlingStegType startSteg;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
                                   NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType, brukerRolle)
            .medBrukerKjønn(kjønn);
    }

    public Behandling lagre(BehandlingskontrollServiceProvider repositoryProvider) {
        build(repositoryProvider);
        return behandling;
    }

    public void leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    private void build(BehandlingskontrollServiceProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        BehandlingRepository behandlingRepo = repositoryProvider.getBehandlingRepository();
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        // opprett og lagre resulater på behandling

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private NavBrukerKjønn getKjønnFraFagsak() {
        return fagsakBuilder.getBrukerBuilder().getKjønn();
    }

    private Builder grunnBuild(BehandlingskontrollServiceProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        return Behandling.nyBehandlingFor(fagsak, behandlingType);
    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            final EntityManager entityManager = (EntityManager) Whitebox.getInternalState(fagsakRepo, "entityManager");
            if (entityManager != null) {
                BrukerTjeneste brukerTjeneste = new BrukerTjeneste(new NavBrukerRepository(entityManager));
                final NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(fagsakBuilder.getBrukerBuilder().getAktørId(),
                    fagsakBuilder.getBrukerBuilder().getSpråkkode() != null ? fagsakBuilder.getBrukerBuilder().getSpråkkode() : Språkkode.NB);
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void leggTilAksjonspunkter(Behandling behandling) {
        aksjonspunktDefinisjoner.forEach(
            (apDef, stegType) -> {
                if (stegType != null) {
                    AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, apDef, stegType);
                } else {
                    AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, apDef);
                }
            });
    }

    public static class NavBrukerBuilder {

        private NavBruker bruker;

        private AktørId aktørId = AktørId.dummy();
        private NavBrukerKjønn kjønn;

        public NavBrukerBuilder() {
            // default ctor
        }

        public NavBrukerBuilder medBruker(NavBruker bruker) {
            this.bruker = bruker;
            return this;
        }

        public NavBrukerBuilder medKjønn(NavBrukerKjønn kjønn) {
            this.kjønn = kjønn;
            return this;
        }

        public NavBrukerKjønn getKjønn() {
            return kjønn;
        }

        public Språkkode getSpråkkode() {
            return Språkkode.NB;
        }

        public AktørId getAktørId() {
            return aktørId;
        }

        public NavBruker build() {
            return bruker;
        }
    }

    public static class FagsakBuilder {

        private NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();

        private RelasjonsRolleType rolle;

        private FagsakYtelseType fagsakYtelseType;

        private FagsakBuilder(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
            this.fagsakYtelseType = fagsakYtelseType;
            this.rolle = rolle;
        }

        public FagsakBuilder medBrukerKjønn(NavBrukerKjønn kjønn) {
            brukerBuilder.medKjønn(kjønn);
            return this;
        }

        public NavBrukerBuilder getBrukerBuilder() {
            return brukerBuilder;
        }

        public FagsakBuilder medBruker(NavBruker bruker) {
            brukerBuilder.medBruker(bruker);
            return this;
        }

        public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
            return new FagsakBuilder(fagsakYtelseType, rolle);
        }

        public Fagsak build() {
            return Fagsak.opprettNy(fagsakYtelseType, brukerBuilder.build(), rolle, new Saksnummer("" + FAKE_ID.getAndIncrement()));
        }
    }

}
