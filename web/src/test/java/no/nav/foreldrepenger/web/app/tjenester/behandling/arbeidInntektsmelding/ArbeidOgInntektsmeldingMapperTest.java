package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangel;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.InntektDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
            Optional.of(new KontaktinformasjonIM("John Johnsen", "11111111")), Optional.empty(), Collections.emptyList(), Collections.emptyList());

        assertThat(mappetRes).isNotNull();
        assertThat(mappetRes.arbeidsgiverIdent()).isEqualTo("99999999");
        assertThat(mappetRes.inntektPrMnd().intValue()).isEqualTo(50000);
        assertThat(mappetRes.refusjonPrMnd()).isNull();
        assertThat(mappetRes.internArbeidsforholdId()).isEqualTo(internRef.getReferanse());
        assertThat(mappetRes.eksternArbeidsforholdId()).isEqualTo("AB-001");
        assertThat(mappetRes.kontaktpersonNavn()).isEqualTo("John Johnsen");
        assertThat(mappetRes.kontaktpersonNummer()).isEqualTo("11111111");
    }

    @Test
    void skal_teste_mapping_av_inntektsmelding_med_årsak_og_vurdering() {
        var internRef = InternArbeidsforholdRef.nyRef();
        var ekstrernRef = EksternArbeidsforholdRef.ref("AB-001");

        var relevantOrgnr = "999999999";
        var irrelevantOrgnr = "342352362";
        var im = lagIM(relevantOrgnr, internRef, 50000, null);
        var relevantMangel = new ArbeidsforholdInntektsmeldingMangel(Arbeidsgiver.virksomhet(relevantOrgnr), InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
        var irrelevantMangel = new ArbeidsforholdInntektsmeldingMangel(Arbeidsgiver.virksomhet(irrelevantOrgnr), InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);
        var mangler = Arrays.asList(relevantMangel, irrelevantMangel);
        var relevantValg = ArbeidsforholdValg.builder()
            .medArbeidsgiver(relevantOrgnr)
            .medBegrunnelse("Dette er en begrunnelse")
            .medVurdering(ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING)
            .build();
        var irrelevantValg = ArbeidsforholdValg.builder()
            .medArbeidsgiver(irrelevantOrgnr)
            .medBegrunnelse("Dette er en annen begrunnelse")
            .medVurdering(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)
            .build();
        var saksbehandlersValg = Arrays.asList(irrelevantValg, relevantValg);
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im,
            Collections.singletonList(lagRef(relevantOrgnr, internRef, ekstrernRef)),
            Optional.of(new KontaktinformasjonIM("John Johnsen", "11111111")), Optional.empty(),
            mangler, saksbehandlersValg);

        assertThat(mappetRes).isNotNull();
        assertThat(mappetRes.arbeidsgiverIdent()).isEqualTo(relevantOrgnr);
        assertThat(mappetRes.inntektPrMnd().intValue()).isEqualTo(50000);
        assertThat(mappetRes.refusjonPrMnd()).isNull();
        assertThat(mappetRes.internArbeidsforholdId()).isEqualTo(internRef.getReferanse());
        assertThat(mappetRes.eksternArbeidsforholdId()).isEqualTo("AB-001");
        assertThat(mappetRes.kontaktpersonNavn()).isEqualTo("John Johnsen");
        assertThat(mappetRes.kontaktpersonNummer()).isEqualTo("11111111");
        assertThat(mappetRes.årsak()).isEqualTo(AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
        assertThat(mappetRes.saksbehandlersVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING);
        assertThat(mappetRes.begrunnelse()).isEqualTo("Dette er en begrunnelse");
    }

    @Test
    void skal_teste_mapping_av_inntektsmelding_uten_ref_og_kontakt() {
        var internRef = InternArbeidsforholdRef.nyRef();

        var im = lagIM("99999999", internRef, 50000, null);
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im,
            Collections.emptyList(),
            Optional.empty(), Optional.empty(), Collections.emptyList(), Collections.emptyList());
        assertThat(mappetRes).isNotNull();
        assertThat(mappetRes.arbeidsgiverIdent()).isEqualTo("99999999");
        assertThat(mappetRes.inntektPrMnd().intValue()).isEqualTo(50000);
        assertThat(mappetRes.refusjonPrMnd()).isNull();
        assertThat(mappetRes.internArbeidsforholdId()).isEqualTo(internRef.getReferanse());
        assertThat(mappetRes.eksternArbeidsforholdId()).isNull();
        assertThat(mappetRes.kontaktpersonNavn()).isNull();
        assertThat(mappetRes.kontaktpersonNummer()).isNull();
    }


    @Test
    void skal_teste_mapping_av_inntekter() {
        // Arrange
        var inntekt = lagInntekter(YearMonth.of(2022, 1), YearMonth.of(2022, 12), "999999999");
        var filter = new InntektFilter(Arrays.asList(inntekt));

        // Act
        var inntekter = ArbeidOgInntektsmeldingMapper.mapInntekter(filter, LocalDate.of(2022, 10, 1));

        // Assert
        assertThat(inntekter).hasSize(1);
        assertThat(inntekter.get(0).inntekter()).hasSize(10);
    }

    private Inntekt lagInntekter(YearMonth fom, YearMonth tom, String orgnr) {
        YearMonth counter = fom;

        var builder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));

        while (counter.isBefore(tom)) {
            builder.leggTilInntektspost(InntektspostBuilder.ny()
                .medPeriode(counter.atDay(1), counter.atEndOfMonth())
                .medBeløp(BigDecimal.valueOf(100)));
            counter = counter.plusMonths(1);
        }
        return builder.build();
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
