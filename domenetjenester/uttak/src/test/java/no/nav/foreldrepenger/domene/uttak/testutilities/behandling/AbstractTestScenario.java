package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;

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

    private static final AtomicLong SAKSUMMER = new AtomicLong(100999L);
    private static final AtomicLong BEHANDLING_ID = new AtomicLong(0L);

    private InntektArbeidYtelseScenario iayScenario;
    private Behandling behandling;
    private final Fagsak fagsak;
    private Behandlingsresultat behandlingsresultat;

    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittDekningsgradEntitet oppgittDekningsgrad;
    private OppgittFordelingEntitet oppgittFordeling;
    private AvklarteUttakDatoerEntitet avklarteUttakDatoer;

    private Behandling originalBehandling;
    private BehandlingÅrsakType behandlingÅrsakType;
    private PerioderUtenOmsorgEntitet perioderUtenOmsorg;
    private PerioderAleneOmsorgEntitet perioderMedAleneomsorg;
    private UttakResultatPerioderEntitet uttak;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle, AktørId aktørId) {
        fagsak = Fagsak.opprettNy(fagsakYtelseType, NavBruker.opprettNy(aktørId, Språkkode.NB), brukerRolle,
                new Saksnummer(nyId() + ""));

    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle) {
        this(fagsakYtelseType, brukerRolle, AktørId.dummy());
    }

    static long nyId() {
        return SAKSUMMER.getAndIncrement();
    }

    @SuppressWarnings("unchecked")
    public S medDefaultInntektArbeidYtelse() {
        getIayScenario().medDefaultInntektArbeidYtelse();
        return (S) this;
    }

    private InntektArbeidYtelseScenario getIayScenario() {
        if (iayScenario == null) {
            iayScenario = new InntektArbeidYtelseScenario();
        }
        return iayScenario;
    }

    public Behandling lagre(UttakRepositoryProvider repositoryProvider) {
        class LegacyBridgeIay implements LagreInntektArbeidYtelse {

            @Override
            public void lagreInntektArbeidYtelseAggregat(Long behandlingId,
                    InntektArbeidYtelseAggregatBuilder builder) {
                throw new UnsupportedOperationException("Get outta here - no longer supporting this");
            }

        }

        return lagre(repositoryProvider, new LegacyBridgeIay());
    }

    public Behandling lagre(UttakRepositoryProvider repositoryProvider,
            BiConsumer<Long, InntektArbeidYtelseAggregatBuilder> lagreIayAggregat) {

        class LegacyBridgeIay implements LagreInntektArbeidYtelse {

            @Override
            public void lagreInntektArbeidYtelseAggregat(Long behandlingId,
                    InntektArbeidYtelseAggregatBuilder builder) {
                lagreIayAggregat.accept(behandlingId, builder);
            }
        }

        return lagre(repositoryProvider, new LegacyBridgeIay());
    }

    private Behandling lagre(UttakRepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        build(repositoryProvider, lagreIay);
        return behandling;
    }

    private void build(UttakRepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        if (behandling != null) {
            throw new IllegalStateException(
                    "build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();
        behandling.setId(BEHANDLING_ID.getAndIncrement());

        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider);
        lagreInntektArbeidYtelse(lagreIay);
        lagreYtelseFordelingOpplysninger(repositoryProvider, behandling);
        lagreUttak(repositoryProvider.getFpUttakRepository());
    }

    private void lagreInntektArbeidYtelse(LagreInntektArbeidYtelse lagreIay) {
        if (iayScenario != null) {
            var iayAggregat = iayScenario.initInntektArbeidYtelseAggregatBuilder();
            iayAggregat.ifPresent(a -> lagreIay.lagreInntektArbeidYtelseAggregat(behandling.getId(), a));

        }
    }

    private void lagreYtelseFordelingOpplysninger(UttakRepositoryProvider repositoryProvider, Behandling behandling) {
        YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        lagreOppgittRettighet(ytelsesFordelingRepository, behandling);
        Long behandlingId = behandling.getId();
        if (oppgittDekningsgrad != null) {
            ytelsesFordelingRepository.lagre(behandlingId, oppgittDekningsgrad);
        }
        if (oppgittFordeling != null) {
            ytelsesFordelingRepository.lagre(behandlingId, oppgittFordeling);
        }
        if (avklarteUttakDatoer != null) {
            ytelsesFordelingRepository.lagre(behandlingId, avklarteUttakDatoer);
        }
        if (perioderUtenOmsorg != null) {
            ytelsesFordelingRepository.lagre(behandlingId, perioderUtenOmsorg);
        }
        if (perioderMedAleneomsorg != null) {
            ytelsesFordelingRepository.lagre(behandlingId, perioderMedAleneomsorg);
        }
    }

    private void lagreOppgittRettighet(YtelsesFordelingRepository ytelsesFordelingRepository, Behandling behandling) {
        if (oppgittRettighet != null) {
            ytelsesFordelingRepository.lagre(behandling.getId(), oppgittRettighet);
        }
    }

    private Builder grunnBuild(UttakRepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                    .medBehandlingÅrsak(
                            BehandlingÅrsak.builder(behandlingÅrsakType).medOriginalBehandlingId(originalBehandling.getId()));
        }

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(UttakRepositoryProvider repoProvider) {
        if (behandlingsresultat == null) {
            behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().build();
        }
        repoProvider.getBehandlingsresultatRepository().lagre(behandling.getId(), behandlingsresultat);
    }

    private void lagreUttak(FpUttakRepository fpUttakRepository) {
        if (uttak == null) {
            return;
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);
    }

    public AktørId getAktørId() {
        return fagsak.getAktørId();
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultOppgittDekningsgrad() {
        medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medUttak(UttakResultatPerioderEntitet uttak) {
        if (behandlingsresultat == null) {
            medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().build());
        }
        this.uttak = uttak;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittDekningsgrad(OppgittDekningsgradEntitet oppgittDekningsgrad) {
        this.oppgittDekningsgrad = oppgittDekningsgrad;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFordeling(OppgittFordelingEntitet oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medAvklarteUttakDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        this.avklarteUttakDatoer = avklarteUttakDatoer;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medPerioderUtenOmsorg(PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
        this.perioderUtenOmsorg = perioderUtenOmsorg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medPeriodeMedAleneomsorg(PerioderAleneOmsorgEntitet perioderAleneOmsorg) {
        this.perioderMedAleneomsorg = perioderAleneOmsorg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        this.originalBehandling = originalBehandling;
        this.behandlingÅrsakType = behandlingÅrsakType;
        return (S) this;
    }

    public InntektArbeidYtelseScenario.InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        return getIayScenario().getInntektArbeidYtelseScenarioTestBuilder();
    }

    interface LagreInntektArbeidYtelse {
        void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder);
    }
}
