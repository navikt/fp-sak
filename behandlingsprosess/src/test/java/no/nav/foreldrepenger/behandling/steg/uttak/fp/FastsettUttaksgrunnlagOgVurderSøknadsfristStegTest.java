package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.FastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.VurderSøknadsfristTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.RelevanteArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.EndringsdatoFørstegangsbehandlingUtleder;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.Utsettelse2021;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;

public class FastsettUttaksgrunnlagOgVurderSøknadsfristStegTest extends EntityManagerAwareTest {

    private static final AktørId AKTØRID = AktørId.dummy();

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    private FastsettUttaksgrunnlagOgVurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
    private FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(entityManager);
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(behandlingRepositoryProvider,
                new YtelseMaksdatoTjeneste(behandlingRepositoryProvider, new RelatertBehandlingTjeneste(behandlingRepositoryProvider)),
                new SkjæringstidspunktUtils(), mock(Utsettelse2021.class));
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
        var andelGraderingTjeneste = new BeregningUttakTjeneste(uttakTjeneste, ytelsesFordelingRepository);
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var uttakInputTjeneste = new UttakInputTjeneste(behandlingRepositoryProvider, beregningsgrunnlagTjeneste,
                iayTjeneste, skjæringstidspunktTjeneste, mock(MedlemTjeneste.class), andelGraderingTjeneste);
        var vurderSøknadsfristTjeneste = new VurderSøknadsfristTjeneste(behandlingRepositoryProvider);
        var uttakRepositoryProvider = new UttakRepositoryProvider(entityManager);
        var fagsakRepository = new FagsakRepository(entityManager);
        var fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(new FagsakRelasjonRepository(entityManager,
                ytelsesFordelingRepository, fagsakLåsRepository), null, fagsakRepository);
        var behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        var dekningsgradTjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, behandlingsresultatRepository);
        var endringsdatoFørstegangsbehandlingUtleder = new EndringsdatoFørstegangsbehandlingUtleder(ytelsesFordelingRepository);
        var fpUttakRepository = uttakRepositoryProvider.getFpUttakRepository();
        var relevanteArbeidsforholdTjeneste = new RelevanteArbeidsforholdTjeneste(
            fpUttakRepository);
        var endringsdatoRevurderingUtleder = new EndringsdatoRevurderingUtlederImpl(uttakRepositoryProvider, dekningsgradTjeneste,
            relevanteArbeidsforholdTjeneste);
        var fastsettUttaksgrunnlagTjeneste = new FastsettUttaksgrunnlagTjeneste(uttakRepositoryProvider, endringsdatoFørstegangsbehandlingUtleder,
                endringsdatoRevurderingUtleder);
        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(
                uttakInputTjeneste, ytelsesFordelingRepository, vurderSøknadsfristTjeneste, fastsettUttaksgrunnlagTjeneste, behandlingRepository,
                new SkalKopiereUttakTjeneste(
                    relevanteArbeidsforholdTjeneste), new KopierForeldrepengerUttaktjeneste(fpUttakRepository, ytelsesFordelingRepository));
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medSaksnummer(new Saksnummer("2"))
                .medBrukerAktørId(AKTØRID).build();

        behandlingRepositoryProvider.getFagsakRepository().opprettNy(fagsak);

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);

        var behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler("VL");

        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        var lås = behandlingRepositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder().buildFor(behandlingsresultat);
        behandlingRepository.lagre(vilkårResultat, lås);
        return behandling;
    }

    @Test
    public void skalOppretteAksjonspunktForÅVurdereSøknadsfristHvisSøktePerioderUtenforSøknadsfrist() {
        var behandling = opprettBehandling();
        var mottattDato = LocalDate.now();
        var førsteUttaksdato = mottattDato.with(DAY_OF_MONTH, 1).minusMonths(3).minusDays(1); // En dag forbi søknadsfrist
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
                .build();

        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
                .build();

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();

        var fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittFordeling(fordeling)
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        final var søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        var fagsak = behandling.getFagsak();
        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        var gjeldendeUttaksperiodegrense = behandlingRepositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandlingId);
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato.plusDays(1));
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    @Test
    public void skalIkkeOppretteAksjonspunktHvisSøktePerioderInnenforSøknadsfrist() {
        var behandling = opprettBehandling();
        var førsteUttaksdato = LocalDate.now().with(DAY_OF_MONTH, 1).minusMonths(3);
        var mottattDato = LocalDate.now();
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
                .build();

        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
                .build();

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var behandlingId = behandling.getId();

        var fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOppgittFordeling(fordeling)
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());

        final var søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        var fagsak = behandling.getFagsak();
        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        var gjeldendeUttaksperiodegrense = behandlingRepositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandlingId);
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato);
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato, Behandling behandling) {
        final var søknadHendelse = familieHendelseRepository.opprettBuilderFor(behandling)
                .medAntallBarn(1)
                .medFødselsDato(fødselsdato);
        familieHendelseRepository.lagre(behandling, søknadHendelse);

        return new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(mottattDato)
                .medElektroniskRegistrert(true)
                .build();
    }

}
