package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Periode;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class SatsReguleringUtil {

    private SatsReguleringUtil() {
    }

    static ProsessTaskData lagFinnSakerTask(String ytelse, String andel) {
        var data = ProsessTaskData.forProsessTask(GrunnbeløpFinnSakerTask.class);
        data.setProperty(GrunnbeløpFinnSakerTask.YTELSE_KEY, ytelse);
        data.setProperty(GrunnbeløpFinnSakerTask.REGULERING_TYPE_KEY, andel);
        data.setProperty(GrunnbeløpFinnSakerTask.REVURDERING_KEY, "true");
        return data;
    }

    static ProsessTaskData lagFinnArenaSakerTask(LocalDate arenaSatsDato, LocalDate arenaJustertDato) {
        var data = ProsessTaskData.forProsessTask(GrunnbeløpFinnSakerTask.class);
        data.setProperty(GrunnbeløpFinnSakerTask.YTELSE_KEY, "FP");
        data.setProperty(GrunnbeløpFinnSakerTask.REGULERING_TYPE_KEY, "ARENA");
        data.setProperty(GrunnbeløpFinnSakerTask.ARENA_SATSFOM_KEY, arenaSatsDato.toString());
        data.setProperty(GrunnbeløpFinnSakerTask.ARENA_JUSTERT_KEY, arenaJustertDato.toString());
        data.setProperty(GrunnbeløpFinnSakerTask.REVURDERING_KEY, "true");
        return data;
    }

    static Optional<ProsessTaskData> finnTaskFor(ArgumentCaptor<ProsessTaskData> captor, Behandling behandling) {
        return captor.getAllValues().stream().filter(t -> t.getFagsakId().equals(behandling.getFagsakId())).findFirst();
    }

    static Behandling opprettFPAT(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettFP(em, AktivitetStatus.ARBEIDSTAKER, status, uttakFom, sats, brutto);
    }

    static Behandling opprettFPMS(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettFP(em, AktivitetStatus.MILITÆR_ELLER_SIVIL, status, uttakFom, sats, brutto);
    }

    static Behandling opprettFPSN(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettFP(em, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, status, uttakFom, sats, brutto);
    }

    static Behandling opprettSVAT(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettSVP(em, AktivitetStatus.ARBEIDSTAKER, status, uttakFom, sats, brutto);
    }

    static Behandling opprettSVMS(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettSVP(em, AktivitetStatus.MILITÆR_ELLER_SIVIL, status, uttakFom, sats, brutto);
    }

    static Behandling opprettSVSN(EntityManager em, BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        return opprettSVP(em, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, status, uttakFom, sats, brutto);
    }

    private static Behandling opprettFP(EntityManager em,
                                        AktivitetStatus aStatus,
                                        BehandlingStatus status,
                                        LocalDate uttakFom,
                                        long sats,
                                        long brutto) {
        var repositoryProvider = new BehandlingRepositoryProvider(em);
        var beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(em);
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse().medFødselsDato(terminDato).medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(LocalDateTime.now()).build();
        }
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(BigDecimal.valueOf(sats)).medSkjæringstidspunkt(uttakFom).build();
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(aStatus).build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .medBruttoPrÅr(BigDecimal.valueOf(brutto))
            .medAvkortetPrÅr(BigDecimal.valueOf(brutto))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode).build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var virksomhetForUttak = arbeidsgiver("456");
        var uttakAktivitet = lagUttakAktivitet(virksomhetForUttak);
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioder, uttakAktivitet, uttakFom, uttakFom.plusWeeks(15).minusDays(1), UttakPeriodeType.MØDREKVOTE);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);

        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

    static Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    static UttakAktivitetEntitet lagUttakAktivitet(Arbeidsgiver arbeidsgiver) {
        return new UttakAktivitetEntitet.Builder().medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }

    static void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                           UttakAktivitetEntitet uttakAktivitet,
                           LocalDate fom,
                           LocalDate tom,
                           UttakPeriodeType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, uttakAktivitet);
    }

    static void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                           LocalDate fom,
                           LocalDate tom,
                           UttakPeriodeType stønadskontoType,
                           UttakAktivitetEntitet uttakAktivitetEntitet) {

        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(false)
            .medFlerbarnsdager(false)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        var trekkdager = new Trekkdager(
            TrekkdagerUtregningUtil.trekkdagerFor(new Periode(periode.getFom(), periode.getTom()), false, BigDecimal.ZERO, null).decimalValue());

        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitetEntitet).medTrekkdager(trekkdager)
            .medTrekkonto(stønadskontoType)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        periode.leggTilAktivitet(aktivitet);
    }

    private static Behandling opprettSVP(EntityManager em,
                                         AktivitetStatus aStatus,
                                         BehandlingStatus status,
                                         LocalDate uttakFom,
                                         long sats,
                                         long brutto) {
        return opprettSVP(em, aStatus, status, uttakFom, sats, brutto, 2300);
    }

    static Behandling opprettSVP(EntityManager em,
                                 AktivitetStatus aStatus,
                                 BehandlingStatus status,
                                 LocalDate uttakFom,
                                 long sats,
                                 long brutto,
                                 int dagsats) {
        var repositoryProvider = new BehandlingRepositoryProvider(em);
        var beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(em);
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse().medFødselsDato(terminDato).medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(LocalDateTime.now()).build();
        }
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(BigDecimal.valueOf(sats)).medSkjæringstidspunkt(uttakFom).build();
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(aStatus).build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .medBruttoPrÅr(BigDecimal.valueOf(brutto))
            .medAvkortetPrÅr(BigDecimal.valueOf(brutto))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode).build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var brFP = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();
        var brFPper = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3)).build(brFP);
        BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medDagsatsFraBg(1000)
            .medBrukerErMottaker(true)
            .medStillingsprosent(new BigDecimal(100))
            .medAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medUtbetalingsgrad(new BigDecimal(100))
            .build(brFPper);

        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

    static Behandling opprettES(EntityManager em, BehandlingStatus status, LocalDate fødselsdato, long sats) {
        var repositoryProvider = new BehandlingRepositoryProvider(em);
        var beregningRepository = new LegacyESBeregningRepository(em);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medSøknadDato(fødselsdato.minusDays(40));

        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(LocalDateTime.now()).build();
        }
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        var beregning = new LegacyESBeregning(sats, 1, sats, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder()
            .medBeregning(beregning)
            .buildFor(behandling, repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()));
        beregningRepository.lagre(beregningResultat, lås);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }
}
