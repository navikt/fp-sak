package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.PerioderForKravDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.konfig.Tid;

class KravperioderMapperTest {
    private static final LocalDate STP = LocalDate.of(2021,12,1);
    private static final Behandling BEHANDLING = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
    private static final BehandlingReferanse BEHANDLING_REF = BehandlingReferanse.fra(BEHANDLING, Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(STP).build());
    private InntektArbeidYtelseAggregatBuilder data = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = data.getAktørArbeidBuilder(BEHANDLING_REF.aktørId());
    private ArbeidsforholdInformasjonBuilder arbeidsforholdOverstyringer = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
    private List<Inntektsmelding> aktiveInntektsmeldinger = new ArrayList<>();

    @Test
    public void inntektsmelding_med_et_arbeidsforhold_mappes_korrekt() {
        var ag = Arbeidsgiver.virksomhet("99999999");
        var ref = InternArbeidsforholdRef.nyRef();
        lagRegisterArbeid(ag, ref, førStp(500), etterSTP(500));
        aktiveInntektsmeldinger.add(lagIM(ag, ref, 500_000, 500_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(1);
        assertKrav(resultat, ag, ref, 500_000, STP, Tid.TIDENES_ENDE);
    }

    @Test
    public void inntektsmelding_med_to_arbeidsforhold_ulike_bedrifter_mappes_korrekt() {
        var ag = Arbeidsgiver.virksomhet("99999999");
        var ag2 = Arbeidsgiver.virksomhet("99999998");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        lagRegisterArbeid(ag, ref, førStp(500), etterSTP(500));
        lagRegisterArbeid(ag2, ref2, førStp(200), etterSTP(100));
        aktiveInntektsmeldinger.add(lagIM(ag, ref, 500_000, 500_000, STP));
        aktiveInntektsmeldinger.add(lagIM(ag2, ref2, 300_000, 300_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(2);
        assertKrav(resultat, ag, ref, 500_000, STP, Tid.TIDENES_ENDE);
        assertKrav(resultat, ag2, ref2, 300_000, STP, Tid.TIDENES_ENDE);
    }

    @Test
    public void inntektsmelding_med_to_arbeidsforhold_i_samme_bedrift_med_id() {
        var ag = Arbeidsgiver.virksomhet("99999999");
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        lagRegisterArbeid(ag, ref, førStp(500), etterSTP(500));
        lagRegisterArbeid(ag, ref2, førStp(200), etterSTP(100));
        aktiveInntektsmeldinger.add(lagIM(ag, InternArbeidsforholdRef.nullRef(), 500_000, 500_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(1);
        assertKrav(resultat, ag, InternArbeidsforholdRef.nullRef(), 500_000, STP, Tid.TIDENES_ENDE);
    }

    @Test
    public void inntektsmelding_med_to_arbeidsforhold_i_samme_bedrift_en_uten_id() {
        var ag = Arbeidsgiver.virksomhet("99999999");
        var ref = InternArbeidsforholdRef.nyRef();
        lagRegisterArbeid(ag, ref, førStp(500), etterSTP(500));
        lagRegisterArbeid(ag, InternArbeidsforholdRef.nullRef(), etterSTP(50), Tid.TIDENES_ENDE);
        aktiveInntektsmeldinger.add(lagIM(ag, InternArbeidsforholdRef.nullRef(), 500_000, 500_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(1);
        assertKrav(resultat, ag, InternArbeidsforholdRef.nullRef(), 500_000, STP, Tid.TIDENES_ENDE);
    }

    @Test
    public void inntektsmelding_med_et_manuelt_opprettet_arbeidsforhold() {
        var ag = Arbeidsgiver.virksomhet("99999999");
        var ref = InternArbeidsforholdRef.nyRef();
        lagOverstyrtArbeid(ag, ref, førStp(500), etterSTP(500));
        aktiveInntektsmeldinger.add(lagIM(ag, ref, 500_000, 500_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(1);
        assertKrav(resultat, ag, ref, 500_000, STP, Tid.TIDENES_ENDE);
    }

    @Test
    public void inntektsmeldinger_med_et_manuelt_opprettet_arbeidsforhold_og_et_vanlig() {
        var ag1 = Arbeidsgiver.virksomhet("99999999");
        var ag2 = Arbeidsgiver.virksomhet("99999998");
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        lagOverstyrtArbeid(ag1, ref1, førStp(500), etterSTP(500));
        lagRegisterArbeid(ag2, ref2, førStp(500), etterSTP(500));
        aktiveInntektsmeldinger.add(lagIM(ag1, ref1, 500_000, 500_000, STP));
        aktiveInntektsmeldinger.add(lagIM(ag2, ref2, 150_000, 150_000, STP));

        var resultat = KravperioderMapper.map(BEHANDLING_REF, aktiveInntektsmeldinger, byggIAY());

        assertThat(resultat).hasSize(2);
        assertKrav(resultat, ag1, ref1, 500_000, STP, Tid.TIDENES_ENDE);
        assertKrav(resultat, ag2, ref2, 150_000, STP, Tid.TIDENES_ENDE);

    }

    @Test
    public void gammel_og_ny_im_for_samme_arbeidsforhold() {
        var ag1 = Arbeidsgiver.virksomhet("99999999");
        var ref1 = InternArbeidsforholdRef.nyRef();
        lagOverstyrtArbeid(ag1, ref1, førStp(500), etterSTP(500));
        var førsteInnsendingstidspunkt = STP.minusDays(200);
        var tidligereInnsendtIM = lagIM(ag1, ref1, 500_000, 500_000, STP, førsteInnsendingstidspunkt);
        var aktivIM = lagIM(ag1, ref1, 500_000, 500_000, STP);
        aktiveInntektsmeldinger.add(aktivIM);
        var alleIMPåSak = Arrays.asList(aktivIM, tidligereInnsendtIM);

        var resultat = KravperioderMapper.map(BEHANDLING_REF, alleIMPåSak, byggIAY());

        assertThat(resultat).hasSize(1);
        var krav = resultat.get(0);
        assertThat(krav.getArbeidsgiver().getIdentifikator()).isEqualTo(ag1.getIdentifikator());
        assertThat(krav.getArbeidsforholdRef().getReferanse()).isEqualTo(ref1.getReferanse());
        assertThat(krav.getPerioder()).hasSize(2);
        var kravperioder = krav.getPerioder().stream()
            .sorted(Comparator.comparing(PerioderForKravDto::getInnsendingsdato))
            .collect(Collectors.toList());
        assertThat(kravperioder.get(0).getPerioder()).hasSize(1);
        assertThat(kravperioder.get(0).getPerioder().get(0).periode().getFomDato()).isEqualTo(STP);
        assertThat(kravperioder.get(0).getPerioder().get(0).periode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(kravperioder.get(0).getInnsendingsdato()).isEqualTo(førsteInnsendingstidspunkt);
        assertThat(kravperioder.get(0).getPerioder().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(500_000));

        assertThat(kravperioder.get(1).getPerioder()).hasSize(1);
        assertThat(kravperioder.get(1).getPerioder().get(0).periode().getFomDato()).isEqualTo(STP);
        assertThat(kravperioder.get(1).getPerioder().get(0).periode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(kravperioder.get(1).getPerioder().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
        assertThat(kravperioder.get(1).getInnsendingsdato()).isEqualTo(STP);

        assertThat(krav.getSisteSøktePerioder()).hasSize(1);
        assertThat(krav.getSisteSøktePerioder().get(0).getFomDato()).isEqualTo(STP);
        assertThat(krav.getSisteSøktePerioder().get(0).getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    public void gammel_og_ny_im_for_samme_arbeidsforhold_ulike_perioder() {
        var ag1 = Arbeidsgiver.virksomhet("99999999");
        var ref1 = InternArbeidsforholdRef.nyRef();
        lagOverstyrtArbeid(ag1, ref1, førStp(500), etterSTP(500));
        var førsteInnsendingstidspunkt = STP.minusDays(200);
        var tidligereInnsendtIM = lagIM(ag1, ref1, 500_000, 500_000, STP, førsteInnsendingstidspunkt);
        var nyStartdato = STP.plusDays(10);
        var aktivIM = lagIM(ag1, ref1, 500_000, 350_000, nyStartdato);
        aktiveInntektsmeldinger.add(aktivIM);
        var alleIMPåSak = Arrays.asList(aktivIM, tidligereInnsendtIM);

        var resultat = KravperioderMapper.map(BEHANDLING_REF, alleIMPåSak, byggIAY());

        assertThat(resultat).hasSize(1);
        var krav = resultat.get(0);
        assertThat(krav.getArbeidsgiver().getIdentifikator()).isEqualTo(ag1.getIdentifikator());
        assertThat(krav.getArbeidsforholdRef().getReferanse()).isEqualTo(ref1.getReferanse());
        assertThat(krav.getPerioder()).hasSize(2);
        var kravperioder = krav.getPerioder().stream()
            .sorted(Comparator.comparing(PerioderForKravDto::getInnsendingsdato))
            .collect(Collectors.toList());
        assertThat(kravperioder.get(0).getPerioder()).hasSize(1);
        assertThat(kravperioder.get(0).getPerioder().get(0).periode().getFomDato()).isEqualTo(STP);
        assertThat(kravperioder.get(0).getPerioder().get(0).periode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(kravperioder.get(0).getInnsendingsdato()).isEqualTo(førsteInnsendingstidspunkt);
        assertThat(kravperioder.get(0).getPerioder().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(500_000));

        assertThat(kravperioder.get(1).getPerioder()).hasSize(1);

        // Siden startdato fra IM var etter stp men startdato for AF var før STP settes startdato for refusjon lik STP
        assertThat(kravperioder.get(1).getPerioder().get(0).periode().getFomDato()).isEqualTo(STP);

        assertThat(kravperioder.get(1).getPerioder().get(0).periode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(kravperioder.get(1).getPerioder().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(350_000));
        assertThat(kravperioder.get(1).getInnsendingsdato()).isEqualTo(nyStartdato);

        assertThat(krav.getSisteSøktePerioder()).hasSize(1);
        assertThat(krav.getSisteSøktePerioder().get(0).getFomDato()).isEqualTo(STP);
        assertThat(krav.getSisteSøktePerioder().get(0).getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
    }

    private void assertKrav(List<KravperioderPrArbeidsforholdDto> resultat,
                            Arbeidsgiver ag,
                            InternArbeidsforholdRef ref,
                            int beløp,
                            LocalDate fom,
                            LocalDate tom) {
        KravperioderPrArbeidsforholdDto mappetKrav = finnrettKrav(resultat, ag, ref);
        assertThat(mappetKrav.getPerioder()).hasSize(1);
        assertThat(mappetKrav.getPerioder()).hasSize(1);
        assertThat(mappetKrav.getPerioder().get(0).getPerioder()).hasSize(1);
        assertThat(mappetKrav.getPerioder().get(0).getPerioder().get(0).beløp()).isEqualByComparingTo(BigDecimal.valueOf(beløp));
        assertThat(mappetKrav.getPerioder().get(0).getPerioder().get(0).periode().getFomDato()).isEqualTo(fom);
        assertThat(mappetKrav.getPerioder().get(0).getPerioder().get(0).periode().getTomDato()).isEqualTo(tom);
    }

    private KravperioderPrArbeidsforholdDto finnrettKrav(List<KravperioderPrArbeidsforholdDto> resultat,
                                                                               Arbeidsgiver ag,
                                                                               InternArbeidsforholdRef ref) {
        var mappetKrav = resultat.stream()
            .filter(krav -> krav.getArbeidsgiver().getIdentifikator().equals(ag.getIdentifikator())
                && Objects.equals(krav.getArbeidsforholdRef().getReferanse(), ref.getReferanse()))
            .findFirst()
            .orElseThrow();
        return mappetKrav;
    }

    private LocalDate førStp(int dagerFør) {
        return STP.minusDays(dagerFør);
    }

    private LocalDate etterSTP(int dagerEtter) {
        return STP.plusDays(dagerEtter);
    }

    private Inntektsmelding lagIM(Arbeidsgiver ag, InternArbeidsforholdRef internRef, Integer inntekt, Integer refusjon, LocalDate startdatoPermisjon) {
        return lagIM(ag, internRef, inntekt, refusjon, startdatoPermisjon, startdatoPermisjon);
    }

    private Inntektsmelding lagIM(Arbeidsgiver ag, InternArbeidsforholdRef internRef, Integer inntekt, Integer refusjon, LocalDate startdatoPermisjon, LocalDate innsendingstidspunkt) {
        return InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.valueOf(inntekt))
            .medRefusjon(refusjon != null ? BigDecimal.valueOf(refusjon) : null)
            .medArbeidsforholdId(internRef)
            .medArbeidsgiver(ag)
            .medStartDatoPermisjon(startdatoPermisjon)
            .medInnsendingstidspunkt(LocalDateTime.of(innsendingstidspunkt, LocalTime.MIDNIGHT))
            .build();
    }

    private void lagOverstyrtArbeid(Arbeidsgiver ag, InternArbeidsforholdRef internRef, LocalDate fom, LocalDate tom) {
        ArbeidsforholdOverstyringBuilder builder = ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(ag)
            .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100)))
            .leggTilOverstyrtPeriode(fom, tom);
        if(internRef!= null) {
            builder.medArbeidsforholdRef(internRef);
        }
        arbeidsforholdOverstyringer.leggTil(builder);
    }

    private void lagRegisterArbeid(Arbeidsgiver ag, InternArbeidsforholdRef internRef, LocalDate fom, LocalDate tom) {
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aaBuilder = yaBuilder.getAktivitetsAvtaleBuilder();
        var aa = aaBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yaBuilder.leggTilAktivitetsAvtale(aa)
            .medArbeidsgiver(ag)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        if(internRef!= null) {
            yaBuilder.medArbeidsforholdId(internRef);
        }
        arbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
    }

    private InntektArbeidYtelseGrunnlag byggIAY() {
        data.leggTilAktørArbeid(arbeidBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medData(data)
            .medInntektsmeldinger(aktiveInntektsmeldinger)
            .medInformasjon(arbeidsforholdOverstyringer.build())
            .build();
    }

}
