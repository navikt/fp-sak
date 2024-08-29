package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattePerioderTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                                          ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             OppdaterBeregningsgrunnlagResultat endringsaggregat,
                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        Map<String, List<Integer>> arbeidsforholdInntekterMap = new HashMap<>();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        var endredePerioder = endringsaggregat.getBeregningsgrunnlagEndring()
            .map(BeregningsgrunnlagEndring::getBeregningsgrunnlagPeriodeEndringer)
            .orElse(Collections.emptyList());
        for (var periode : dto.getFastsatteTidsbegrensedePerioder()) {
            var bgPerioderSomSkalFastsettesAvDennePerioden = endredePerioder.stream()
                .filter(p -> !p.getPeriode().getFom().isBefore(periode.getPeriodeFom()))
                .toList();
            var fastatteAndeler = periode.getFastsatteTidsbegrensedeAndeler();
            fastatteAndeler.forEach(andel -> lagEndretHistorikkForAndelIPeriode(arbeidsforholdInntekterMap, periode,
                bgPerioderSomSkalFastsettesAvDennePerioden, andel, overstyringer));
        }

        var andelerIFørstePeriode = endredePerioder.stream()
            .min(Comparator.comparing(p -> p.getPeriode().getFom()))
            .map(BeregningsgrunnlagPeriodeEndring::getBeregningsgrunnlagPrStatusOgAndelEndringer)
            .orElse(List.of());
        var forrigeOverstyrtFrilansinntekt = andelerIFørstePeriode.stream()
            .filter(a -> a.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring)
            .flatMap(BeløpEndring::getFraBeløp)
            .orElse(null);

        lagHistorikkInnslag(dto, param, arbeidsforholdInntekterMap, forrigeOverstyrtFrilansinntekt);
    }

    private void lagEndretHistorikkForAndelIPeriode(Map<String, List<Integer>> arbeidsforholdInntekterMap,
                                                    FastsattePerioderTidsbegrensetDto periode,
                                                    List<BeregningsgrunnlagPeriodeEndring> bgPerioderSomSkalFastsettesAvDennePerioden,
                                                    FastsatteAndelerTidsbegrensetDto andel,
                                                    List<ArbeidsforholdOverstyring> overstyringer) {
        bgPerioderSomSkalFastsettesAvDennePerioden.forEach(p -> {
            var atAndeler = p.getBeregningsgrunnlagPrStatusOgAndelEndringer()
                .stream()
                .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
                .toList();
            atAndeler.forEach(endretAndel -> {
                mapArbeidsforholdInntekter(andel, arbeidsforholdInntekterMap, endretAndel, overstyringer);

            });
        });

    }

    private void mapArbeidsforholdInntekter(FastsatteAndelerTidsbegrensetDto arbeidsforhold,
                                            Map<String, List<Integer>> tilHistorikkInnslag,
                                            BeregningsgrunnlagPrStatusOgAndelEndring korrektAndel,
                                            List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            korrektAndel.getAktivitetStatus(), korrektAndel.getArbeidsgiver(), Optional.ofNullable(korrektAndel.getArbeidsforholdRef()),
            overstyringer);
        if (tilHistorikkInnslag.containsKey(arbeidsforholdInfo)) {
            var inntekter = tilHistorikkInnslag.get(arbeidsforholdInfo);
            inntekter.add(arbeidsforhold.getBruttoFastsattInntekt());
            tilHistorikkInnslag.put(arbeidsforholdInfo, inntekter);
        } else {
            List<Integer> inntekter = new ArrayList<>();
            inntekter.add(arbeidsforhold.getBruttoFastsattInntekt());
            tilHistorikkInnslag.put(arbeidsforholdInfo, inntekter);
        }
    }

    private void lagHistorikkInnslag(FastsettBGTidsbegrensetArbeidsforholdDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     Map<String, List<Integer>> tilHistorikkInnslag,
                                     BigDecimal forrigeFrilansInntekt) {
        oppdaterVedEndretVerdi(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, tilHistorikkInnslag);
        oppdaterFrilansInntektVedEndretVerdi(HistorikkEndretFeltType.FRILANS_INNTEKT, forrigeFrilansInntekt, dto);

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                        Map<String, List<Integer>> tilHistorikkInnslag) {
        for (var entry : tilHistorikkInnslag.entrySet()) {
            var arbeidsforholdInfo = entry.getKey();
            var inntekter = entry.getValue();
            historikkAdapter.tekstBuilder()
                .medEndretFelt(historikkEndretFeltType, arbeidsforholdInfo, null, formaterInntekter(inntekter));
        }
    }

    private void oppdaterFrilansInntektVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                                      BigDecimal forrigeFrilansInntekt,
                                                      FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        if (forrigeFrilansInntekt != null && dto.getFrilansInntekt() != null) {
            historikkAdapter.tekstBuilder()
                .medEndretFelt(historikkEndretFeltType, forrigeFrilansInntekt, dto.getFrilansInntekt());
        } else if (dto.getFrilansInntekt() != null) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, null, dto.getFrilansInntekt());
        }
    }

    private String formaterInntekter(List<Integer> inntekter) {
        if (inntekter.size() > 1) {
            var inntekterString = inntekter.stream().map(String::valueOf).collect(Collectors.joining(", "));
            return inntekterString.substring(0, inntekterString.lastIndexOf(',')) + " og " + inntekterString.substring(
                inntekterString.lastIndexOf(',') + 1);
        }
        return inntekter.get(0).toString();
    }

}
