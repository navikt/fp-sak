package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OppdragKvitteringTjenesteTest {

    @Test
    void harPositivKvittering_ja_hvis_00() {
        // Arrange
        var oppdrag110 = lagOppdrag110(Alvorlighetsgrad.OK);

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void harPositivKvittering_ja_hvis_04() {
        // Arrange
        var oppdrag110 = lagOppdrag110(Alvorlighetsgrad.OK_MED_MERKNAD);

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void harPositivKvittering_nei_hvis_08() {
        // Arrange
        var oppdrag110 = lagOppdrag110(Alvorlighetsgrad.FEIL);

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void harPositivKvittering_nei_hvis_ingen_kvittering() {
        // Arrange
        var oppdrag110 = lagOppdrag110(null);

        // Act
        var resultat = OppdragKvitteringTjeneste.harPositivKvittering(oppdrag110);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Oppdrag110 lagOppdrag110(Alvorlighetsgrad alvorlighetsgrad) {
        var oppdragskontroll = OppdragTestDataHelper.oppdragskontrollUtenOppdrag();
        var oppdrag110 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 123L);
        if (alvorlighetsgrad != null) {
            lagOppdragKvittering(oppdrag110, alvorlighetsgrad);
        }
        return oppdrag110;
    }

    private OppdragKvittering lagOppdragKvittering(Oppdrag110 oppdrag110, Alvorlighetsgrad alvorlighetsgrad) {
        return OppdragKvittering.builder()
            .medOppdrag110(oppdrag110)
            .medAlvorlighetsgrad(alvorlighetsgrad)
            .build();
    }
}
