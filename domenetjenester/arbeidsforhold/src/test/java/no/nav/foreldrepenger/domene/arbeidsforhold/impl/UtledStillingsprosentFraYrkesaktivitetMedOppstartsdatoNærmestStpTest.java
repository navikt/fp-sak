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
        LocalDate oppstartsdatoNærmestStp = SKJÆRINGSTIDSPUNKT.minusYears(1);

        int stillingsprosent1 = 25;
        Yrkesaktivitet yrkesaktivitet1 = lagYrkesakvitetet(oppstartsdatoNærmestStp, SKJÆRINGSTIDSPUNKT, stillingsprosent1);

        int stillingsprosent2 = 50;
        LocalDate fom2 = oppstartsdatoNærmestStp.minusYears(1);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        Yrkesaktivitet yrkesaktivitet2 = lagYrkesakvitetet(fom2, tom2, stillingsprosent2);

        int stillingsprosent3 = 75;
        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        Yrkesaktivitet yrkesaktivitet3 = lagYrkesakvitetet(fom3, tom3, stillingsprosent3);

        int stillingsprosent4 = 100;
        Yrkesaktivitet yrkesaktivitet4 = lagYrkesakvitetet(oppstartsdatoNærmestStp, SKJÆRINGSTIDSPUNKT, stillingsprosent4);

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        BigDecimal stillingsprosent = UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT, oppstartsdatoNærmestStp);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(100));

    }

    @Test
    public void skal_finne_høyest_stillingsprosent_for_yrkesaktivteter_som_tilkommer_etter_stp_og_har_fom_dato_ikke_er_etter_oppstartsdato_nærmest_stp() {

        // Arrange
        LocalDate oppstartsdatoNærmestStp = SKJÆRINGSTIDSPUNKT.plusDays(1);

        int stillingsprosent1 = 100;
        LocalDate tom1 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        Yrkesaktivitet yrkesaktivitet1 = lagYrkesakvitetet(oppstartsdatoNærmestStp, tom1, stillingsprosent1);

        int stillingsprosent2 = 75;
        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        Yrkesaktivitet yrkesaktivitet2 = lagYrkesakvitetet(fom2, tom2, stillingsprosent2);

        int stillingsprosent3 = 50;
        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.minusYears(3);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Yrkesaktivitet yrkesaktivitet3 = lagYrkesakvitetet(fom3, tom3, stillingsprosent3);

        int stillingsprosent4 = 25;
        LocalDate tom4 = SKJÆRINGSTIDSPUNKT.plusYears(4);
        Yrkesaktivitet yrkesaktivitet4 = lagYrkesakvitetet(oppstartsdatoNærmestStp, tom4, stillingsprosent4);

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        BigDecimal stillingsprosent = UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp
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
