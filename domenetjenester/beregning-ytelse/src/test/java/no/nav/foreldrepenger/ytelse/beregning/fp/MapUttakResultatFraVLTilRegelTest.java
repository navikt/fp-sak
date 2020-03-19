package no.nav.foreldrepenger.ytelse.beregning.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.AVSLÅTT;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.INNVILGET;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.fp.MapUttakResultatFraVLTilRegel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;

public class MapUttakResultatFraVLTilRegelTest {

    private static final String ARBEIDSFORHOLD_ORGNR = "000000000";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final LocalDate TERMIN_DATO = LocalDate.now();
    private static final LocalDate TOM_FELLESPERIODE = TERMIN_DATO.plusWeeks(20).minusDays(1);
    private static final LocalDate TOM_MØDREKVOTE = TERMIN_DATO.plusWeeks(10).minusDays(1);
    private static final LocalDate TOM_FØR_FØDSEL = TERMIN_DATO.minusDays(1);
    private static final LocalDate FOM_FELLESPERIODE = TERMIN_DATO.plusWeeks(10);
    private static final LocalDate FOM_MØDREKVOTE = TERMIN_DATO;
    private static final LocalDate FOM_FØR_FØDSEL = TERMIN_DATO.minusWeeks(3);

    private UttakResultatEntitet vlPlan;
    private Behandling behandling;
    private MapUttakResultatFraVLTilRegel mapper;

    @Before
    public void setup() {
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor().build();
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Behandlingsresultat.opprettFor(behandling);
        vlPlan = lagUttakResultatPlan();
        mapper = new MapUttakResultatFraVLTilRegel() {
            @Override
            protected BigDecimal finnStillingsprosent(UttakInput input, UttakResultatPeriodeAktivitetEntitet uttakAktivitet) {
                return BigDecimal.valueOf(50);
            }
        };
    }

    @Test
    public void skalMappeUttakResultatPlan() {
        // Act
        UttakResultat regelPlan = mapper.mapFra(vlPlan, lagRef(behandling));

        // Assert
        assertThat(regelPlan).isNotNull();
        var uttakResultatPerioder = regelPlan.getUttakResultatPerioder();
        assertThat(uttakResultatPerioder).isNotNull();
        assertThat(uttakResultatPerioder).hasSize(3);

        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode førFødselPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FØR_FØDSEL);
        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode mødrePeriode = getPeriodeByFom(uttakResultatPerioder, FOM_MØDREKVOTE);
        no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode fellesPeriode = getPeriodeByFom(uttakResultatPerioder, FOM_FELLESPERIODE);

        assertPeriode(førFødselPeriode, FOM_FØR_FØDSEL, TOM_FØR_FØDSEL);
        assertPeriode(mødrePeriode, FOM_MØDREKVOTE, TOM_MØDREKVOTE);
        assertPeriode(fellesPeriode, FOM_FELLESPERIODE, TOM_FELLESPERIODE);
    }

    private UttakInput lagRef(Behandling behandling) {
        LocalDate skjæringstidspunkt = LocalDate.now();
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt), null, null);
    }

    @Test
    public void skalMappeUttakAktivitet() {
        //Arrange
        BigDecimal prosentArbeid = BigDecimal.valueOf(10);
        BigDecimal prosentUtbetaling = BigDecimal.valueOf(66); //overstyrt
        UttakResultatEntitet uttakPlan = lagUttaksPeriode(prosentArbeid, prosentUtbetaling);
        //Act
        UttakResultat resultat = mapper.mapFra(uttakPlan, lagRef(behandling));
        //Assert
        UttakResultatPeriode resultPeriode = onlyOne(resultat);
        UttakAktivitet uttakAktivitet = resultPeriode.getUttakAktiviteter().get(0);
        assertThat(uttakAktivitet.getUtbetalingsgrad()).isEqualByComparingTo(prosentUtbetaling);
        assertThat(uttakAktivitet.getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(uttakAktivitet.getAktivitetStatus()).isEqualTo(AktivitetStatus.ATFL);
        assertThat(uttakAktivitet.isErGradering()).isTrue();
        assertThat(uttakAktivitet.getArbeidsforhold().getIdentifikator()).isEqualTo(ARBEIDSFORHOLD_ORGNR);
        assertThat(uttakAktivitet.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARBEIDSFORHOLD_ID.getReferanse());
        assertThat(uttakAktivitet.getArbeidsforhold().erFrilanser()).isFalse();
    }

    private void assertPeriode(UttakResultatPeriode periode, LocalDate expectedFom, LocalDate expectedTom) {
        assertThat(periode).isNotNull();
        assertThat(periode.getFom()).as("fom").isEqualTo(expectedFom);
        assertThat(periode.getTom()).as("tom").isEqualTo(expectedTom);
        assertThat(periode.getErOppholdsPeriode()).isFalse();
    }

    private no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode getPeriodeByFom(List<UttakResultatPeriode> uttakResultatPerioder, LocalDate fom) {
        return uttakResultatPerioder.stream().filter(a -> fom.equals(a.getFom())).findFirst().orElse(null);
    }

    private UttakResultatEntitet lagUttakResultatPlan() {
        UttakResultatPeriodeEntitet førFødselPeriode = lagUttakResultatPeriode(FORELDREPENGER_FØR_FØDSEL, FOM_FØR_FØDSEL, TOM_FØR_FØDSEL, INNVILGET);
        UttakResultatPeriodeEntitet mødrekvote = lagUttakResultatPeriode(MØDREKVOTE, FOM_MØDREKVOTE, TOM_MØDREKVOTE, INNVILGET);
        UttakResultatPeriodeEntitet fellesperiode = lagUttakResultatPeriode(FELLESPERIODE, FOM_FELLESPERIODE, TOM_FELLESPERIODE, AVSLÅTT);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        perioder.leggTilPeriode(førFødselPeriode);
        perioder.leggTilPeriode(mødrekvote);
        perioder.leggTilPeriode(fellesperiode);

        UttakResultatEntitet resultat = new UttakResultatEntitet();
        resultat.setOpprinneligPerioder(perioder);
        return resultat;
    }

    private UttakResultatPeriode onlyOne(UttakResultat resultat) {
        assertThat(resultat.getUttakResultatPerioder()).hasSize(1);
        return resultat.getUttakResultatPerioder().iterator().next();
    }

    private UttakResultatEntitet lagUttaksPeriode(BigDecimal prosentArbeid, BigDecimal prosentUtbetaling) {
        LocalDate idag = LocalDate.now();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(idag, idag.plusDays(6))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medGraderingInnvilget(true)
            .build();
        UttakAktivitetEntitet uttakAktivtet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivtet)
            .medUtbetalingsprosent(prosentUtbetaling)
            .medArbeidsprosent(prosentArbeid)
            .medErSøktGradering(true)
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        return new UttakResultatEntitet.Builder(Mockito.mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(perioder)
            .build();
    }

    private UttakResultatPeriodeEntitet lagUttakResultatPeriode(UttakPeriodeType periodeType, LocalDate fom, LocalDate tom, PeriodeResultatType periodeResultatType) {
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeSøknadEntitet søknadPeriode = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(periodeType)
            .medMottattDato(LocalDate.now())
            .build();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(periodeResultatType, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(søknadPeriode)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsprosent(BigDecimal.ZERO)
            .medErSøktGradering(true)
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        return periode;
    }
}
