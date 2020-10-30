package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.FastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.VurderSøknadsfristTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoFørstegangsbehandlingUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
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
            new SkjæringstidspunktUtils());
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
        var andelGraderingTjeneste = new AndelGraderingTjeneste(uttakTjeneste, ytelsesFordelingRepository);
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
        var endringsdatoRevurderingUtleder = new EndringsdatoRevurderingUtlederImpl(uttakRepositoryProvider, dekningsgradTjeneste);
        var fastsettUttaksgrunnlagTjeneste = new FastsettUttaksgrunnlagTjeneste(uttakRepositoryProvider, endringsdatoFørstegangsbehandlingUtleder,
            endringsdatoRevurderingUtleder);
        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(
            uttakInputTjeneste, ytelsesFordelingRepository, vurderSøknadsfristTjeneste, fastsettUttaksgrunnlagTjeneste, behandlingRepository);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medSaksnummer(new Saksnummer("2"))
                .medBrukerAktørId(AKTØRID).build();

        behandlingRepositoryProvider.getFagsakRepository().opprettNy(fagsak);

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);

        var behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler("VL");

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        var lås = behandlingRepositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder().buildFor(behandlingsresultat);
        behandlingRepository.lagre(vilkårResultat, lås);
        return behandling;
    }

    @Test
    public void skalOppretteAksjonspunktForÅVurdereSøknadsfristHvisSøktePerioderUtenforSøknadsfrist() {
        var behandling = opprettBehandling();
        LocalDate mottattDato = LocalDate.now();
        LocalDate førsteUttaksdato = mottattDato.with(DAY_OF_MONTH, 1).minusMonths(3).minusDays(1); //En dag forbi søknadsfrist
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
            .build();

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Arrays.asList(periode1, periode2), true);
        ytelsesFordelingRepository.lagre(behandlingId, fordeling);

        final SøknadEntitet søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        Fagsak fagsak = behandling.getFagsak();
        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato.plusDays(1));
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    @Test
    public void skalIkkeOppretteAksjonspunktHvisSøktePerioderInnenforSøknadsfrist() {
        var behandling = opprettBehandling();
        LocalDate førsteUttaksdato = LocalDate.now().with(DAY_OF_MONTH, 1).minusMonths(3);
        LocalDate mottattDato = LocalDate.now();
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(førsteUttaksdato, førsteUttaksdato.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(førsteUttaksdato.plusWeeks(6).plusDays(1), førsteUttaksdato.plusWeeks(10))
            .build();

        OppgittDekningsgradEntitet dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        Long behandlingId = behandling.getId();
        ytelsesFordelingRepository.lagre(behandlingId, dekningsgrad);

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Arrays.asList(periode1, periode2), true);
        ytelsesFordelingRepository.lagre(behandlingId, fordeling);

        final SøknadEntitet søknad = opprettSøknad(førsteUttaksdato, mottattDato, behandling);
        behandlingRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        Fagsak fagsak = behandling.getFagsak();
        // Act
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        BehandleStegResultat behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Behandling lagretBehandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = lagretBehandling.getBehandlingsresultat().getGjeldendeUttaksperiodegrense();
        assertThat(gjeldendeUttaksperiodegrense).isPresent();
        assertThat(gjeldendeUttaksperiodegrense.get().getFørsteLovligeUttaksdag()).isEqualTo(førsteUttaksdato);
        assertThat(gjeldendeUttaksperiodegrense.get().getMottattDato()).isEqualTo(mottattDato);
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato, Behandling behandling) {
        final FamilieHendelseBuilder søknadHendelse = familieHendelseRepository.opprettBuilderFor(behandling)
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
