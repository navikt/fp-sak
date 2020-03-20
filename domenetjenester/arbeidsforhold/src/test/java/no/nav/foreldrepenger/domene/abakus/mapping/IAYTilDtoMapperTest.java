package no.nav.foreldrepenger.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IAYTilDtoMapperTest {
    private static final AktørId AKTØR_ID = new AktørId("9999999999999");
    private static final UUID GRUNNLAG_REF = UUID.randomUUID();
    private static final UUID BEH_REF = UUID.randomUUID();

    @Test
    public void skal_teste_at_overstyrt_skal_kunne_mappes_uten_register() {
        // Arrange
        IAYTilDtoMapper mapper = new IAYTilDtoMapper(AKTØR_ID, GRUNNLAG_REF, BEH_REF);
        InntektArbeidYtelseAggregatBuilder sbhVersjon = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aaBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty()).medAktørId(AKTØR_ID);
        YrkesaktivitetBuilder yrkesaktivitetBuilderForType = aaBuilder.getYrkesaktivitetBuilderForType(ArbeidType.VANLIG);
        aaBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        sbhVersjon.leggTilAktørArbeid(aaBuilder);
        ArbeidsforholdOverstyringBuilder overstyring = ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty()).medArbeidsgiver(Arbeidsgiver.person(AKTØR_ID)).medBeskrivelse("Test");
        ArbeidsforholdInformasjon osAgg = ArbeidsforholdInformasjonBuilder.builder(Optional.empty()).leggTil(overstyring).build();
        InntektArbeidYtelseGrunnlag iay = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(sbhVersjon).medInformasjon(osAgg).build();

        // Act
        InntektArbeidYtelseGrunnlagDto inntektArbeidYtelseGrunnlagDto = mapper.mapTilDto(iay);

        // Assert
        assertThat(inntektArbeidYtelseGrunnlagDto).isNotNull();
    }

}
