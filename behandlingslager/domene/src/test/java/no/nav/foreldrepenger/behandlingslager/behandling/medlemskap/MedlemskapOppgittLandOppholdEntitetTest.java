package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.vedtak.konfig.Tid;

class MedlemskapOppgittLandOppholdEntitetTest {

    @Test
    void medPeriode_uten_tom_faller_tilbake_til_tidenes_ende() {
        var fom = LocalDate.of(2024, 1, 1);

        var opphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medLand(Landkoder.SWE)
            .medPeriode(fom, null)
            .erTidligereOpphold(false)
            .build();

        assertThat(opphold.getPeriodeFom()).isEqualTo(fom);
        assertThat(opphold.getPeriodeTom()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    void medPeriode_med_tom_beholder_oppgitt_dato() {
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 6, 30);

        var opphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medLand(Landkoder.SWE)
            .medPeriode(fom, tom)
            .erTidligereOpphold(true)
            .build();

        assertThat(opphold.getPeriodeFom()).isEqualTo(fom);
        assertThat(opphold.getPeriodeTom()).isEqualTo(tom);
    }
}
