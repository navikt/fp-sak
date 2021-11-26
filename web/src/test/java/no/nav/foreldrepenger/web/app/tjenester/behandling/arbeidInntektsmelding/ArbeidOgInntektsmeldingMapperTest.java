package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeidOgInntektsmeldingMapperTest {

    @Test
    void skal_teste_mapping_av_inntektsmelding_med_ref_og_kontakt() {
        var internRef = InternArbeidsforholdRef.nyRef();
        var ekstrernRef = EksternArbeidsforholdRef.ref("AB-001");

        var im = lagIM("99999999", internRef, 50000, null);
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im,
            Collections.singletonList(lagRef("99999999", internRef, ekstrernRef)),
            Optional.of(new KontaktinformasjonIM("John Johnsen", "11111111")));

        assertThat(mappetRes).isNotNull();
        assertThat(mappetRes.getArbeidsgiverIdent()).isEqualTo("99999999");
        assertThat(mappetRes.getInntektPrMnd().intValue()).isEqualTo(50000);
        assertThat(mappetRes.getRefusjonPrMnd()).isNull();
        assertThat(mappetRes.getInternArbeidsforholdId()).isEqualTo(internRef.getReferanse());
        assertThat(mappetRes.getEksternArbeidsforholdId()).isEqualTo("AB-001");
        assertThat(mappetRes.getKontaktpersonNavn()).isEqualTo("John Johnsen");
        assertThat(mappetRes.getKontaktpersonNummer()).isEqualTo("11111111");
    }

    @Test
    void skal_teste_mapping_av_inntektsmelding_uten_ref_og_kontakt() {
        var internRef = InternArbeidsforholdRef.nyRef();

        var im = lagIM("99999999", internRef, 50000, null);
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im,
            Collections.emptyList(),
            Optional.empty());

        assertThat(mappetRes).isNotNull();
        assertThat(mappetRes.getArbeidsgiverIdent()).isEqualTo("99999999");
        assertThat(mappetRes.getInntektPrMnd().intValue()).isEqualTo(50000);
        assertThat(mappetRes.getRefusjonPrMnd()).isNull();
        assertThat(mappetRes.getInternArbeidsforholdId()).isEqualTo(internRef.getReferanse());
        assertThat(mappetRes.getEksternArbeidsforholdId()).isNull();
        assertThat(mappetRes.getKontaktpersonNavn()).isNull();
        assertThat(mappetRes.getKontaktpersonNummer()).isNull();
    }

    private ArbeidsforholdReferanse lagRef(String orgnr, InternArbeidsforholdRef intern, EksternArbeidsforholdRef ekstern) {
        return new ArbeidsforholdReferanse(Arbeidsgiver.virksomhet(orgnr), intern, ekstern);
    }

    private Inntektsmelding lagIM(String orgnr, InternArbeidsforholdRef internRef, Integer inntekt, Integer refusjon) {
        return InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.valueOf(inntekt))
            .medRefusjon(refusjon != null ? BigDecimal.valueOf(refusjon) : null)
            .medArbeidsforholdId(internRef)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .build();
    }

}
