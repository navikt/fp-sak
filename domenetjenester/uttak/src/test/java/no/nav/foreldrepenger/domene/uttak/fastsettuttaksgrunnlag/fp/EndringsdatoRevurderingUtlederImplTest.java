package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.BERØRT_BEHANDLING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
import static no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil.FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class EndringsdatoRevurderingUtlederImplTest {

    private static final LocalDate MANUELT_SATT_FØRSTE_UTTAKSDATO = FØDSELSDATO.plusDays(1);
    private static final LocalDate OMSORGSOVERTAKELSEDATO = FØDSELSDATO.plusDays(10);
    private static final LocalDate ANKOMSTDATO = FØDSELSDATO.plusDays(11);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());

    private UttakRevurderingTestUtil testUtil;

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    private AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    @Inject
    private DekningsgradTjeneste dekningsgradTjeneste;
    @Inject
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    private EndringsdatoRevurderingUtlederImpl utleder;
    private UttakBeregningsandelTjenesteTestUtil uttakBeregningsandelTjeneste;

    @Before
    public void before() {
        testUtil = new UttakRevurderingTestUtil(repositoryProvider, iayTjeneste);
        uttakBeregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();
        utleder = new EndringsdatoRevurderingUtlederImpl(inntektsmeldingTjeneste,
            repositoryProvider, dekningsgradTjeneste);
    }

    @Test
    public void skal_utlede_at_endringsdatoen_er_første_uttaksdato_til_startdato_for_uttak_når_dekningsgrad_er_endret() {
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1),
            MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        List<UttakResultatPeriodeEntitet> opprinneligUttak = Collections.singletonList(opprinneligPeriode);
        OppgittDekningsgradEntitet oppgittDekningsgrad = OppgittDekningsgradEntitet.bruk80();
        Behandling revurdering = testUtil.opprettRevurdering(AktørId.dummy(), RE_OPPLYSNINGER_OM_DØD, opprinneligUttak,
            new OppgittFordelingEntitet(Collections.emptyList(), true), oppgittDekningsgrad);
        AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(MANUELT_SATT_FØRSTE_UTTAKSDATO).build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), entitet);

        endreDekningsgrad(revurdering, Dekningsgrad._100);

        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(opprinneligPeriode.getFom());
    }

    private void endreDekningsgrad(Behandling revurdering, Dekningsgrad dekningsgrad) {
        fagsakRelasjonRepository.overstyrDekningsgrad(revurdering.getFagsak(), dekningsgrad);
        Behandlingsresultat ekisterende = repositoryProvider.getBehandlingsresultatRepository().hentHvisEksisterer(revurdering.getId()).get();
        var behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(ekisterende).medEndretDekningsgrad(true);
        repositoryProvider.getBehandlingsresultatRepository().lagre(revurdering.getId(), behandlingsresultat.build());
    }

    @Test
    public void skal_utlede_at_endringsdatoen_er_første_uttaksdato_til_startdato_for_uttak_når_dekningsgrad_er_endret_hvis_endringssøknad() {
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1),
            MANUELT_SATT_FØRSTE_UTTAKSDATO.minusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        List<UttakResultatPeriodeEntitet> opprinneligUttak = Collections.singletonList(opprinneligPeriode);
        List<OppgittPeriodeEntitet> fordeling = Collections.singletonList(OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build());
        Behandling revurdering = testUtil.opprettRevurdering(AktørId.dummy(), RE_ENDRING_FRA_BRUKER, opprinneligUttak,
            new OppgittFordelingEntitet(fordeling, true), OppgittDekningsgradEntitet.bruk100());
        AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(MANUELT_SATT_FØRSTE_UTTAKSDATO).build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), entitet);

        endreDekningsgrad(revurdering, Dekningsgrad._80);

        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(opprinneligPeriode.getFom());
    }

    @Test // #1.1
    public void skal_utlede_at_endringsdato_er_fødselsdato_når_fødsel_har_forekommet_før_første_uttaksdato() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        FamilieHendelse bekreftetHendelse = FamilieHendelse.forFødsel(FØDSELSDATO, FØDSELSDATO, List.of(new Barn()), 1);
        var ref = BehandlingReferanse.fra(revurdering);
        FamilieHendelser familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var originalSøknadshendelse = FamilieHendelse.forFødsel(FØDSELSDATO, null, List.of(new Barn()), 1);
        var originalBehandling = new OriginalBehandling(revurdering.getOriginalBehandling().get().getId(), new FamilieHendelser().medSøknadHendelse(originalSøknadshendelse));
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(originalBehandling);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(ref, ytelsespesifiktGrunnlag));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    private UttakInput lagInput(BehandlingReferanse ref, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        return new UttakInput(ref, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(uttakBeregningsandelTjeneste.hentStatuser());
    }

    @Test // #1.2
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_fødsel_har_forekommet_etter_første_uttaksdato() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        FamilieHendelse bekreftetHendelse = FamilieHendelse.forFødsel(FØDSELSDATO, FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK, List.of(new Barn()), 1);
        var ref = BehandlingReferanse.fra(revurdering);
        FamilieHendelser familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var originalSøknadshendelse = FamilieHendelse.forFødsel(FØDSELSDATO, null, List.of(new Barn()), 1);
        var originalBehandling = new OriginalBehandling(revurdering.getOriginalBehandling().get().getId(),
            new FamilieHendelser().medSøknadHendelse(originalSøknadshendelse));
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(originalBehandling);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(ref, ytelsespesifiktGrunnlag));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #2
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_søknad_når_endringssøknad_er_mottatt() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingForMorFPFF(revurdering);

        // Act
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF);
    }

    @Test // #2
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_søknad_når_endringssøknad_er_mottatt_selv_om_mottatt_dato_før_vedtaksdato_på_original_behandling() {
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        originalScenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(oppgittPeriode), true));

        Behandling originalBehandling = originalScenario.lagre(repositoryProvider);

        UttakResultatPerioderEntitet originaltUttak = new UttakResultatPerioderEntitet();
        originaltUttak.leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build());
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), originaltUttak);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        revurderingScenario.medDefaultInntektArbeidYtelse();
        revurderingScenario.medDefaultOppgittDekningsgrad();
        revurderingScenario.medOriginalBehandling(originalBehandling, RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medBehandlingType(BehandlingType.REVURDERING);
        OppgittPeriodeEntitet nyOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2))
            .build();

        revurderingScenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(nyOppgittPeriode), true));

        Behandling revurdering = lagre(revurderingScenario);

        // Act
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(nyOppgittPeriode.getFom());
    }

    @Test // #2
    public void skal_utlede_at_endringsdato_er_siste_uttaksdato_pluss_1_virkedag_fra_original_behandling_når_første_uttaksdato_fra_søknad_er_senere() {
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        OppgittPeriodeEntitet originalOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        originalScenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(originalOppgittPeriode), true));
        Behandling originalBehandling = originalScenario.lagre(repositoryProvider);

        UttakResultatPerioderEntitet originaltUttak = new UttakResultatPerioderEntitet();
        originaltUttak.leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(1).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build());
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), originaltUttak);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        revurderingScenario.medDefaultInntektArbeidYtelse();
        revurderingScenario.medOriginalBehandling(originalBehandling, RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medBehandlingType(BehandlingType.REVURDERING);
        OppgittPeriodeEntitet nyOppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.now().plusWeeks(3), LocalDate.now().plusWeeks(4))
            .build();

        revurderingScenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(nyOppgittPeriode), true));

        Behandling revurdering = lagre(revurderingScenario);

        // Act
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(Virkedager.plusVirkedager(originalOppgittPeriode.getTom(), 1));
    }

    @Test // #3
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtak_når_revurdering_er_manuelt_opprettet() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_HENDELSE_FØDSEL))
            .medBehandlingManueltOpprettet(true);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #4.1
    public void skal_utlede_at_endringsdato_på_mors_berørte_behandling_er_lik_fars_første_uttaksdag() {
        // Arrange førstegangsbehandling mor
        Behandling behandling = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_MOR, testUtil.uttaksresultatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK), Optional.empty());

        // Arrange førstegangsbehandling far
        LocalDate fomFar = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(11);
        Behandling behandlingFar = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_FAR, testUtil.uttaksresultatBerørtSak(fomFar), Optional.of(behandling.getFagsak()));
        behandlingRepository.lagre(behandlingFar, behandlingRepository.taSkriveLås(behandlingFar));

        // Arrange berørt behandling mor
        Behandling revurderingBerørtSak = testUtil.opprettRevurderingBerørtSak(AKTØR_ID_MOR, BERØRT_BEHANDLING, behandling);
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BERØRT_BEHANDLING)
            .medOriginalBehandling(revurderingBerørtSak.getOriginalBehandling().get());
        revurderingÅrsak.buildFor(revurderingBerørtSak);
        behandlingRepository.lagre(revurderingBerørtSak, behandlingRepository.taSkriveLås(revurderingBerørtSak));

        var familieHendelser = new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1));
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag()
            .medErTapendeBehandling(true)
            .medOriginalBehandling(new OriginalBehandling(behandling.getId(), familieHendelser))
            .medFamilieHendelser(familieHendelser)
            .medAnnepart(new Annenpart(false, behandlingFar.getId()));
        var iayGrunnlag = iayTjeneste.hentGrunnlag(revurderingBerørtSak.getId());
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBerørtSak), iayGrunnlag, fpGrunnlag)
            .medBehandlingÅrsaker(Set.of(BERØRT_BEHANDLING));

        // Act
        LocalDate endringsdatoMor = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdatoMor).isEqualTo(fomFar);
    }

    @Test // #4.2
    public void skal_utlede_at_endringsdato_på_mors_berørte_behandling_er_første_uttaksdag_fra_vedtaket_når_fars_endringsdato_er_tidligere() {
        // Arrange førstegangsbehandling mor
        Behandling behandling = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_MOR, testUtil.uttaksresultatBerørtSak(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK), Optional.empty());

        // Arrange førstegangsbehandling far
        LocalDate fomFar = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.minusDays(2L);
        Behandling behandlingFar = testUtil.byggFørstegangsbehandlingForRevurderingBerørtSak(AKTØR_ID_FAR, testUtil.uttaksresultatBerørtSak(fomFar), Optional.of(behandling.getFagsak()));
        behandlingRepository.lagre(behandlingFar, behandlingRepository.taSkriveLås(behandlingFar));

        // Arrange berørt behandling mor
        Behandling revurderingBerørtSak = testUtil.opprettRevurderingBerørtSak(AKTØR_ID_MOR, BERØRT_BEHANDLING, behandling);
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BERØRT_BEHANDLING)
            .medOriginalBehandling(revurderingBerørtSak.getOriginalBehandling().get());
        revurderingÅrsak.buildFor(revurderingBerørtSak);
        behandlingRepository.lagre(revurderingBerørtSak, behandlingRepository.taSkriveLås(revurderingBerørtSak));

        var familieHendelser = new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1));
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag()
            .medErTapendeBehandling(true)
            .medOriginalBehandling(new OriginalBehandling(behandling.getId(), familieHendelser))
            .medFamilieHendelser(familieHendelser)
            .medAnnepart(new Annenpart(false, behandlingFar.getId()));
        var iayGrunnlag = iayTjeneste.hentGrunnlag(revurderingBerørtSak.getId());
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBerørtSak), iayGrunnlag, fpGrunnlag)
            .medBehandlingÅrsaker(Set.of(BERØRT_BEHANDLING));

        // Act
        LocalDate endringsdatoMor = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdatoMor).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #5
    public void skal_utlede_at_endringsdato_er_manuelt_satt_første_uttaksdato() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);
        AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(MANUELT_SATT_FØRSTE_UTTAKSDATO).build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), entitet);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(MANUELT_SATT_FØRSTE_UTTAKSDATO);
    }

    @Test // #2 + #5
    public void skal_utlede_at_endringsdato_er_første_uttaksdag_fra_søknad_når_denne_er_tidligere_enn_manuelt_satt_første_uttaksdato() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingForMorFPFF(revurdering);
        AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(MANUELT_SATT_FØRSTE_UTTAKSDATO).build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), entitet);
        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1);

        // Act
        var input = lagInput(revurdering, familieHendelse)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF);
    }

    @Test // #6
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_når_inntektsmelding_endrer_uttak() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRET_INNTEKTSMELDING);
        testUtil.opprettInntektsmelding(revurdering);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test // #7
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_ved_opplysninger_om_død() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_HENDELSE_FØDSEL);

        // Act
        var input = lagInput(revurdering)
            .medErOpplysningerOmDødEndret(true);
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_vedtaket_når_endring_i_ytelse_ikke_fører_til_endring_i_grunnlaget() {
        // Arrange
        LocalDate startdatoEndringssøknad = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(10);
        Behandling revurdering = testUtil.opprettEndringssøknadRevurdering(AKTØR_ID_MOR, startdatoEndringssøknad, RE_ENDRING_FRA_BRUKER);
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(startdatoEndringssøknad);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_i_endring_dersom_endringer_i_ytelse_stammer_fra_samme_fagsak() {

        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(RE_ENDRING_FRA_BRUKER);
        testUtil.byggOgLagreOppgittFordelingMedPeriode(revurdering, FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(11), FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(50), UttakPeriodeType.FELLESPERIODE);

        leggTilFpsakYtelse(revurdering);
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(11));
    }

    @Test // Adopsjon.1
    public void skal_utlede_at_endringsdato_er_omsorgsovertakelsedato_ved_adopsjon_uten_ankomstdato() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurderingAdopsjon();
        var ref = BehandlingReferanse.fra(revurdering);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(OMSORGSOVERTAKELSEDATO, List.of(new Barn()), 1, null, false);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familieHendelser)
            .medOriginalBehandling(new OriginalBehandling(revurdering.getOriginalBehandling().get().getId(), familieHendelser));
        var uttakInput = new UttakInput(ref, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(uttakBeregningsandelTjeneste.hentStatuser());

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(uttakInput);

        // Assert
        assertThat(endringsdato).isEqualTo(OMSORGSOVERTAKELSEDATO);
    }

    @Test // Adopsjon.2
    public void skal_utlede_at_endringsdato_er_ankomstdato_ved_adopsjon_når_ankomstdatoen_er_satt() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurderingAdopsjon();
        var ref = BehandlingReferanse.fra(revurdering);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(OMSORGSOVERTAKELSEDATO, List.of(new Barn()), 1, ANKOMSTDATO, false);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familieHendelser)
            .medOriginalBehandling(new OriginalBehandling(revurdering.getOriginalBehandling().get().getId(), familieHendelser));
        var uttakInput = new UttakInput(ref, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(uttakBeregningsandelTjeneste.hentStatuser());

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(uttakInput);

        // Assert
        assertThat(endringsdato).isEqualTo(ANKOMSTDATO);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_forrige_behandling_når_uten_uttaksresultat() {
        // Arrange
        Behandling revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_HENDELSE_FØDSEL);

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_dato_i_vedtak_hvis_endringssøknad_men_beregningsandel_er_fjernet() {
        LocalDate fomOpprinneligUttak = FØDSELSDATO;
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak, fomOpprinneligUttak.plusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        UttakAktivitetEntitet uttakAktivitet1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        UttakAktivitetEntitet uttakAktivitet2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet2)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        List<UttakResultatPeriodeEntitet> opprinneligUttak = List.of(opprinneligPeriode);
        LocalDate søknadsFom = FØDSELSDATO.plusWeeks(1);
        OppgittPeriodeEntitet søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .build();
        OppgittFordelingEntitet nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        Behandling revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER,
            opprinneligUttak, nyFordeling, OppgittDekningsgradEntitet.bruk100());
        uttakBeregningsandelTjeneste.leggTilSelvNæringdrivende(Arbeidsgiver.virksomhet("123"));

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_dato_i_vedtak_hvis_endringssøknad_men_beregningsandel_er_lagt_til() {
        LocalDate fomOpprinneligUttak = FØDSELSDATO;
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak, fomOpprinneligUttak.plusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        UttakAktivitetEntitet uttakAktivitet1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        List<UttakResultatPeriodeEntitet> opprinneligUttak = List.of(opprinneligPeriode);
        LocalDate søknadsFom = FØDSELSDATO.plusWeeks(1);
        OppgittPeriodeEntitet søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .build();
        OppgittFordelingEntitet nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        Behandling revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER,
            opprinneligUttak, nyFordeling, OppgittDekningsgradEntitet.bruk100());
        uttakBeregningsandelTjeneste.leggTilSelvNæringdrivende(Arbeidsgiver.virksomhet("123"));
        uttakBeregningsandelTjeneste.leggTilFrilans();

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        // Assert
        assertThat(endringsdato).isEqualTo(FØDSELSDATO);
    }

    @Test
    public void arbeidsforholdref_null_object_skal_ikke_sette_endringsdato_første_dag_uttak() {
        LocalDate fomOpprinneligUttak = FØDSELSDATO;
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(fomOpprinneligUttak, fomOpprinneligUttak.plusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123");
        UttakAktivitetEntitet uttakAktivitet1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, null)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(opprinneligPeriode, uttakAktivitet1)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        List<UttakResultatPeriodeEntitet> opprinneligUttak = List.of(opprinneligPeriode);
        LocalDate søknadsFom = FØDSELSDATO.plusWeeks(1);
        OppgittPeriodeEntitet søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(søknadsFom, søknadsFom.plusWeeks(4))
            .build();
        OppgittFordelingEntitet nyFordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);
        Behandling revurdering = testUtil.opprettRevurdering(AKTØR_ID_MOR, RE_ENDRING_FRA_BRUKER,
            opprinneligUttak, nyFordeling, OppgittDekningsgradEntitet.bruk100());
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, InternArbeidsforholdRef.nullRef());
        var input = lagInput(revurdering)
            .medBehandlingÅrsaker(Set.of(RE_ENDRING_FRA_BRUKER));

        // Act
        LocalDate endringsdato = utleder.utledEndringsdato(input);

        // Assert
        assertThat(endringsdato).isEqualTo(søknadsFom);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_gjeldende_vedtak_når_alle_perioder_er_tapt_til_annenpart() {
        ScenarioMorSøkerForeldrepenger morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        UttakResultatPerioderEntitet morUttak = new UttakResultatPerioderEntitet();
        LocalDate morFom = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
        LocalDate morTom = morFom.plusDays(10);
        UttakResultatPeriodeEntitet morPeriode = new UttakResultatPeriodeEntitet.Builder(morFom, morTom)
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .build();
        morUttak.leggTilPeriode(morPeriode);
        morScenario.medUttak(morUttak);
        Behandling førstegangsbehandling = morScenario.lagre(repositoryProvider);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        revurderingScenario.medOriginalBehandling(førstegangsbehandling, RE_OPPLYSNINGER_OM_FORDELING);
        revurderingScenario.medBehandlingType(BehandlingType.REVURDERING);

        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        leggTilAktørArbeid(revurdering);
        LocalDate endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    @Test
    public void skal_utlede_at_endringsdato_er_første_uttaksdato_fra_gjeldende_vedtak_arbeidsforhold_har_en_aktivitet_i_første_uttaksperiode_har_startdato_etter_første_uttaksperiode() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morUttak = new UttakResultatPerioderEntitet();
        var morFom = FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK;
        var morTom = morFom.plusDays(10);
        var morPeriode = new UttakResultatPeriodeEntitet.Builder(morFom, morTom)
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .build();
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("123");
        var aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(morPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(arbeidsgiver1, null).build())
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        var nyArbeidsgiver = Arbeidsgiver.virksomhet("456");
        var aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(morPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(nyArbeidsgiver, null).build())
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        morPeriode.leggTilAktivitet(aktivitet1);
        morPeriode.leggTilAktivitet(aktivitet2);
        morUttak.leggTilPeriode(morPeriode);
        morScenario.medUttak(morUttak);
        var førstegangsbehandling = morScenario.lagre(repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver1, null);
        uttakBeregningsandelTjeneste.leggTilOrdinærtArbeid(nyArbeidsgiver, null);
        revurderingScenario.medOriginalBehandling(førstegangsbehandling, RE_ENDRING_FRA_BRUKER);
        var oppgittPeriodeEndringssøknad = OppgittPeriodeBuilder.ny().medPeriode(morFom.plusDays(2), morTom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        revurderingScenario.medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriodeEndringssøknad), true));
        revurderingScenario.medBehandlingType(BehandlingType.REVURDERING);

        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var startdatoNyttArbeidsforhold = morFom.plusDays(2);

        var iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(revurdering.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(lagYrkesaktivitet(arbeidsgiver1, DatoIntervallEntitet.fraOgMed(morFom)));
        aktørArbeidBuilder.leggTilYrkesaktivitet(lagYrkesaktivitet(nyArbeidsgiver, DatoIntervallEntitet.fraOgMed(startdatoNyttArbeidsforhold)));
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);

        var endringsdato = utleder.utledEndringsdato(lagInput(revurdering));

        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitet(Arbeidsgiver arbeidsgiver, DatoIntervallEntitet periode) {
        var aktivitetsAvtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(periode)
            .medProsentsats(BigDecimal.valueOf(50))
            .medSisteLønnsendringsdato(periode.getFomDato());
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(periode);
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);
    }

    private UttakInput lagInput(Behandling behandling) {
        var årsaker = behandling.getBehandlingÅrsaker().stream()
            .map(behandlingÅrsak -> behandlingÅrsak.getBehandlingÅrsakType())
            .collect(Collectors.toSet());
        return lagInput(behandling, FamilieHendelse.forFødsel(null, FØDSELSDATO, List.of(new Barn()), 1))
            .medBehandlingÅrsaker(årsaker);
    }

    private UttakInput lagInput(Behandling behandling, FamilieHendelse bekreftetHendelse) {
        var ref = BehandlingReferanse.fra(behandling, bekreftetHendelse.getFamilieHendelseDato());
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        FamilieHendelser familiehendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familiehendelser)
            .medOriginalBehandling(new OriginalBehandling(behandling.getOriginalBehandling().get().getId(), new FamilieHendelser().medBekreftetHendelse(bekreftetHendelse)));
        return new UttakInput(ref, iayGrunnlag, ytelsespesifiktGrunnlag).medBeregningsgrunnlagStatuser(uttakBeregningsandelTjeneste.hentStatuser());
    }

    private void leggTilAktørArbeid(Behandling revurdering) {
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(AKTØR_ID_MOR);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.person(AKTØR_ID_MOR));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);
    }

    private void leggTilFpsakYtelse(Behandling revurdering) {
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(revurdering.getId());
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = iayBuilder.getAktørYtelseBuilder(AKTØR_ID_MOR);
        YtelseBuilder ytelselseBuilder = aktørYtelseBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, RelatertYtelseType.FORELDREPENGER, new Saksnummer("1"));
        ytelselseBuilder.tilbakestillAnvisteYtelser();
        YtelseBuilder ytelse = ytelselseBuilder.medKilde(Fagsystem.FPSAK)
            .medYtelseType(RelatertYtelseType.FORELDREPENGER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medSaksnummer(revurdering.getFagsak().getSaksnummer())
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(6)));
        aktørYtelseBuilder.leggTilYtelse(ytelse);
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(revurdering.getId(), iayBuilder);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

}
