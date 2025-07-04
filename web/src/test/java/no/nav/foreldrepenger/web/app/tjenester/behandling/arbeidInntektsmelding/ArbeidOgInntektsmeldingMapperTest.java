package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ArbeidsgiverIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.vedtak.konfig.Tid;

class ArbeidOgInntektsmeldingMapperTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(20);

    @Test
    void skal_teste_mapping_av_inntektsmelding_med_ref_og_kontakt() {
        var internRef = InternArbeidsforholdRef.nyRef();
        var ekstrernRef = EksternArbeidsforholdRef.ref("AB-001");

        var im = lagIM("99999999", internRef, 50000, null);
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, Collections.singletonList(lagRef("99999999", internRef, ekstrernRef)),
            Optional.of(new KontaktinformasjonIM("John Johnsen", "11111111")), Optional.empty(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList());

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
        var relevantMangel = new ArbeidsforholdMangel(Arbeidsgiver.virksomhet(relevantOrgnr), InternArbeidsforholdRef.nullRef(),
            AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
        var irrelevantMangel = new ArbeidsforholdMangel(Arbeidsgiver.virksomhet(irrelevantOrgnr), InternArbeidsforholdRef.nullRef(),
            AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING);
        var permisjonUtenSluttdatoMangel = new ArbeidsforholdMangel(Arbeidsgiver.virksomhet(irrelevantOrgnr), InternArbeidsforholdRef.nullRef(),
            AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO);
        var mangler = Arrays.asList(relevantMangel, irrelevantMangel, permisjonUtenSluttdatoMangel);
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
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, Collections.singletonList(lagRef(relevantOrgnr, internRef, ekstrernRef)),
            Optional.of(new KontaktinformasjonIM("John Johnsen", "11111111")), Optional.empty(), mangler, saksbehandlersValg,
            Collections.emptyList());

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
        var mappetRes = ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, Collections.emptyList(), Optional.empty(), Optional.empty(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
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
        var filter = new InntektFilter(List.of(inntekt));

        // Act
        var inntekter = ArbeidOgInntektsmeldingMapper.mapInntekter(filter, LocalDate.of(2022, 10, 1));

        // Assert
        assertThat(inntekter).hasSize(1);
        assertThat(inntekter.get(0).inntekter()).hasSize(10);
    }

    @Test
    void mapping_av_arbeidsforhold_med_permisjon_uten_sluttdato() {
        //Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdReferanse = arbeidsforholdId.getReferanse();
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG), arbeidsforholdReferanse);

        var arbeidsgiver = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());

        var yrkesaktivitet = lagYrkesAktivitetMedPermisjon(arbeidsgiver, BigDecimal.valueOf(100), LocalDate.now().minusWeeks(1),
            LocalDate.now().plusMonths(1), arbeidsforholdId, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null).build();


        var arbeidsforholdReferanser = List.of(lagReferanser(arbeidsgiver, arbeidsforholdId, arbeidsforholdReferanse));
        var stp = LocalDate.now().minusDays(3);
        var mangler = List.of(new ArbeidsforholdMangel(arbeidsgiver, arbeidsforholdId, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING),
            new ArbeidsforholdMangel(arbeidsgiver, arbeidsforholdId, AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO));

        //Act
        var arbeidsforholdDto = ArbeidOgInntektsmeldingMapper.mapTilArbeidsforholdDto(arbeidsforholdReferanser, stp, yrkesaktivitet, mangler,
            Collections.emptyList(), Collections.emptyList()).orElse(null);

        //Assert
        assertThat(arbeidsforholdDto).isNotNull();
        assertThat(arbeidsforholdDto.internArbeidsforholdId()).isEqualTo(arbeidsforholdId.getReferanse());
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonFom()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonStatus()).isNull();
    }

    @Test
    void mapping_av_yrkesaktivitet_med_overlappende_aktivitetsavtaler() {
        //Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdReferanse = arbeidsforholdId.getReferanse();
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG), arbeidsforholdReferanse);

        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtale1 = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE))
            .medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale2 = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(6), Tid.TIDENES_ENDE))
            .medProsentsats(BigDecimal.valueOf(50));

        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(3), Tid.TIDENES_ENDE));
        yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        var arbeidsgiver = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());


        var arbeidsforholdReferanser = List.of(lagReferanser(arbeidsgiver, arbeidsforholdId, arbeidsforholdReferanse));

        //Act
        var arbeidsforholdDto = ArbeidOgInntektsmeldingMapper.mapTilArbeidsforholdDto(arbeidsforholdReferanser, SKJÆRINGSTIDSPUNKT,
            yrkesaktivitetBuilder.build(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()).orElse(null);

        //Assert
        assertThat(arbeidsforholdDto).isNotNull();
        assertThat(arbeidsforholdDto.stillingsprosent().intValue()).isEqualTo(50);
    }

    @Test
    void mapping_av_yrkesaktivitet_med_stillingsprosent_utenfor_ansettelsesperiode() {
        //Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdReferanse = arbeidsforholdId.getReferanse();
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG), arbeidsforholdReferanse);

        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtale1 = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.plusYears(1), Tid.TIDENES_ENDE))
            .medProsentsats(BigDecimal.valueOf(50));
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(3), SKJÆRINGSTIDSPUNKT.plusMonths(11)));
        yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        var arbeidsgiver = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());


        var arbeidsforholdReferanser = List.of(lagReferanser(arbeidsgiver, arbeidsforholdId, arbeidsforholdReferanse));

        //Act
        var arbeidsforholdDto = ArbeidOgInntektsmeldingMapper.mapTilArbeidsforholdDto(arbeidsforholdReferanser, SKJÆRINGSTIDSPUNKT,
            yrkesaktivitetBuilder.build(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()).orElse(null);

        //Assert
        assertThat(arbeidsforholdDto).isNotNull();
        assertThat(arbeidsforholdDto.stillingsprosent()).isNotNull();
        assertThat(arbeidsforholdDto.stillingsprosent().intValue()).isEqualTo(50);
    }

    @Test
    void mapping_av_arbeidsforhold_med_permisjon_uten_sluttdato_hvor_aksjonspunkt_er_bekreftet() {
        //Arrange
        var internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdReferanse = internArbeidsforholdRef.getReferanse();
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG), arbeidsforholdReferanse);

        var arbeidsgiver = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());

        var yrkesaktivitet = lagYrkesAktivitetMedPermisjon(arbeidsgiver, BigDecimal.valueOf(100), LocalDate.now().minusWeeks(1),
            LocalDate.now().plusMonths(1), internArbeidsforholdRef, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, null).build();


        var arbeidsforholdReferanser = List.of(lagReferanser(arbeidsgiver, internArbeidsforholdRef, arbeidsforholdReferanse));
        var stp = LocalDate.now().minusDays(3);
        var mangler = List.of(new ArbeidsforholdMangel(arbeidsgiver, internArbeidsforholdRef, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING),
            new ArbeidsforholdMangel(arbeidsgiver, internArbeidsforholdRef, AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO));

        var overstyring = List.of(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(internArbeidsforholdRef)
            .medBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.BRUK_PERMISJON))
            .build());

        //Act
        var arbeidsforholdDto = ArbeidOgInntektsmeldingMapper.mapTilArbeidsforholdDto(arbeidsforholdReferanser, stp, yrkesaktivitet, mangler,
            Collections.emptyList(), overstyring).orElse(null);

        //Assert
        assertThat(arbeidsforholdDto).isNotNull();
        assertThat(arbeidsforholdDto.internArbeidsforholdId()).isEqualTo(internArbeidsforholdRef.getReferanse());
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonStatus()).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
    }

    @Test
    void mapping_av_permisjon_uten_mangel() {
        //Arrange
        var internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdReferanse = internArbeidsforholdRef.getReferanse();
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG), arbeidsforholdReferanse);

        var arbeidsgiver = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());

        var yrkesaktivitet = lagYrkesAktivitetMedPermisjon(arbeidsgiver, BigDecimal.valueOf(100), LocalDate.now().minusWeeks(1),
            LocalDate.now().plusMonths(1), internArbeidsforholdRef, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, LocalDate.now().plusMonths(8)).build();


        var arbeidsforholdReferanser = List.of(lagReferanser(arbeidsgiver, internArbeidsforholdRef, arbeidsforholdReferanse));
        var stp = LocalDate.now().minusDays(3);


        //Act
        var arbeidsforholdDto = ArbeidOgInntektsmeldingMapper.mapTilArbeidsforholdDto(arbeidsforholdReferanser, stp, yrkesaktivitet,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList()).orElse(null);

        //Assert
        assertThat(arbeidsforholdDto).isNotNull();
        assertThat(arbeidsforholdDto.internArbeidsforholdId()).isEqualTo(internArbeidsforholdRef.getReferanse());
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(arbeidsforholdDto.permisjonOgMangel().permisjonStatus()).isNull();
    }

    private ArbeidsforholdReferanse lagReferanser(Arbeidsgiver arbeidsgiver1, InternArbeidsforholdRef arbeidsforholdRef2, String eksternReferanse2) {
        return new ArbeidsforholdReferanse(arbeidsgiver1, arbeidsforholdRef2, EksternArbeidsforholdRef.ref(eksternReferanse2));
    }

    private Inntekt lagInntekter(YearMonth fom, YearMonth tom, String orgnr) {
        var counter = fom;

        var builder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));

        while (counter.isBefore(tom)) {
            builder.leggTilInntektspost(
                InntektspostBuilder.ny().medPeriode(counter.atDay(1), counter.atEndOfMonth()).medBeløp(BigDecimal.valueOf(100)));
            counter = counter.plusMonths(1);
        }
        return builder.build();
    }

    private Arbeidsgiver lagVirksomhetArbeidsgiver(ArbeidsgiverIdentifikator arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator.value());
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

    private YrkesaktivitetBuilder lagYrkesAktivitetMedPermisjon(Arbeidsgiver virksomhet,
                                                                BigDecimal stillingsprosent,
                                                                LocalDate fraOgMed,
                                                                LocalDate tilOgMed,
                                                                InternArbeidsforholdRef arbeidsforholdId,
                                                                ArbeidType arbeidType,
                                                                LocalDate permisjonTom) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var permisjonsBuilder = YrkesaktivitetBuilder.nyPermisjonBuilder()
            .medPeriode(SKJÆRINGSTIDSPUNKT, permisjonTom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
            .medProsentsats(BigDecimal.valueOf(100));

        var aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
            .medProsentsats(stillingsprosent)
            .medSisteLønnsendringsdato(fraOgMed);
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));
        yrkesaktivitetBuilder.medArbeidType(arbeidType)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .leggTilPermisjon(permisjonsBuilder.build());

        return yrkesaktivitetBuilder;
    }

}
