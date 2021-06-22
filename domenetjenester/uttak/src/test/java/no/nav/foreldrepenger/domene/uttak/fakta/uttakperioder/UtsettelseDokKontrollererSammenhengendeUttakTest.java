package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder.ny;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;

class UtsettelseDokKontrollererSammenhengendeUttakTest {

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"ARBEID", "FERIE"})
    @DisplayName("Utsettelser med arbeid og ferie skal ikke kontrolleres")
    void årsaker_som_ikke_skal_kontrollres(UtsettelseÅrsak årsak) {
        var fom = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny()
            .medPeriode(fom, fom.plusMonths(1))
            .medÅrsak(årsak)
            .build();
        var resultat = kontroller(søknadsperiode);
        assertThat(resultat).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"SYKDOM", "INSTITUSJON_SØKER", "INSTITUSJON_BARN", "HV_OVELSE", "NAV_TILTAK"})
    @DisplayName("Utsettelser med arbeid og ferie skal ikke kontrolleres")
    void årsaker_som_skal_kontrollres(UtsettelseÅrsak årsak) {
        var fom = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny()
            .medPeriode(fom, fom.plusMonths(1))
            .medÅrsak(årsak)
            .build();
        var resultat = kontroller(søknadsperiode);
        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("Skal kaste exception hvis søknadsperiode ikke er en utsettelse")
    void exception_hvis_ikke_utsettelse() {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse, familiehendelse.plusMonths(1)).build();
        assertThrows(IllegalArgumentException.class, () -> kontroller(søknadsperiode));
    }

    private static boolean kontroller(OppgittPeriodeEntitet søknadsperiode) {
        var kontrollererFrittUttak = new UtsettelseDokKontrollererSammenhengendeUttak();
        return kontrollererFrittUttak.måSaksbehandlerManueltBekrefte(søknadsperiode);
    }

}
