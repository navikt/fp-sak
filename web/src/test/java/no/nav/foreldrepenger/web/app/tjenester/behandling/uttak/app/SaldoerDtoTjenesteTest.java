package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
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
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Periode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetSaldoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.StønadskontoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;

class SaldoerDtoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;

    private SaldoerDtoTjeneste tjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        var uttakRepositoryProvider = new UttakRepositoryProvider(entityManager);
        fpUttakRepository = uttakRepositoryProvider.getFpUttakRepository();
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);
        var utregnetKontoTjeneste = new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, uttakTjeneste);
        var stønadskontoSaldoTjeneste = new StønadskontoSaldoTjeneste(uttakRepositoryProvider, utregnetKontoTjeneste);
        var tapteDagerFpffTjeneste = new TapteDagerFpffTjeneste(uttakRepositoryProvider,
            new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager)));
        tjeneste = new SaldoerDtoTjeneste(stønadskontoSaldoTjeneste, uttakTjeneste, tapteDagerFpffTjeneste, utregnetKontoTjeneste);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    private static Stønadskonto lagStønadskonto(StønadskontoType stønadskontoType, int maxDager) {
        return Stønadskonto.builder().medMaxDager(maxDager).medStønadskontoType(stønadskontoType).build();
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
    void riktig_saldo_for_mors_uttaksplan() {

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
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6),
            fødseldato.plusWeeks(16).minusDays(1), FELLESPERIODE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        lagre(morsBehandling);
        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var kontoer = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        lagreStønadskontoBeregning(morsBehandling, kontoer);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), kontoer, uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);


        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 10 * 5);
    }

    private void lagre(Behandlingsresultat behandlingsresultatForMor) {
        behandlingsresultatRepository
            .lagre(behandlingsresultatForMor.getBehandlingId(), behandlingsresultatForMor);
    }

    private BehandlingLås lås(Behandling behandling) {
        return new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId());
    }

    private UttakInput input(Behandling behandling, Annenpart annenpart, LocalDate skjæringstidspunkt) {
        return input(behandling, annenpart, skjæringstidspunkt, 1);
    }
    private UttakInput input(Behandling behandling, Annenpart annenpart, LocalDate skjæringstidspunkt, int antallBarn) {
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medFørsteUttaksdato(skjæringstidspunkt)
            .medUtenMinsterett(true)
            .medKreverSammenhengendeUttak(false);
        return new UttakInput(BehandlingReferanse.fra(behandling), stp.build(),
                InntektArbeidYtelseGrunnlagBuilder.nytt().build(), fpGrunnlag(annenpart, skjæringstidspunkt, antallBarn));
    }

    private UttakInput input(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag, LocalDate skjæringstidspunkt) {
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medFørsteUttaksdato(skjæringstidspunkt)
            .medUtenMinsterett(true)
            .medKreverSammenhengendeUttak(false);
        return new UttakInput(BehandlingReferanse.fra(behandling), stp.build(),
                InntektArbeidYtelseGrunnlagBuilder.nytt().build(), fpGrunnlag);
    }

    private UttakInput inputFAB(Behandling behandling, ForeldrepengerGrunnlag fpGrunnlag, LocalDate skjæringstidspunkt) {
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medFørsteUttaksdato(skjæringstidspunkt)
            .medUtenMinsterett(false)
            .medKreverSammenhengendeUttak(false);
        return new UttakInput(BehandlingReferanse.fra(behandling), stp.build(),
                InntektArbeidYtelseGrunnlagBuilder.nytt().build(), fpGrunnlag);
    }

    private UttakInput input(Behandling behandling, LocalDate skjæringstidspunkt) {
        return input(behandling, fpGrunnlag(null, skjæringstidspunkt, 1), skjæringstidspunkt);
    }

    private UttakInput input(Behandling behandling, LocalDate skjæringstidspunkt, int antallBarn) {
        return input(behandling, fpGrunnlag(null, skjæringstidspunkt, antallBarn), skjæringstidspunkt);
    }

    private ForeldrepengerGrunnlag fpGrunnlag() {
        return fpGrunnlag(null);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(Annenpart annenpart) {
        return fpGrunnlag(annenpart, LocalDate.now(), 1);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(Annenpart annenpart, LocalDate fødselsdato, int antallBarn) {
        var barn = new ArrayList<Barn>();
        for (var i = antallBarn; i > 0; i--) barn.add(new Barn());
        var familieHendelse = FamilieHendelse.forFødsel(null, fødselsdato, barn, antallBarn);
        var familieHendelser = new FamilieHendelser().medBekreftetHendelse(familieHendelse);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser).medAnnenpart(annenpart);
    }

    @Test
    void saldo_for_arbeidstype_uten_arbeidsgiver() {

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
            MØDREKVOTE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var kontoer = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        lagreStønadskontoBeregning(morsBehandling, kontoer);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), kontoer, uttakResultatPerioderForMor);
        lagre(morsBehandling);



        var aktivitetDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsgiver(null)
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .medTrekkdager(BigDecimal.valueOf(6 * 5))
            .medStønadskontoType(MØDREKVOTE)
            .build();
        var dto = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(LocalDate.of(2019, 2, 19), LocalDate.of(2019, 2, 19))
            .medAktiviteter(Collections.singletonList(aktivitetDto))
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato),
            Collections.singletonList(dto));

        // Assert
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
    }

    private Long lagre(Behandling morsBehandling) {
        return behandlingRepository.lagre(morsBehandling, lås(morsBehandling));
    }


    @Test
    void riktig_saldo_for_mors_dersom_for_mange_dager_blir_trukket() {

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
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(15).minusDays(1),
            MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(15),
            fødseldato.plusWeeks(15 + 17).minusDays(1), FELLESPERIODE);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var kontoer = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        lagreStønadskontoBeregning(morsBehandling, kontoer);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), kontoer, uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);


        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 15 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 17 * 5);
        //Info: ikke relevant å teste maxdato da denne datoen ikke skal være satt dersom det finnes manuelle perioder som er eneste måten en kan få en negativ saldo.
    }

    private Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    @Test
    void riktig_saldo_for_mors_uttaksplan_ved_flerbarnsdager() {

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
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6),
            fødseldato.plusWeeks(16).minusDays(1), FELLESPERIODE, true, true);

        var behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        lagre(behandlingsresultatForMor);

        lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 33 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var maxDagerFlerbarn = 17 * 5;
        var stønadskontoberegning = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP), lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK), lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK),
            lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));

        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(morsBehandling.getFagsak(), stønadskontoberegning);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), stønadskontoberegning, uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);



        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato, 2));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 10 * 5);
        var fbDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FLERBARNSDAGER);
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - 10 * 5);

    }

    @Test
    void riktig_saldo_for_far_som_stjeler_flerbarnsdager_fra_mor() {

        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE, false, true);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var maxDagerFlerbarn = 17 * 5;
        var stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));
        scenarioMor.medStønadskontoberegning(stønadskontoberegning);
        var behandlingMor = avsluttetBehandlingMedUttak(scenarioMor, uttakMor);

        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(behandlingMor.getFagsak(), stønadskontoberegning);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(15).minusDays(1),
            FELLESPERIODE, false, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(15), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE, true, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            FELLESPERIODE, false, true);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar, stønadskontoberegning);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(behandlingMor.getId(), fødseldato.atStartOfDay()), fødseldato));

        // Assert
        var fbDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FLERBARNSDAGER);
        //5 uker mor, 4 uker som far stjeler fra mor, 1 uker der far og mor har samtidig uttak, 5 uker far
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - 15 * 5);
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
    void riktig_saldo_for_mors_uttaksplan_med_flere_arbeidsforhold() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor1 = arbeidsgiver("123");
        var virksomhetForMor2 = arbeidsgiver("456");
        var uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        var uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);

        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            FORELDREPENGER_FØR_FØDSEL, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), MØDREKVOTE, false,
            false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.of(new Trekkdager(25))),
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioMor.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(Dekningsgrad._100);
        scenarioMor.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenarioMor.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        scenarioMor.medUttak(uttakMor);
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var kontoer = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        scenarioMor.medStønadskontoberegning(kontoer);
        var behandlingMor = scenarioMor.lagre(repositoryProvider);

        lagreStønadskontoBeregning(behandlingMor, kontoer);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(input(behandlingMor, fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(fpffDto.maxDager()).isEqualTo(maxDagerFPFF);
        assertThat(fpffDto.aktivitetSaldoDtoList()).hasSize(2);
        assertThat(fpffDto.aktivitetSaldoDtoList().getFirst().saldo()).isZero();
        assertThat(fpffDto.aktivitetSaldoDtoList().get(1).saldo()).isZero();

        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertThat(mkDto.maxDager()).isEqualTo(maxDagerMK);
        assertThat(mkDto.aktivitetSaldoDtoList()).hasSize(2);
        assertThat(mkDto.aktivitetSaldoDtoList().getFirst().saldo()).isEqualTo(maxDagerMK - 6 * 5);
        assertThat(mkDto.aktivitetSaldoDtoList().get(1).saldo()).isEqualTo(maxDagerMK - 6 * 5);

        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertThat(fpDto.maxDager()).isEqualTo(maxDagerFP);
        assertThat(fpDto.aktivitetSaldoDtoList()).hasSize(2);
        var aktivitetSaldo1 = finnRiktigAktivitetSaldo(fpDto.aktivitetSaldoDtoList(), uttakAktivitetForMor1);
        var aktivitetSaldo2 = finnRiktigAktivitetSaldo(fpDto.aktivitetSaldoDtoList(), uttakAktivitetForMor2);
        assertThat(aktivitetSaldo1).isPresent();
        assertThat(aktivitetSaldo2).isPresent();
        assertThat(aktivitetSaldo1.get().saldo()).isEqualTo(maxDagerFP - 25);
        assertThat(aktivitetSaldo2.get().saldo()).isEqualTo(maxDagerFP - 10 * 5);
        assertThat(fpDto.saldo()).isEqualTo(maxDagerFP - 25);
    }

    @Test
    void riktig_saldo_med_uttak_på_både_mor_og_fars_uten_overlapp() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            MØDREKVOTE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var stønadskontoberegning = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        scenarioMor.medStønadskontoberegning(stønadskontoberegning);
        var behandlingMor = avsluttetBehandlingMedUttak(scenarioMor, uttakMor);


        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(6), fødseldato.plusWeeks(18).minusDays(1),
            FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar, stønadskontoberegning);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(behandlingMor.getId(), fødseldato.atStartOfDay()), fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 12 * 5);
        var fkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FEDREKVOTE);
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    @Test
    void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var aktiviteterMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            MØDREKVOTE);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var stønadskontoberegning = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        scenarioMor.medStønadskontoberegning(stønadskontoberegning);
        var behandlingMor = avsluttetBehandlingMedUttak(scenarioMor, uttakMor);

        lagreStønadskontoBeregning(behandlingMor, stønadskontoberegning);


        var virksomhetForFar = arbeidsgiver("456");
        var aktiviteterFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE);
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar, stønadskontoberegning);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(behandlingMor.getId(), fødseldato.atStartOfDay()), fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 15 * 5);
        var fkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FEDREKVOTE);
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
    }

    private Behandling behandlingMedUttakFar(LocalDate fødseldato,
                                             Behandling behandlingMor,
                                             UttakResultatPerioderEntitet uttakFar, Stønadskontoberegning stønadskontoberegning) {
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        scenarioFar.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenarioFar.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioFar.medStønadskontoberegning(stønadskontoberegning);
        scenarioFar.medUttak(uttakFar);

        var behandlingFar = scenarioFar.lagre(repositoryProvider);

        repositoryProvider.getFagsakRelasjonRepository()
            .kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak());
        return behandlingFar;
    }

    private Stønadskontoberegning lagreStønadskontoBeregning(int maxDagerFPFF,
                                                             int maxDagerFP,
                                                             int maxDagerFK,
                                                             int maxDagerMK) {
        return lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP), lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK), lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK));
    }

    private void lagreStønadskontoBeregning(Behandling behandling,
                                            Stønadskontoberegning stønadskontoberegning) {
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);
    }

    private Behandling avsluttetBehandlingMedUttak(AbstractTestScenario<?> scenarioMor,
                                                   UttakResultatPerioderEntitet uttak) {
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(Dekningsgrad._100);
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
    void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_samtidig_uttak() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1),
            MØDREKVOTE);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var stønadskontoberegning = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        scenarioMor.medStønadskontoberegning(stønadskontoberegning);
        var behandlingMor = avsluttetBehandlingMedUttak(scenarioMor, uttakMor);

        lagreStønadskontoBeregning(behandlingMor, stønadskontoberegning);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE, true, false);


        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar, stønadskontoberegning);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(behandlingMor.getId(), fødseldato.atStartOfDay()), fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - 15 * 5);
        var fkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FEDREKVOTE);
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
    }

    @Test
    void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_gradering_på_motpart() {
        var fødseldato = LocalDate.of(2018, Month.MAY, 1);

        var virksomhetForMor1 = arbeidsgiver("123");
        var virksomhetForMor2 = arbeidsgiver("789");
        var uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        var uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);
        var uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            FORELDREPENGER_FØR_FØDSEL, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), MØDREKVOTE, false,
            false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.empty()), new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE, false, false,
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor1, Optional.of(new Trekkdager(10))),
            new UttakAktivitetMedTrekkdager(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var maxDagerFPFF = 3 * 5;
        var maxDagerFP = 16 * 5;
        var maxDagerFK = 15 * 5;
        var maxDagerMK = 15 * 5;
        var stønadskontoberegning = lagreStønadskontoBeregning(maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);
        scenarioMor.medStønadskontoberegning(stønadskontoberegning);
        var behandlingMor = avsluttetBehandlingMedUttak(scenarioMor, uttakMor);

        lagreStønadskontoBeregning(behandlingMor, stønadskontoberegning);

        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1),
            FELLESPERIODE);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1),
            FELLESPERIODE);

        var behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar, stønadskontoberegning);

        // Act
        var saldoer = tjeneste.lagStønadskontoerDto(
            input(behandlingFar, new Annenpart(behandlingMor.getId(), fødseldato.atStartOfDay()), fødseldato));

        // Assert
        var fpffDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        assertKonto(fpffDto, maxDagerFPFF, 0);
        var mkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MØDREKVOTE);
        assertKonto(mkDto, maxDagerMK, maxDagerMK - 6 * 5);
        var fpDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FELLESPERIODE);
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (5 /* gradert uttak 20% 5 uker */ + 50 /* fullt uttak 10 uker */));
        var fkDto = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FEDREKVOTE);
        assertKonto(fkDto, maxDagerFK, maxDagerFK);

    }

    @Test
    void bhfr_mor_ufør() {
        var fødseldato = LocalDate.of(2022, 1, 1);

        var uttakAktivitet = lagUttakAktivitet(arbeidsgiver("123"));
        var uttak = new UttakResultatPerioderEntitet();

        //13 uker
        var periode1 = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusWeeks(6), fødseldato.plusWeeks(20).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode1, uttakAktivitet)
            .medTrekkonto(FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(13 * 5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode1);
        //26 uker
        var periode2 = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusWeeks(20), fødseldato.plusWeeks(40).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode2, uttakAktivitet)
            .medTrekkonto(FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(26 * 5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode2);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(false, false, true, false, false));
        scenario.medSøknadHendelse().medFødselsDato(fødseldato, 1);
        scenario.medBekreftetHendelse().medFødselsDato(fødseldato, 1);
        var annenPartAktørId = AktørId.dummy();
        scenario.medSøknadAnnenPart().medAktørId(annenPartAktørId);
        var maxDager = 40 * 5;
        var stønadskontoberegning = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FORELDREPENGER, maxDager), lagStønadskonto(StønadskontoType.UFØREDAGER, 75));
        scenario.medStønadskontoberegning(stønadskontoberegning);
        var behandling = avsluttetBehandlingMedUttak(scenario, uttak);

        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);

        var input = input(behandling, fpGrunnlag(null, fødseldato, 1), fødseldato);
        var saldoer = tjeneste.lagStønadskontoerDto(input);

        var totalSaldo = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER);
        assertKonto(totalSaldo, maxDager, 5);
        var utenAktKravSaldo = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.UTEN_AKTIVITETSKRAV);
        assertKonto(utenAktKravSaldo, 15 * 5, 5);
    }

    @Test
    void bhfr_fab_minsterett() {
        var fødseldato = LocalDate.of(2022, 1, 1);

        var uttakAktivitet = lagUttakAktivitet(arbeidsgiver("123"));
        var uttak = new UttakResultatPerioderEntitet();

        //20 uker MSP
        var periode1 = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusWeeks(6), fødseldato.plusWeeks(26).minusDays(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.BARE_FAR_RETT_IKKE_SØKT).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode1, uttakAktivitet)
            .medTrekkonto(FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medTrekkdager(new Trekkdager(20 * 5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode1);
        //12 uker uttak m/aktivitet
        var periode2 = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusWeeks(26), fødseldato.plusWeeks(38).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode2, uttakAktivitet)
            .medTrekkonto(FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(12 * 5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode2);
        //6 uker minsterett
        var periode3 = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusYears(1), fødseldato.plusYears(1).plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode3, uttakAktivitet)
            .medTrekkonto(FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(6* 5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode3);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(false, false, true, false, false));
        var annenPartAktørId = AktørId.dummy();
        scenario.medSøknadAnnenPart().medAktørId(annenPartAktørId);
        var maxDager = 40 * 5;
        var stønadskontoberegning = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FORELDREPENGER, maxDager), lagStønadskonto(StønadskontoType.BARE_FAR_RETT, 40));
        scenario.medStønadskontoberegning(stønadskontoberegning);
        var behandling = avsluttetBehandlingMedUttak(scenario, uttak);

        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);

        var input = inputFAB(behandling, fpGrunnlag(), fødseldato);
        var saldoer = tjeneste.lagStønadskontoerDto(input);

        var totalSaldo = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER);
        assertKonto(totalSaldo, maxDager, 10 );
        var minsterettSaldo = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MINSTERETT);
        assertKonto(minsterettSaldo, 8 * 5, 10);
    }

    @Test
    void minsterett_neste_stønadsperiode() {
        var fødseldato = LocalDate.of(2022, 1, 1);

        var uttakAktivitet = lagUttakAktivitet(arbeidsgiver("123"));
        var uttak = new UttakResultatPerioderEntitet();

        var periode = new UttakResultatPeriodeEntitet.Builder(fødseldato.plusWeeks(6), fødseldato.plusWeeks(8).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build();
        var akt1 = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medTrekkonto(FEDREKVOTE)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(40))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode);
        var stønadskontoberegning = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FEDREKVOTE, 75), lagStønadskonto(StønadskontoType.TETTE_SAKER_FAR, 40));
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medStønadskontoberegning(stønadskontoberegning);
        var behandling = avsluttetBehandlingMedUttak(scenario, uttak);

        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);

        var saksnummer = behandling.getSaksnummer();
        var fødselNesteSak = periode.getTom().minusDays(1);
        var nesteSakGrunnlag = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId())
            .medSaksnummer(saksnummer)
            .medStartdato(fødselNesteSak.minusWeeks(3))
            .medHendelsedato(fødselNesteSak)
            .build();
        var fpGrunnlag = fpGrunnlag()
            .medNesteSakGrunnlag(nesteSakGrunnlag);
        var input = inputFAB(behandling, fpGrunnlag, fødseldato);
        var saldoer = tjeneste.lagStønadskontoerDto(input);

        var saldo = saldoer.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.MINSTERETT_NESTE_STØNADSPERIODE);
        var maxDager = 8 * 5;
        assertKonto(saldo, maxDager, maxDager - akt1.getTrekkdager().decimalValue().intValue());
    }

    @Test
    void skal_gi_tapte_dager_fpff_ved_fødsel_før_termin() {
        var fødseldato = LocalDate.of(2023, 11, 15);

        var uttakAktivitet = lagUttakAktivitet(arbeidsgiver("123"));
        var uttak = new UttakResultatPerioderEntitet();

        var fpff = new UttakResultatPeriodeEntitet.Builder(fødseldato.minusWeeks(2), fødseldato.minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.INNVILGET_FORELDREPENGER_FØR_FØDSEL).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fpff, uttakAktivitet).medTrekkonto(FORELDREPENGER_FØR_FØDSEL)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        var mødrekvote = new UttakResultatPeriodeEntitet.Builder(fødseldato, fødseldato.plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(mødrekvote, uttakAktivitet).medTrekkonto(MØDREKVOTE)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medTrekkdager(new Trekkdager(30))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(fpff);
        uttak.leggTilPeriode(mødrekvote);
        var termindato = fødseldato.plusWeeks(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriode(termindato.minusWeeks(3), termindato.minusDays(1))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(FORELDREPENGER_FØR_FØDSEL)
                .build(), OppgittPeriodeBuilder.ny()
                .medPeriode(termindato, termindato.plusWeeks(6).minusWeeks(1))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(MØDREKVOTE)
                .build()), true));
        var stønadskontoberegning = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, 15),
            lagStønadskonto(StønadskontoType.MØDREKVOTE, 15 * 5));
        scenario.medStønadskontoberegning(stønadskontoberegning);
        var behandling = avsluttetBehandlingMedUttak(scenario, uttak);


        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), stønadskontoberegning);

        var fpGrunnlag = fpGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(termindato, fødseldato, List.of(), 1))
                .medSøknadHendelse(FamilieHendelse.forFødsel(termindato, null, List.of(), 0)));
        var input = inputFAB(behandling, fpGrunnlag, fødseldato);
        var dto = tjeneste.lagStønadskontoerDto(input);

        var gjenværendeFpff = dto.stonadskontoer().get(SaldoerDto.SaldoVisningStønadskontoType.FORELDREPENGER_FØR_FØDSEL).saldo();
        assertThat(gjenværendeFpff).isEqualTo(5);
        assertThat(dto.tapteDagerFpff()).isEqualTo(5);
    }

    private Optional<AktivitetSaldoDto> finnRiktigAktivitetSaldo(List<AktivitetSaldoDto> aktivitetSaldoer,
                                                                 UttakAktivitetEntitet aktivitetEntitet) {
        return aktivitetSaldoer.stream().filter(as -> {
            var aktId = as.aktivitetIdentifikator();
            return aktId.uttakArbeidType().equals(aktivitetEntitet.getUttakArbeidType()) &&
                aktId.arbeidsgiverReferanse()
                    .equals(aktivitetEntitet.getArbeidsgiver().isPresent() ? aktivitetEntitet.getArbeidsgiver()
                        .get()
                        .getIdentifikator() : null) &&
                aktId.arbeidsforholdId().equals(aktivitetEntitet.getArbeidsforholdRef().getReferanse());
        }).findFirst();
    }


    private void assertKonto(StønadskontoDto stønadskontoDto, int maxDager, int saldo) {
        assertThat(stønadskontoDto.maxDager()).isEqualTo(maxDager);
        assertThat(stønadskontoDto.aktivitetSaldoDtoList()).hasSize(1);
        assertThat(stønadskontoDto.saldo()).isEqualTo(saldo);
        assertThat(stønadskontoDto.aktivitetSaldoDtoList().getFirst().saldo()).isEqualTo(saldo);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            UttakPeriodeType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, uttakAktivitet, fom, tom, stønadskontoType, false, false);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            UttakPeriodeType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, samtidigUttak, flerbarnsdager,
            new UttakAktivitetMedTrekkdager(uttakAktivitet, Optional.empty()));
    }

    private record UttakAktivitetMedTrekkdager(UttakAktivitetEntitet aktivitet, Optional<Trekkdager> trekkdagerOptional) {}

    @SafeVarargs
    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom, LocalDate tom,
                            UttakPeriodeType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager,
                            UttakAktivitetMedTrekkdager... aktiviteter) {

        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medSamtidigUttak(samtidigUttak)
            .medFlerbarnsdager(flerbarnsdager)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        for (var aktivitetTuple : aktiviteter) {
            var trekkdager = aktivitetTuple.trekkdagerOptional()
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
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        return scenario.lagre(repositoryProvider);
    }
}
