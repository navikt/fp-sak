package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class AvklarAktivitetsPerioderOppdatererTest {

    @Test
    public void skal_sjekke_om_perioden_er_endret_hvis_orginal_periode_er_større() {
        var oppdaterer = new AvklarAktivitetsPerioderOppdaterer();

        var idag = LocalDate.now();

        var beregnet = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag);
        var orginal = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(2), idag.plusMonths(1));

        var uendretIGUI = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag);
        assertThat(oppdaterer.erEndret(beregnet, uendretIGUI, orginal)).isFalse();
        var endretCase1 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag);
        assertThat(oppdaterer.erEndret(beregnet, endretCase1, orginal)).isTrue();
        var endretCase2 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag.minusDays(10));
        assertThat(oppdaterer.erEndret(beregnet, endretCase2, orginal)).isTrue();
        var endretCase3 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag.minusDays(5));
        assertThat(oppdaterer.erEndret(beregnet, endretCase3, orginal)).isTrue();
    }

    @Test
    public void skal_sjekke_om_perioden_er_endret_hvis_orginal_periode_er_mindre() {
        var oppdaterer = new AvklarAktivitetsPerioderOppdaterer();

        var idag = LocalDate.now();

        var beregnet = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag);
        var orginal = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(20), idag.minusDays(5));

        var uendretIGUI = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(20), idag.minusDays(5));
        assertThat(oppdaterer.erEndret(beregnet, uendretIGUI, orginal)).isFalse();
        var endretCase1 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag);
        assertThat(oppdaterer.erEndret(beregnet, endretCase1, orginal)).isTrue();
        var endretCase2 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag.minusDays(10));
        assertThat(oppdaterer.erEndret(beregnet, endretCase2, orginal)).isTrue();
        var endretCase3 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag.minusDays(5));
        assertThat(oppdaterer.erEndret(beregnet, endretCase3, orginal)).isTrue();
    }

    @Test
    public void skal_sjekke_om_perioden_er_endret_hvis_orginal_periode_starte_inni_men_slutter_utenfor() {
        var oppdaterer = new AvklarAktivitetsPerioderOppdaterer();

        var idag = LocalDate.now();

        var beregnet = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag);
        var orginal = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(20), idag.plusDays(15));

        var uendretIGUI = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(20), idag);
        assertThat(oppdaterer.erEndret(beregnet, uendretIGUI, orginal)).isFalse();
        var endretCase1 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag);
        assertThat(oppdaterer.erEndret(beregnet, endretCase1, orginal)).isTrue();
        var endretCase2 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag.minusDays(10));
        assertThat(oppdaterer.erEndret(beregnet, endretCase2, orginal)).isTrue();
        var endretCase3 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag.minusDays(5));
        assertThat(oppdaterer.erEndret(beregnet, endretCase3, orginal)).isTrue();
    }

    @Test
    public void skal_sjekke_om_perioden_er_endret_hvis_orginal_periode_slutter_inni_men_starter_utenfor() {
        var oppdaterer = new AvklarAktivitetsPerioderOppdaterer();

        var idag = LocalDate.now();

        var beregnet = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag);
        var orginal = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(2), idag.minusDays(10));

        var uendretIGUI = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag.minusDays(10));
        assertThat(oppdaterer.erEndret(beregnet, uendretIGUI, orginal)).isFalse();
        var endretCase1 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag);
        assertThat(oppdaterer.erEndret(beregnet, endretCase1, orginal)).isTrue();
        var endretCase2 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusMonths(1), idag.minusDays(14));
        assertThat(oppdaterer.erEndret(beregnet, endretCase2, orginal)).isTrue();
        var endretCase3 = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(10), idag.minusDays(5));
        assertThat(oppdaterer.erEndret(beregnet, endretCase3, orginal)).isTrue();
    }
}
