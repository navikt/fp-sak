package no.nav.foreldrepenger.domene.medlem.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;

class MedlemEndringssjekkerEngangsstønadTest {
    private MedlemEndringssjekker endringssjekker;

    @BeforeEach
    public void before() {
        endringssjekker = new MedlemEndringssjekkerEngangsstønad();
    }


    @Test
    void skal_finne_endring_når_det_kommer_en_ekstra_medlemskapsperiode() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder().medMedlId(1L).build();
        var periode2 = new MedlemskapPerioderBuilder().medMedlId(2L).build();

        // Act
        var endringsresultat = endringssjekker.erEndret(List.of(periode1), List.of(periode1, periode2));

        // Assert
        assertThat(endringsresultat).isTrue();
    }

    @Test
    void skal_finne_endring_når_det_forsvinner_en_medlemskapsperiode() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder().medMedlId(1L).build();
        var periode2 = new MedlemskapPerioderBuilder().medMedlId(2L).build();

        // Act
        var endringsresultat = endringssjekker.erEndret(List.of(periode1, periode2), List.of(periode1));

        // Assert
        assertThat(endringsresultat).isTrue();
    }

    @Test
    void skal_finne_endring_når_en_medlemskapsperiode_endres() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder().medMedlId(1L).medErMedlem(true).build();
        var periode2 = new MedlemskapPerioderBuilder().medMedlId(1L).medErMedlem(false).build();

        // Act
        var endringsresultat = endringssjekker.erEndret(List.of(periode1), List.of(periode2));

        // Assert
        assertThat(endringsresultat).isTrue();
    }


}
