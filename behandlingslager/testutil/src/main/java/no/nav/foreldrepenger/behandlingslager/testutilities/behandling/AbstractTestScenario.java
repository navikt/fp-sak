package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRegistrertEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personstatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Statsborgerskap;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
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
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {

    public static final String ADOPSJON = "adopsjon";
    public static final String FØDSEL = "fødsel";
    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private static final String IKKE_IMPLEMENTERT = "Ikke implementert";
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, FamilieHendelseGrunnlagEntitet> familieHendelseAggregatMap = new IdentityHashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private final ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
    private final ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private OppgittAnnenPartBuilder oppgittAnnenPartBuilder;
    private BehandlingVedtak.Builder behandlingVedtakBuilder;
    private MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder;
    private BehandlingStegType startSteg;

    private final Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new EnumMap<>(AksjonspunktDefinisjon.class);
    private final Map<VilkårType, VilkårUtfallType> vilkårTyper = new EnumMap<>(VilkårType.class);
    private final List<MedlemskapPerioderEntitet> medlemskapPerioder = new ArrayList<>();
    private Long fagsakId = nyId();
    private LocalDate behandlingstidFrist;
    private LocalDateTime opplysningerOppdatertTidspunkt;
    private String behandlendeEnhet;
    private BehandlingRepository mockBehandlingRepository;
    private FamilieHendelseBuilder søknadHendelseBuilder;
    private FamilieHendelseBuilder bekreftetHendelseBuilder;
    private FamilieHendelseBuilder overstyrtHendelseBuilder;
    private BehandlingVedtak behandlingVedtak;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittRettighetEntitet overstyrtRettighet;
    private Dekningsgrad oppgittDekningsgrad;
    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private AvklarteUttakDatoerEntitet avklarteUttakDatoer;

    // Registret og overstyrt personinfo
    private List<PersonInformasjon> personer;

    private Behandling originalBehandling;
    private List<BehandlingÅrsakType> behandlingÅrsakTyper;
    private BehandlingRepositoryProvider repositoryProvider;
    private PersonInformasjon.Builder personInformasjonBuilder;
    private UttakResultatPerioderEntitet uttak;
    private Stønadskontoberegning stønadskontoberegning;
    private boolean manueltOpprettet;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBrukerKjønn(kjønn);
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBrukerKjønn kjønn, AktørId aktørId) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBruker(new NavBrukerBuilder().medAktørId(aktørId).medKjønn(kjønn).build());
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBruker navBruker) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBruker(navBruker);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(BehandlingRepositoryProvider repositoryProvider) {
        var behandlingRepository = mock(BehandlingRepository.class);

        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        var mockFagsakRepository = mockFagsakRepository();
        var mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        var mockMedlemskapRepository = lagMockMedlemskapRepository();
        var familieHendelseRepository = mockFamilieHendelseGrunnlagRepository();
        var søknadRepository = mockSøknadRepository();
        var fagsakLåsRepository = mockFagsakLåsRepository();
        var fagsakRelasjonRepositoryMock = mockFagsakRelasjonRepository();
        var resultatRepository = mockBehandlingresultatRepository();
        var opptjeningRepository = mockOpptjeningRepository();
        var historikkinnslag2Repository = mockHistorikkinnslag2Repository();

        var behandlingLåsReposiory = mockBehandlingLåsRepository();

        var behandlingVedtakRepository = mockBehandlingVedtakRepository();
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode,
        // forhindrer feil oppsett, så det
        // blir enklere å refactorere

        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        lenient().when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mockPersonopplysningRepository);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);
        lenient().when(repositoryProvider.getMedlemskapRepository()).thenReturn(mockMedlemskapRepository);
        lenient().when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        lenient().when(repositoryProvider.getBehandlingVedtakRepository()).thenReturn(behandlingVedtakRepository);
        lenient().when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        lenient().when(repositoryProvider.getBehandlingLåsRepository()).thenReturn(behandlingLåsReposiory);
        lenient().when(repositoryProvider.getFagsakRelasjonRepository()).thenReturn(fagsakRelasjonRepositoryMock);
        lenient().when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(resultatRepository);
        lenient().when(repositoryProvider.getOpptjeningRepository()).thenReturn(opptjeningRepository);
        lenient().when(repositoryProvider.getHistorikkinnslag2Repository()).thenReturn(historikkinnslag2Repository);

        return behandlingRepository;
    }

    private Historikkinnslag2Repository mockHistorikkinnslag2Repository() {
        return new Historikkinnslag2Repository() {

            private final List<Historikkinnslag2> historikkinnslagListe = new ArrayList<>();

            @Override
            public void lagre(Historikkinnslag2 historikkinnslag) {
                historikkinnslagListe.add(historikkinnslag);
            }

            @Override
            public List<Historikkinnslag2> hent(Saksnummer saksnummer) {
                return historikkinnslagListe;
            }
        };
    }

    private OpptjeningRepository mockOpptjeningRepository() {
        return mock(OpptjeningRepository.class);
    }

    private BehandlingsresultatRepository mockBehandlingresultatRepository() {
        return new BehandlingsresultatRepository() {
            @Override
            public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(mockBehandlingRepository.hentBehandling(behandlingId).getBehandlingsresultat());
            }

            @Override
            public Behandlingsresultat hent(Long behandlingId) {
                return hentHvisEksisterer(behandlingId).orElseThrow();
            }
        };
    }

    private BehandlingLåsRepository mockBehandlingLåsRepository() {
        return new BehandlingLåsRepository() {

            @Override
            public BehandlingLås taLås(Long behandlingId) {
                return new BehandlingLås(behandlingId);
            }

            @Override
            public void oppdaterLåsVersjon(BehandlingLås behandlingLås) {
                // Brukes ikke i test
            }

            @Override
            public BehandlingLås taLås(UUID eksternBehandlingRef) {
                return null;
            }
        };
    }

    private FagsakLåsRepository mockFagsakLåsRepository() {
        return new FagsakLåsRepository() {
            @Override
            public FagsakLås taLås(Long fagsakId) {
                return new FagsakLås(fagsakId) {

                };
            }

            @Override
            public FagsakLås taLås(Fagsak fagsak) {
                return new FagsakLås(fagsak.getId()) {

                };
            }

            @Override
            public void oppdaterLåsVersjon(FagsakLås fagsakLås) {
                // Brukes ikke i test
            }
        };
    }

    private FagsakRelasjonRepository mockFagsakRelasjonRepository() {
        return new FagsakRelasjonRepositoryStub();
    }

    private BehandlingVedtakRepository mockBehandlingVedtakRepository() {
        var behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);
        var behVedtak = mockBehandlingVedtak();
        lenient().when(behandlingVedtakRepository.hentForBehandlingHvisEksisterer(any())).thenReturn(Optional.of(behVedtak));

        return behandlingVedtakRepository;
    }

    public BehandlingVedtak mockBehandlingVedtak() {
        if (behandlingVedtak == null) {
            behandlingVedtak = Mockito.mock(BehandlingVedtak.class);
        }
        return behandlingVedtak;
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
            public void lagre(Long behandlingId, FamilieHendelseBuilder hendelseBuilder) {
                var kladd = hentAggregatHvisEksisterer(behandlingId);
                var builder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                var type = utledTypeForMock(kladd);
                switch (type) {
                    case SØKNAD -> builder.medSøknadVersjon(hendelseBuilder);
                    case BEKREFTET -> builder.medBekreftetVersjon(hendelseBuilder);
                    case OVERSTYRT -> builder.medOverstyrtVersjon(hendelseBuilder);
                    default -> throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
                }
                familieHendelseAggregatMap.remove(behandlingId);
                familieHendelseAggregatMap.put(behandlingId, builder.build());
            }

            @Override
            public void lagreRegisterHendelse(Long behandlingId, FamilieHendelseBuilder hendelse) {
                var kladd = hentAggregatHvisEksisterer(behandling.getId());
                var aggregatBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                aggregatBuilder.medBekreftetVersjon(hendelse);
                try {
                    var m = FamilieHendelseGrunnlagBuilder.class.getDeclaredMethod("getKladd");
                    var invoke = (FamilieHendelseGrunnlagEntitet) m.invoke(aggregatBuilder);
                    if (harOverstyrtTerminOgOvergangTilFødselMock(invoke)) {
                        aggregatBuilder.medOverstyrtVersjon(null);
                    }
                    var id = behandling.getId();
                    familieHendelseAggregatMap.remove(id);
                    familieHendelseAggregatMap.put(id, aggregatBuilder.build());
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }

            private boolean harOverstyrtTerminOgOvergangTilFødselMock(FamilieHendelseGrunnlagEntitet kladd) {
                return kladd.getHarOverstyrteData() && kladd.getOverstyrtVersjon()
                        .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT).equals(FamilieHendelseType.TERMIN)
                        && kladd.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT)
                                .equals(FamilieHendelseType.FØDSEL);
            }

            @Override
            public void lagreOverstyrtHendelse(Long behandlingId, FamilieHendelseBuilder hendelse) {
                var kladd = hentAggregatHvisEksisterer(behandling.getId());
                var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                oppdatere.medOverstyrtVersjon(hendelse);
                familieHendelseAggregatMap.remove(behandling.getId());
                familieHendelseAggregatMap.put(behandling.getId(), oppdatere.build());
            }

            @Override
            public void slettAvklarteData(Long behandlingId, BehandlingLås lås) {
                throw new UnsupportedOperationException(IKKE_IMPLEMENTERT);
            }

            @Override
            public Optional<Long> hentIdPåAktivFamiliehendelse(Long behandlingId) {
                throw new UnsupportedOperationException(IKKE_IMPLEMENTERT);
            }

            @Override
            public FamilieHendelseGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
                throw new UnsupportedOperationException(IKKE_IMPLEMENTERT);
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
                var familieHendelseAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
                var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);

                familieHendelseAggregatMap.remove(nyBehandlingId);
                familieHendelseAggregatMap.put(nyBehandlingId, oppdatere.build());
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long gammelBehandlingId, Long nyBehandlingId) {
                var familieHendelseAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
                var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
                oppdatere.medOverstyrtVersjon(null);
                oppdatere.medBekreftetVersjon(null);

                familieHendelseAggregatMap.remove(nyBehandlingId);
                familieHendelseAggregatMap.put(nyBehandlingId, oppdatere.build());
            }

            @Override
            public FamilieHendelseBuilder opprettBuilderFor(Long behandlingId, boolean register) {
                var aggregatBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(hentAggregatHvisEksisterer(behandlingId));
                return opprettBuilderFor(aggregatBuilder);
            }

            FamilieHendelseBuilder opprettBuilderFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
                var type = utledTypeForMock(aggregat);

                if (type.equals(HendelseVersjonType.SØKNAD)) {
                    return FamilieHendelseBuilder.oppdatere(aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon), type);
                }
                if (type.equals(HendelseVersjonType.BEKREFTET)) {
                    return FamilieHendelseBuilder.oppdatere(aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getBekreftetVersjon), type);
                }
                return FamilieHendelseBuilder.oppdatere(aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getOverstyrtVersjon), type);
            }

            private HendelseVersjonType utledTypeForMock(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
                if (aggregat.isPresent()) {
                    if (aggregat.get().getHarOverstyrteData()) {
                        return HendelseVersjonType.OVERSTYRT;
                    }
                    if (aggregat.get().getHarBekreftedeData() || aggregat.get().getSøknadVersjon() != null) {
                        return HendelseVersjonType.BEKREFTET;
                    }
                    if (aggregat.get().getSøknadVersjon() == null) {
                        return HendelseVersjonType.SØKNAD;
                    }
                    throw new IllegalStateException();
                }
                return HendelseVersjonType.SØKNAD;
            }

            private FamilieHendelseBuilder opprettBuilderFor(FamilieHendelseGrunnlagBuilder aggregatBuilder) {
                try {
                    var m = FamilieHendelseGrunnlagBuilder.class.getDeclaredMethod("getKladd");
                    m.setAccessible(true);
                    return opprettBuilderFor(Optional.ofNullable((FamilieHendelseGrunnlagEntitet) m.invoke(aggregatBuilder)));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
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
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        var behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        lenient().when(behandlingRepository.hentBehandling(any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        lenient().when(behandlingRepository.hentBehandlingReadOnly(any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        lenient().when(behandlingRepository.hentBehandling(any(UUID.class))).thenAnswer(a -> behandlingMap.entrySet().stream().filter(e -> {
            UUID uuid = a.getArgument(0);
            return Objects.equals(e.getValue().getUuid(), uuid);
        }).findFirst().map(Map.Entry::getValue).orElseThrow());
        lenient().when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(any())).thenAnswer(a -> List.copyOf(behandlingMap.values()));
        lenient().when(behandlingRepository.finnUnikBehandlingForBehandlingId(any())).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return Optional.ofNullable(behandlingMap.getOrDefault(id, null));
        });
        lenient().when(behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(any(), any(BehandlingType.class)))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    BehandlingType type = a.getArgument(1);
                    return behandlingMap.values().stream().filter(b -> type.equals(b.getType()) && b.getFagsakId().equals(id)).sorted().findFirst();
                });
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values().stream().filter(b -> BehandlingType.getYtelseBehandlingTyper().contains(b.getType()))
                            .filter(b -> b.getFagsakId().equals(id)).sorted().findFirst();
                });
        lenient().when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values().stream()
                            .filter(b -> b.getFagsakId().equals(id) && b.getBehandlingsresultat() != null
                                    && !b.getBehandlingsresultat().isBehandlingHenlagt())
                            .sorted()
                            .findFirst();
                });

        lenient().when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        lenient().when(behandlingRepository.hentSistOppdatertTidspunkt(any()))
                .thenAnswer(a -> Optional.ofNullable(opplysningerOppdatertTidspunkt));

        lenient().when(behandlingRepository.lagre(behandlingCaptor.capture(), any()))
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

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return repositoryProvider;
    }

    private MedlemskapRepository lagMockMedlemskapRepository() {
        var dummy = new MedlemskapRepository(null) {

            private MedlemskapBehandlingsgrunnlagEntitet grunnlag;

            @Override
            public void lagreOgFlush(Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag,
                                     MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag) {
                grunnlag = nyttGrunnlag;
            }

            @Override
            public void lagreMedlemskapRegistrert(MedlemskapRegistrertEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            public void lagreOppgittTilknytning(MedlemskapOppgittTilknytningEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            protected BehandlingLås taLås(Long behandlingId) {
                return null;
            }

            @Override
            protected void oppdaterLås(BehandlingLås lås) {
                // NO-OP i mock
            }

            @Override
            protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
                assert behandlingId != null : "behandlingId er null!";
                return Optional.ofNullable(grunnlag);
            }
        };
        return Mockito.spy(dummy);
    }

    private PersonopplysningRepository lagMockPersonopplysningRepository() {
        return new MockPersonopplysningRepository();
    }

    public FagsakRepository mockFagsakRepository() {
        var fagsakRepository = mock(FagsakRepository.class);
        lenient().when(fagsakRepository.finnEksaktFagsak(anyLong())).thenAnswer(a -> fagsak);
        lenient().when(fagsakRepository.finnUnikFagsak(anyLong())).thenAnswer(a -> Optional.of(fagsak));
        lenient().when(fagsakRepository.hentSakGittSaksnummer(any(Saksnummer.class))).thenAnswer(a -> Optional.of(fagsak));
        lenient().when(fagsakRepository.hentForBruker(any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));
        lenient().when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fsak = invocation.getArgument(0);
            var id = fsak.getId();
            if (id == null) {
                id = fagsakId;
                fsak.setId(id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.lenient().doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            fagsak.setStatus(status);
            return null;
        }).when(fagsakRepository)
                .oppdaterFagsakStatus(eq(fagsakId), any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Fagsak lagreFagsak(BehandlingRepositoryProvider repositoryProvider) {
        lagFagsak(repositoryProvider.getFagsakRepository());
        return fagsak;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {
        build(repositoryProvider);
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

    protected Behandling buildAvsluttet(BehandlingRepositoryProvider repositoryProvider) {
        var behandlingBuilder = grunnBuild(repositoryProvider);

        behandling = behandlingBuilder.medAvsluttetDato(LocalDateTime.now()).build();
        var behandlingRepo = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagrePersonopplysning(repositoryProvider, behandling);
        behandling.setStatus(BehandlingStatus.AVSLUTTET);

        var builder = Behandlingsresultat.builder();

        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreUttak(repositoryProvider.getFpUttakRepository());
        builder.medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
                .medAvslagsårsak(Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR).buildFor(behandling);

        behandlingRepo.lagre(behandling, lås);
        return behandling;
    }

    private void lagrePersonopplysning(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        var behandlingId = behandling.getId();
        if (oppgittAnnenPartBuilder != null) {
            personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder.build());
        }

        if (personer != null && !personer.isEmpty()) {
            personer.stream().filter(e -> e.getType().equals(PersonopplysningVersjonType.REGISTRERT))
                    .findFirst().ifPresent(e -> lagrePersoninfo(behandling, e, personopplysningRepository));

            personer.stream().filter(a -> a.getType().equals(PersonopplysningVersjonType.OVERSTYRT))
                    .findFirst().ifPresent(b -> {
                        throw new IllegalArgumentException("Overstyrt personinfo er kun legacy");
                    });

        } else {
            var registerInformasjon = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT)
                    .leggTilPersonopplysninger(
                            Personopplysning.builder()
                                    .aktørId(behandling.getAktørId())
                                    .navn("Forelder")
                                    .brukerKjønn(getKjønnFraFagsak())
                                    .fødselsdato(LocalDate.now().minusYears(25))
                                    .sivilstand(SivilstandType.UOPPGITT))
                    .leggTilPersonstatus(new Personstatus(behandling.getAktørId(),
                        new PersonstatusPeriode(Gyldighetsperiode.innenfor(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)),
                            PersonstatusType.BOSA)))
                    .leggTilStatsborgerskap(new Statsborgerskap(behandling.getAktørId(),
                        new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(LocalDate.now().minusYears(20), LocalDate.now().plusYears(10)), Landkoder.NOR)))
                    .build();
            lagrePersoninfo(behandling, registerInformasjon, personopplysningRepository);
        }
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        Objects.requireNonNull(behandling);
        Objects.requireNonNull(personInformasjon);

        if (PersonopplysningVersjonType.REGISTRERT.equals(personInformasjon.getType())) {
            lagreRegisterPersoninfo(behandling, personInformasjon, repository);
        } else {
            throw new IllegalArgumentException("Overstyrt personopplysning er legacy");
        }
    }

    private void lagreRegisterPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForRegisterdata(behandling.getId()), personInformasjon, repository);
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjon personInformasjon,
            PersonopplysningRepository repository) {
        personInformasjon.getPersonopplysninger().forEach(e -> {
            var builder = personInformasjonBuilder.getPersonopplysningBuilder(e.getAktørId());
            builder.medNavn(e.getNavn())
                    .medFødselsdato(e.getFødselsdato())
                    .medDødsdato(e.getDødsdato())
                    .medKjønn(e.getBrukerKjønn())
                    .medSivilstand(e.getSivilstand());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getAdresser().forEach(e -> {
            var builder = personInformasjonBuilder.getAdresseBuilder(e.aktørId(),
                DatoIntervallEntitet.fraOgMedTilOgMed(e.adressePeriode().gyldighetsperiode().fom(), e.adressePeriode().gyldighetsperiode().tom()),
                    e.adressePeriode().adresse().getAdresseType());
            builder.medAdresselinje1(e.adressePeriode().adresse().getAdresselinje1())
                    .medAdresselinje2(e.adressePeriode().adresse().getAdresselinje2())
                    .medAdresselinje3(e.adressePeriode().adresse().getAdresselinje3())
                    .medAdresselinje4(e.adressePeriode().adresse().getAdresselinje4())
                    .medLand(e.adressePeriode().adresse().getLand())
                    .medPostnummer(e.adressePeriode().adresse().getPostnummer())
                    .medPoststed(e.adressePeriode().adresse().getPoststed());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getPersonstatuser().forEach(e -> {
            var builder = personInformasjonBuilder.getPersonstatusBuilder(e.aktørId(),
                DatoIntervallEntitet.fraOgMedTilOgMed(e.personstatusPeriode().gyldighetsperiode().fom(), e.personstatusPeriode().gyldighetsperiode().tom()));
            builder.medPersonstatus(e.personstatusPeriode().personstatus());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getStatsborgerskap().forEach(e -> {
            var builder = personInformasjonBuilder.getStatsborgerskapBuilder(e.aktørId(),
                DatoIntervallEntitet.fraOgMedTilOgMed(e.statsborgerskapPeriode().gyldighetsperiode().fom(), e.statsborgerskapPeriode().gyldighetsperiode().tom()),
                    e.statsborgerskapPeriode().statsborgerskap());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getRelasjoner().forEach(e -> {
            var builder = personInformasjonBuilder.getRelasjonBuilder(e.getAktørId(), e.getTilAktørId(),
                    e.getRelasjonsrolle());
            builder.harSammeBosted(e.getHarSammeBosted());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getOpphold().forEach(e -> {
            var builder = personInformasjonBuilder.getOppholdstillatelseBuilder(e.aktørId(),
                DatoIntervallEntitet.fraOgMedTilOgMed(e.oppholdstillatelsePeriode().gyldighetsperiode().fom(), e.oppholdstillatelsePeriode().gyldighetsperiode().tom()))
                .medOppholdstillatelse(e.oppholdstillatelsePeriode().tillatelse());
            personInformasjonBuilder.leggTil(builder);
        });

        repository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    protected void validerTilstandVedMocking() {
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

    private void build(BehandlingRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        var behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        var behandlingId = behandling.getId();

        opprettHendelseGrunnlag(repositoryProvider);
        lagrePersonopplysning(repositoryProvider, behandling);
        lagreMedlemskapOpplysninger(repositoryProvider, behandlingId);
        lagreYtelseFordelingOpplysninger(repositoryProvider, behandling);
        lagreSøknad(repositoryProvider);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreUttak(repositoryProvider.getFpUttakRepository());

        if (this.opplysningerOppdatertTidspunkt != null) {
            behandlingRepository.oppdaterSistOppdatertTidspunkt(this.behandling, this.opplysningerOppdatertTidspunkt);
        }

        // få med behandlingsresultat etc.
        behandlingRepository.lagre(behandling, lås);
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

    private void lagreSøknad(BehandlingRepositoryProvider repositoryProvider) {
        if (søknadBuilder != null) {
            var søknadRepository = repositoryProvider.getSøknadRepository();
            søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        }
    }

    private void lagreMedlemskapOpplysninger(BehandlingRepositoryProvider repositoryProvider, Long behandlingId) {
        repositoryProvider.getMedlemskapRepository().lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapPerioder);

        if (oppgittTilknytningBuilder != null) {
            var oppgittTilknytning = medOppgittTilknytning().build();
            repositoryProvider.getMedlemskapRepository().lagreOppgittTilkytning(behandlingId, oppgittTilknytning);
        }
    }

    private void lagreYtelseFordelingOpplysninger(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        if (oppgittRettighet == null && oppgittDekningsgrad == null && oppgittFordeling == null && overstyrtRettighet == null
            && avklarteUttakDatoer == null && justertFordeling == null) {
            return;
        }
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yf = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(oppgittRettighet)
            .medOverstyrtRettighet(overstyrtRettighet)
            .medOppgittDekningsgrad(oppgittDekningsgrad)
            .medOppgittFordeling(oppgittFordeling)
            .medJustertFordeling(justertFordeling)
            .medAvklarteDatoer(avklarteUttakDatoer);
        ytelsesFordelingRepository.lagre(behandling.getId(), yf.build());
    }

    private FamilieHendelseRepository opprettHendelseGrunnlag(BehandlingRepositoryProvider repositoryProvider) {
        var grunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        grunnlagRepository.lagre(behandling.getId(), medSøknadHendelse());
        if (bekreftetHendelseBuilder != null) {
            grunnlagRepository.lagre(behandling.getId(), bekreftetHendelseBuilder);
        }
        if (overstyrtHendelseBuilder != null) {
            grunnlagRepository.lagre(behandling.getId(), overstyrtHendelseBuilder);
        }
        resetBuilders();
        return grunnlagRepository;
    }

    private void resetBuilders() {
        medSøknadHendelse(null);
        medBekreftetHendelse(null);
        medOverstyrtHendelse(null);
    }

    private Builder grunnBuild(BehandlingRepositoryProvider repositoryProvider) {
        var fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                    .medBehandlingÅrsak(BehandlingÅrsak.builder(behandlingÅrsakTyper).medManueltOpprettet(manueltOpprettet)
                            .medOriginalBehandlingId(originalBehandling.getId()));
        }

        if (behandlingstidFrist != null) {
            behandlingBuilder.medBehandlingstidFrist(behandlingstidFrist);
        }

        if (behandlendeEnhet != null) {
            behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }

        return behandlingBuilder;

    }

    protected void lagFagsak(FagsakRepository fagsakRepo) {
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
        fagsak.setEndretTidspunkt(LocalDateTime.now());
        var fsakId = fagsakRepo.opprettNy(fagsak);
        fagsak.setId(fsakId);
    }

    private NavBrukerKjønn getKjønnFraFagsak() {
        return
            fagsakBuilder.getBrukerBuilder().getKjønn() != null ? fagsakBuilder.getBrukerBuilder().getKjønn() :
                RelasjonsRolleType.erMor(fagsakBuilder.getRolle()) || RelasjonsRolleType.erMedmor(
                    fagsakBuilder.getRolle()) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.MANN;
    }

    private void lagreBehandlingsresultatOgVilkårResultat(BehandlingRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        var behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
                : behandlingresultatBuilder).buildFor(behandling);
        behandlingresultatBuilder = null; // resett

        var inngangsvilkårBuilder = VilkårResultat
                .builderFraEksisterende(behandlingsresultat.getVilkårResultat());

        vilkårTyper.forEach((vilkårType, vilkårUtfallType) -> inngangsvilkårBuilder.leggTilVilkår(vilkårType, vilkårUtfallType, VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallType) ? VilkårUtfallMerknad.VM_1019 : VilkårUtfallMerknad.UDEFINERT));

        var vilkårResultat = inngangsvilkårBuilder.buildFor(behandling);

        repoProvider.getBehandlingRepository().lagre(vilkårResultat, lås);

        if (behandlingVedtakBuilder != null) {
            // Må lagre Behandling for at Behandlingsresultat ikke skal være transient når
            // BehandlingVedtak blir lagret:
            repoProvider.getBehandlingRepository().lagre(behandling, lås);
            behandlingVedtak = behandlingVedtakBuilder.medBehandlingsresultat(behandlingsresultat).build();
            repoProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);
        }
    }

    private void lagreUttak(FpUttakRepository fpUttakRepository) {
        if (uttak == null) {
            return;
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), stønadskontoberegning, uttak);
    }

    @SuppressWarnings("unchecked")
    public S medFødselAdopsjonsdato(List<LocalDate> fødselAdopsjonDatoer) {
        for (var localDate : fødselAdopsjonDatoer) {
            medSøknadHendelse().leggTilBarn(localDate);
        }
        return (S) this;
    }

    public S medFødselAdopsjonsdato(LocalDate fødselAdopsjonDato) {
        return medFødselAdopsjonsdato(List.of(fødselAdopsjonDato));
    }

    public Fagsak getFagsak() {
        if (fagsak == null) {
            throw new IllegalStateException("Kan ikke hente Fagsak før denne er bygd");
        }
        return fagsak;
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

    @SuppressWarnings("unchecked")
    public S medFagsakId(Long id) {
        this.fagsakId = id;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medSøknadHendelse(FamilieHendelseBuilder søknadHendelseBuilder) {
        this.søknadHendelseBuilder = søknadHendelseBuilder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittTilknytning(MedlemskapOppgittTilknytningEntitet.Builder builder) {
        this.oppgittTilknytningBuilder = builder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBekreftetHendelse(FamilieHendelseBuilder bekreftetHendelseBuilder) {
        this.bekreftetHendelseBuilder = bekreftetHendelseBuilder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOverstyrtHendelse(FamilieHendelseBuilder overstyrtHendelseBuilder) {
        this.overstyrtHendelseBuilder = overstyrtHendelseBuilder;
        return (S) this;
    }

    public OppgittAnnenPartBuilder medSøknadAnnenPart() {
        if (oppgittAnnenPartBuilder == null) {
            oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder();
        }
        return oppgittAnnenPartBuilder;
    }

    public FamilieHendelseBuilder medSøknadHendelse() {
        if (søknadHendelseBuilder == null) {
            søknadHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        }
        return søknadHendelseBuilder;
    }

    public BehandlingVedtak.Builder medBehandlingVedtak() {
        if (behandlingVedtakBuilder == null) {
            behandlingVedtakBuilder = BehandlingVedtak.builder()
                    // Setter defaultverdier
                    .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
                    .medAnsvarligSaksbehandler("Nav Navesen");
        }
        return behandlingVedtakBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(Behandlingsresultat.Builder builder) {
        if (behandlingresultatBuilder == null) {
            behandlingresultatBuilder = builder;
        }
        return (S) this;
    }

    public MedlemskapOppgittTilknytningEntitet.Builder medOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        return oppgittTilknytningBuilder;
    }

    public S medDefaultOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        var oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(true)
                .medLand(Landkoder.NOR)
                .medPeriode(
                        LocalDate.now().minusYears(1),
                        LocalDate.now())
                .build();
        var oppholdNorgeNestePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.NOR)
                .medPeriode(
                        LocalDate.now(),
                        LocalDate.now().plusYears(1))
                .build();
        var oppholdNorge = List.of(oppholdNorgeNestePeriode, oppholdNorgeSistePeriode);

        oppgittTilknytningBuilder.medOpphold(oppholdNorge).medOppholdNå(true).medOppgittDato(LocalDate.now());
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultOppgittDekningsgrad() {
        medOppgittDekningsgrad(Dekningsgrad._100);
        return (S) this;
    }

    public FamilieHendelseBuilder medBekreftetHendelse() {
        if (bekreftetHendelseBuilder == null) {
            bekreftetHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET);
        }
        return bekreftetHendelseBuilder;
    }

    public FamilieHendelseBuilder medOverstyrtHendelse() {
        if (overstyrtHendelseBuilder == null) {
            overstyrtHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.OVERSTYRT);
        }
        return overstyrtHendelseBuilder;
    }

    public SøknadEntitet.Builder medSøknad() {
        if (søknadBuilder == null) {
            søknadBuilder = new SøknadEntitet.Builder();
        }
        return søknadBuilder;
    }

    protected void utenSøknad() {
        this.søknadBuilder = null;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S leggTilVilkår(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
        vilkårTyper.put(vilkårType, vilkårUtfallType);
        return (S) this;
    }

    public S medUttak(UttakResultatPerioderEntitet uttak) {
        if (behandlingresultatBuilder == null) {
            medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        }
        if (behandlingVedtakBuilder == null) {
            medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        }
        this.uttak = uttak;
        return (S) this;
    }

    public S medStønadskontoberegning(Stønadskontoberegning stønadskontoberegning) {
        this.stønadskontoberegning = stønadskontoberegning;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
        return (S) this;
    }

    public S leggTilMedlemskapPeriode(MedlemskapPerioderEntitet medlemskapPeriode) {
        this.medlemskapPerioder.add(medlemskapPeriode);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultSøknadTerminbekreftelse() {
        var terminbekreftelse = medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().plusDays(40))
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(LocalDate.now().minusDays(7));
        medSøknadHendelse()
                .medTerminbekreftelse(terminbekreftelse);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultBekreftetTerminbekreftelse() {
        var terminbekreftelse = medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().plusDays(40))
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(LocalDate.now().minusDays(7));
        medBekreftetHendelse()
                .medTerminbekreftelse(terminbekreftelse);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId, NavBrukerKjønn kjønn) {
        fagsakBuilder
                .medBrukerAktørId(aktørId)
                .medBrukerKjønn(kjønn);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId) {
        fagsakBuilder
                .medBrukerAktørId(aktørId);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBrukerKjønn(NavBrukerKjønn kjønn) {
        fagsakBuilder
                .medBrukerKjønn(kjønn);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medTilleggsopplysninger(String tilleggsopplysninger) {
        medSøknad().medTilleggsopplysninger(tilleggsopplysninger);
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
    public S medAvklarteUttakDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        this.avklarteUttakDatoer = avklarteUttakDatoer;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingstidFrist(LocalDate behandlingstidFrist) {
        this.behandlingstidFrist = behandlingstidFrist;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOpplysningerOppdatertTidspunkt(LocalDateTime opplysningerOppdatertTidspunkt) {
        this.opplysningerOppdatertTidspunkt = opplysningerOppdatertTidspunkt;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medRegisterOpplysninger(PersonInformasjon personinfo) {
        Objects.requireNonNull(personinfo);
        if (!PersonopplysningVersjonType.REGISTRERT.equals(personinfo.getType())) {
            throw new IllegalStateException("Feil versjontype, må være PersonopplysningVersjonType.REGISTRERT");
        }
        if (this.personer == null) {
            this.personer = new ArrayList<>();
            this.personer.add(personinfo);
        }
        return (S) this;
    }

    public PersonInformasjon.Builder opprettBuilderForRegisteropplysninger() {
        if (personInformasjonBuilder == null) {
            personInformasjonBuilder = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT);
        }
        return personInformasjonBuilder;
    }

    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        return medOriginalBehandling(originalBehandling, List.of(behandlingÅrsakType), false);
    }

    @SuppressWarnings("unchecked")
    public S medOriginalBehandling(Behandling originalBehandling, List<BehandlingÅrsakType> behandlingÅrsakType, boolean manueltOpprettet) {
        this.originalBehandling = originalBehandling;
        this.behandlingÅrsakTyper = behandlingÅrsakType;
        this.manueltOpprettet = manueltOpprettet;
        this.behandlingType = BehandlingType.REVURDERING;
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
        public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPartHvisEksisterer(Long behandlingId) {
            if (personopplysningMap.isEmpty() || personopplysningMap.get(behandlingId) == null || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null).getOppgittAnnenPart();
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                throw new IllegalArgumentException("Overstyrt personopplysning er legacy");
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
            throw new java.lang.UnsupportedOperationException(IKKE_IMPLEMENTERT);
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
            throw new java.lang.UnsupportedOperationException(IKKE_IMPLEMENTERT);
        }
    }

}
