package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
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
abstract class AbstractIAYTestScenario<S extends AbstractIAYTestScenario<S>> {

    public static final String ADOPSJON = "adopsjon";
    public static final String FØDSEL = "fødsel";
    public static final String TERMINBEKREFTELSE = "terminbekreftelse";
    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, FamilieHendelseGrunnlagEntitet> familieHendelseAggregatMap = new IdentityHashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private InntektArbeidYtelseScenario iayScenario;
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private OppgittAnnenPartBuilder oppgittAnnenPartBuilder;
    private BehandlingStegType startSteg;

    private Long fagsakId = nyId();
    private String behandlendeEnhet;
    private BehandlingRepository mockBehandlingRepository;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    private Behandling originalBehandling;
    private BehandlingÅrsakType behandlingÅrsakType;
    private IAYRepositoryProvider repositoryProvider;
    private PersonInformasjon.Builder personInformasjonBuilder;

    protected AbstractIAYTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBrukerKjønn(kjønn);
    }

    protected AbstractIAYTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBruker navBruker) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBruker(navBruker);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(IAYRepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        FagsakRepository mockFagsakRepository = mockFagsakRepository();
        PersonopplysningRepository mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        SøknadRepository søknadRepository = mockSøknadRepository();
        MottatteDokumentRepository mottatteDokumentRepository = mockMottatteDokumentRepository();
        OpptjeningRepository opptjeningRepository = Mockito.mock(OpptjeningRepository.class);
        FamilieHendelseRepository familieHendelseRepository = mockFamilieHendelseGrunnlagRepository();
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode,
        // forhindrer feil oppsett, så det
        // blir enklere å refactorere

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mockPersonopplysningRepository);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        when(repositoryProvider.getOpptjeningRepository()).thenReturn(opptjeningRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        return behandlingRepository;
    }

    private SøknadRepository mockSøknadRepository() {
        return new SøknadRepository() {

            private SøknadEntitet søknad;

            @Override
            public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(søknad);
            }

            @Override
            public SøknadEntitet hentFørstegangsSøknad(Behandling behandling) {
                return søknad;
            }

            @Override
            public SøknadEntitet hentSøknad(Long behandlingId) {
                return søknad;
            }

            @Override
            public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad1) {
                this.søknad = søknad1;
            }

        };
    }

    private MottatteDokumentRepository mockMottatteDokumentRepository() {
        MottatteDokumentRepository dokumentRepository = Mockito.mock(MottatteDokumentRepository.class);
        return dokumentRepository;
    }

    private FamilieHendelseRepository mockFamilieHendelseGrunnlagRepository() {
        return new FamilieHendelseRepository() {
            @Override
            public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
                return familieHendelseAggregatMap.get(behandlingId);
            }

            @Override
            public Optional<FamilieHendelseGrunnlagEntitet> hentAggregatHvisEksisterer(Long behandlingId) {
                return familieHendelseAggregatMap.entrySet().stream().filter(e -> Objects.equals(behandlingId, e.getKey())).map(e -> e.getValue())
                        .findFirst();
            }

            public DiffResult diffResultat(FamilieHendelseGrunnlagEntitet grunnlag1, FamilieHendelseGrunnlagEntitet grunnlag2,
                    boolean kunSporedeEndringer) {
                return null;
            }

            @Override
            public void lagre(Behandling behandling, FamilieHendelseBuilder hendelseBuilder) {
                Long behandlingId = behandling.getId();
                var kladd = hentAggregatHvisEksisterer(behandlingId);
                var builder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                var type = utledTypeFor(kladd);
                switch (type) {
                    case SØKNAD:
                        builder.medSøknadVersjon(hendelseBuilder);
                        break;
                    case BEKREFTET:
                        builder.medBekreftetVersjon(hendelseBuilder);
                        break;
                    case OVERSTYRT:
                        builder.medOverstyrtVersjon(hendelseBuilder);
                        break;
                    default:
                        throw new UnsupportedOperationException("Støtter ikke HendelseVersjonType:" + type);
                }
                familieHendelseAggregatMap.remove(behandlingId);
                familieHendelseAggregatMap.put(behandlingId, builder.build());
            }

            @Override
            public void lagreRegisterHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void slettAvklarteData(Long behandlingId, BehandlingLås lås) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public Optional<Long> hentIdPåAktivFamiliehendelse(Long behandlingId) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public FamilieHendelseGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long gammelBehandlingId, Long nyBehandlingId) {
                throw new UnsupportedOperationException();
            }

            private HendelseVersjonType utledTypeFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
                if (aggregat.isPresent()) {
                    if (aggregat.get().getHarOverstyrteData()) {
                        return HendelseVersjonType.OVERSTYRT;
                    } else if (aggregat.get().getHarBekreftedeData() || (aggregat.get().getSøknadVersjon() != null)) {
                        return HendelseVersjonType.BEKREFTET;
                    } else if (aggregat.get().getSøknadVersjon() == null) {
                        return HendelseVersjonType.SØKNAD;
                    }
                    throw new IllegalStateException();
                }
                return HendelseVersjonType.SØKNAD;
            }

            @Override
            public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    public BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(IAYRepositoryProvider.class);
        BehandlingRepository behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        when(behandlingRepository.hentBehandling(ArgumentMatchers.any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(ArgumentMatchers.any())).thenAnswer(a -> {
            return List.copyOf(behandlingMap.values());
        });
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(ArgumentMatchers.any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values().stream()
                            .filter(b -> b.getFagsakId().equals(id) && (b.getBehandlingsresultat() != null)
                                    && !b.getBehandlingsresultat().isBehandlingHenlagt())
                            .sorted()
                            .findFirst();
                });

        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        when(behandlingRepository.lagre(behandlingCaptor.capture(), ArgumentMatchers.any()))
                .thenAnswer((Answer<Long>) invocation -> {
                    Behandling beh = invocation.getArgument(0);
                    Long id = beh.getId();
                    if (id == null) {
                        id = nyId();
                        beh.setId(id);
                        // Whitebox.setInternalState(beh, "id", id);
                    }

                    beh.getAksjonspunkter().forEach(punkt -> punkt.setId(nyId()));
                    behandlingMap.put(id, beh);
                    return id;
                });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    private PersonopplysningRepository lagMockPersonopplysningRepository() {
        return new MockPersonopplysningRepository();
    }

    private InntektArbeidYtelseScenario getIayScenario() {
        if (iayScenario == null) {
            iayScenario = new InntektArbeidYtelseScenario();
        }
        return iayScenario;
    }

    public FagsakRepository mockFagsakRepository() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.hentForBruker(ArgumentMatchers.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));

        ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            Long id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                fagsak.setId(id);
                // Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            fagsak.setStatus(status);
            // Whitebox.setInternalState(fagsak, "fagsakStatus", status);
            return null;
        }).when(fagsakRepository)
                .oppdaterFagsakStatus(eq(fagsakId), ArgumentMatchers.any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Behandling lagre(IAYRepositoryProvider repositoryProvider) {
        build(repositoryProvider.getBehandlingRepository(), repositoryProvider);
        return behandling;
    }

    BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if ((mockBehandlingRepository != null) && (behandling != null)) {
            return mockBehandlingRepository;
        }
        validerTilstandVedMocking();

        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider); // NOSONAR
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    private void lagrePersonopplysning(IAYRepositoryProvider repositoryProvider, Behandling behandling) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();
        if (oppgittAnnenPartBuilder != null) {
            personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder);
        }

    }

    private void validerTilstandVedMocking() {
        if (startSteg != null) {
            throw new IllegalArgumentException(
                    "Kan ikke sette startSteg ved mocking siden dette krever Kodeverk.  Bruk ManipulerInternBehandling til å justere etterpå.");
        }
    }

    @SuppressWarnings("unchecked")
    public S medSøknadDato(LocalDate søknadsdato) {
        medSøknad().medSøknadsdato(søknadsdato);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
        return (S) this;
    }

    private void build(BehandlingRepository behandlingRepo, IAYRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        // FIXME: Fjern avh til startSteg
        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagrePersonopplysning(repositoryProvider, behandling);
        if (iayScenario != null) {
            iayScenario.lagreVirksomhet();
            iayScenario.lagreOppgittOpptjening(repositoryProvider, behandling);
            iayScenario.lagreOpptjening(repositoryProvider, behandling);
        }
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);

        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Builder grunnBuild(IAYRepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        if (fagsak == null) {
            lagFagsak(fagsakRepo);
        }

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                    .medBehandlingÅrsak(BehandlingÅrsak.builder(behandlingÅrsakType).medOriginalBehandlingId(originalBehandling.getId()));
        }

        if (behandlendeEnhet != null) {
            behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            final EntityManager entityManager = fagsakRepo.getEntityManager();
            if (entityManager != null) {
                NavBrukerRepository brukerRepository = new NavBrukerRepository(entityManager);
                final NavBruker navBruker = brukerRepository.hent(fagsakBuilder.getBrukerBuilder().getAktørId())
                        .orElseGet(() -> NavBruker.opprettNy(fagsakBuilder.getBrukerBuilder().getAktørId(), Språkkode.NB));
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak);
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(IAYRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        Behandlingsresultat behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
                : behandlingresultatBuilder).buildFor(behandling);

        repoProvider.getBehandlingRepository().lagre(behandlingsresultat.getVilkårResultat(), lås);

    }

    public AktørId getDefaultBrukerAktørId() {
        return fagsakBuilder.getBrukerBuilder().getAktørId();
    }

    public Behandling getBehandling() {
        if (behandling == null) {
            throw new IllegalStateException("Kan ikke hente Behandling før denne er bygd");
        }
        return behandling;
    }

    @SuppressWarnings("unchecked")
    public S medSaksnummer(Saksnummer saksnummer) {
        fagsakBuilder.medSaksnummer(saksnummer);
        return (S) this;
    }

    public OppgittAnnenPartBuilder medSøknadAnnenPart() {
        if (oppgittAnnenPartBuilder == null) {
            oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder();
        }
        return oppgittAnnenPartBuilder;
    }

    public SøknadEntitet.Builder medSøknad() {
        if (søknadBuilder == null) {
            søknadBuilder = new SøknadEntitet.Builder();
        }
        return søknadBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId) {
        fagsakBuilder
                .medBrukerAktørId(aktørId);
        return (S) this;
    }

    /** @deprecated Skal ikke ha kjennskap til steg tilstand i denne modulen. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    public PersonInformasjon.Builder opprettBuilderForRegisteropplysninger() {
        if (personInformasjonBuilder == null) {
            personInformasjonBuilder = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT);
        }
        return personInformasjonBuilder;
    }

    public InntektArbeidYtelseScenario.InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        return getIayScenario().getInntektArbeidYtelseScenarioTestBuilder();
    }

    @SuppressWarnings("unchecked")
    public S medOppgittOpptjening(OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        getIayScenario().medOppgittOpptjening(oppgittOpptjeningBuilder);
        return (S) this;
    }

    private final class MockPersonopplysningRepository extends PersonopplysningRepository {
        @Override
        public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(eksisterendeBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
            return Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
            if (personopplysningMap.isEmpty() || (personopplysningMap.get(behandlingId) == null) || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null);
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                oppdatere.medOverstyrtVersjon(builder);
            }
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public void lagre(Long behandlingId, OppgittAnnenPartBuilder oppgittAnnenPart) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            oppdatere.medOppgittAnnenPart(oppgittAnnenPart.build());
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForOverstyring(Long behandlingId) {
            final Optional<PersonopplysningGrunnlagEntitet> grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getOverstyrtVersjon),
                    PersonopplysningVersjonType.OVERSTYRT);
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
            final Optional<PersonopplysningGrunnlagEntitet> grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
                    PersonopplysningVersjonType.REGISTRERT);
        }

        @Override
        public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(gammelBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }
    }
}
