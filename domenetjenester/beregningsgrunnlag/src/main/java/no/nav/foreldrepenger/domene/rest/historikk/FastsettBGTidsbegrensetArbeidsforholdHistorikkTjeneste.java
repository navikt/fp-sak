package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste.oppdaterFrilansInntektVedEndretVerdi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattePerioderTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;

@ApplicationScoped
public class FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste {

    private final static Logger LOG = LoggerFactory.getLogger(FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste.class);

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                  HistorikkinnslagRepository historikkinnslagRepository) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             BeregningsgrunnlagEntitet aktivtGrunnlag,
                             Optional<BeregningsgrunnlagEntitet> gammeltGrunnlag,
                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        Map<String, List<Integer>> arbeidsforholdInntekterMap = new HashMap<>();

        var perioder = aktivtGrunnlag.getBeregningsgrunnlagPerioder();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        for (var periode : dto.getFastsatteTidsbegrensedePerioder()) {
            var bgPerioderSomSkalFastsettesAvDennePerioden = perioder.stream()
                .filter(p -> !p.getBeregningsgrunnlagPeriodeFom().isBefore(periode.getPeriodeFom()))
                .toList();
            var fastatteAndeler = periode.getFastsatteTidsbegrensedeAndeler();
            fastatteAndeler.forEach(
                andel -> lagHistorikkForAndelIPeriode(arbeidsforholdInntekterMap, periode, bgPerioderSomSkalFastsettesAvDennePerioden, andel,
                    overstyringer));
        }
        var forrigeOverstyrtFrilansinntekt = gammeltGrunnlag.flatMap(bg -> finnFrilansAndel(hentFørstePeriode(bg)))
            .map(BeregningsgrunnlagPrStatusOgAndel::getOverstyrtPrÅr)
            .orElse(null);

        lagHistorikkInnslag(dto, param, arbeidsforholdInntekterMap, forrigeOverstyrtFrilansinntekt);
    }

    private BeregningsgrunnlagPeriode hentFørstePeriode(BeregningsgrunnlagEntitet bg) {
        return bg.getBeregningsgrunnlagPerioder().getFirst();
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
            var korrektAndel = p.getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
                .findFirst();
            if (korrektAndel.isPresent() && skalLageHistorikkinnslag(korrektAndel.get(), periode)) {
                mapArbeidsforholdInntekter(andel, arbeidsforholdInntekterMap, korrektAndel.get(), overstyringer);
            }
        });
    }

    private boolean skalLageHistorikkinnslag(BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                             FastsattePerioderTidsbegrensetDto fastsattArbeidsforhold) {
        // Lager kun historikkinnslag dersom perioden ble eksplisitt fastsatt av saksbehandler.
        // Perioder som "arver" verdier fra foregående periode får ikke historikkinnslag
        var bgPeriode = korrektAndel.getBeregningsgrunnlagPeriode();
        return bgPeriode.getBeregningsgrunnlagPeriodeFom().equals(fastsattArbeidsforhold.getPeriodeFom());
    }

    private void mapArbeidsforholdInntekter(FastsatteAndelerTidsbegrensetDto arbeidsforhold,
                                            Map<String, List<Integer>> tilHistorikkInnslag,
                                            BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                            List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(korrektAndel.getAktivitetStatus(),
            korrektAndel.getArbeidsgiver(), korrektAndel.getArbeidsforholdRef(), overstyringer);
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
            var hva = String.format("Inntekt fra %s", arbeidsforholdInfo);
            linjeBuilderList.add(linjeBuilder.fraTil(hva, null, formaterInntekter(inntekter)));
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
