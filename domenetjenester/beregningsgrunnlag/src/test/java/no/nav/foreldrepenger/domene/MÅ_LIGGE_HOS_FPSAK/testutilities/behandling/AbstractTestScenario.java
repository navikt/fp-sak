package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.RepositoryProvider;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.Whitebox;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private List<TestScenarioTillegg> testScenarioTilleggListe = new ArrayList<>();
    private ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
    private ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
    private InntektArbeidYtelseScenario iayScenario;
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private VilkårResultatType vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
    private Long fagsakId = nyId();
    private BehandlingRepository mockBehandlingRepository;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    private RepositoryProvider repositoryProvider;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
                                   NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType, brukerRolle)
            .medSaksnummer(new Saksnummer(nyId() + ""))
            .medBrukerKjønn(kjønn);
    }

    public AktørId getSøkerAktørId() {
        return this.fagsakBuilder.getBrukerBuilder().getAktørId();
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    public BehandlingReferanse lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return BehandlingReferanse.fra(behandling);
    }


    public BehandlingReferanse lagre(RepositoryProvider repositoryProvider) {
        class LegacyBridgeIay implements LagreInntektArbeidYtelse {
            @Override
            public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder builder) {
                throw new UnsupportedOperationException("Get outta here - no longer supporting this");
            }

            @Override
            public void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
                throw new UnsupportedOperationException("Get outta here - no longer supporting this");
            }

        }

        return BehandlingReferanse.fra(lagre(repositoryProvider, new LegacyBridgeIay()));
    }

    public BehandlingReferanse lagre(RepositoryProvider repositoryProvider,
                            BiConsumer<Long, InntektArbeidYtelseAggregatBuilder> lagreIayAggregat,
                            BiConsumer<Long, OppgittOpptjeningBuilder> lagreOppgittOpptjening) {

        class LegacyBridgeIay implements LagreInntektArbeidYtelse {
            @Override
            public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder builder) {
                lagreOppgittOpptjening.accept(behandlingId, builder);
            }

            @Override
            public void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
                lagreIayAggregat.accept(behandlingId, builder);
            }
        }

        return BehandlingReferanse.fra(lagre(repositoryProvider, new LegacyBridgeIay()));
    }

    public Behandling lagre(RepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        build(repositoryProvider.getBehandlingRepository(), repositoryProvider, lagreIay);
        return behandling;
    }

    public void leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
    }

    @SuppressWarnings("unchecked")
    public S leggTilScenario(TestScenarioTillegg testScenarioTillegg) {
        testScenarioTilleggListe.add(testScenarioTillegg);
        return (S) this;
    }

    public InntektArbeidYtelseScenario.InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        return getIayScenario().getInntektArbeidYtelseScenarioTestBuilder();
    }

    @SuppressWarnings("unchecked")
    public S medDefaultInntektArbeidYtelse() {
        getIayScenario().medDefaultInntektArbeidYtelse();
        return (S) this;
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(RepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        FagsakRepository mockFagsakRepository = mockFagsakRepository();
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);

        return behandlingRepository;
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    private BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(RepositoryProvider.class);
        BehandlingRepository behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        when(behandlingRepository.lagre(behandlingCaptor.capture(), Mockito.any()))
            .thenAnswer((Answer<Long>) invocation -> {
                Behandling beh = invocation.getArgument(0);
                Long id = beh.getId();
                if (id == null) {
                    id = nyId();
                    Whitebox.setInternalState(beh, "id", id);
                }

                beh.getAksjonspunkter().forEach(punkt -> Whitebox.setInternalState(punkt, "id", nyId()));
                behandlingMap.put(id, beh);
                return id;
            });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    private InntektArbeidYtelseScenario getIayScenario() {
        if (iayScenario == null) {
            iayScenario = new InntektArbeidYtelseScenario();
        }
        return iayScenario;
    }

    private FagsakRepository mockFagsakRepository() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            Long id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        return fagsakRepository;
    }

    private BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider); // NOSONAR //$NON-NLS-1$
        return mockBehandlingRepository;
    }

    private void build(BehandlingRepository behandlingRepo, RepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        leggTilAksjonspunkter(behandling, repositoryProvider);

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagreInntektArbeidYtelse(lagreIay);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreTilleggsScenarier(repositoryProvider);

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private void lagreInntektArbeidYtelse(LagreInntektArbeidYtelse lagreIay) {
        if (iayScenario != null) {
            var oppgittOpptjeningBuilder = iayScenario.initOppgittOpptjeningBuilder();
            oppgittOpptjeningBuilder.ifPresent(b -> lagreIay.lagreOppgittOpptjening(behandling.getId(), b));

            var iayAggregat = iayScenario.initInntektArbeidYtelseAggregatBuilder();
            iayAggregat.ifPresent(a -> lagreIay.lagreInntektArbeidYtelseAggregat(behandling.getId(), a));

        }
    }

    private void leggTilAksjonspunkter(Behandling behandling, RepositoryProvider repositoryProvider) {
        aksjonspunktDefinisjoner.forEach(
            (apDef, stegType) -> {
                new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef, stegType);
            });
    }

    private Builder grunnBuild(RepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        return behandlingBuilder;

    }

    protected void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(RepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        Behandlingsresultat behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
            : behandlingresultatBuilder).buildFor(behandling);

        VilkårResultat.Builder inngangsvilkårBuilder = VilkårResultat
            .builderFraEksisterende(behandlingsresultat.getVilkårResultat())
            .medVilkårResultatType(vilkårResultatType);

        VilkårResultat vilkårResultat = inngangsvilkårBuilder.buildFor(behandling);

        repoProvider.getBehandlingRepository().lagre(vilkårResultat, lås);

    }

    private void lagreTilleggsScenarier(RepositoryProvider repositoryProvider) {
        testScenarioTilleggListe.forEach(tillegg -> tillegg.lagre(behandling, repositoryProvider));
    }

    interface LagreInntektArbeidYtelse {
        void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder builder);

        void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder);
    }
}
