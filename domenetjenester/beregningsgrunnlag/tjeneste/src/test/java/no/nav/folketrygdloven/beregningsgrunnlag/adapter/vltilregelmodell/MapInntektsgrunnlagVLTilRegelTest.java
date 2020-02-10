package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapInntektsgrunnlagVLTilRegelTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    public static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
    private MapInntektsgrunnlagVLTilRegel mapper = new MapInntektsgrunnlagVLTilRegel(5, new FakeUnleash());
    private AktørId aktørId = AktørId.dummy();
    private BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);

    @Before
    public void setUp() {
        when(behandlingReferanse.getAktørId()).thenReturn(aktørId);
    }

    @Test
    public void skal_mappe_inntektsmelding_for_arbeid_med_fleire_yrkesaktiviteter() {
        // Arrange
        DatoIntervallEntitet p1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12),
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        DatoIntervallEntitet p2 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1));
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagIAYGrunnlagMedArbeidIPerioder(List.of(
            p1, p2));
        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.TEN)
            .medArbeidsgiver(VIRKSOMHET)
            .build();
        // Act
        Inntektsgrunnlag map = mapper.map(behandlingReferanse, List.of(im), SKJÆRINGSTIDSPUNKT_BEREGNING, iayGrunnlag);

        assertThat(map.getPeriodeinntekter().size()).isEqualTo(1);
    }

    @Test
    public void skal_ikkje_mappe_inntektsmelding_for_arbeid_som_slutter_dagen_før_skjæringstidspunktet() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagIAYGrunnlagMedArbeidIPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1)));
        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.TEN)
            .medArbeidsgiver(VIRKSOMHET)
            .build();
        // Act
        Inntektsgrunnlag map = mapper.map(behandlingReferanse, List.of(im), SKJÆRINGSTIDSPUNKT_BEREGNING, iayGrunnlag);

        assertThat(map.getPeriodeinntekter()).isEmpty();
    }

    @Test
    public void skal_mappe_inntektsmelding_for_arbeid_som_slutter_på_skjæringstidspunktet() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagIAYGrunnlagMedArbeidIPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12), SKJÆRINGSTIDSPUNKT_BEREGNING));
        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.TEN)
            .medArbeidsgiver(VIRKSOMHET)
            .build();
        // Act
        Inntektsgrunnlag map = mapper.map(behandlingReferanse, List.of(im), SKJÆRINGSTIDSPUNKT_BEREGNING, iayGrunnlag);

        assertThat(map.getPeriodeinntekter().size()).isEqualTo(1);
    }

    @Test
    public void skal_mappe_inntektsmelding_for_arbeid_som_slutter_dagen_etter_skjæringstidspunktet() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagIAYGrunnlagMedArbeidIPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1)));
        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.TEN)
            .medArbeidsgiver(VIRKSOMHET)
            .build();
        // Act
        Inntektsgrunnlag map = mapper.map(behandlingReferanse, List.of(im), SKJÆRINGSTIDSPUNKT_BEREGNING, iayGrunnlag);

        assertThat(map.getPeriodeinntekter().size()).isEqualTo(1);
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagMedArbeidIPeriode(DatoIntervallEntitet periode) {
        InntektArbeidYtelseAggregatBuilder registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = registerBuilder.getAktørArbeidBuilder(aktørId);
        aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
        .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(periode))
            .medArbeidsgiver(VIRKSOMHET)
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef()));
        registerBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(registerBuilder).build();
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagMedArbeidIPerioder(List<DatoIntervallEntitet> perioder) {
        InntektArbeidYtelseAggregatBuilder registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = registerBuilder.getAktørArbeidBuilder(aktørId);
        perioder.forEach(periode -> aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(periode))
            .medArbeidsgiver(VIRKSOMHET)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef()))
        );
        registerBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(registerBuilder).build();
    }
}
