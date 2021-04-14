package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;

public class MedlemEndringssjekkerEngangsstønadTest {
    private MedlemEndringssjekker endringssjekker;

    @BeforeEach
    public void before() {
        endringssjekker = new MedlemEndringssjekkerEngangsstønad();
    }


    @Test
    public void skal_finne_endring_når_det_kommer_en_ekstra_medlemskapsperiode() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder()
            .medMedlId(1L)
            .build();
        var periode2 = new MedlemskapPerioderBuilder()
            .medMedlId(2L)
            .build();

        // Act
        var endringsresultat = endringssjekker.erEndret(Optional.empty(), asList(periode1), asList(periode1, periode2));

        // Assert
        assertThat(endringsresultat).isTrue();
    }

    @Test
    public void skal_finne_endring_når_det_forsvinner_en_medlemskapsperiode() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder()
            .medMedlId(1L)
            .build();
        var periode2 = new MedlemskapPerioderBuilder()
            .medMedlId(2L)
            .build();

        // Act
        var endringsresultat = endringssjekker.erEndret(Optional.empty(), asList(periode1, periode2), asList(periode1));

        // Assert
        assertThat(endringsresultat).isTrue();
    }

    @Test
    public void skal_finne_endring_når_en_medlemskapsperiode_endres() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder()
            .medMedlId(1L)
            .medErMedlem(true)
            .build();
        var periode2 = new MedlemskapPerioderBuilder()
            .medMedlId(1L)
            .medErMedlem(false)
            .build();

        // Act
        var endringsresultat = endringssjekker.erEndret(Optional.empty(), asList(periode1), asList(periode2));

        // Assert
        assertThat(endringsresultat).isTrue();
    }


}
