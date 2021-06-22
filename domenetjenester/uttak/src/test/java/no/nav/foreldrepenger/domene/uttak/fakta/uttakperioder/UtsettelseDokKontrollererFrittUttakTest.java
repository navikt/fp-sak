package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;


import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder.ny;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.SYKDOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;

class UtsettelseDokKontrollererFrittUttakTest {

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"INSTITUSJON_SØKER", "INSTITUSJON_BARN", "SYKDOM"})
    @DisplayName("Utsettelser med innleggelse eller sykdom skal kontrolleres hvis utsettelser er innenfor 6 første ukene")
    void gyldige_årsaker_innefor(UtsettelseÅrsak årsak) {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse, familiehendelse.plusMonths(1)).medÅrsak(årsak).build();
        var resultat = kontroller(familiehendelse, søknadsperiode);
        assertThat(resultat).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"FERIE", "ARBEID", "HV_OVELSE", "NAV_TILTAK"})
    @DisplayName("Andre utsettelser skal kontrolleres ikke selv om utsettelsen er innenfor 6 første ukene")
    void ikke_gyldige_årsaker_innefor(UtsettelseÅrsak årsak) {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse, familiehendelse.plusMonths(1)).medÅrsak(årsak).build();
        var resultat = kontroller(familiehendelse, søknadsperiode);
        assertThat(resultat).isFalse();
    }

    @Test
    @DisplayName("Sykdom delvis innenfor 6 første ukene skal kontrolleres. Dette er en forenkling")
    void delvis_innenfor() {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse.plusWeeks(4), familiehendelse.plusMonths(3))
            .medÅrsak(SYKDOM)
            .build();
        var resultat = kontroller(familiehendelse, søknadsperiode);
        assertThat(resultat).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"INSTITUSJON_SØKER", "INSTITUSJON_BARN", "SYKDOM"})
    @DisplayName("Sykdom og innleggelse før tidsperiode forbeholdt mor skal ikke kontrolleres")
    void utsettelse_ligger_før() {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse.minusWeeks(6), familiehendelse.minusWeeks(3).minusDays(1))
            .medÅrsak(SYKDOM)
            .build();
        var resultat = kontroller(familiehendelse, søknadsperiode);
        assertThat(resultat).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"INSTITUSJON_SØKER", "INSTITUSJON_BARN", "SYKDOM"})
    @DisplayName("Sykdom og innleggelse etter tidsperiode forbeholdt mor skal ikke kontrolleres")
    void utsettelse_ligger_etter() {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse.plusWeeks(6), familiehendelse.plusWeeks(9))
            .medÅrsak(SYKDOM)
            .build();
        var resultat = kontroller(familiehendelse, søknadsperiode);
        assertThat(resultat).isFalse();
    }

    @Test
    @DisplayName("Skal kaste exception hvis søknadsperiode ikke er en utsettelse")
    void exception_hvis_ikke_utsettelse() {
        var familiehendelse = LocalDate.of(2021, 6, 22);

        var søknadsperiode = ny().medPeriode(familiehendelse, familiehendelse.plusMonths(1)).build();
        assertThrows(IllegalArgumentException.class, () -> kontroller(familiehendelse, søknadsperiode));
    }

    private static boolean kontroller(LocalDate familiehendelse, OppgittPeriodeEntitet søknadsperiode) {
        var kontrollererFrittUttak = new UtsettelseDokKontrollererFrittUttak(familiehendelse);
        return kontrollererFrittUttak.måSaksbehandlerManueltBekrefte(søknadsperiode);
    }
}
