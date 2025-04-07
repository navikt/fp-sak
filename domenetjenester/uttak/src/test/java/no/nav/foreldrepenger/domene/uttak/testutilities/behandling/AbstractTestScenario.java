package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.List;
import java.util.Set;
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
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
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
    private OppgittRettighetEntitet overstyrtRettighet;
    private Dekningsgrad oppgittDekningsgrad;
    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private OppgittFordelingEntitet overstyrtFordeling;
    private AvklarteUttakDatoerEntitet avklarteUttakDatoer;

    private Behandling originalBehandling;
    private Set<BehandlingÅrsakType> behandlingÅrsaker;
    private Boolean overstyrtOmsorg;
    private UttakResultatPerioderEntitet uttak;
    private Stønadskontoberegning stønadskontoberegning;

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
        var behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();
        behandling.setId(BEHANDLING_ID.getAndIncrement());

        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider);
        lagreInntektArbeidYtelse(lagreIay);
        lagreYtelseFordeling(repositoryProvider, behandling);
        lagreUttak(repositoryProvider.getFpUttakRepository());
    }

    private void lagreInntektArbeidYtelse(LagreInntektArbeidYtelse lagreIay) {
        if (iayScenario != null) {
            var iayAggregat = iayScenario.initInntektArbeidYtelseAggregatBuilder();
            iayAggregat.ifPresent(a -> lagreIay.lagreInntektArbeidYtelseAggregat(behandling.getId(), a));

        }
    }

    private void lagreYtelseFordeling(UttakRepositoryProvider repositoryProvider, Behandling behandling) {
        if (oppgittRettighet == null && oppgittDekningsgrad == null && oppgittFordeling == null && overstyrtRettighet == null
            && avklarteUttakDatoer == null && justertFordeling == null && overstyrtFordeling == null && overstyrtOmsorg == null) {
            return;
        }
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yf = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(oppgittRettighet)
            .medAvklartRettighet(overstyrtRettighet)
            .medOppgittDekningsgrad(oppgittDekningsgrad)
            .medOppgittFordeling(oppgittFordeling)
            .medJustertFordeling(justertFordeling)
            .medOverstyrtFordeling(overstyrtFordeling)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .medOverstyrtOmsorg(overstyrtOmsorg)
            ;
        ytelsesFordelingRepository.lagre(behandling.getId(), yf.build());
    }

    private Builder grunnBuild(UttakRepositoryProvider repositoryProvider) {
        var fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                    .medBehandlingÅrsak(
                            BehandlingÅrsak.builder(List.copyOf(behandlingÅrsaker)).medOriginalBehandlingId(originalBehandling.getId()));
        }

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        var fagsakId = fagsakRepo.opprettNy(fagsak);
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

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), stønadskontoberegning, uttak);
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
        medOppgittDekningsgrad(Dekningsgrad._100);
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
    public S medStønadskontoberegning(Stønadskontoberegning stønadskontoberegning) {
        this.stønadskontoberegning = stønadskontoberegning;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOverstyrtRettighet(OppgittRettighetEntitet overstyrtRettighet) {
        this.overstyrtRettighet = overstyrtRettighet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittDekningsgrad(Dekningsgrad oppgittDekningsgrad) {
        this.oppgittDekningsgrad = oppgittDekningsgrad;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFordeling(OppgittFordelingEntitet oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medJustertFordeling(OppgittFordelingEntitet justertFordeling) {
        this.justertFordeling = justertFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOverstyrtFordeling(OppgittFordelingEntitet overstyrtFordeling) {
        this.overstyrtFordeling = overstyrtFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medAvklarteUttakDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        this.avklarteUttakDatoer = avklarteUttakDatoer;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOverstyrtOmsorg(Boolean overstyrtOmsorg) {
        this.overstyrtOmsorg = overstyrtOmsorg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        return medOriginalBehandling(originalBehandling, Set.of(behandlingÅrsakType));
    }

    public S medOriginalBehandling(Behandling originalBehandling, Set<BehandlingÅrsakType> behandlingÅrsakType) {
        this.originalBehandling = originalBehandling;
        this.behandlingÅrsaker = behandlingÅrsakType;
        this.behandlingType = BehandlingType.REVURDERING;
        return (S) this;
    }

    public InntektArbeidYtelseScenario.InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        return getIayScenario().getInntektArbeidYtelseScenarioTestBuilder();
    }

    interface LagreInntektArbeidYtelse {
        void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder);
    }
}
