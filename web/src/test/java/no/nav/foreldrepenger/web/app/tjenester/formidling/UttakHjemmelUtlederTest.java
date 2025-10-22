package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

class UttakHjemmelUtlederTest {

    @Test
    void skal_finne_lovhjemler() {
        var periode1 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.now(), LocalDate.now())
            .medResultatÅrsak(PeriodeResultatÅrsak.GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR)
            .build();
        var periode2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .build();
        var periode3 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(2))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medGraderingInnvilget(false)
            .medGraderingAvslagÅrsak(GraderingAvslagÅrsak.MANGLENDE_GRADERINGSAVTALE)
            .build();

        assertThat(UttakHjemmelUtleder.finnLovhjemler(periode1)).containsExactlyInAnyOrder("14-14", "14-16");
        assertThat(UttakHjemmelUtleder.finnLovhjemler(periode2)).containsExactlyInAnyOrder("14-9");
        assertThat(UttakHjemmelUtleder.finnLovhjemler(periode3)).containsExactlyInAnyOrder("14-12", "14-16", "21-3");
    }

    @Test
    void alle_årsaker_skal_ha_minst_en_lovhjemmel() {
        for (var årsak : Arrays.stream(PeriodeResultatÅrsak.values()).filter(å -> å != PeriodeResultatÅrsak.UKJENT).collect(Collectors.toSet())) {
            assertThat(UttakHjemmelUtleder.finnLovhjemler(årsak)).as("PeriodeResultatÅrsak.%s skal ha minst en lovhjemmel", årsak).isNotEmpty();
        }
        for (var årsak : Arrays.stream(GraderingAvslagÅrsak.values()).filter(å -> å != GraderingAvslagÅrsak.UKJENT).collect(Collectors.toSet())) {
            assertThat(UttakHjemmelUtleder.finnLovhjemler(årsak)).as("GraderingAvslagÅrsak.%s skal ha minst en lovhjemmel", årsak).isNotEmpty();
        }
    }

}
