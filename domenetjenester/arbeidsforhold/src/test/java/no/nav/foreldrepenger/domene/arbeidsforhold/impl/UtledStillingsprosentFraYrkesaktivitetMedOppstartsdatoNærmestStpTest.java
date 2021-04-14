package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStpTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("1");
    private static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.nyRef();

    @Test
    public void skal_finne_høyest_stillingsprosent_for_yrkesaktivteter_som_overlapper_stp_og_har_fom_dato_ikke_er_før_oppstartsdato_nærmest_stp() {

        // Arrange
        var oppstartsdatoNærmestStp = SKJÆRINGSTIDSPUNKT.minusYears(1);

        var stillingsprosent1 = 25;
        var yrkesaktivitet1 = lagYrkesakvitetet(oppstartsdatoNærmestStp, SKJÆRINGSTIDSPUNKT, stillingsprosent1);

        var stillingsprosent2 = 50;
        var fom2 = oppstartsdatoNærmestStp.minusYears(1);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var yrkesaktivitet2 = lagYrkesakvitetet(fom2, tom2, stillingsprosent2);

        var stillingsprosent3 = 75;
        var fom3 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom3 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var yrkesaktivitet3 = lagYrkesakvitetet(fom3, tom3, stillingsprosent3);

        var stillingsprosent4 = 100;
        var yrkesaktivitet4 = lagYrkesakvitetet(oppstartsdatoNærmestStp, SKJÆRINGSTIDSPUNKT, stillingsprosent4);

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        var stillingsprosent = UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT, oppstartsdatoNærmestStp);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(100));

    }

    @Test
    public void skal_finne_høyest_stillingsprosent_for_yrkesaktivteter_som_tilkommer_etter_stp_og_har_fom_dato_ikke_er_etter_oppstartsdato_nærmest_stp() {

        // Arrange
        var oppstartsdatoNærmestStp = SKJÆRINGSTIDSPUNKT.plusDays(1);

        var stillingsprosent1 = 100;
        var tom1 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var yrkesaktivitet1 = lagYrkesakvitetet(oppstartsdatoNærmestStp, tom1, stillingsprosent1);

        var stillingsprosent2 = 75;
        var fom2 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        var yrkesaktivitet2 = lagYrkesakvitetet(fom2, tom2, stillingsprosent2);

        var stillingsprosent3 = 50;
        var fom3 = SKJÆRINGSTIDSPUNKT.minusYears(3);
        var tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var yrkesaktivitet3 = lagYrkesakvitetet(fom3, tom3, stillingsprosent3);

        var stillingsprosent4 = 25;
        var tom4 = SKJÆRINGSTIDSPUNKT.plusYears(4);
        var yrkesaktivitet4 = lagYrkesakvitetet(oppstartsdatoNærmestStp, tom4, stillingsprosent4);

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        var stillingsprosent = UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT, oppstartsdatoNærmestStp);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(100));

    }

    private Yrkesaktivitet lagYrkesakvitetet(LocalDate fom, LocalDate tom, int stillingsprosent) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(INTERN_ARBEIDSFORHOLD_REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                        .medProsentsats(BigDecimal.valueOf(stillingsprosent)))
                .build();
    }

}
