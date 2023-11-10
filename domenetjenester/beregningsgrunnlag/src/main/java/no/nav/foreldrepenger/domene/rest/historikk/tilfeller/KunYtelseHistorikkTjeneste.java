package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagAndeltype;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE)
public class KunYtelseHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private static final int MND_I_1_ÅR = 12;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public KunYtelseHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public KunYtelseHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var kunYtelseDto = dto.getKunYtelseFordeling();
        var periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var forrigePeriode = forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0));
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        var andeler = kunYtelseDto.getAndeler();
        for (var andel : andeler) {
            if (andel.getNyAndel()) {
                leggTilHistorikkinnslagForNyAndel(andel, tekstBuilder);
            } else {
                var korrektAndel = getKorrektAndel(andel, periode, forrigePeriode);
                leggTilHistorikkinnslag(andel, korrektAndel, tekstBuilder, forrigePeriode, arbeidsforholdOverstyringer);
            }
        }
    }

    private void leggTilHistorikkinnslag(FastsattBrukersAndel andel,
                                         BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                         HistorikkInnslagTekstBuilder tekstBuilder,
                                         Optional<BeregningsgrunnlagPeriode> forrigePeriode,
                                         List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Integer fastsattÅrsbeløp = andel.getFastsattBeløp() * MND_I_1_ÅR;
        var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            korrektAndel.getAktivitetStatus(),
            korrektAndel.getArbeidsgiver(),
            korrektAndel.getArbeidsforholdRef(),
            arbeidsforholdOverstyringer);
        if (forrigePeriode.isPresent()) {
            var andelIForrige = finnAndelFraPeriode(forrigePeriode.get(), andel);
            var forrigeInntektskategori = andelIForrige.getInntektskategori();
            var forrigeBeløp = andelIForrige.getBeregnetPrÅr() == null ? null : andelIForrige.getBeregnetPrÅr().intValue();
            if (forrigeBeløp != null && !forrigeBeløp.equals(fastsattÅrsbeløp)) {
                lagHistorikkinnslagdelForFordeling(andelsInfo, forrigeInntektskategori, andel.getFastsattBeløp(), andelIForrige.getBeregnetPrÅr().divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP).intValue(),
                    tekstBuilder);
            }
            if (forrigeInntektskategori != null && !forrigeInntektskategori.equals(andel.getInntektskategori())) {
                lagHistorikkinnslagdelForInntektskategori(andelsInfo, andel.getInntektskategori(), forrigeInntektskategori, tekstBuilder);
            }
        } else {
            var forrigeBeløp = korrektAndel.getBeregnetPrÅr() == null ? null : korrektAndel.getBeregnetPrÅr().divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP).intValue();
            lagHistorikkinnslagdelForFordeling(andelsInfo, andel.getInntektskategori(), andel.getFastsattBeløp(), forrigeBeløp, tekstBuilder);
            lagHistorikkinnslagdelForInntektskategori(andelsInfo, andel.getInntektskategori(), null, tekstBuilder);
        }
    }

    private void lagHistorikkinnslagdelForInntektskategori(String andelsInfo, Inntektskategori inntektskategori, Inntektskategori forrigeInntektskategori,
                                                           HistorikkInnslagTekstBuilder tekstBuilder) {
        if (inntektskategori != null && !inntektskategori.equals(forrigeInntektskategori)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI_FOR_ANDEL, andelsInfo, forrigeInntektskategori, inntektskategori);
        }
    }

    private void lagHistorikkinnslagdelForFordeling(String andel, Inntektskategori inntektskategori, Integer fastsattBeløp,
                                                    Integer forrigeBeløp, HistorikkInnslagTekstBuilder tekstBuilder) {
        var fastsattÅrsbeløp = fastsattBeløp == null ? null : fastsattBeløp * MND_I_1_ÅR;
        if (fastsattÅrsbeløp != null && !fastsattÅrsbeløp.equals(forrigeBeløp)){
            tekstBuilder
                .medTema(HistorikkEndretFeltType.FORDELING_FOR_ANDEL, andel)
                .medEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL, inntektskategori.getNavn(), forrigeBeløp, fastsattBeløp);
        }
    }

    private void leggTilHistorikkinnslagForNyAndel(FastsattBrukersAndel andel, HistorikkInnslagTekstBuilder tekstBuilder) {
        lagHistorikkinnslagdelForNyAndel(BeregningsgrunnlagAndeltype.BRUKERS_ANDEL.getNavn(), andel.getInntektskategori().getNavn(), andel.getFastsattBeløp(), tekstBuilder);
    }

    private void lagHistorikkinnslagdelForNyAndel(String andel, String inntektskategori, Integer fastsattBeløp, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder
            .medTema(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL, andel)
            .medEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL, inntektskategori, null, fastsattBeløp);
    }

    private BeregningsgrunnlagPrStatusOgAndel getKorrektAndel(FastsattBrukersAndel andel, BeregningsgrunnlagPeriode periode, Optional<BeregningsgrunnlagPeriode> forrigePeriodeOpt) {
        if (andel.getLagtTilAvSaksbehandler() && !andel.getNyAndel()) {
            var forrigePeriode = forrigePeriodeOpt
                .orElseThrow(() -> new IllegalStateException("Skal ha bereninsgrunnlag fra KOFAKBER_UT om man har lagt til en andel tidligere"));
            return finnAndelFraPeriode(forrigePeriode, andel);
        }
        return finnAndelFraPeriode(periode, andel);
    }

    private BeregningsgrunnlagPrStatusOgAndel finnAndelFraPeriode(BeregningsgrunnlagPeriode periode, FastsattBrukersAndel andel) {
        return periode
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke andel med andelsnr " + andel.getAndelsnr()));
    }

}
