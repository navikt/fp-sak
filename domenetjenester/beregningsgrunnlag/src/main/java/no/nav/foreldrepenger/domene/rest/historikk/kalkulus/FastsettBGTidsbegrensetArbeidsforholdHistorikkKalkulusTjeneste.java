package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
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
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

@ApplicationScoped
public class FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste {

    private static final String FRILANSINNTEKT = "Frilansinntekt";

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
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
            fastatteAndeler.forEach(
                andel -> lagEndretHistorikkForAndelIPeriode(arbeidsforholdInntekterMap, bgPerioderSomSkalFastsettesAvDennePerioden, andel,
                    overstyringer));
        }

        var andelerIFørstePeriode = endredePerioder.stream()
            .min(Comparator.comparing(p -> p.getPeriode().getFom()))
            .map(BeregningsgrunnlagPeriodeEndring::getBeregningsgrunnlagPrStatusOgAndelEndringer)
            .orElse(List.of());
        var forrigeOverstyrtFrilansinntekt = andelerIFørstePeriode.stream()
            .filter(a -> a.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring)
            .map(BeløpEndring::fraBeløp)
            .orElse(null);

        lagHistorikkInnslag(dto, param, arbeidsforholdInntekterMap, forrigeOverstyrtFrilansinntekt);
    }

    private void lagEndretHistorikkForAndelIPeriode(Map<String, List<Integer>> arbeidsforholdInntekterMap,
                                                    List<BeregningsgrunnlagPeriodeEndring> bgPerioderSomSkalFastsettesAvDennePerioden,
                                                    FastsatteAndelerTidsbegrensetDto andel,
                                                    List<ArbeidsforholdOverstyring> overstyringer) {
        bgPerioderSomSkalFastsettesAvDennePerioden.forEach(p -> {
            var atAndeler = p.getBeregningsgrunnlagPrStatusOgAndelEndringer()
                .stream()
                .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
                .toList();
            atAndeler.forEach(endretAndel -> mapArbeidsforholdInntekter(andel, arbeidsforholdInntekterMap, endretAndel, overstyringer));
        });

    }

    private void mapArbeidsforholdInntekter(FastsatteAndelerTidsbegrensetDto arbeidsforhold,
                                            Map<String, List<Integer>> tilHistorikkInnslag,
                                            BeregningsgrunnlagPrStatusOgAndelEndring korrektAndel,
                                            List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(korrektAndel.getAktivitetStatus(),
            korrektAndel.getArbeidsgiver(), Optional.ofNullable(korrektAndel.getArbeidsforholdRef()), overstyringer);
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
        List<HistorikkinnslagLinjeBuilder> linjeBuilderList = new ArrayList<>(oppdaterVedEndretVerdi(tilHistorikkInnslag));
        linjeBuilderList.addAll(oppdaterFrilansInntektVedEndretVerdi(forrigeFrilansInntekt, dto.getFrilansInntekt()));
        linjeBuilderList.add(new HistorikkinnslagLinjeBuilder().tekst(dto.getBegrunnelse()));

        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getRef().fagsakId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .medLinjer(linjeBuilderList)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static List<HistorikkinnslagLinjeBuilder> oppdaterVedEndretVerdi(Map<String, List<Integer>> tilHistorikkInnslag) {
        List<HistorikkinnslagLinjeBuilder> linjeBuilderList = new ArrayList<>();
        for (var entry : tilHistorikkInnslag.entrySet()) {
            var linjeBuilder = new HistorikkinnslagLinjeBuilder();
            var arbeidsforholdInfo = entry.getKey();
            var inntekter = entry.getValue();
            linjeBuilderList.add(linjeBuilder.fraTil(String.format("Inntekt fra %s", arbeidsforholdInfo), null, formaterInntekter(inntekter)));
            linjeBuilderList.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjeBuilderList;
    }

    public static List<HistorikkinnslagLinjeBuilder> oppdaterFrilansInntektVedEndretVerdi(BigDecimal forrigeFrilansInntekt,
                                                                                          Integer nyFrilansInntekt) {
        List<HistorikkinnslagLinjeBuilder> linjeBuilderList = new ArrayList<>();
        HistorikkinnslagLinjeBuilder linjeBuilder = new HistorikkinnslagLinjeBuilder();
        if (forrigeFrilansInntekt != null && nyFrilansInntekt != null) {
            var fraInntekt = (int) Math.round(forrigeFrilansInntekt.doubleValue());
            if (Objects.equals(fraInntekt, nyFrilansInntekt)) {
                linjeBuilderList.add(linjeBuilder.til(FRILANSINNTEKT, nyFrilansInntekt));
            } else {
                linjeBuilderList.add(linjeBuilder.fraTil(FRILANSINNTEKT, fraInntekt, nyFrilansInntekt));
            }
            linjeBuilderList.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        } else if (nyFrilansInntekt != null) {
            linjeBuilderList.add(linjeBuilder.til(FRILANSINNTEKT, nyFrilansInntekt));
            linjeBuilderList.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjeBuilderList;
    }

    private static String formaterInntekter(List<Integer> inntekter) {
        if (inntekter.size() > 1) {
            var inntekterString = inntekter.stream().map(String::valueOf).collect(Collectors.joining(", "));
            return inntekterString.substring(0, inntekterString.lastIndexOf(',')) + " og " + inntekterString.substring(
                inntekterString.lastIndexOf(',') + 1);
        }
        return inntekter.getFirst().toString();
    }

}
