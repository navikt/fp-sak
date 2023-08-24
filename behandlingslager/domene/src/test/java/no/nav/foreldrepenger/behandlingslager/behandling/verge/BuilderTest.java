package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

class BuilderTest {

    private NavBruker vergeBruker = NavBruker.opprettNyNB(AktørId.dummy());

    @Test
    void ska_opprette_verge() {
        // Act
        var vergeEntitet = new VergeEntitet.Builder()
            .medVergeType(VergeType.BARN)
            .medBruker(vergeBruker)
            .gyldigPeriode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            .build();

        // Assert
        assertThat(vergeEntitet.getGyldigFom()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(vergeEntitet.getGyldigTom()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void skal_støtte_at_TOM_er_null_og_sette_tidenes_ende() {
        // Act
        var vergeEntitet = new VergeEntitet.Builder()
            .medVergeType(VergeType.BARN)
            .medBruker(vergeBruker)
            .gyldigPeriode(LocalDate.now().minusDays(1), null)
            .build();

        // Assert
        assertThat(vergeEntitet.getGyldigFom()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(vergeEntitet.getGyldigTom()).isEqualTo(TIDENES_ENDE);
    }
}
