package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.StønadskontoRegelAdapter;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetSaldoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.StønadskontoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;

public class SaldoerDtoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private FpUttakRepository fpUttakRepository;
    private StønadskontoRegelAdapter stønadskontoRegelAdapter;
    private TapteDagerFpffTjeneste tapteDagerFpffTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private SaldoerDtoTjeneste tjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        var uttakRepositoryProvider = new UttakRepositoryProvider(entityManager);
        stønadskontoSaldoTjeneste = new StønadskontoSaldoTjeneste(uttakRepositoryProvider);
        fpUttakRepository = new FpUttakRepository(entityManager);
        stønadskontoRegelAdapter = new StønadskontoRegelAdapter();
        tapteDagerFpffTjeneste = new TapteDagerFpffTjeneste(uttakRepositoryProvider,
            new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager)));
        uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);
        var maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(fpUttakRepository,
            stønadskontoSaldoTjeneste);
        tjeneste = new SaldoerDtoTjeneste(stønadskontoSaldoTjeneste, maksDatoUttakTjeneste,
            stønadskontoRegelAdapter, repositoryProvider, uttakTjeneste,
            tapteDagerFpffTjeneste);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    private static Stønadskonto lagStønadskonto(StønadskontoType fellesperiode, int maxDager) {
        return Stønadskonto.builder().medMaxDager(maxDager).medStønadskontoType(fellesperiode).build();
    }

    private static Stønadskontoberegning lagStønadskontoberegning(Stønadskonto... stønadskontoer) {
        var builder = Stønadskontoberegning.builder()
            .medRegelEvaluering("asdf")
            .medRegelInput("asdf");
        Stream.of(stønadskontoer)
            .forEach(builder::medStønadskonto);
        return builder.build();
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        var morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6),
            fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);


        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (10 * 5));
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 6 /* saldo FP */).minusDays(1));
    }

    private void lagre(Behandlingsresultat behandlingsresultatForMor) {
        behandlingsresultatRepository
            .lagre(behandlingsresultatForMor.getBehandlingId(), behandlingsresultatForMor);
    }

    private BehandlingLås lås(Behandling behandling) {
        return new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId());
    }

    private UttakInput input(Behandling behandling, Annenpart annenpart, LocalDate skjæringstidspunkt) {
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt),
            InntektArbeidYtelseGrunnlagBuilder.nytt().build(), fpGrunnlag(annenpart));
    }

    private UttakInput input(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag, LocalDate skjæringstidspunkt) {
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt),
            InntektArbeidYtelseGrunnlagBuilder.nytt().build(), fpGrunnlag);
    }

    private UttakInput input(Behandling behandling, LocalDate skjæringstidspunkt) {
        return input(behandling, fpGrunnlag(), skjæringstidspunkt);
    }

    private ForeldrepengerGrunnlag fpGrunnlag() {
        return fpGrunnlag(null);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(Annenpart annenpart) {
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser).medAnnenpart(annenpart);
    }

    @Test
    public void saldo_for_arbeidstype_uten_arbeidsgiver() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        var morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        var uttakAktivitetForMor = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        var aktivitetDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsgiver(null)
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .medTrekkdager(BigDecimal.valueOf(6 * 5))
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .build();
        var dto = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(LocalDate.of(2019, 2, 19), LocalDate.of(2019, 2, 19))
            .medAktiviteter(Collections.singletonList(aktivitetDto))
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medPeriodeResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato),
            Collections.singletonList(dto));

        // Assert
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
    }

    private Long lagre(Behandling morsBehandling) {
        return behandlingRepository.lagre(morsBehandling, lås(morsBehandling));
    }


    @Test
    public void riktig_saldo_for_mors_dersom_for_mange_dager_blir_trukket() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        var morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(15).minusDays(1),
            StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(15),
            fødseldato.plusWeeks(15 + 17).minusDays(1), StønadskontoType.FELLESPERIODE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (15 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (17 * 5));
        //Info: ikke relevant å teste maxdato da denne datoen ikke skal være satt dersom det finnes manuelle perioder som er eneste måten en kan få en negativ saldo.
    }

    private Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan_ved_flerbarnsdager() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        var morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6),
            fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, true, true);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 33 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var maxDagerFlerbarn = 17 * 5;
        final var stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP),
            lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK),
            lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK),
            lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));

        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(morsBehandling.getFagsak(), morsBehandling.getId(), stønadskontoberegning);


        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (10 * 5));
        var fbDto = saldoer.getStonadskontoer().get(StønadskontoType.FLERBARNSDAGER.getKode());
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - (10 * 5));
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 23 /* saldo FP */).minusDays(1));
    }

    @Test
    public void riktig_saldo_for_far_som_stjeler_flerbarnsdager_fra_mor() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE, false, true);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        var maxDagerFlerbarn = 17 * 5;
        var stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));
        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(behandlingMor.getFagsak(), behandlingMor.getId(), stønadskontoberegning);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(15).minusDays(1),
            StønadskontoType.FELLESPERIODE, false, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(15), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE, true, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            StønadskontoType.FELLESPERIODE, false, true);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        var fbDto = saldoer.getStonadskontoer().get(StønadskontoType.FLERBARNSDAGER.getKode());
        //5 uker mor, 4 uker som far stjeler fra mor, 1 uker der far og mor har samtidig uttak, 5 uker far
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - (15 * 5));
    }

    private void lagreEndringsdato(Behandling behandling) {
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medAvklarteDatoer(avklarteDatoer)
            .build();
        ytelsesFordelingRepository.lagre(behandling.getId(), ytelseFordelingAggregat);
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan_med_flere_arbeidsforhold() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor1 = arbeidsgiver("123");
        var virksomhetForMor2 = arbeidsgiver("456");
        var uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        var uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);

        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE, false,
            false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.of(new Trekkdager(25))),
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioMor.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        scenarioMor.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenarioMor.medUttak(uttakMor);
        var behandlingMor = scenarioMor.lagre(repositoryProvider);

        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        // Act
        var tjeneste = new SaldoerDtoTjeneste(stønadskontoSaldoTjeneste,
            new MaksDatoUttakTjenesteImpl(repositoryProvider.getFpUttakRepository(), stønadskontoSaldoTjeneste),
            stønadskontoRegelAdapter,
            repositoryProvider,
            uttakTjeneste,
            tapteDagerFpffTjeneste);
        var saldoer = tjeneste.lagStønadskontoerDto(input(behandlingMor, fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertThat(fpffDto.getMaxDager()).isEqualTo(maxDagerFPFF);
        assertThat(fpffDto.getAktivitetSaldoDtoList()).hasSize(2);
        assertThat(fpffDto.getAktivitetSaldoDtoList().get(0).getSaldo()).isEqualTo(0);
        assertThat(fpffDto.getAktivitetSaldoDtoList().get(1).getSaldo()).isEqualTo(0);

        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertThat(mkDto.getMaxDager()).isEqualTo(maxDagerMK);
        assertThat(mkDto.getAktivitetSaldoDtoList()).hasSize(2);
        assertThat(mkDto.getAktivitetSaldoDtoList().get(0).getSaldo()).isEqualTo(maxDagerMK - (6 * 5));
        assertThat(mkDto.getAktivitetSaldoDtoList().get(1).getSaldo()).isEqualTo(maxDagerMK - (6 * 5));

        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertThat(fpDto.getMaxDager()).isEqualTo(maxDagerFP);
        assertThat(fpDto.getAktivitetSaldoDtoList()).hasSize(2);
        var aktivitetSaldo1 = finnRiktigAktivitetSaldo(fpDto.getAktivitetSaldoDtoList(),
            uttakAktivitetForMor1);
        var aktivitetSaldo2 = finnRiktigAktivitetSaldo(fpDto.getAktivitetSaldoDtoList(),
            uttakAktivitetForMor2);
        assertThat(aktivitetSaldo1).isPresent();
        assertThat(aktivitetSaldo2).isPresent();
        assertThat(aktivitetSaldo1.get().getSaldo()).isEqualTo(maxDagerFP - 25);
        assertThat(aktivitetSaldo2.get().getSaldo()).isEqualTo(maxDagerFP - (10 * 5));
        assertThat(fpDto.getSaldo()).isEqualTo(maxDagerFP - 25);

        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 11 /* saldo FP */).minusDays(1));
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_uten_overlapp() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(6), fødseldato.plusWeeks(18).minusDays(1),
            StønadskontoType.FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (12 * 5));
        var fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(18 /* forbrukte uker */ + 4 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var aktiviteterMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        var virksomhetForFar = arbeidsgiver("456");
        var aktiviteterFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE);
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            StønadskontoType.FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (15 * 5));
        var fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(21 /* forbrukte uker */ + 1 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    private Behandling behandlingMedUttakFar(LocalDate fødseldato,
                                             Behandling behandlingMor,
                                             UttakResultatPerioderEntitet uttakFar) {
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenarioFar.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioFar.medUttak(uttakFar);

        var behandlingFar = scenarioFar.lagre(repositoryProvider);

        repositoryProvider.getFagsakRelasjonRepository()
            .kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak(), behandlingMor);
        return behandlingFar;
    }

    private void lagreStønadskontoBeregning(Behandling behandling,
                                            int maxDagerFPFF,
                                            int maxDagerFP,
                                            int maxDagerFK,
                                            int maxDagerMK) {
        final var stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP),
            lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK),
            lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK));
        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private Behandling avsluttetBehandlingMedUttak(LocalDate fødseldato,
                                                   AbstractTestScenario<?> scenarioMor,
                                                   UttakResultatPerioderEntitet uttak) {
        scenarioMor.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        scenarioMor.medUttak(uttak);
        return lagAvsluttet(scenarioMor);
    }

    private Behandling lagAvsluttet(AbstractTestScenario<?> scenario) {
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_samtidig_uttak() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE, true, false);


        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (15 * 5));
        var fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(16 /* forbrukte uker */ + 1 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_gradering_på_motpart() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor1 = arbeidsgiver("123");
        var virksomhetForMor2 = arbeidsgiver("789");
        var uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        var uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            StønadskontoType.FORELDREPENGER_FØR_FØDSEL, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE, false,
            false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.of(new Trekkdager(10))),
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            StønadskontoType.FELLESPERIODE);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            StønadskontoType.FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        var fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        var fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (5 /* gradert uttak 20% 5 uker */ + 50 /* fullt uttak 10 uker */));
        var fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(
            fødseldato.plusWeeks(21 /* forbrukte uker */ + 5 /* saldo FP */ + 15 /* saldo FK */).minusDays(1));
    }

    private Optional<AktivitetSaldoDto> finnRiktigAktivitetSaldo(List<AktivitetSaldoDto> aktivitetSaldoer,
                                                                 UttakAktivitetEntitet aktivitetEntitet) {
        return aktivitetSaldoer.stream().filter(as -> {
            var aktId = as.getAktivitetIdentifikator();
            return aktId.getUttakArbeidType().equals(aktivitetEntitet.getUttakArbeidType()) &&
                aktId.getArbeidsgiverReferanse()
                    .equals(aktivitetEntitet.getArbeidsgiver().isPresent() ? aktivitetEntitet.getArbeidsgiver()
                        .get()
                        .getIdentifikator() : null) &&
                aktId.getArbeidsforholdId().equals(aktivitetEntitet.getArbeidsforholdRef().getReferanse());
        }).findFirst();
    }


    private void assertKonto(StønadskontoDto stønadskontoDto, int maxDager, int saldo) {
        assertThat(stønadskontoDto.getMaxDager()).isEqualTo(maxDager);
        assertThat(stønadskontoDto.getAktivitetSaldoDtoList()).hasSize(1);
        assertThat(stønadskontoDto.getSaldo()).isEqualTo(saldo);
        assertThat(stønadskontoDto.getAktivitetSaldoDtoList().get(0).getSaldo()).isEqualTo(saldo);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, uttakAktivitet, fom, tom, stønadskontoType, false, false);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, samtidigUttak, flerbarnsdager,
            new UttakAktivitetMedTrekkdager(uttakAktivitet, Optional.empty()));
    }

    private static record UttakAktivitetMedTrekkdager(UttakAktivitetEntitet aktivitet, Optional<Trekkdager> trekkdagerOptional) {}

    @SafeVarargs
    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager,
                            UttakAktivitetMedTrekkdager... aktiviteter) {

        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medSamtidigUttak(samtidigUttak)
            .medFlerbarnsdager(flerbarnsdager)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        for (var aktivitetTuple : aktiviteter) {
            Trekkdager trekkdager = aktivitetTuple.trekkdagerOptional()
                .orElseGet(() -> new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(new Periode(periode.getFom(), periode.getTom()),
                    false, BigDecimal.ZERO, null).decimalValue()));

            var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
                aktivitetTuple.aktivitet())
                .medTrekkdager(trekkdager)
                .medTrekkonto(stønadskontoType)
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .build();
            periode.leggTilAktivitet(aktivitet);

        }
    }

    private UttakAktivitetEntitet lagUttakAktivitet(Arbeidsgiver arbeidsgiver) {
        return new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }

    private Behandling lagBehandling(RelasjonsRolleType relasjonsRolleType) {
        AbstractTestScenario<?> scenario;
        if (relasjonsRolleType.equals(RelasjonsRolleType.MORA)) {
            scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        } else {
            scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        }
        scenario.medDefaultBekreftetTerminbekreftelse();
        scenario.medDefaultOppgittDekningsgrad();
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        return scenario.lagre(repositoryProvider);
    }
}
