package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAndeltype;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_BG_KUN_YTELSE")
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
        FastsettBgKunYtelseDto kunYtelseDto = dto.getKunYtelseFordeling();
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        Optional<BeregningsgrunnlagPeriode> forrigePeriode = forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0));
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        List<FastsattBrukersAndel> andeler = kunYtelseDto.getAndeler();
        for (FastsattBrukersAndel andel : andeler) {
            if (andel.getNyAndel()) {
                leggTilHistorikkinnslagForNyAndel(andel, tekstBuilder);
            } else {
                BeregningsgrunnlagPrStatusOgAndel korrektAndel = getKorrektAndel(andel, periode, forrigePeriode);
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
        String andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            korrektAndel.getAktivitetStatus(),
            korrektAndel.getArbeidsgiver(),
            korrektAndel.getArbeidsforholdRef(),
            arbeidsforholdOverstyringer);
        if (forrigePeriode.isPresent()) {
            BeregningsgrunnlagPrStatusOgAndel andelIForrige = finnAndelFraPeriode(forrigePeriode.get(), andel);
            Inntektskategori forrigeInntektskategori = andelIForrige.getInntektskategori();
            Integer forrigeBeløp = andelIForrige.getBeregnetPrÅr() == null ? null : andelIForrige.getBeregnetPrÅr().intValue();
            if (forrigeBeløp != null && !forrigeBeløp.equals(fastsattÅrsbeløp)) {
                lagHistorikkinnslagdelForFordeling(andelsInfo, forrigeInntektskategori, andel.getFastsattBeløp(), andelIForrige.getBeregnetPrÅr().divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP).intValue(),
                    tekstBuilder);
            }
            if (forrigeInntektskategori != null && !forrigeInntektskategori.equals(andel.getInntektskategori())) {
                lagHistorikkinnslagdelForInntektskategori(andelsInfo, andel.getInntektskategori(), forrigeInntektskategori, tekstBuilder);
            }
        } else {
            Integer forrigeBeløp = korrektAndel.getBeregnetPrÅr() == null ? null : korrektAndel.getBeregnetPrÅr().divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP).intValue();
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
        Integer fastsattÅrsbeløp = fastsattBeløp == null ? null : fastsattBeløp * MND_I_1_ÅR;
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
            BeregningsgrunnlagPeriode forrigePeriode = forrigePeriodeOpt
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
