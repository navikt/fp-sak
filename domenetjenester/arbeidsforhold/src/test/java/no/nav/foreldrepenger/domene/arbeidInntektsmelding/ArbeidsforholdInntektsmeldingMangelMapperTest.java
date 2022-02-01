package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArbeidsforholdInntektsmeldingMangelMapperTest {

    @Test
    void mapper_manglende_inntektsmelding_som_skal_vurderes() {
        // Arrange
        String orgnr = "999999999";
        String begrunnelse = "Begrunnelse";
        ManglendeOpplysningerVurderingDto dto = new ManglendeOpplysningerVurderingDto(UUID.randomUUID(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING,
            begrunnelse, orgnr, null);
        var mangler = Arrays.asList(lagMangel(orgnr, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING));

        // Act
        ArbeidsforholdValg resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManglendeOpplysningerVurdering(dto, mangler);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat.getArbeidsgiver().getOrgnr()).isEqualTo(orgnr);
        assertThat(resultat.getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(resultat.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
    }

    @Test
    void mapper_manglende_inntektsmelding_som_ikke_skal_vurderes() {
        // Arrange
        String orgnr1 = "999999999";
        String orgnr2 = "999999998";
        String begrunnelse = "Begrunnelse";
        ManglendeOpplysningerVurderingDto dto = new ManglendeOpplysningerVurderingDto(UUID.randomUUID(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING,
            begrunnelse, orgnr1, null);
        var mangler = Arrays.asList(lagMangel(orgnr2, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING));

        // Act
        assertThrows(IllegalStateException.class, () -> ArbeidsforholdInntektsmeldingMangelMapper.mapManglendeOpplysningerVurdering(dto, mangler));
    }

    @Test
    void mapper_til_nytt_arbeidsforhold_som_er_basert_på_inntektsmeldingen() {
        // Arrange
        String orgnr = "999999999";
        String begrunnelse = "Begrunnelse";
        LocalDate fom = LocalDate.of(2022,1,1);
        LocalDate tom = LocalDate.of(2023,1,1);
        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        var dto = new ManueltArbeidsforholdDto(UUID.randomUUID(), begrunnelse, orgnr, null, null,
            fom, tom, 50, ArbeidsforholdKomplettVurderingType.OPPRETT_BASERT_PÅ_INNTEKTSMELDING);
        var mangler = Arrays.asList(lagMangel(orgnr, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));

        // Act
        var resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, mangler, informasjonBuilder).build();

        // Assert
        assertThat(resultat.getOverstyringer().size()).isEqualTo(1);
        var overstyring = finnOverstyringFor(resultat, orgnr);
        assertThat(overstyring).isNotNull();
        assertThat(overstyring.getArbeidsgiver().getOrgnr()).isEqualTo(orgnr);
        assertThat(overstyring.getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(overstyring.getArbeidsgiverNavn()).isNull();
        assertThat(overstyring.getHandling()).isEqualTo(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        assertThat(overstyring.getStillingsprosent()).isEqualTo(new Stillingsprosent(50));
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getFomDato()).isEqualTo(fom);
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getTomDato()).isEqualTo(tom);
    }

    @Test
    void mapper_til_nytt_arbeidsforhold_som_er_lagt_til_av_saksbehandler() {
        // Arrange
        String orgnr = "999999999";
        String begrunnelse = "Begrunnelse";
        LocalDate fom = LocalDate.of(2022,1,1);
        LocalDate tom = LocalDate.of(2023,1,1);
        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        var ref = InternArbeidsforholdRef.nyRef().getReferanse();
        var dto = new ManueltArbeidsforholdDto(UUID.randomUUID(), begrunnelse, orgnr, ref, "Dette er et navn",
            fom, tom, 50, ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER);
        var mangler = Arrays.asList(lagMangel(orgnr, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));

        // Act
        var resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, mangler, informasjonBuilder).build();

        // Assert
        assertThat(resultat.getOverstyringer().size()).isEqualTo(1);
        var overstyring = finnOverstyringFor(resultat, orgnr);
        assertThat(overstyring).isNotNull();
        assertThat(overstyring.getArbeidsgiver().getOrgnr()).isEqualTo(orgnr);
        assertThat(overstyring.getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(overstyring.getArbeidsgiverNavn()).isEqualTo("Dette er et navn");
        assertThat(overstyring.getArbeidsforholdRef().getReferanse()).isEqualTo(ref);
        assertThat(overstyring.getHandling()).isEqualTo(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        assertThat(overstyring.getStillingsprosent()).isEqualTo(new Stillingsprosent(50));
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getFomDato()).isEqualTo(fom);
        assertThat(overstyring.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getTomDato()).isEqualTo(tom);
    }

    @Test
    void mapper_til_nytt_arbeidsforhold_når_annet_arbeidsforhold_finnes_fra_før() {
        // Arrange
        String nyttOrgnr = "999999999";
        String eksisterendeOrgnr = "999999998";
        String begrunnelse = "Begrunnelse";
        LocalDate fom = LocalDate.of(2022,1,1);
        LocalDate tom = LocalDate.of(2023,1,1);

        // Gammel avklaring fra saksbehandler
        ArbeidsforholdInformasjon eksisterendeInformasjonBuilder = getArbeidsforholdInformasjonBuilder(eksisterendeOrgnr, Optional.empty());

        // Saksbehandler sender inn ny overstyring
        ArbeidsforholdInformasjonBuilder nyInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.of(eksisterendeInformasjonBuilder));

        var dto = new ManueltArbeidsforholdDto(UUID.randomUUID(), begrunnelse, nyttOrgnr, null, null,
            fom, tom, 50, ArbeidsforholdKomplettVurderingType.OPPRETT_BASERT_PÅ_INNTEKTSMELDING);
        var mangler = Arrays.asList(lagMangel(nyttOrgnr, InternArbeidsforholdRef.nullRef(), AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));

        // Act
        var resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, mangler, nyInformasjonBuilder).build();

        // Assert
        assertThat(resultat.getOverstyringer().size()).isEqualTo(2);
        var overstyringEksisterende = finnOverstyringFor(resultat, eksisterendeOrgnr);
        assertThat(overstyringEksisterende).isNotNull();

        var overstyringNy = finnOverstyringFor(resultat, nyttOrgnr);
        assertThat(overstyringNy).isNotNull();
        assertThat(overstyringNy.getArbeidsgiver().getOrgnr()).isEqualTo(nyttOrgnr);
        assertThat(overstyringNy.getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(overstyringNy.getArbeidsgiverNavn()).isNull();
        assertThat(overstyringNy.getHandling()).isEqualTo(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        assertThat(overstyringNy.getStillingsprosent()).isEqualTo(new Stillingsprosent(50));
        assertThat(overstyringNy.getArbeidsforholdOverstyrtePerioder()).hasSize(1);
        assertThat(overstyringNy.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getFomDato()).isEqualTo(fom);
        assertThat(overstyringNy.getArbeidsforholdOverstyrtePerioder().get(0).getOverstyrtePeriode().getTomDato()).isEqualTo(tom);
    }

    @Test
    void skal_kunne_fjerne_eksisterende_overstyring() {
        // Arrange
        String eksisterendeOrgnr = "342352362";
        String begrunnelse = "Begrunnelse";
        LocalDate fom = LocalDate.of(2022,1,1);
        LocalDate tom = LocalDate.of(2023,1,1);

        ArbeidsforholdInformasjon eksisterendeInformasjonBuilder = getArbeidsforholdInformasjonBuilder(eksisterendeOrgnr, Optional.empty());
        ArbeidsforholdInformasjonBuilder nyInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.of(eksisterendeInformasjonBuilder));

        var dto = new ManueltArbeidsforholdDto(UUID.randomUUID(), begrunnelse, eksisterendeOrgnr, null, null,
            fom, tom, 50, ArbeidsforholdKomplettVurderingType.FJERN_FRA_BEHANDLINGEN);
        List<ArbeidsforholdInntektsmeldingMangel> mangler = Collections.emptyList();

        // Act
        var resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, mangler, nyInformasjonBuilder).build();

        // Assert
        assertThat(resultat.getOverstyringer()).isEmpty();
    }

    @Test
    void skal_kunne_fjerne_eksisterende_overstyring_men_ta_vare_på_andre() {
        // Arrange
        String orgNrSomIkkeSkalSlettes = "999999998";
        String orgNrSomSkalSlettes = "999999999";
        String begrunnelse = "Begrunnelse";
        LocalDate fom = LocalDate.of(2022,1,1);
        LocalDate tom = LocalDate.of(2023,1,1);

        ArbeidsforholdInformasjon informasjonMedEksisterendeOrgnr = getArbeidsforholdInformasjonBuilder(orgNrSomIkkeSkalSlettes, Optional.empty());
        ArbeidsforholdInformasjon informasjonMedOrgnrSomSkalSlettes = getArbeidsforholdInformasjonBuilder(orgNrSomSkalSlettes, Optional.of(informasjonMedEksisterendeOrgnr));
        ArbeidsforholdInformasjonBuilder nyInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.of(informasjonMedOrgnrSomSkalSlettes));

        var dto = new ManueltArbeidsforholdDto(UUID.randomUUID(), begrunnelse, orgNrSomSkalSlettes, null, null,
            fom, tom, 50, ArbeidsforholdKomplettVurderingType.FJERN_FRA_BEHANDLINGEN);
        List<ArbeidsforholdInntektsmeldingMangel> mangler = Collections.emptyList();

        // Act
        var resultat = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, mangler, nyInformasjonBuilder).build();

        // Assert
        assertThat(resultat.getOverstyringer().size()).isEqualTo(1);
        var overstyringEksisterende = finnOverstyringFor(resultat, orgNrSomIkkeSkalSlettes);
        assertThat(overstyringEksisterende).isNotNull();
    }



    private ArbeidsforholdInformasjon getArbeidsforholdInformasjonBuilder(String eksisterendeOrgnr, Optional<ArbeidsforholdInformasjon> eksisterende) {
        ArbeidsforholdInformasjonBuilder eksisterendeInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(eksisterende);
        var eksisterendeOSBuilder = eksisterendeInformasjonBuilder.getOverstyringBuilderFor(Arbeidsgiver.virksomhet(eksisterendeOrgnr), InternArbeidsforholdRef.nullRef());
        eksisterendeOSBuilder.medAngittStillingsprosent(new Stillingsprosent(100))
            .medBeskrivelse("Dette er en beskrivelse")
            .medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        eksisterendeInformasjonBuilder.leggTil(eksisterendeOSBuilder);
        return eksisterendeInformasjonBuilder.build();
    }

    private ArbeidsforholdOverstyring finnOverstyringFor(ArbeidsforholdInformasjon resultat, String orgnr) {
        return resultat.getOverstyringer().stream().filter(os -> os.getArbeidsgiver().getIdentifikator().equals(orgnr)).findFirst().orElse(null);
    }

    private ArbeidsforholdInntektsmeldingMangel lagMangel(String orgnr, InternArbeidsforholdRef ref, AksjonspunktÅrsak årsak) {
        return new ArbeidsforholdInntektsmeldingMangel(Arbeidsgiver.virksomhet(orgnr), ref, årsak);
    }

}
