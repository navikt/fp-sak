package no.nav.foreldrepenger.domene.abakus.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class IAYTilDtoMapperTest {
    private static final AktørId AKTØR_ID = new AktørId("9999999999999");
    private static final UUID GRUNNLAG_REF = UUID.randomUUID();
    private static final UUID BEH_REF = UUID.randomUUID();

    @Test
    public void skal_teste_at_overstyrt_skal_kunne_mappes_uten_register() {
        // Arrange
        var mapper = new IAYTilDtoMapper(AKTØR_ID, YtelseType.FORELDREPENGER, GRUNNLAG_REF, BEH_REF);
        var sbhVersjon = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        var aaBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty()).medAktørId(AKTØR_ID);
        var yrkesaktivitetBuilderForType = aaBuilder.getYrkesaktivitetBuilderForType(ArbeidType.VANLIG);
        aaBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        sbhVersjon.leggTilAktørArbeid(aaBuilder);
        var overstyring = ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.person(AKTØR_ID)).medBeskrivelse("Test");
        var osAgg = ArbeidsforholdInformasjonBuilder.builder(Optional.empty()).leggTil(overstyring).build();
        var iay = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(sbhVersjon).medInformasjon(osAgg).build();

        // Act
        var inntektArbeidYtelseGrunnlagDto = mapper.mapTilDto(iay);

        // Assert
        assertThat(inntektArbeidYtelseGrunnlagDto).isNotNull();
    }

}
