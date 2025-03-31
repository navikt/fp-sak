package no.nav.foreldrepenger.behandlingslager.uttak.fp;


import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.INNLEGGELSE_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class FpUttakRepositoryTest extends EntityManagerAwareTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private FpUttakRepository fpUttakRepository;

    @BeforeEach
    public void setUp() {
        fpUttakRepository = new FpUttakRepository(getEntityManager());
    }

    @Test
    void hentOpprinneligUttakResultat() {
        //Arrange
        var fom = LocalDate.now();
        var tom = fom.plusMonths(3);
        var trekkonto = UttakPeriodeType.FORELDREPENGER;
        var resultatType = PeriodeResultatType.INNVILGET;
        var perioder = opprettUttakResultatPeriode(resultatType, fom, tom, trekkonto);

        //Act
        var behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), perioder);

        //Assert
        var hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(hentetUttakResultatOpt).isPresent();
        var hentetUttakResultat = hentetUttakResultatOpt.get();

        assertThat(hentetUttakResultat.getStønadskontoberegning().getStønadskontoutregning()).isEqualTo(Map.of(StønadskontoType.FORELDREPENGER, 200));

        var resultat = hentetUttakResultat.getOpprinneligPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(fom);
        assertThat(resultat.get(0).getTom()).isEqualTo(tom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(resultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(trekkonto);
        assertThat(resultat.get(0).getDokRegel()).isNotNull();
        assertThat(resultat.get(0).getPeriodeSøknad()).isNotEmpty();
        assertThat(resultat.get(0).getAktiviteter().get(0).getUttakAktivitet()).isNotNull();
        assertThat(resultat.get(0).getPeriodeSøknad().orElseThrow().getDokumentasjonVurdering().type()).isEqualTo(
            INNLEGGELSE_SØKER_GODKJENT);
    }

    @Test
    void skal_kunne_endre_opprinnelig_flere_ganger_uten_å_feile_pga_unikhetssjekk_for_aktiv() {
        var uttakResultat1 = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var uttakResultat2 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var uttakResultat3 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);

        //Act
        var behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), uttakResultat1);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt1);
        assertOpprinneligHarResultatType(PeriodeResultatType.MANUELL_BEHANDLING, behandlingId);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).get().getOverstyrtPerioder()).isNotNull();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), uttakResultat2);
        assertOpprinneligHarResultatType(PeriodeResultatType.AVSLÅTT, behandlingId);
        assertThat(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).get().getOverstyrtPerioder()).isNull();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), uttakResultat3);
        assertOpprinneligHarResultatType(PeriodeResultatType.INNVILGET, behandlingId);
    }

    @Test
    void hentOverstyrtUttakResultat() {
        //Arrange
        var opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), opprinnelig);

        var overstyrtFom = LocalDate.now().plusDays(1);
        var overstyrtTom = LocalDate.now().plusMonths(4);
        var overstyrtResultatType = PeriodeResultatType.AVSLÅTT;
        var overstyrtKonto = UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
        var overstyrt = opprettUttakResultatPeriode(
            overstyrtResultatType,
            overstyrtFom,
            overstyrtTom,
            overstyrtKonto);

        //Act
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt);

        //Assert
        var hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(hentetUttakResultatOpt).isPresent();
        var hentetUttakResultat = hentetUttakResultatOpt.get();

        assertThat(hentetUttakResultat.getOpprinneligPerioder().getPerioder()).hasSize(1);
        var resultat = hentetUttakResultat.getOverstyrtPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(overstyrtFom);
        assertThat(resultat.get(0).getTom()).isEqualTo(overstyrtTom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(overstyrtResultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(overstyrtKonto);
    }

    @Test
    void endringAvOverstyrtSkalResultereINyttUttakResultatMedSammeOpprinnelig() {
        var opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var overstyrt2 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(),
            LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER);
        var behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), opprinnelig);

        //Act
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt1);
        assertOverstyrtHarResultatType(PeriodeResultatType.AVSLÅTT, behandlingId);
        assertOpprinneligHarResultatType(PeriodeResultatType.MANUELL_BEHANDLING, behandlingId);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, overstyrt2);
        assertOverstyrtHarResultatType(PeriodeResultatType.INNVILGET, behandlingId);
        assertOpprinneligHarResultatType(PeriodeResultatType.MANUELL_BEHANDLING, behandlingId);
    }

    @Test
    void utbetalingsgradOgArbeidstidsprosentSkalHa2Desimaler() {
        //Arrange
        var opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), UttakPeriodeType.FORELDREPENGER,
            new BigDecimal("10.55"), new Utbetalingsgrad(20.57));
        var behandlingId = lagBehandling();
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, lagKontoberegning(), opprinnelig);

        //Assert
        var hentetUttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);

        assertThat(hentetUttakResultatOpt.orElseThrow().getStønadskontoberegning().getStønadskontoutregning()).isEqualTo(Map.of(StønadskontoType.FORELDREPENGER, 200));

        var aktivitet = hentetUttakResultatOpt.orElseThrow().getGjeldendePerioder().getPerioder().get(0).getAktiviteter().get(0);
        assertThat(aktivitet.getUtbetalingsgrad().decimalValue()).isEqualTo(new BigDecimal("20.57"));
        assertThat(aktivitet.getArbeidsprosent()).isEqualTo(new BigDecimal("10.55"));
    }

    private void assertOverstyrtHarResultatType(PeriodeResultatType type, Long behandlingId) {
        var uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOverstyrtPerioder());
    }

    private void assertOpprinneligHarResultatType(PeriodeResultatType type, Long behandlingId) {
        var uttakResultatEntitet = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOpprinneligPerioder());
    }

    private void assertHarResultatType(PeriodeResultatType type, UttakResultatPerioderEntitet perioderEntitet) {
        var perioder = perioderEntitet.getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getResultatType()).isEqualTo(type);
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     UttakPeriodeType trekkonto) {
        return opprettUttakResultatPeriode(resultat, fom, tom, trekkonto, new BigDecimal("100.00"));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     UttakPeriodeType trekkonto,
                                                                     BigDecimal graderingArbeidsprosent) {
        return opprettUttakResultatPeriode(resultat, fom, tom, trekkonto, graderingArbeidsprosent, new Utbetalingsgrad(100));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     UttakPeriodeType trekkonto,
                                                                     BigDecimal graderingArbeidsprosent,
                                                                     Utbetalingsgrad utbetalingsgrad) {

        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ORGNR), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medDokumentasjonVurdering(new DokumentasjonVurdering(INNLEGGELSE_SØKER_GODKJENT))
            .build();
        var dokRegel = new UttakResultatDokRegelEntitet.Builder()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        var periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(trekkonto)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medArbeidsprosent(graderingArbeidsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        var perioder = new UttakResultatPerioderEntitet();
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

    private Stønadskontoberegning lagKontoberegning() {
        var stønadskontoberegningBuilder = Stønadskontoberegning.builder()
            .medRegelEvaluering("inn")
            .medRegelInput("ut");

        var stønadskonto = Stønadskonto.builder()
            .medMaxDager(200)
            .medStønadskontoType(StønadskontoType.FORELDREPENGER)
            .build();
        stønadskontoberegningBuilder.medStønadskonto(stønadskonto);
        return stønadskontoberegningBuilder.build();
    }
}
