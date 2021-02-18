package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattePerioderTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                                  ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param, BeregningsgrunnlagEntitet aktivtGrunnlag,
                             Optional<BeregningsgrunnlagEntitet> gammeltGrunnlag, FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        Map<String, List<Integer>> arbeidsforholdInntekterMap = new HashMap<>();

        List<BeregningsgrunnlagPeriode> perioder = aktivtGrunnlag.getBeregningsgrunnlagPerioder();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        List<ArbeidsforholdOverstyring> overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        for (FastsattePerioderTidsbegrensetDto periode: dto.getFastsatteTidsbegrensedePerioder()) {
            List<BeregningsgrunnlagPeriode> bgPerioderSomSkalFastsettesAvDennePerioden = perioder
                .stream()
                .filter(p -> !p.getBeregningsgrunnlagPeriodeFom().isBefore(periode.getPeriodeFom()))
                .collect(Collectors.toList());
            List<FastsatteAndelerTidsbegrensetDto> fastatteAndeler = periode.getFastsatteTidsbegrensedeAndeler();
            fastatteAndeler.forEach(andel ->
                lagHistorikkForAndelIPeriode(arbeidsforholdInntekterMap, periode, bgPerioderSomSkalFastsettesAvDennePerioden, andel, overstyringer));
        }
        BigDecimal forrigeOverstyrtFrilansinntekt = gammeltGrunnlag.flatMap(bg -> finnFrilansAndel(hentFørstePeriode(bg)))
            .map(BeregningsgrunnlagPrStatusOgAndel::getOverstyrtPrÅr).orElse(null);

        lagHistorikkInnslag(dto, param, arbeidsforholdInntekterMap, forrigeOverstyrtFrilansinntekt);
    }

    private BeregningsgrunnlagPeriode hentFørstePeriode(BeregningsgrunnlagEntitet bg) {
        return bg.getBeregningsgrunnlagPerioder().get(0);
    }


    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnFrilansAndel(BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .findFirst();
    }

    private void lagHistorikkForAndelIPeriode(Map<String, List<Integer>> arbeidsforholdInntekterMap,
                                              FastsattePerioderTidsbegrensetDto periode,
                                              List<BeregningsgrunnlagPeriode> bgPerioderSomSkalFastsettesAvDennePerioden,
                                              FastsatteAndelerTidsbegrensetDto andel,
                                              List<ArbeidsforholdOverstyring> overstyringer) {
        bgPerioderSomSkalFastsettesAvDennePerioden.forEach(p -> {
            Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndel = p.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andel.getAndelsnr())).findFirst();
            if (korrektAndel.isPresent() && skalLageHistorikkinnslag(korrektAndel.get(), periode)) {
                mapArbeidsforholdInntekter(andel, arbeidsforholdInntekterMap, korrektAndel.get(), overstyringer);
            }
        });
    }

    private boolean skalLageHistorikkinnslag(BeregningsgrunnlagPrStatusOgAndel korrektAndel, FastsattePerioderTidsbegrensetDto fastsattArbeidsforhold) {
        // Lager kun historikkinnslag dersom perioden ble eksplisitt fastsatt av saksbehandler.
        // Perioder som "arver" verdier fra foregående periode får ikke historikkinnslag
        BeregningsgrunnlagPeriode bgPeriode = korrektAndel.getBeregningsgrunnlagPeriode();
        return bgPeriode.getBeregningsgrunnlagPeriodeFom().equals(fastsattArbeidsforhold.getPeriodeFom());
    }

    private void mapArbeidsforholdInntekter(FastsatteAndelerTidsbegrensetDto arbeidsforhold, Map<String, List<Integer>> tilHistorikkInnslag,
                                            BeregningsgrunnlagPrStatusOgAndel korrektAndel, List<ArbeidsforholdOverstyring> overstyringer) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(korrektAndel.getAktivitetStatus(), korrektAndel.getArbeidsgiver(),
            korrektAndel.getArbeidsforholdRef(), overstyringer);
        if (tilHistorikkInnslag.containsKey(arbeidsforholdInfo)) {
            List<Integer> inntekter = tilHistorikkInnslag.get(arbeidsforholdInfo);
            inntekter.add(arbeidsforhold.getBruttoFastsattInntekt());
            tilHistorikkInnslag.put(arbeidsforholdInfo, inntekter);
        } else {
            List<Integer> inntekter = new ArrayList<>();
            inntekter.add(arbeidsforhold.getBruttoFastsattInntekt());
            tilHistorikkInnslag.put(arbeidsforholdInfo, inntekter);
        }
    }

    private void lagHistorikkInnslag(FastsettBGTidsbegrensetArbeidsforholdDto dto, AksjonspunktOppdaterParameter param, Map<String, List<Integer>> tilHistorikkInnslag, BigDecimal forrigeFrilansInntekt) {
        oppdaterVedEndretVerdi(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, tilHistorikkInnslag);
        oppdaterFrilansInntektVedEndretVerdi(HistorikkEndretFeltType.FRILANS_INNTEKT, forrigeFrilansInntekt, dto);

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, Map<String, List<Integer>> tilHistorikkInnslag) {
        for (Map.Entry<String, List<Integer>> entry : tilHistorikkInnslag.entrySet()) {
            String arbeidsforholdInfo = entry.getKey();
            List<Integer> inntekter = entry.getValue();
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, arbeidsforholdInfo, null, formaterInntekter(inntekter));
        }
    }

    private void oppdaterFrilansInntektVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, BigDecimal forrigeFrilansInntekt, FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        if (forrigeFrilansInntekt != null && dto.getFrilansInntekt() != null) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, forrigeFrilansInntekt, dto.getFrilansInntekt());
        } else if (dto.getFrilansInntekt() != null){
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, null, dto.getFrilansInntekt());
        }
    }

    private String formaterInntekter(List<Integer> inntekter) {
        if(inntekter.size() > 1) {
            String inntekterString = inntekter.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            return inntekterString.substring(0, inntekterString.lastIndexOf(',')) + " og " + inntekterString.substring(inntekterString.lastIndexOf(',') + 1);
        }
        return inntekter.get(0).toString();
    }
}
