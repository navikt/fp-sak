package no.nav.foreldrepenger.domene.prosess.testutilities.behandling;

import java.util.concurrent.atomic.AtomicLong;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.RepositoryProvider;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt
 * innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {

    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private Behandling behandling;

    private Fagsak fagsak;

    private final VilkårResultatType vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType,
                                   RelasjonsRolleType brukerRolle,
                                   NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder.nyFagsak(fagsakYtelseType, brukerRolle)
            .medSaksnummer(new Saksnummer(nyId() + ""))
            .medBrukerKjønn(kjønn);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    public Behandling lagre(RepositoryProvider repositoryProvider) {
        build(repositoryProvider.getBehandlingRepository(), repositoryProvider);
        return behandling;
    }

    private void build(BehandlingRepository behandlingRepo, RepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException(
                "build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        var behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        var lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private Builder grunnBuild(RepositoryProvider repositoryProvider) {
        var fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        return Behandling.nyBehandlingFor(fagsak, behandlingType);

    }

    protected void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        fagsak = fagsakBuilder.build();
        var fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(RepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().buildFor(behandling);

        var inngangsvilkårBuilder = VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat())
            .medVilkårResultatType(vilkårResultatType);

        var vilkårResultat = inngangsvilkårBuilder.buildFor(behandling);

        repoProvider.getBehandlingRepository().lagre(vilkårResultat, lås);

    }
}
