package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.persistence.EntityManager;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
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
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.Personstatus;
import no.nav.foreldrepenger.domene.uttak.testutilities.fagsak.FagsakBuilder;
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
    private InntektArbeidYtelseScenario iayScenario;
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;

    private OppgittAnnenPartBuilder oppgittAnnenPartBuilder;

    private VilkårResultatType vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittDekningsgradEntitet oppgittDekningsgrad;
    private OppgittFordelingEntitet oppgittFordeling;
    private AvklarteUttakDatoerEntitet avklarteUttakDatoer;

    // Registret og overstyrt personinfo
    private List<PersonInformasjon> personer;

    private Behandling originalBehandling;
    private BehandlingÅrsakType behandlingÅrsakType;
    private PerioderUtenOmsorgEntitet perioderUtenOmsorg;
    private PerioderAleneOmsorgEntitet perioderMedAleneomsorg;
    private no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonInformasjon.Builder personInformasjonBuilder;
    private UttakResultatPerioderEntitet uttak;

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
            public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder builder) {
                throw new UnsupportedOperationException("Get outta here - no longer supporting this");
            }

            @Override
            public void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
                throw new UnsupportedOperationException("Get outta here - no longer supporting this");
            }

        }

        return lagre(repositoryProvider, new LegacyBridgeIay());
    }

    public Behandling lagre(UttakRepositoryProvider repositoryProvider,
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

        return lagre(repositoryProvider, new LegacyBridgeIay());
    }

    private Behandling lagre(UttakRepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        build(repositoryProvider, lagreIay);
        return behandling;
    }

    private void lagrePersonopplysning(UttakRepositoryProvider repositoryProvider, Behandling behandling) {
        PersonopplysningRepository personopplysningRepository = new PersonopplysningRepository(repositoryProvider.getEntityManager());
        Long behandlingId = behandling.getId();
        if (oppgittAnnenPartBuilder != null) {
            personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder);
        }

        if (personer != null && !personer.isEmpty()) {
            personer.stream().filter(e -> e.getType().equals(PersonopplysningVersjonType.REGISTRERT))
                .findFirst().ifPresent(e -> lagrePersoninfo(behandling, e, personopplysningRepository));

            personer.stream().filter(a -> a.getType().equals(PersonopplysningVersjonType.OVERSTYRT))
                .findFirst().ifPresent(b -> {
                if (personer.stream().noneMatch(c -> c.getType().equals(PersonopplysningVersjonType.REGISTRERT))) {
                    // Sjekker om overstyring er ok, mao om registeropplysninger finnes
                    personopplysningRepository.opprettBuilderForOverstyring(behandlingId);
                }
                lagrePersoninfo(behandling, b, personopplysningRepository);
            });

        } else {
            PersonInformasjon registerInformasjon = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT)
                .leggTilPersonopplysninger(
                    no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.Personopplysning.builder()
                        .aktørId(behandling.getAktørId())
                        .navn("Forelder")
                        .brukerKjønn(getKjønnFraFagsak())
                        .fødselsdato(LocalDate.now().minusYears(25))
                        .sivilstand(SivilstandType.UOPPGITT)
                        .region(Region.NORDEN))
                .leggTilPersonstatus(Personstatus.builder()
                    .personstatus(PersonstatusType.BOSA)
                    .periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                    .aktørId(behandling.getAktørId()))
                .build();
            lagrePersoninfo(behandling, registerInformasjon, personopplysningRepository);
        }
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        Objects.requireNonNull(behandling);
        Objects.requireNonNull(personInformasjon);

        if (personInformasjon.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            lagreRegisterPersoninfo(behandling, personInformasjon, repository);
        } else {
            lagreOverstyrtPersoninfo(behandling, personInformasjon, repository);
        }
    }

    private void lagreRegisterPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForRegisterdata(behandling.getId()), personInformasjon, repository);
    }

    private void lagreOverstyrtPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForOverstyring(behandling.getId()), personInformasjon, repository);
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjon personInformasjon,
                                 PersonopplysningRepository repository) {
        personInformasjon.getPersonopplysninger().forEach(e -> {
            PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(e.getAktørId());
            builder.medNavn(e.getNavn())
                .medFødselsdato(e.getFødselsdato())
                .medDødsdato(e.getDødsdato())
                .medKjønn(e.getBrukerKjønn())
                .medRegion(e.getRegion())
                .medSivilstand(e.getSivilstand());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getAdresser().forEach(e -> {
            PersonInformasjonBuilder.AdresseBuilder builder = personInformasjonBuilder.getAdresseBuilder(e.getAktørId(), e.getPeriode(), e.getAdresseType());
            builder.medAdresselinje1(e.getAdresselinje1())
                .medLand(e.getLand())
                .medPostnummer(e.getPostnummer())
                .medPoststed(e.getPoststed());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getPersonstatuser().forEach(e -> {
            PersonInformasjonBuilder.PersonstatusBuilder builder = personInformasjonBuilder.getPersonstatusBuilder(e.getAktørId(), e.getPeriode());
            builder.medPersonstatus(e.getPersonstatus());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getRelasjoner().forEach(e -> {
            PersonInformasjonBuilder.RelasjonBuilder builder = personInformasjonBuilder.getRelasjonBuilder(e.getAktørId(), e.getTilAktørId(),
                e.getRelasjonsrolle());
            builder.harSammeBosted(e.getHarSammeBosted());
            personInformasjonBuilder.leggTil(builder);
        });

        repository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    private void build(UttakRepositoryProvider repositoryProvider, LagreInntektArbeidYtelse lagreIay) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        BehandlingRepository behandlingRepo = new BehandlingRepository(repositoryProvider.getEntityManager());
        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagrePersonopplysning(repositoryProvider, behandling);
        lagreInntektArbeidYtelse(lagreIay);
        lagreYtelseFordelingOpplysninger(repositoryProvider, behandling);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreUttak(repositoryProvider.getFpUttakRepository());

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
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
                .medBehandlingÅrsak(BehandlingÅrsak.builder(behandlingÅrsakType).medOriginalBehandling(originalBehandling));
        }

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            final EntityManager entityManager = (EntityManager) Whitebox.getInternalState(fagsakRepo, "entityManager");
            if (entityManager != null) {
                BrukerTjeneste brukerTjeneste = new BrukerTjeneste(new NavBrukerRepository(entityManager));
                final Personinfo personinfo = new Personinfo.Builder()
                    .medFødselsdato(LocalDate.now())
                    .medPersonIdent(PersonIdent.fra("123451234123"))
                    .medNavn("asdf")
                    .medAktørId(fagsakBuilder.getBrukerBuilder().getAktørId())
                    .medNavBrukerKjønn(getKjønnFraFagsak())
                    .medForetrukketSpråk(
                        fagsakBuilder.getBrukerBuilder().getSpråkkode() != null ? fagsakBuilder.getBrukerBuilder().getSpråkkode() : Språkkode.nb)
                    .build();
                final NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(personinfo);
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private NavBrukerKjønn getKjønnFraFagsak() {
        return fagsakBuilder.getBrukerBuilder().getKjønn() != null ? fagsakBuilder.getBrukerBuilder().getKjønn()
            : (RelasjonsRolleType.erMor(fagsakBuilder.getRolle()) || RelasjonsRolleType.erMedmor(fagsakBuilder.getRolle()) ? NavBrukerKjønn.KVINNE
            : NavBrukerKjønn.MANN);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(UttakRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat
        Behandlingsresultat behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
            : behandlingresultatBuilder).buildFor(behandling);

        VilkårResultat.Builder inngangsvilkårBuilder = VilkårResultat
            .builderFraEksisterende(behandlingsresultat.getVilkårResultat())
            .medVilkårResultatType(vilkårResultatType);

        VilkårResultat vilkårResultat = inngangsvilkårBuilder.buildFor(behandling);

        new BehandlingRepository(repoProvider.getEntityManager()).lagre(vilkårResultat, lås);
        repoProvider.getBehandlingsresultatRepository().lagre(behandling.getId(), behandlingsresultat);
    }

    private void lagreUttak(FpUttakRepository fpUttakRepository) {
        if (uttak == null) {
            return;
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);
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

    public OppgittAnnenPartBuilder medSøknadAnnenPart() {
        if (oppgittAnnenPartBuilder == null) {
            oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder();
        }
        return oppgittAnnenPartBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(Behandlingsresultat.Builder builder) {
        if (behandlingresultatBuilder == null) {
            behandlingresultatBuilder = builder;
        }
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
    public S medVilkårResultatType(VilkårResultatType vilkårResultatType) {
        this.vilkårResultatType = vilkårResultatType;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medUttak(UttakResultatPerioderEntitet uttak) {
        if (behandlingresultatBuilder == null) {
            medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
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
    public S medRegisterOpplysninger(PersonInformasjon personinfo) {
        Objects.nonNull(personinfo);
        if (!personinfo.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
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
        void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder builder);
        void lagreInntektArbeidYtelseAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder);
    }
}
