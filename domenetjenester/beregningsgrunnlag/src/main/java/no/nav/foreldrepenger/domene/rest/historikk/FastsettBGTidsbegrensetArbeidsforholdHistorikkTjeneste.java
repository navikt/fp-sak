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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
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
                                                                  ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             BeregningsgrunnlagEndring beregningsgrunnlagEndring,
                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        Map<String, List<Integer>> arbeidsforholdInntekterMap = new HashMap<>();


        var perioder = beregningsgrunnlagEndring.getBeregningsgrunnlagPeriodeEndringer();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        for (var periode : dto.getFastsatteTidsbegrensedePerioder()) {
            var bgPeriodeSomSkalFastsettesAvDennePerioden = perioder.stream()
                .filter(p -> p.getPeriode().getFomDato().equals(periode.getPeriodeFom()))
                .findFirst()
                .orElseThrow();
            var fastatteAndeler = periode.getFastsatteTidsbegrensedeAndeler();
            fastatteAndeler.forEach(
                andel -> lagHistorikkForAndelIPeriode(arbeidsforholdInntekterMap, bgPeriodeSomSkalFastsettesAvDennePerioden, andel, overstyringer));
        }
        var forrigeOverstyrtFrilansinntekt = perioder.get(0)
            .getBeregningsgrunnlagPrStatusOgAndelEndringer()
            .stream()
            .filter(a -> a.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring)
            .flatMap(BeløpEndring::getFraBeløp)
            .orElse(null);

        lagHistorikkInnslag(dto, param, arbeidsforholdInntekterMap, forrigeOverstyrtFrilansinntekt);
    }

    private void lagHistorikkForAndelIPeriode(Map<String, List<Integer>> arbeidsforholdInntekterMap,
                                              BeregningsgrunnlagPeriodeEndring periodeEndring,
                                              FastsatteAndelerTidsbegrensetDto andel,
                                              List<ArbeidsforholdOverstyring> overstyringer) {
        var andelEndring = periodeEndring.getBeregningsgrunnlagPrStatusOgAndelEndringer()
            .stream()
            .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
            .findFirst();
        andelEndring.ifPresent(beregningsgrunnlagPrStatusOgAndelEndring -> mapArbeidsforholdInntekter(andel, arbeidsforholdInntekterMap,
            beregningsgrunnlagPrStatusOgAndelEndring, overstyringer));
    }

    private void mapArbeidsforholdInntekter(FastsatteAndelerTidsbegrensetDto arbeidsforhold,
                                            Map<String, List<Integer>> tilHistorikkInnslag,
                                            BeregningsgrunnlagPrStatusOgAndelEndring andelEndring,
                                            List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andelEndring.getAktivitetStatus(),
            andelEndring.getArbeidsgiver(), Optional.ofNullable(andelEndring.getArbeidsforholdRef()), overstyringer);
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

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, Map<String, List<Integer>> tilHistorikkInnslag) {
        for (var entry : tilHistorikkInnslag.entrySet()) {
            var arbeidsforholdInfo = entry.getKey();
            var inntekter = entry.getValue();
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, arbeidsforholdInfo, null, formaterInntekter(inntekter));
        }
    }

    private void oppdaterFrilansInntektVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                                      BigDecimal forrigeFrilansInntekt,
                                                      FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        if (forrigeFrilansInntekt != null && dto.getFrilansInntekt() != null) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, forrigeFrilansInntekt, dto.getFrilansInntekt());
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
