package no.nav.foreldrepenger.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.økonomistøtte.AlleMottakereHarPositivKvittering;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.AlleMottakereHarPositivKvitteringEngangsstønad;

public class AlleMottakereHarPositivKvitteringTest {

    private AlleMottakereHarPositivKvittering tjeneste = new AlleMottakereHarPositivKvitteringEngangsstønad();

    @Test
    public void skal_gi_nei_for_ingen_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_for_negativ_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_ja_for_positiv_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_feil_for_negativ_og_positiv_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        // Act
        try {
            OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag110);
            OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);
            fail("Exception forventet");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Mottat økonomi kvittering kan ikke overskrive en allerede eksisterende kvittering");
        }
    }
}
