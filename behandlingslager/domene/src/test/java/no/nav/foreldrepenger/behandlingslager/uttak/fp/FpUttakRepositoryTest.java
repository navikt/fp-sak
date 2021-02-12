package no.nav.foreldrepenger.behandlingslager.uttak.fp;


import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class FpUttakRepositoryTest extends EntityManagerAwareTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private FpUttakRepository fpUttakRepository;

    @BeforeEach
    public void setUp() {
        fpUttakRepository = new FpUttakRepository(getEntityManager());
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
        Long behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, perioder);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();

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
        Long behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, uttakResultat1);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt1);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT, behandlingId);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).get().getOverstyrtPerioder()).isNotNull();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, uttakResultat2);
        assertOpprinneligHarResultatType(PeriodeResultatType.AVSLÅTT, behandlingId);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).get().getOverstyrtPerioder()).isNull();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, uttakResultat3);
        assertOpprinneligHarResultatType(PeriodeResultatType.INNVILGET, behandlingId);
    }

    @Test
    public void hentOverstyrtUttakResultat() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        Long behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, opprinnelig);

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
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();

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
        Long behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, opprinnelig);

        //Act
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt1);
        assertOverstyrtHarResultatType(PeriodeResultatType.AVSLÅTT, behandlingId);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT, behandlingId);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt2);
        assertOverstyrtHarResultatType(PeriodeResultatType.INNVILGET, behandlingId);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT, behandlingId);
    }

    @Test
    public void utbetalingsgradOgArbeidstidsprosentSkalHa2Desimaler() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER,
            new BigDecimal("10.55"), new Utbetalingsgrad(20.57));
        Long behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, opprinnelig);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);

        UttakResultatPeriodeAktivitetEntitet aktivitet = hentetUttakResultatOpt.orElseThrow().getGjeldendePerioder().getPerioder().get(0).getAktiviteter().get(0);
        assertThat(aktivitet.getUtbetalingsgrad().decimalValue()).isEqualTo(new BigDecimal("20.57"));
        assertThat(aktivitet.getArbeidsprosent()).isEqualTo(new BigDecimal("10.55"));
    }

    private void assertOverstyrtHarResultatType(PeriodeResultatType type, Long behandlingId) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOverstyrtPerioder());
    }

    private void assertOpprinneligHarResultatType(PeriodeResultatType type, Long behandlingId) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOpprinneligPerioder());
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
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, graderingArbeidsprosent, new Utbetalingsgrad(100));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent,
                                                                     Utbetalingsgrad utbetalingsgrad) {

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
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

    private Long lagBehandling() {
        var entityManager = getEntityManager();
        var behandling = new BasicBehandlingBuilder(entityManager)
            .opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(entityManager).lagre(behandling.getId(), behandlingsresultat);
        return behandling.getId();
    }
}
