package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.AktivitetPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class OpptjeningsgrunnlagAdapterTest {


    @Test
    void skal_filtrere_ut_underkjente_aktiviteter_hvis_det_finnes_en_aktivitet_på_samme_orgnummer_som_skal_til_vurdering() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.TIL_VURDERING)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        var aktivitet2 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId2, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();


        opptjeningAktiveter.add(aktivitet1);
        opptjeningAktiveter.add(aktivitet2);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(1);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_kappe_underkjente_aktiviteter_hvis_det_finnes_en_aktivitet_på_samme_orgnummer_som_skal_til_vurdering_som_overlapper() {
        var iDag = LocalDate.now();
        var femMånederSiden = iDag.minusMonths(5L);
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(femMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.TIL_VURDERING)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        var aktivitet2 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId2, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningAktiveter.add(aktivitet1);
        opptjeningAktiveter.add(aktivitet2);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(2);
        var aktivitetPeriode1 = opptjeningsgrunnlag.aktivitetPerioder().get(0);
        assertThat(aktivitetPeriode1.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT);
        assertThat(aktivitetPeriode1.getDatoIntervall()).isEqualTo(new LocalDateInterval(tiMånederSiden, femMånederSiden.minusDays(1)));

        var aktivitetPeriod2 = opptjeningsgrunnlag.aktivitetPerioder().get(1);
        assertThat(aktivitetPeriod2.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
        assertThat(aktivitetPeriod2.getDatoIntervall()).isEqualTo(new LocalDateInterval(femMånederSiden, iDag));
    }

    @Test
    void skal_ikke_kappe_godkjente_aktiviteter_hvis_det_finnes_en_aktivitet_på_samme_orgnummer_som_skal_til_vurdering_som_overlapper() {
        var iDag = LocalDate.now();
        var femMånederSiden = iDag.minusMonths(5L);
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(femMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.TIL_VURDERING)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        var aktivitet2 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId2, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningAktiveter.add(aktivitet1);
        opptjeningAktiveter.add(aktivitet2);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(2);

        var aktivitetPeriode1 = opptjeningsgrunnlag.aktivitetPerioder().get(0);
        assertThat(aktivitetPeriode1.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT);
        assertThat(aktivitetPeriode1.getDatoIntervall()).isEqualTo(new LocalDateInterval(tiMånederSiden, femMånederSiden.minusDays(1)));

        var aktivitetPeriode2 = opptjeningsgrunnlag.aktivitetPerioder().get(1);
        assertThat(aktivitetPeriode2.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT);
        assertThat(aktivitetPeriode2.getDatoIntervall()).isEqualTo(new LocalDateInterval(femMånederSiden, iDag));
    }

    @Test
    void skal_ikke_filtrere_ut_hvis_det_bare_er_en_periode() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.TIL_VURDERING)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningAktiveter.add(aktivitet1);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(1);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_ikke_filtrere_ut_hvis_det_bare_er_en_periode_godkjent() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningAktiveter.add(aktivitet1);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(1);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT);
    }

    @Test
    void skal_ikke_filtrere_ut_hvis_det_bare_er_en_periode_ikke_godkjent() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningAktiveter.add(aktivitet1);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(1);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT);
    }

    @Test
    void skal_filtrere_ut_underkjente_aktiviteter_hvis_det_finnes_en_aktivitet_på_samme_orgnummer_som_er_godkjent() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId1, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        var aktivitet2 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId2, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();


        opptjeningAktiveter.add(aktivitet1);
        opptjeningAktiveter.add(aktivitet2);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(1);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT);
    }

    @Test
    void skal_ikke_filterer_ut_underkjente_aktiviteter_hvis_det_finnes_en_aktivitet_på_samme_orgnummer_som_er_godkjent() {
        var iDag = LocalDate.now();
        var tiMånederSiden = iDag.minusMonths(10L);
        var orgNummer = "123";
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        var adapter = new OpptjeningsgrunnlagAdapter(iDag, tiMånederSiden, iDag);

        Collection<OpptjeningAktivitetPeriode> opptjeningAktiveter = new ArrayList<>();
        Collection<OpptjeningInntektPeriode> opptjeningInntekter = new ArrayList<>();


        var ny = InntektspostBuilder.ny();
        var inntektspost = ny.medInntektspostType(InntektspostType.LØNN).medBeløp(BigDecimal.TEN).medPeriode(tiMånederSiden, iDag).build();
        var opptjeningInntektPeriode = new OpptjeningInntektPeriode(inntektspost, Opptjeningsnøkkel.forOrgnummer(orgNummer));
        opptjeningInntekter.add(opptjeningInntektPeriode);


        var aktivitet1 = OpptjeningAktivitetPeriode.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE)
            .build();

        var aktivitet2 = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(arbeidsforholdId2, orgNummer, null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tiMånederSiden, iDag))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();


        opptjeningAktiveter.add(aktivitet1);
        opptjeningAktiveter.add(aktivitet2);
        var opptjeningsgrunnlag = adapter.mapTilGrunnlag(opptjeningAktiveter, opptjeningInntekter);

        assertThat(opptjeningsgrunnlag.aktivitetPerioder()).hasSize(2);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(0).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT);
        assertThat(opptjeningsgrunnlag.aktivitetPerioder().get(1).getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT);
    }
}
