package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
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

    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, FamilieHendelseGrunnlagEntitet> familieHendelseAggregatMap = new IdentityHashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private Behandling behandling;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private BehandlingStegType startSteg;

    private final Long fagsakId = nyId();
    private final BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    private BehandlingRepository mockBehandlingRepository;

    private IAYRepositoryProvider repositoryProvider;

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
        var behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        var mockFagsakRepository = mockFagsakRepository();
        var mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        var søknadRepository = mockSøknadRepository();
        var mottatteDokumentRepository = mockMottatteDokumentRepository();
        var opptjeningRepository = Mockito.mock(OpptjeningRepository.class);
        var familieHendelseRepository = mockFamilieHendelseGrunnlagRepository();
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
        return Mockito.mock(MottatteDokumentRepository.class);
    }

    private FamilieHendelseRepository mockFamilieHendelseGrunnlagRepository() {
        return new FamilieHendelseRepository() {
            @Override
            public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
                return familieHendelseAggregatMap.get(behandlingId);
            }

            @Override
            public Optional<FamilieHendelseGrunnlagEntitet> hentAggregatHvisEksisterer(Long behandlingId) {
                return familieHendelseAggregatMap.entrySet().stream().filter(e -> Objects.equals(behandlingId, e.getKey())).map(Map.Entry::getValue)
                        .findFirst();
            }

            @Override
            public void lagreSøknadHendelse(Long behandlingId, FamilieHendelseBuilder hendelseBuilder) {
                var kladd = hentAggregatHvisEksisterer(behandlingId);
                var builder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd)
                    .medSøknadVersjon(hendelseBuilder)
                    .medBekreftetVersjon(null)
                    .medOverstyrtVersjon(null);
                familieHendelseAggregatMap.remove(behandlingId);
                familieHendelseAggregatMap.put(behandlingId, builder.build());
            }

            @Override
            public void lagreRegisterHendelse(Long behandlingId, FamilieHendelseBuilder hendelse) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void lagreOverstyrtHendelse(Long behandlingId, FamilieHendelseBuilder hendelse) {
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
            public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long gammelBehandlingId, Long nyBehandlingId) {
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
        var behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        when(behandlingRepository.hentBehandling(ArgumentMatchers.any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(ArgumentMatchers.any())).thenAnswer(a -> List.copyOf(behandlingMap.values()));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(ArgumentMatchers.any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values()
                        .stream()
                        .filter(b -> b.getFagsakId().equals(id) && b.getBehandlingsresultat() != null && !b.getBehandlingsresultat()
                            .isBehandlingHenlagt())
                            .sorted()
                            .findFirst();
                });

        var behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId());
        });

        when(behandlingRepository.lagre(behandlingCaptor.capture(), ArgumentMatchers.any()))
                .thenAnswer((Answer<Long>) invocation -> {
                    Behandling beh = invocation.getArgument(0);
                    var id = beh.getId();
                    if (id == null) {
                        id = nyId();
                        beh.setId(id);
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

    public FagsakRepository mockFagsakRepository() {
        var fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.hentForBruker(ArgumentMatchers.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));

        var fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0);
            var id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                fagsak.setId(id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            fagsak.setStatus(status);
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
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        validerTilstandVedMocking();

        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider);
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    private void validerTilstandVedMocking() {
        if (startSteg != null) {
            throw new IllegalArgumentException(
                    "Kan ikke sette startSteg ved mocking siden dette krever Kodeverk.  Bruk ManipulerInternBehandling til å justere etterpå.");
        }
    }

    private void build(BehandlingRepository behandlingRepo, IAYRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        var behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        var lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);

        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Builder grunnBuild(IAYRepositoryProvider repositoryProvider) {
        var fagsakRepo = repositoryProvider.getFagsakRepository();

        if (fagsak == null) {
            lagFagsak(fagsakRepo);
        }

        // oppprett og lagre behandling
        return Behandling.nyBehandlingFor(fagsak, behandlingType);

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            var entityManager = fagsakRepo.getEntityManager();
            if (entityManager != null) {
                var brukerRepository = new NavBrukerRepository(entityManager);
                var navBruker = brukerRepository.hent(fagsakBuilder.getBrukerBuilder().getAktørId())
                    .orElseGet(() -> NavBruker.opprettNy(fagsakBuilder.getBrukerBuilder().getAktørId(), Språkkode.NB));
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        var fagsakId = fagsakRepo.opprettNy(fagsak);
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(IAYRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().buildFor(behandling);
        repoProvider.getBehandlingRepository().lagre(behandlingsresultat.getVilkårResultat(), lås);
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

    private final class MockPersonopplysningRepository extends PersonopplysningRepository {
        @Override
        public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long eksisterendeBehandlingId, Long nyBehandlingId) {
            var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(eksisterendeBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
            return Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
            if (personopplysningMap.isEmpty() || personopplysningMap.get(behandlingId) == null || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null);
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                oppdatere.medOverstyrtVersjon(builder);
            }
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public void lagre(Long behandlingId, OppgittAnnenPartEntitet oppgittAnnenPart) {
            var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            oppdatere.medOppgittAnnenPart(oppgittAnnenPart);
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
            var grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
                    PersonopplysningVersjonType.REGISTRERT);
        }

        @Override
        public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
            var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
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
