package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;

import no.nav.foreldrepenger.domene.iay.modell.Refusjon;

import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoggRefusjonsavvikTjenesteTest {
    private static final Arbeidsgiver AG1 = Arbeidsgiver.virksomhet("999999999");
    private static final Arbeidsgiver AG2 = Arbeidsgiver.virksomhet("999999998");
    private static final String SAKSNUMMER = "12345678";
    private static final LocalDate STP = LocalDate.of(2023,10,1);

    @Test
    void skal_ikke_finne_avvik_ved_0_avvik() {
        // Arrange
        var nyeImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).isEmpty();
    }

    @Test
    void skal_ikke_finne_avvik_ved_lavt_avvik() {
        // Arrange
        var nyeImer = Arrays.asList(lagIM(AG1, 10000, null, Collections.emptyList()));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).isEmpty();
    }

    @Test
    void skal_ikke_finne_avvik_når_refusjon_øker() {
        // Arrange
        var nyeImer = Arrays.asList(lagIM(AG1, 25000, null, Collections.emptyList()));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).isEmpty();
    }

    @Test
    void skal_finne_avvik_når_stor_reduksjon_i_refusjon_fra_start() {
        // Arrange
        var nyeImer = Arrays.asList(lagIM(AG1, 1500, null, Collections.emptyList()));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).hasSize(1);
        var diff = alleEndringer.get(0);
        assertThat(diff.endringSum()).isEqualByComparingTo(BigDecimal.valueOf(10500));
        assertThat(diff.endringProsent()).isEqualByComparingTo(BigDecimal.valueOf(87.5));
        assertThat(diff.endringsdato()).isEqualTo(STP);
        assertThat(diff.saksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(diff.skjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_finne_avvik_når_opphør_i_refusjon() {
        // Arrange
        var opphørsdato = STP.plusMonths(2);
        var opphørsdato2 = STP.plusMonths(3);
        var nyeImer = Arrays.asList(lagIM(AG1, 12000, opphørsdato, Collections.emptyList()),
            lagIM(AG2, 5000, opphørsdato2, Collections.emptyList()));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, Collections.emptyList()),
            lagIM(AG2, 5000, null, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnEndringIOpphørsdato(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).hasSize(2);
        var diff = alleEndringer.get(0);
        assertThat(diff.refusjonsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(12000));
        assertThat(diff.opphørsdato()).isEqualTo(opphørsdato);
        assertThat(diff.saksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(diff.skjæringstidspunkt()).isEqualTo(STP);
    }
    @Test
    void logg_kun_avvik_i_refusjon_når_endring() {
        // Arrange
        var opphørsdato = STP.plusMonths(2);
        var opphørsdato2 = STP.plusMonths(3);

        var gamleImer = Arrays.asList(lagIM(AG1, 12000, opphørsdato, Collections.emptyList()),
            lagIM(AG2, 5000, null, Collections.emptyList()));
        var nyeImer = Arrays.asList(lagIM(AG1, 12000, opphørsdato, Collections.emptyList()),
            lagIM(AG2, 5000, opphørsdato2, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnEndringIOpphørsdato(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).hasSize(1);
        var diff = alleEndringer.get(0);
        assertThat(diff.refusjonsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(diff.opphørsdato()).isEqualTo(opphørsdato2);
        assertThat(diff.saksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(diff.skjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_finne_avvik_når_endring_underveis() {
        // Arrange
        var opphørsdato = STP.plusMonths(2);
        var endringdsato = STP.plusMonths(1);
        var endring = new Refusjon(BigDecimal.valueOf(5000), endringdsato);
        var nyeImer = Arrays.asList(lagIM(AG1, 12000, opphørsdato, Collections.singletonList(endring)));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, opphørsdato, Collections.emptyList()));

        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).hasSize(1);
        var diff = alleEndringer.get(0);
        assertThat(diff.endringSum()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        assertThat(diff.endringProsent()).isEqualByComparingTo(BigDecimal.valueOf(58.33));
        assertThat(diff.endringsdato()).isEqualTo(endringdsato);
        assertThat(diff.saksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(diff.skjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_finne_avvik_når_flere_im() {
        // Arrange
        var endringsdato1 = STP.plusMonths(2);
        var endringsdato2 = STP.plusMonths(3);
        var endring = new Refusjon(BigDecimal.valueOf(3000), endringsdato1);
        var endring2 = new Refusjon(BigDecimal.valueOf(1000), endringsdato2);

        var nyeImer = Arrays.asList(lagIM(AG1, 12000, null, List.of(endring)),
            lagIM(AG2, 5000, null, List.of(endring2)));
        var gamleImer = Arrays.asList(lagIM(AG1, 12000, null, List.of(endring) ),
            lagIM(AG2, 5000, null, Collections.emptyList()));
        // Act
        var alleEndringer = LoggRefusjonsavvikTjeneste.finnAvvik(SAKSNUMMER, STP, nyeImer, gamleImer);

        // Assert
        assertThat(alleEndringer).hasSize(1);
        var diff = alleEndringer.get(0);
        assertThat(diff.endringSum()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(diff.endringProsent()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(diff.endringsdato()).isEqualTo(endringsdato2);
        assertThat(diff.saksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(diff.skjæringstidspunkt()).isEqualTo(STP);
    }

    private Inntektsmelding lagIM(Arbeidsgiver ag, int refusjon, LocalDate opphørsdato, List<Refusjon> endringer) {
        var builder = InntektsmeldingBuilder.builder()
            .medRefusjon(BigDecimal.valueOf(refusjon), opphørsdato == null ? Tid.TIDENES_ENDE : opphørsdato)
            .medArbeidsgiver(ag);
        endringer.forEach(builder::leggTil);
        return builder.build();

    }
}
