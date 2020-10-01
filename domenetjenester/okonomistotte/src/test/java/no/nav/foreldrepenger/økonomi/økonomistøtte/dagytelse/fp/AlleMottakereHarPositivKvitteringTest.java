package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.økonomi.økonomistøtte.AlleMottakereHarPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragKvitteringTestUtil;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragTestDataHelper;

public class AlleMottakereHarPositivKvitteringTest {

    private AlleMottakereHarPositivKvittering tjeneste = new AlleMottakereHarPositivKvitteringImpl();

    @Test
    public void skal_gi_nei_for_ingen_kvittering_bruker() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_for_negativ_kvittering_bruker() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_ja_for_positiv_kvittering_bruker() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_nei_for_ingen_kvittering_arbeidsgiver() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 1L);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_for_negativ_kvittering_arbeidsgiver() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagNegativKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_ja_for_positiv_kvittering_arbeidsgiver() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_nei_bruker_ingen_arbeidsgiver_ingen_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_ingen_arbeidsgiver_negativ_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_ingen_arbeidsgiver_positiv_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagPositivKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_negativ_arbeidsgiver_ingen_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(bruker110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_negativ_arbeidsgiver_negativ_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(bruker110);
        OppdragKvitteringTestUtil.lagNegativKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_negativ_arbeidsgiver_positiv_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(bruker110);
        OppdragKvitteringTestUtil.lagPositivKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_positiv_arbeidsgiver_ingen_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagPositivKvitting(bruker110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_bruker_positiv_arbeidsgiver_negativ_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagPositivKvitting(bruker110);
        OppdragKvitteringTestUtil.lagNegativKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_ja_bruker_positiv_arbeidsgiver_positiv_kvittering() {
        // Arrange
        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var bruker110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdragskontroll, 1L);
        var arbeidsgiver110 = OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagPositivKvitting(bruker110);
        OppdragKvitteringTestUtil.lagPositivKvitting(arbeidsgiver110);

        // Act
        boolean resultat = tjeneste.vurder(oppdragskontroll);

        // Assert
        assertThat(resultat).isTrue();
    }

}
