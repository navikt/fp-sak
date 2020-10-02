package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TapteDagerFpffTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.StønadskontoRegelAdapter;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetIdentifikatorDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AktivitetSaldoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.StønadskontoDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class SaldoerDtoTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Inject
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Inject
    private FpUttakRepository fpUttakRepository;

    @Inject @FagsakYtelseTypeRef("FP")
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    @Inject
    private StønadskontoRegelAdapter stønadskontoRegelAdapter;

    @Inject
    private TapteDagerFpffTjeneste tapteDagerFpffTjeneste;

    @Inject
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private SaldoerDtoTjeneste tjeneste;

    private static Stønadskonto lagStønadskonto(StønadskontoType fellesperiode, int maxDager) {
        return Stønadskonto.builder().medMaxDager(maxDager).medStønadskontoType(fellesperiode).build();
    }

    private static Stønadskontoberegning lagStønadskontoberegning(Stønadskonto... stønadskontoer) {
        Stønadskontoberegning.Builder builder = Stønadskontoberegning.builder()
            .medRegelEvaluering("asdf")
            .medRegelInput("asdf");
        Stream.of(stønadskontoer)
            .forEach(builder::medStønadskonto);
        return builder.build();
    }

    @Before
    public void setUp() {
        tjeneste = new SaldoerDtoTjeneste(stønadskontoSaldoTjeneste, maksDatoUttakTjeneste,
            mock(ArbeidsgiverDtoTjeneste.class), stønadskontoRegelAdapter, repositoryProvider, uttakTjeneste, tapteDagerFpffTjeneste);
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan() {

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        Behandling morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandlingsresultat behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repositoryRule.getRepository().lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryRule.getRepository().lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);


        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (10 * 5));
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 6 /* saldo FP */).minusDays(1));
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

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        Behandling morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        UttakAktivitetEntitet uttakAktivitetForMor = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        UttakResultatPerioderEntitet uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);

        Behandlingsresultat behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repositoryRule.getRepository().lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryRule.getRepository().lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        UttakResultatPeriodeAktivitetLagreDto aktivitetDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsgiver(null)
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .medTrekkdager(BigDecimal.valueOf(6 * 5))
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .build();
        UttakResultatPeriodeLagreDto dto = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(LocalDate.of(2019, 2, 19), LocalDate.of(2019, 2, 19))
            .medAktiviteter(Collections.singletonList(aktivitetDto))
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medPeriodeResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato), Collections.singletonList(dto));

        // Assert
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
    }


    @Test
    public void riktig_saldo_for_mors_dersom_for_mange_dager_blir_trukket() {

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        Behandling morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(15).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(15), fødseldato.plusWeeks(15 + 17).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandlingsresultat behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repositoryRule.getRepository().lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryRule.getRepository().lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(morsBehandling, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (15 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (17 * 5));
        //Info: ikke relevant å teste maxdato da denne datoen ikke skal være satt dersom det finnes manuelle perioder som er eneste måten en kan få en negativ saldo.
    }

    private Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan_ved_flerbarnsdager() {

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        //
        // --- Mors behandling
        //
        Behandling morsBehandling = lagBehandling(RelasjonsRolleType.MORA);

        lagreEndringsdato(morsBehandling);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, true, true);

        Behandlingsresultat behandlingsresultatForMor = getBehandlingsresultat(morsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repositoryRule.getRepository().lagre(behandlingsresultatForMor);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryRule.getRepository().lagre(morsBehandling);


        //
        // --- Stønadskontoer
        //
        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 33 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        int maxDagerFlerbarn = 17 * 5;
        final Stønadskontoberegning stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP),
            lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK),
            lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK),
            lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));

        repositoryProvider.getFagsakRelasjonRepository().lagre(morsBehandling.getFagsak(), morsBehandling.getId(), stønadskontoberegning);


        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(morsBehandling, fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (10 * 5));
        StønadskontoDto fbDto = saldoer.getStonadskontoer().get(StønadskontoType.FLERBARNSDAGER.getKode());
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - (10 * 5));
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 23 /* saldo FP */).minusDays(1));
    }

    @Test
    public void riktig_saldo_for_far_som_stjeler_flerbarnsdager_fra_mor() {

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, false, true);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        int maxDagerFlerbarn = 17 * 5;
        Stønadskontoberegning stønadskontoberegning  = lagStønadskontoberegning(lagStønadskonto(StønadskontoType.FLERBARNSDAGER, maxDagerFlerbarn));
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandlingMor.getFagsak(), behandlingMor.getId(), stønadskontoberegning);

        Arbeidsgiver virksomhetForFar = arbeidsgiver("456");
        UttakAktivitetEntitet uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        UttakResultatPerioderEntitet uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(15).minusDays(1), StønadskontoType.FELLESPERIODE, false, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(15), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, true, true);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1), StønadskontoType.FELLESPERIODE, false, true);

        Behandling behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        StønadskontoDto fbDto = saldoer.getStonadskontoer().get(StønadskontoType.FLERBARNSDAGER.getKode());
        //5 uker mor, 4 uker som far stjeler fra mor, 1 uker der far og mor har samtidig uttak, 5 uker far
        assertKonto(fbDto, maxDagerFlerbarn, maxDagerFlerbarn - (15 * 5));
    }

    private void lagreEndringsdato(Behandling behandling) {
        AvklarteUttakDatoerEntitet avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), avklarteDatoer);
    }

    @Test
    public void riktig_saldo_for_mors_uttaksplan_med_flere_arbeidsforhold() {
        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor1 = arbeidsgiver("123");
        Arbeidsgiver virksomhetForMor2 = arbeidsgiver("456");
        UttakAktivitetEntitet uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        UttakAktivitetEntitet uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);

        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.empty()), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.empty()), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.of(new Trekkdager(25))), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioMor.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        scenarioMor.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenarioMor.medUttak(uttakMor);
        Behandling behandlingMor = scenarioMor.lagre(repositoryProvider);

        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        // Act
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhetForMor1.getOrgnr())).thenReturn(new Virksomhet.Builder().medOrgnr(virksomhetForMor1.getOrgnr()).build());
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhetForMor2.getOrgnr())).thenReturn(new Virksomhet.Builder().medOrgnr(virksomhetForMor2.getOrgnr()).build());

        SaldoerDtoTjeneste tjeneste = new SaldoerDtoTjeneste(stønadskontoSaldoTjeneste,
            new MaksDatoUttakTjenesteImpl(repositoryProvider.getFpUttakRepository(), stønadskontoSaldoTjeneste),
            new ArbeidsgiverDtoTjeneste(arbeidsgiverTjeneste),
            stønadskontoRegelAdapter,
            repositoryProvider,
            uttakTjeneste,
            tapteDagerFpffTjeneste);
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingMor, fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertThat(fpffDto.getMaxDager()).isEqualTo(maxDagerFPFF);
        assertThat(fpffDto.getAktivitetSaldoDtoList()).hasSize(2);
        assertThat(fpffDto.getAktivitetSaldoDtoList().get(0).getSaldo()).isEqualTo(0);
        assertThat(fpffDto.getAktivitetSaldoDtoList().get(1).getSaldo()).isEqualTo(0);

        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertThat(mkDto.getMaxDager()).isEqualTo(maxDagerMK);
        assertThat(mkDto.getAktivitetSaldoDtoList()).hasSize(2);
        assertThat(mkDto.getAktivitetSaldoDtoList().get(0).getSaldo()).isEqualTo(maxDagerMK - (6 * 5));
        assertThat(mkDto.getAktivitetSaldoDtoList().get(1).getSaldo()).isEqualTo(maxDagerMK - (6 * 5));

        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertThat(fpDto.getMaxDager()).isEqualTo(maxDagerFP);
        assertThat(fpDto.getAktivitetSaldoDtoList()).hasSize(2);
        Optional<AktivitetSaldoDto> aktivitetSaldo1 = finnRiktigAktivitetSaldo(fpDto.getAktivitetSaldoDtoList(), uttakAktivitetForMor1);
        Optional<AktivitetSaldoDto> aktivitetSaldo2 = finnRiktigAktivitetSaldo(fpDto.getAktivitetSaldoDtoList(), uttakAktivitetForMor2);
        assertThat(aktivitetSaldo1).isPresent();
        assertThat(aktivitetSaldo2).isPresent();
        assertThat(aktivitetSaldo1.get().getSaldo()).isEqualTo(maxDagerFP - 25);
        assertThat(aktivitetSaldo2.get().getSaldo()).isEqualTo(maxDagerFP - (10 * 5));
        assertThat(fpDto.getSaldo()).isEqualTo(maxDagerFP - 25);

        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(16 /* forbrukte uker */ + 9 /* saldo MK */ + 11 /* saldo FP */).minusDays(1));
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_uten_overlapp() {
        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        Arbeidsgiver virksomhetForFar = arbeidsgiver("456");
        UttakAktivitetEntitet uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        UttakResultatPerioderEntitet uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(6), fødseldato.plusWeeks(18).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandling behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (12 * 5));
        StønadskontoDto fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(18 /* forbrukte uker */ + 4 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp() {
        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet aktiviteterMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakMor, aktiviteterMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        Arbeidsgiver virksomhetForFar = arbeidsgiver("456");
        UttakAktivitetEntitet aktiviteterFar = lagUttakAktivitet(virksomhetForFar);
        UttakResultatPerioderEntitet uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);
        lagPeriode(uttakFar, aktiviteterFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandling behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (15 * 5));
        StønadskontoDto fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(21 /* forbrukte uker */ + 1 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    private Behandling behandlingMedUttakFar(LocalDate fødseldato, Behandling behandlingMor, UttakResultatPerioderEntitet uttakFar) {
        ScenarioFarSøkerForeldrepenger scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenarioFar.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioFar.medUttak(uttakFar);

        Behandling behandlingFar = scenarioFar.lagre(repositoryProvider);

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak(), behandlingMor);
        return behandlingFar;
    }

    private void lagreStønadskontoBeregning(Behandling behandling, int maxDagerFPFF, int maxDagerFP, int maxDagerFK, int maxDagerMK) {
        final Stønadskontoberegning stønadskontoberegning = lagStønadskontoberegning(
            lagStønadskonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, maxDagerFPFF),
            lagStønadskonto(StønadskontoType.FELLESPERIODE, maxDagerFP),
            lagStønadskonto(StønadskontoType.FEDREKVOTE, maxDagerFK),
            lagStønadskonto(StønadskontoType.MØDREKVOTE, maxDagerMK));
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private Behandling avsluttetBehandlingMedUttak(LocalDate fødseldato, AbstractTestScenario<?> scenarioMor, UttakResultatPerioderEntitet uttak) {
        scenarioMor.medSøknadHendelse().medFødselsDato(fødseldato);
        scenarioMor.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioMor.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        scenarioMor.medUttak(uttak);
        return lagAvsluttet(scenarioMor);
    }

    private Behandling lagAvsluttet(AbstractTestScenario<?> scenario) {
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_samtidig_uttak() {
        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        Arbeidsgiver virksomhetForFar = arbeidsgiver("456");
        UttakAktivitetEntitet uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        UttakResultatPerioderEntitet uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, true, false);


        Behandling behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (15 * 5));
        StønadskontoDto fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(16 /* forbrukte uker */ + 1 /* saldo FP */ + 15 /* saldo FK*/).minusDays(1));
    }

    @Test
    public void riktig_saldo_med_uttak_på_både_mor_og_fars_med_overlapp_og_gradering_på_motpart() {
        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        Arbeidsgiver virksomhetForMor1 = arbeidsgiver("123");
        Arbeidsgiver virksomhetForMor2 = arbeidsgiver("789");
        UttakAktivitetEntitet uttakAktivitetForMor1 = lagUttakAktivitet(virksomhetForMor1);
        UttakAktivitetEntitet uttakAktivitetForMor2 = lagUttakAktivitet(virksomhetForMor2);
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        lagPeriode(uttakMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.empty()), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.empty()), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));
        lagPeriode(uttakMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE, false, false,
            new Tuple<>(uttakAktivitetForMor1, Optional.of(new Trekkdager(10))), new Tuple<>(uttakAktivitetForMor2, Optional.empty()));

        AbstractTestScenario<?> scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandlingMor = avsluttetBehandlingMedUttak(fødseldato, scenarioMor, uttakMor);

        int maxDagerFPFF = 3 * 5;
        int maxDagerFP = 16 * 5;
        int maxDagerFK = 15 * 5;
        int maxDagerMK = 15 * 5;
        lagreStønadskontoBeregning(behandlingMor, maxDagerFPFF, maxDagerFP, maxDagerFK, maxDagerMK);

        Arbeidsgiver virksomhetForFar = arbeidsgiver("456");
        UttakAktivitetEntitet uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        UttakResultatPerioderEntitet uttakFar = new UttakResultatPerioderEntitet();
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(11), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);
        lagPeriode(uttakFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(21).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandling behandlingFar = behandlingMedUttakFar(fødseldato, behandlingMor, uttakFar);

        // Act
        SaldoerDto saldoer = tjeneste.lagStønadskontoerDto(input(behandlingFar, new Annenpart(false, behandlingMor.getId()), fødseldato));

        // Assert
        StønadskontoDto fpffDto = saldoer.getStonadskontoer().get(StønadskontoType.FORELDREPENGER_FØR_FØDSEL.getKode());
        assertKonto(fpffDto, maxDagerFPFF, 0);
        StønadskontoDto mkDto = saldoer.getStonadskontoer().get(StønadskontoType.MØDREKVOTE.getKode());
        assertKonto(mkDto, maxDagerMK, maxDagerMK - (6 * 5));
        StønadskontoDto fpDto = saldoer.getStonadskontoer().get(StønadskontoType.FELLESPERIODE.getKode());
        assertKonto(fpDto, maxDagerFP, maxDagerFP - (5 /* gradert uttak 20% 5 uker */ + 50 /* fullt uttak 10 uker */));
        StønadskontoDto fkDto = saldoer.getStonadskontoer().get(StønadskontoType.FEDREKVOTE.getKode());
        assertKonto(fkDto, maxDagerFK, maxDagerFK);
        assertThat(saldoer.getMaksDatoUttak()).isPresent();
        assertThat(saldoer.getMaksDatoUttak().get()).isEqualTo(fødseldato.plusWeeks(21 /* forbrukte uker */ + 5 /* saldo FP */ + 15 /* saldo FK */).minusDays(1));
    }

    private Optional<AktivitetSaldoDto> finnRiktigAktivitetSaldo(List<AktivitetSaldoDto> aktivitetSaldoer, UttakAktivitetEntitet aktivitetEntitet) {
        return aktivitetSaldoer.stream().filter(as -> {
            AktivitetIdentifikatorDto aktId = as.getAktivitetIdentifikator();
            return aktId.getUttakArbeidType().equals(aktivitetEntitet.getUttakArbeidType()) &&
                aktId.getArbeidsgiver().getIdentifikator().equals(aktivitetEntitet.getArbeidsgiver().isPresent() ? aktivitetEntitet.getArbeidsgiver().get().getIdentifikator() : null) &&
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
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, samtidigUttak, flerbarnsdager, new Tuple<>(uttakAktivitet, Optional.empty()));
    }

    @SafeVarargs
    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager,
                            Tuple<UttakAktivitetEntitet, Optional<Trekkdager>>... aktiviteter) {

        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medSamtidigUttak(samtidigUttak)
            .medFlerbarnsdager(flerbarnsdager)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        for (Tuple<UttakAktivitetEntitet, Optional<Trekkdager>> aktivitetTuple : aktiviteter) {
            Trekkdager trekkdager;
            if (aktivitetTuple.getElement2().isPresent()) {
                trekkdager = aktivitetTuple.getElement2().get();
            } else {
                trekkdager = new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(
                    new Periode(periode.getFom(), periode.getTom()),
                    false,
                    BigDecimal.ZERO,
                    null).decimalValue());
            }

            UttakResultatPeriodeAktivitetEntitet aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitetTuple.getElement1())
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
