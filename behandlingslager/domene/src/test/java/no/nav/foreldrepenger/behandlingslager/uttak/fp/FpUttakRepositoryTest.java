package no.nav.foreldrepenger.behandlingslager.uttak.fp;


import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class FpUttakRepositoryTest {

    private static final String ORGNR = KUNSTIG_ORG;
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private final FpUttakRepository fpUttakRepository = new FpUttakRepository(repoRule.getEntityManager());
    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        var behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(repoRule.getEntityManager()).lagre(behandling.getId(), behandlingsresultat);
    }

    @Test
    public void hentOpprinneligUttakResultat() {
        //Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusMonths(3);
        StønadskontoType stønadskontoType = StønadskontoType.FORELDREPENGER;
        PeriodeResultatType resultatType = PeriodeResultatType.INNVILGET;
        UttakResultatPerioderEntitet perioder = opprettUttakResultatPeriode(resultatType, fom, tom, stønadskontoType);

        //Act
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), perioder);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();//NOSONAR

        List<UttakResultatPeriodeEntitet> resultat = hentetUttakResultat.getOpprinneligPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(fom);
        assertThat(resultat.get(0).getTom()).isEqualTo(tom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(resultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(stønadskontoType);
        assertThat(resultat.get(0).getDokRegel()).isNotNull();
        assertThat(resultat.get(0).getPeriodeSøknad()).isNotNull();
        assertThat(resultat.get(0).getAktiviteter().get(0).getUttakAktivitet()).isNotNull();
    }

    @Test
    public void skal_kunne_endre_opprinnelig_flere_ganger_uten_å_feile_pga_unikhetssjekk_for_aktiv() {
        UttakResultatPerioderEntitet uttakResultat1 = opprettUttakResultatPeriode(PeriodeResultatType.IKKE_FASTSATT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet uttakResultat2 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet uttakResultat3 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);

        //Act
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat1);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt1);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId()).get().getOverstyrtPerioder()).isNotNull(); //NOSONAR
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat2);
        assertOpprinneligHarResultatType(PeriodeResultatType.AVSLÅTT);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId()).get().getOverstyrtPerioder()).isNull(); //NOSONAR
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat3);
        assertOpprinneligHarResultatType(PeriodeResultatType.INNVILGET);
    }

    @Test
    public void hentOverstyrtUttakResultat() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        LocalDate overstyrtFom = LocalDate.now().plusDays(1);
        LocalDate overstyrtTom = LocalDate.now().plusMonths(4);
        PeriodeResultatType overstyrtResultatType = PeriodeResultatType.AVSLÅTT;
        StønadskontoType overstyrtKonto = StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
        UttakResultatPerioderEntitet overstyrt = opprettUttakResultatPeriode(
            overstyrtResultatType,
            overstyrtFom,
            overstyrtTom,
            overstyrtKonto);

        //Act
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();//NOSONAR

        assertThat(hentetUttakResultat.getOpprinneligPerioder().getPerioder()).hasSize(1);
        List<UttakResultatPeriodeEntitet> resultat = hentetUttakResultat.getOverstyrtPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(overstyrtFom);
        assertThat(resultat.get(0).getTom()).isEqualTo(overstyrtTom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(overstyrtResultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(overstyrtKonto);
    }

    @Test
    public void endringAvOverstyrtSkalResultereINyttUttakResultatMedSammeOpprinnelig() {
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.IKKE_FASTSATT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt2 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        //Act
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt1);
        assertOverstyrtHarResultatType(PeriodeResultatType.AVSLÅTT);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt2);
        assertOverstyrtHarResultatType(PeriodeResultatType.INNVILGET);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
    }

    @Test
    public void utbetalingsgradOgArbeidstidsprosentSkalHa2Desimaler() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER,
            new BigDecimal("10.55"), new BigDecimal("20.57"));
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());

        UttakResultatPeriodeAktivitetEntitet aktivitet = hentetUttakResultatOpt.get().getGjeldendePerioder().getPerioder().get(0).getAktiviteter().get(0);
        assertThat(aktivitet.getUtbetalingsgrad()).isEqualTo(new BigDecimal("20.57"));
        assertThat(aktivitet.getArbeidsprosent()).isEqualTo(new BigDecimal("10.55"));
    }

    private void assertOverstyrtHarResultatType(PeriodeResultatType type) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOverstyrtPerioder()); //NOSONAR
    }

    private void assertOpprinneligHarResultatType(PeriodeResultatType type) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOpprinneligPerioder()); //NOSONAR
    }

    private void assertHarResultatType(PeriodeResultatType type, UttakResultatPerioderEntitet perioderEntitet) {
        List<UttakResultatPeriodeEntitet> perioder = perioderEntitet.getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getResultatType()).isEqualTo(type);
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, new BigDecimal("100.00"));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, graderingArbeidsprosent, BigDecimal.valueOf(100));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent,
                                                                     BigDecimal utbetalingsgrad) {

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(BigDecimal.TEN)
            .build();
        UttakResultatDokRegelEntitet dokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medArbeidsprosent(graderingArbeidsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttakResultatPeriode);

        return perioder;
    }
}
