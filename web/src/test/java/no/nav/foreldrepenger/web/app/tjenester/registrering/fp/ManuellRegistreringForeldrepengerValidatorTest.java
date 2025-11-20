package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.FrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;

class ManuellRegistreringForeldrepengerValidatorTest {

    @Test
    void skal_validere_dekningsgrad() {
        assertThat(ManuellRegistreringSøknadValidator.validerDekningsgrad(null)).as("Dekningsgrad skal ikke kunne være null").isNotEmpty();

        var dekningsgrad = DekningsgradDto.HUNDRE;
        assertThat(ManuellRegistreringSøknadValidator.validerDekningsgrad(dekningsgrad)).as("Deksningsgrad skal være gyldig når dekningsgrad er satt").isEmpty();
    }

    @Test
    void skal_validere_fellesperiode_for_far_eller_medmor_dato_satt_til_null() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigFellesPerioder());

        // Setter en av datoene i fellesperioden til null
        permisjon.getPermisjonsPerioder().get(0).setPeriodeFom(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fellesperiode_for_far_eller_medmor_overlappende_perioder() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigFellesPerioder());

        // Gjør perioder overlappende
        permisjon.getPermisjonsPerioder().get(1).setPeriodeFom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fellesperiode_for_far_eller_medmor() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigFellesPerioder());

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isNotPresent();
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fellesperiode_for_far_eller_medmor_start_for_sluttdato() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigFellesPerioder());

        // Sett start før slutt
        permisjon.getPermisjonsPerioder().get(1).setPeriodeTom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fedrekvote_dato_satt_til_null() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FEDREKVOTE));

        // Setter en av datoene i perioden til null
        permisjon.getPermisjonsPerioder().get(0).setPeriodeFom(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fedrekvote_overlappende_perioder() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FEDREKVOTE));

        // Gjør perioder overlappende
        permisjon.getPermisjonsPerioder().get(1).setPeriodeFom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fedrekvote_start_for_sluttdato() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FEDREKVOTE));

        // Sett start før slutt
        permisjon.getPermisjonsPerioder().get(1).setPeriodeTom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_fedrekvote() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FEDREKVOTE));

        var feltFeil = ManuellRegistreringSøknadValidator.validerPermisjonsperiode(permisjon);
        assertThat(feltFeil).isNotPresent();
        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_finne_manglende_mors_aktivitet_far_foreldrepenger() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FORELDREPENGER));

        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isNotEmpty();
    }

    @Test
    void skal_ignorere_manglende_mors_aktivitet_far_fedrekvote() {
        var permisjon = new TidsromPermisjonDto();
        permisjon.setPermisjonsPerioder(lagGyldigPermisjonPeriode(UttakPeriodeType.FEDREKVOTE));

        var feltFeilAktKrav = ManuellRegistreringSøknadValidator.validerAktivitetskravFarMedmor(permisjon);
        assertThat(feltFeilAktKrav).isEmpty();
    }

    @Test
    void skal_validere_utsettelse_dato_satt_til_null() {
        var utsettelsePerioder = lagGyldigUtsettelsePerioder();

        // Setter en av datoene i perioden til null
        utsettelsePerioder.get(0).setPeriodeFom(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerUtsettelse(utsettelsePerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_utsettelse_overlappende_perioder() {
        var utsettelsePerioder = lagGyldigUtsettelsePerioder();

        // Gjør perioder overlappende
        utsettelsePerioder.get(1).setPeriodeFom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerUtsettelse(utsettelsePerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
    }

    @Test
    void skal_validere_utsettelse_start_for_sluttdato() {
        var utsettelsePerioder = lagGyldigUtsettelsePerioder();

        // Sett start før slutt
        utsettelsePerioder.get(1).setPeriodeTom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerUtsettelse(utsettelsePerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
    }

    @Test
    void skal_validere_utsettelse_årsak_må_være_satt() {
        var utsettelsePerioder = lagGyldigUtsettelsePerioder();

        // Sett start årsak til null
        utsettelsePerioder.get(1).setArsakForUtsettelse(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerUtsettelse(utsettelsePerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_utsettelse() {
        var utsettelsePerioder = lagGyldigUtsettelsePerioder();

        var feltFeil = ManuellRegistreringSøknadValidator.validerUtsettelse(utsettelsePerioder);
        assertThat(feltFeil).isEmpty();
    }

    @Test
    void skal_validere_gradering_dato_satt_til_null() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Setter en av datoene i perioden til null
        graderingPerioder.get(0).setPeriodeFom(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_gradering_overlappende_perioder() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Gjør perioder overlappende
        graderingPerioder.get(1).setPeriodeFom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER);
    }

    @Test
    void skal_validere_gradering_start_for_sluttdato() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Sett start før slutt
        graderingPerioder.get(1).setPeriodeTom(LocalDate.now());

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO);
    }

    @Test
    void skal_validere_gradering_prosentandel_må_være_satt() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Sett start årsak til null
        graderingPerioder.get(1).setProsentandelArbeid(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_gradering_samtidig_uttak_samtidig_uttaksprosent_må_være_satt() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Sett start årsak til null
        graderingPerioder.get(1).setHarSamtidigUttak(true);
        graderingPerioder.get(1).setSamtidigUttaksprosent(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_gradering_periode_må_være_satt() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        // Sett gradering periode til null
        graderingPerioder.get(1).setPeriodeForGradering(null);

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_validere_gradering() {
        var graderingPerioder = lagGyldigGraderingPerioder();

        var feltFeil = ManuellRegistreringSøknadValidator.validerGradering(graderingPerioder);
        assertThat(feltFeil).isEmpty();
    }

    private List<PermisjonPeriodeDto> lagGyldigPermisjonPeriode(UttakPeriodeType type) {
        List<PermisjonPeriodeDto> permisjonsPerioder = new ArrayList<>();
        var permisjonPeriode1 = new PermisjonPeriodeDto();
        permisjonPeriode1.setPeriodeFom(LocalDate.now());
        permisjonPeriode1.setPeriodeTom(LocalDate.now().plusWeeks(3));
        permisjonPeriode1.setPeriodeType(type);
        permisjonsPerioder.add(permisjonPeriode1);

        var permisjonPeriode2 = new PermisjonPeriodeDto();
        permisjonPeriode2.setPeriodeFom(LocalDate.now().plusWeeks(3));
        permisjonPeriode2.setPeriodeTom(LocalDate.now().plusWeeks(5));
        permisjonPeriode2.setPeriodeType(type);
        permisjonsPerioder.add(permisjonPeriode2);

        return permisjonsPerioder;
    }

    private List<UtsettelseDto> lagGyldigUtsettelsePerioder() {
        List<UtsettelseDto> utsettelsePerioder = new ArrayList<>();

        var utsettelsePeriode1 = new UtsettelseDto();
        utsettelsePeriode1.setPeriodeFom(LocalDate.now());
        utsettelsePeriode1.setPeriodeTom(LocalDate.now().plusWeeks(3));
        utsettelsePeriode1.setArsakForUtsettelse(UtsettelseÅrsak.ARBEID);
        utsettelsePeriode1.setPeriodeForUtsettelse(UttakPeriodeType.FELLESPERIODE);
        utsettelsePerioder.add(utsettelsePeriode1);

        var utsettelsePeriode2 = new UtsettelseDto();
        utsettelsePeriode2.setPeriodeFom(LocalDate.now().plusWeeks(3));
        utsettelsePeriode2.setPeriodeTom(LocalDate.now().plusWeeks(5));
        utsettelsePeriode2.setArsakForUtsettelse(UtsettelseÅrsak.ARBEID);
        utsettelsePeriode2.setPeriodeForUtsettelse(UttakPeriodeType.FELLESPERIODE);
        utsettelsePerioder.add(utsettelsePeriode2);

        return utsettelsePerioder;
    }

    @Test
    void skal_validere_at_frilansperioder_må_være_satt() {
        var frilansDto = new FrilansDto();
        frilansDto.setHarSøkerPeriodeMedFrilans(true);
        frilansDto.setPerioder(new ArrayList<>());
        var feltFeil = ManuellRegistreringSøknadValidator.validerFrilans(frilansDto);
        assertThat(feltFeil).hasSize(1);
        assertThat(feltFeil.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.MINDRE_ELLER_LIK_LENGDE);
    }

    private List<PermisjonPeriodeDto> lagGyldigFellesPerioder() {
        List<PermisjonPeriodeDto> fellesPerioder = new ArrayList<>();
        var permisjonPeriode1 = new PermisjonPeriodeDto();
        permisjonPeriode1.setPeriodeFom(LocalDate.now());
        permisjonPeriode1.setPeriodeTom(LocalDate.now().plusWeeks(3));
        permisjonPeriode1.setMorsAktivitet(MorsAktivitet.ARBEID);
        permisjonPeriode1.setPeriodeType(UttakPeriodeType.FELLESPERIODE);
        fellesPerioder.add(permisjonPeriode1);

        var fellesPeriode2 = new PermisjonPeriodeDto();
        fellesPeriode2.setPeriodeFom(LocalDate.now().plusWeeks(3));
        fellesPeriode2.setPeriodeTom(LocalDate.now().plusWeeks(5));
        fellesPeriode2.setMorsAktivitet(MorsAktivitet.INNLAGT);
        fellesPeriode2.setPeriodeType(UttakPeriodeType.FELLESPERIODE);
        fellesPerioder.add(fellesPeriode2);

        return fellesPerioder;
    }

    private List<GraderingDto> lagGyldigGraderingPerioder() {
        List<GraderingDto> graderingPerioder = new ArrayList<>();
        var graderingPeriode1 = new GraderingDto();
        graderingPeriode1.setPeriodeFom(LocalDate.now());
        graderingPeriode1.setPeriodeTom(LocalDate.now().plusWeeks(3));
        graderingPeriode1.setArbeidsgiverIdentifikator(KUNSTIG_ORG + "1");
        graderingPeriode1.setProsentandelArbeid(BigDecimal.valueOf(20));
        graderingPeriode1.setPeriodeForGradering(UttakPeriodeType.MØDREKVOTE);
        graderingPeriode1.setSkalGraderes(true);
        graderingPeriode1.setHarSamtidigUttak(true);
        graderingPeriode1.setSamtidigUttaksprosent(BigDecimal.TEN);
        graderingPerioder.add(graderingPeriode1);

        var graderingPeriode2 = new GraderingDto();
        graderingPeriode2.setPeriodeFom(LocalDate.now().plusWeeks(3));
        graderingPeriode2.setPeriodeTom(LocalDate.now().plusWeeks(5));
        graderingPeriode2.setArbeidsgiverIdentifikator(KUNSTIG_ORG + "2");
        graderingPeriode2.setProsentandelArbeid(BigDecimal.valueOf(20));
        graderingPeriode2.setPeriodeForGradering(UttakPeriodeType.FELLESPERIODE);
        graderingPeriode2.setSkalGraderes(false);
        graderingPerioder.add(graderingPeriode2);

        return graderingPerioder;
    }

}
