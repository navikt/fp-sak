package no.nav.foreldrepenger.domene.mappers.fra_kalkulus;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.response.v1.besteberegning.BesteberegningGrunnlagDto;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

class KalkulusTilFpsakMapperTest {

    @Test
    void skal_teste_mapping() {
        var kontraktBgg = bgFraJson();

        var domeneBgg = KalkulusTilFpsakMapper.map(kontraktBgg, Optional.empty());

        var domenebg = domeneBgg.getBeregningsgrunnlag().orElseThrow();
        var kontraktbg = kontraktBgg.getBeregningsgrunnlag();
        assertThat(domenebg.getSkjæringstidspunkt()).isEqualTo(kontraktbg.getSkjæringstidspunkt());
        assertThat(domenebg.getGrunnbeløp().getVerdi()).isEqualTo(kontraktbg.getGrunnbeløp().verdi());
        assertThat(domenebg.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus))
            .containsAll(kontraktbg.getAktivitetStatuser().stream().map(KodeverkFraKalkulusMapper::mapAktivitetstatus).collect(Collectors.toList()));
        assertThat(domenebg.getBeregningsgrunnlagPerioder()).hasSameSizeAs(kontraktbg.getBeregningsgrunnlagPerioder());
        assertPerioder(domenebg.getBeregningsgrunnlagPerioder().stream()
            .sorted(Comparator.comparing(bgp -> bgp.getPeriode().getFomDato())).collect(Collectors.toList()), kontraktbg.getBeregningsgrunnlagPerioder().stream()
            .sorted(Comparator.comparing(bgp -> bgp.getPeriode().getFom())).collect(Collectors.toList()));
        if (kontraktBgg.getRegisterAktiviteter() != null) {
            assertThat(domeneBgg.getRegisterAktiviteter()).isNotNull();
            assertThat(domeneBgg.getRegisterAktiviteter().getSkjæringstidspunktOpptjening()).isEqualTo(kontraktBgg.getRegisterAktiviteter().getSkjæringstidspunktOpptjening());
            assertLikeAktiviteter(kontraktBgg.getRegisterAktiviteter().getAktiviteter(), domeneBgg.getRegisterAktiviteter().getBeregningAktiviteter());
        }
    }

    @Test
    void skal_teste_mapping_med_besteberegning() {
        var kontraktBgg = bgFraJson();


        var arbRef = UUID.randomUUID().toString();
        var inntektDPDto = new BesteberegningGrunnlagDto.BesteberegningInntektDto(OpptjeningAktivitetType.DAGPENGER, Beløp.fra(5000),
            null, null);
        var inntektATDto = new BesteberegningGrunnlagDto.BesteberegningInntektDto(OpptjeningAktivitetType.ARBEID, Beløp.fra(1234),
            new no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver("999999999", null), new InternArbeidsforholdRefDto(arbRef));
        var inntektMåned = new BesteberegningGrunnlagDto.BesteberegningMånedDto(
            new Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)), List.of(inntektDPDto, inntektATDto));
        var besteberegningDto = new BesteberegningGrunnlagDto(Collections.singletonList(inntektMåned), Beløp.fra(70000));
        var domeneBgg = KalkulusTilFpsakMapper.map(kontraktBgg, Optional.of(besteberegningDto));

        var domenebg = domeneBgg.getBeregningsgrunnlag().orElseThrow();
        var kontraktbg = kontraktBgg.getBeregningsgrunnlag();
        assertThat(domenebg.getSkjæringstidspunkt()).isEqualTo(kontraktbg.getSkjæringstidspunkt());
        assertThat(domenebg.getGrunnbeløp().getVerdi()).isEqualTo(kontraktbg.getGrunnbeløp().verdi());
        assertThat(domenebg.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus))
            .containsAll(kontraktbg.getAktivitetStatuser().stream().map(KodeverkFraKalkulusMapper::mapAktivitetstatus).collect(Collectors.toList()));
        assertThat(domenebg.getBeregningsgrunnlagPerioder()).hasSameSizeAs(kontraktbg.getBeregningsgrunnlagPerioder());
        assertPerioder(domenebg.getBeregningsgrunnlagPerioder().stream()
            .sorted(Comparator.comparing(bgp -> bgp.getPeriode().getFomDato())).collect(Collectors.toList()), kontraktbg.getBeregningsgrunnlagPerioder().stream()
            .sorted(Comparator.comparing(bgp -> bgp.getPeriode().getFom())).collect(Collectors.toList()));
        if (kontraktBgg.getRegisterAktiviteter() != null) {
            assertThat(domeneBgg.getRegisterAktiviteter()).isNotNull();
            assertThat(domeneBgg.getRegisterAktiviteter().getSkjæringstidspunktOpptjening()).isEqualTo(kontraktBgg.getRegisterAktiviteter().getSkjæringstidspunktOpptjening());
            assertLikeAktiviteter(kontraktBgg.getRegisterAktiviteter().getAktiviteter(), domeneBgg.getRegisterAktiviteter().getBeregningAktiviteter());
        }

        var domeneBbg = domenebg.getBesteberegningGrunnlag();
        assertThat(domeneBbg).isPresent();
        assertThat(domeneBbg.get().getAvvik().get()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(domeneBbg.get().getSeksBesteMåneder()).hasSize(1);
        assertThat(domeneBbg.get().getSeksBesteMåneder().stream().findFirst().get().getInntekter()).hasSize(2);
        assertThat(domeneBbg.get().getSeksBesteMåneder().stream().findFirst().get().getPeriode().getFomDato()).isEqualTo(inntektMåned.periode().getFom());
        assertThat(domeneBbg.get().getSeksBesteMåneder().stream().findFirst().get().getPeriode().getTomDato()).isEqualTo(inntektMåned.periode().getTom());
        var atAndel = domeneBbg.get().getSeksBesteMåneder().stream().findFirst().get().getInntekter().stream().filter(i -> i.getOpptjeningAktivitetType().equals(
            no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.ARBEID)).findFirst();
        var dpAndel = domeneBbg.get().getSeksBesteMåneder().stream().findFirst().get().getInntekter().stream().filter(i -> i.getOpptjeningAktivitetType().equals(
            no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.DAGPENGER)).findFirst();
        assertThat(dpAndel.get().getInntekt()).isEqualByComparingTo(BigDecimal.valueOf(5000));

        assertThat(atAndel.get().getInntekt()).isEqualByComparingTo(BigDecimal.valueOf(1234));
        assertThat(atAndel.get().getArbeidsgiver().getIdentifikator()).isEqualTo("999999999");
        assertThat(atAndel.get().getArbeidsforholdRef().getReferanse()).isEqualTo(arbRef);
    }

    private void assertLikeAktiviteter(List<BeregningAktivitetDto> kontraktAktiviteter, List<BeregningAktivitet> domeneAktiviteter) {
        assertThat(kontraktAktiviteter).hasSameSizeAs(domeneAktiviteter);
        kontraktAktiviteter.forEach(ka -> {
            var domeneAktivitet = domeneAktiviteter.stream()
                .filter(a -> matcherArbeidsgiver(a.getArbeidsgiver(), ka.getArbeidsgiver()))
                .filter(a -> Objects.equals(ka.getArbeidsforholdRef().getAbakusReferanse(), a.getArbeidsforholdRef().getReferanse()))
                .findFirst()
                .orElseThrow();
            assertThat(domeneAktivitet.getPeriode().getFomDato()).isEqualTo(ka.getPeriode().getFom());
            assertThat(domeneAktivitet.getPeriode().getTomDato()).isEqualTo(ka.getPeriode().getTom());
        });
    }

    private boolean matcherArbeidsgiver(Arbeidsgiver domeneAG, no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver kontraktAG) {
        if (domeneAG == null) {
            return kontraktAG == null;
        }
        return kontraktAG.getArbeidsgiverOrgnr().equals(domeneAG.getIdentifikator());
    }

    private void assertPerioder(List<BeregningsgrunnlagPeriode> domenePerioder,
                                List<BeregningsgrunnlagPeriodeDto> kontraktPerioder) {
        for (var i = 0; i < domenePerioder.size(); i++) {
            var kontraktperiode = kontraktPerioder.get(i);
            var domeneperiode = domenePerioder.get(i);
            assertThat(kontraktperiode.getPeriode().getFom()).isEqualTo(domeneperiode.getPeriode().getFomDato());
            assertThat(kontraktperiode.getPeriode().getTom()).isEqualTo(domeneperiode.getPeriode().getTomDato());
            assertThat(kontraktperiode.getAvkortetPrÅr().verdi()).isEqualTo(domeneperiode.getAvkortetPrÅr());
            assertThat(kontraktperiode.getRedusertPrÅr().verdi()).isEqualTo(domeneperiode.getRedusertPrÅr());
            assertThat(kontraktperiode.getBruttoPrÅr().verdi()).isEqualTo(domeneperiode.getBruttoPrÅr());
            assertThat(kontraktperiode.getDagsats()).isEqualTo(domeneperiode.getDagsats());
            assertThat(domeneperiode.getPeriodeÅrsaker()).containsAll(kontraktperiode.getPeriodeÅrsaker()
                .stream().map(KodeverkFraKalkulusMapper::mapPeriodeÅrsak).toList());
            assertThat(kontraktperiode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSameSizeAs(domeneperiode.getBeregningsgrunnlagPrStatusOgAndelList());
            assertAndeler(domeneperiode.getBeregningsgrunnlagPrStatusOgAndelList(), kontraktperiode.getBeregningsgrunnlagPrStatusOgAndelList());
        }

    }

    private void assertAndeler(List<BeregningsgrunnlagPrStatusOgAndel> domeneAndeler,
                               List<BeregningsgrunnlagPrStatusOgAndelDto> kontraktAndeler) {
        domeneAndeler.forEach(domeneAndel ->{
            var kontraktAndel = kontraktAndeler.stream()
                .filter(a -> a.getAndelsnr().equals(domeneAndel.getAndelsnr()))
                .findFirst()
                .orElseThrow();
            assertThat(KodeverkFraKalkulusMapper.mapAktivitetstatus(kontraktAndel.getAktivitetStatus())).isEqualTo(domeneAndel.getAktivitetStatus());
            assertBeløp(kontraktAndel.getBeregnetPrÅr(), domeneAndel.getBeregnetPrÅr());
            assertBeløp(kontraktAndel.getFordeltPrÅr(), domeneAndel.getFordeltPrÅr());
            assertBeløp(kontraktAndel.getOverstyrtPrÅr(), domeneAndel.getOverstyrtPrÅr());
            assertBeløp(kontraktAndel.getBruttoPrÅr(), domeneAndel.getBruttoPrÅr());
            assertBeløp(kontraktAndel.getAvkortetPrÅr(), domeneAndel.getAvkortetPrÅr());
            assertBeløp(kontraktAndel.getRedusertBrukersAndelPrÅr(), domeneAndel.getRedusertBrukersAndelPrÅr());
            assertBeløp(kontraktAndel.getRedusertRefusjonPrÅr(), domeneAndel.getRedusertRefusjonPrÅr());
            assertBeløp(kontraktAndel.getAvkortetBrukersAndelPrÅr(), domeneAndel.getAvkortetBrukersAndelPrÅr());
            assertBeløp(kontraktAndel.getAvkortetRefusjonPrÅr(), domeneAndel.getAvkortetRefusjonPrÅr());
            assertThat(kontraktAndel.getDagsatsBruker()).isEqualTo(domeneAndel.getDagsatsBruker());
            assertThat(kontraktAndel.getDagsatsArbeidsgiver()).isEqualTo(domeneAndel.getDagsatsArbeidsgiver());
            assertArbeidsforhold(kontraktAndel.getBgAndelArbeidsforhold(), domeneAndel.getBgAndelArbeidsforhold());
        });

    }

    private void assertBeløp(Beløp beløpKontrakt, BigDecimal beløpDomene) {
        if (beløpKontrakt == null) {
            assertThat(beløpDomene).isNull();
        } else {
            assertThat(beløpKontrakt.verdi()).isEqualTo(beløpDomene);
        }
    }

    private void assertArbeidsforhold(BGAndelArbeidsforhold kontraktArbeidsforhold,
                                      Optional<no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold> domeneArbeidsforhold) {
        if (kontraktArbeidsforhold == null) {
            assertThat(domeneArbeidsforhold).isEmpty();
        } else {
            var domeneArb = domeneArbeidsforhold.orElseThrow();
            assertThat(kontraktArbeidsforhold.getArbeidsforholdRef()).isEqualTo(domeneArb.getArbeidsforholdRef().getUUIDReferanse());
            if (kontraktArbeidsforhold.getArbeidsgiver() != null) {
                assertThat(kontraktArbeidsforhold.getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo(domeneArb.getArbeidsgiver().getIdentifikator());
            }
            assertThat(kontraktArbeidsforhold.getArbeidsperiodeFom()).isEqualTo(domeneArb.getArbeidsperiodeFom());
            assertThat(kontraktArbeidsforhold.getArbeidsperiodeTom()).isEqualTo(domeneArb.getArbeidsperiodeTom().orElse(null));
        }
    }

    private BeregningsgrunnlagGrunnlagDto bgFraJson() {
        var json = """
            {
                 "beregningsgrunnlag": {
                     "skjæringstidspunkt": "2019-07-16",
                 "aktivitetStatuserMedHjemmel": [{
                    "aktivitetStatus": "AT",
                    "hjemmel": "F_14_7_8_30"
                }
                ],
                     "beregningsgrunnlagPerioder": [
                         {
                             "beregningsgrunnlagPrStatusOgAndelList": [
                                 {
                                     "andelsnr": 1,
                                     "aktivitetStatus": "AT",
                                     "beregningsperiode": {
                                         "fom": "2019-04-01",
                                         "tom": "2019-06-30"
                                     },
                                     "arbeidsforholdType": "ARBEID",
                                     "bruttoPrÅr": 540000.0,
                                     "redusertRefusjonPrÅr": 420000.0,
                                     "redusertBrukersAndelPrÅr": 120000.0,
                                     "dagsatsBruker": 462,
                                     "dagsatsArbeidsgiver": 1615,
                                     "inntektskategori": "ARBEIDSTAKER",
                                     "bgAndelArbeidsforhold": {
                                         "arbeidsgiver": {
                                             "arbeidsgiverOrgnr": "973861778"
                                         },
                                         "arbeidsforholdRef": "127b7791-8f38-4910-9424-0d764d7b2298",
                                         "refusjonskravPrÅr": 420000.0,
                                         "arbeidsperiodeFom": "2018-04-01",
                                         "arbeidsperiodeTom": "9999-12-31"
                                     },
                                     "avkortetPrÅr": 540000.0,
                                     "redusertPrÅr": 540000.0,
                                     "beregnetPrÅr": 540000.0,
                                     "maksimalRefusjonPrÅr": 420000.0,
                                     "avkortetRefusjonPrÅr": 420000.0,
                                     "avkortetBrukersAndelPrÅr": 120000.0,
                                     "årsbeløpFraTilstøtendeYtelse": 540000.0,
                                     "fastsattAvSaksbehandler": false,
                                     "lagtTilAvSaksbehandler": false
                                 }
                             ],
                             "periode": {
                                 "fom": "2019-07-16",
                                 "tom": "2019-09-30"
                             },
                             "bruttoPrÅr": 540000.0,
                             "avkortetPrÅr": 540000.0,
                             "redusertPrÅr": 540000.0,
                             "dagsats": 2077,
                             "periodeÅrsaker": []
                         },
                         {
                             "beregningsgrunnlagPrStatusOgAndelList": [
                                 {
                                     "andelsnr": 1,
                                     "aktivitetStatus": "AT",
                                     "beregningsperiode": {
                                         "fom": "2019-04-01",
                                         "tom": "2019-06-30"
                                     },
                                     "arbeidsforholdType": "ARBEID",
                                     "bruttoPrÅr": 236250.0,
                                     "redusertRefusjonPrÅr": 236250.0,
                                     "redusertBrukersAndelPrÅr": 0.0,
                                     "dagsatsBruker": 0,
                                     "dagsatsArbeidsgiver": 909,
                                     "inntektskategori": "ARBEIDSTAKER",
                                     "bgAndelArbeidsforhold": {
                                         "arbeidsgiver": {
                                             "arbeidsgiverOrgnr": "973861778"
                                         },
                                         "arbeidsforholdRef": "127b7791-8f38-4910-9424-0d764d7b2298",
                                         "refusjonskravPrÅr": 420000.0,
                                         "arbeidsperiodeFom": "2018-04-01",
                                         "arbeidsperiodeTom": "9999-12-31"
                                     },
                                     "avkortetPrÅr": 236250.0,
                                     "redusertPrÅr": 236250.0,
                                     "beregnetPrÅr": 540000.0,
                                     "fordeltPrÅr": 236250.0,
                                     "maksimalRefusjonPrÅr": 236250.0,
                                     "avkortetRefusjonPrÅr": 236250.0,
                                     "avkortetBrukersAndelPrÅr": 0.0,
                                     "årsbeløpFraTilstøtendeYtelse": 236250.0,
                                     "fastsattAvSaksbehandler": false,
                                     "lagtTilAvSaksbehandler": false
                                 },
                                 {
                                     "andelsnr": 2,
                                     "aktivitetStatus": "AT",
                                     "arbeidsforholdType": "ARBEID",
                                     "bruttoPrÅr": 303750.0,
                                     "redusertRefusjonPrÅr": 303750.0,
                                     "redusertBrukersAndelPrÅr": 0.0,
                                     "dagsatsBruker": 0,
                                     "dagsatsArbeidsgiver": 1168,
                                     "inntektskategori": "ARBEIDSTAKER",
                                     "bgAndelArbeidsforhold": {
                                         "arbeidsgiver": {
                                             "arbeidsgiverOrgnr": "974761076"
                                         },
                                         "arbeidsforholdRef": "dc3601cc-4676-4fc5-af86-35f825c91804",
                                         "refusjonskravPrÅr": 540000.0,
                                         "arbeidsperiodeFom": "2019-10-01",
                                         "arbeidsperiodeTom": "9999-12-31"
                                     },
                                     "avkortetPrÅr": 303750.0,
                                     "redusertPrÅr": 303750.0,
                                     "fordeltPrÅr": 303750.0,
                                     "maksimalRefusjonPrÅr": 303750.0,
                                     "avkortetRefusjonPrÅr": 303750.0,
                                     "avkortetBrukersAndelPrÅr": 0.0,
                                     "årsbeløpFraTilstøtendeYtelse": 303750.0,
                                     "fastsattAvSaksbehandler": false,
                                     "lagtTilAvSaksbehandler": false
                                 }
                             ],
                             "periode": {
                                 "fom": "2019-10-01",
                                 "tom": "9999-12-31"
                             },
                             "bruttoPrÅr": 540000.0,
                             "avkortetPrÅr": 540000.0,
                             "redusertPrÅr": 540000.0,
                             "dagsats": 2077,
                             "periodeÅrsaker": [
                                 "ENDRING_I_REFUSJONSKRAV"
                             ]
                         }
                     ],
                     "sammenligningsgrunnlagPrStatusListe": [
                         {
                             "sammenligningsperiode": {
                                 "fom": "2018-07-01",
                                 "tom": "2019-06-30"
                             },
                             "sammenligningsgrunnlagType": "SAMMENLIGNING_AT_FL",
                             "rapportertPrÅr": 536796.0,
                             "avvikPromilleNy": 5.9687479000
                         }
                     ],
                     "faktaOmBeregningTilfeller": [],
                     "overstyrt": false,
                     "grunnbeløp": 99858.0
                 },
                 "registerAktiviteter": {
                     "aktiviteter": [
                         {
                             "periode": {
                                 "fom": "2019-01-01",
                                 "tom": "2020-07-31"
                             },
                             "arbeidsgiver": {
                                 "arbeidsgiverOrgnr": "973861778"
                             },
                             "arbeidsforholdRef": {
                                 "abakusReferanse": "127b7791-8f38-4910-9424-0d764d7b2298"
                             },
                             "opptjeningAktivitetType": "ARBEID"
                         }
                     ],
                     "skjæringstidspunktOpptjening": "2019-07-16"
                 },
                 "beregningsgrunnlagTilstand": "FASTSATT"
             }
             """;
        return StandardJsonConfig.fromJson(json, BeregningsgrunnlagGrunnlagDto.class);
    }

}
