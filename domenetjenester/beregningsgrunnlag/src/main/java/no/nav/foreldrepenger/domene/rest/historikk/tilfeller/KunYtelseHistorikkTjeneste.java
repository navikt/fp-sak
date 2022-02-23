package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAndeltype;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
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
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var kunYtelseDto = dto.getKunYtelseFordeling();
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        var andeler = kunYtelseDto.getAndeler();
        for (var andel : andeler) {
            if (andel.getNyAndel()) {
                leggTilHistorikkinnslagForNyAndel(andel, tekstBuilder);
            } else {
                var endring = oppdaterResultat.getBeregningsgrunnlagEndring()
                    .stream()
                    .flatMap(beregningsgrunnlagEndring -> beregningsgrunnlagEndring.getAndelerFørstePeriode().stream())
                    .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
                    .findFirst();
                endring.ifPresent(e -> leggTilHistorikkinnslag(e, tekstBuilder, arbeidsforholdOverstyringer));
            }
        }
    }

    private void leggTilHistorikkinnslag(BeregningsgrunnlagPrStatusOgAndelEndring e,
                                         HistorikkInnslagTekstBuilder tekstBuilder,
                                         List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(e.getAktivitetStatus(),
            e.getArbeidsgiver(), Optional.ofNullable(e.getArbeidsforholdRef()), arbeidsforholdOverstyringer);
        var forrigeInntektskategori = e.getInntektskategoriEndring().map(InntektskategoriEndring::getFraVerdi).orElse(Inntektskategori.UDEFINERT);
        if (e.getInntektEndring().map(BeløpEndring::erEndret).orElse(false)) {
            lagHistorikkinnslagdelForFordeling(andelsInfo, forrigeInntektskategori,
                e.getInntektEndring().map(BeløpEndring::getTilMånedsbeløp).map(BigDecimal::intValue).orElse(null),
                e.getInntektEndring().map(BeløpEndring::getFraMånedsbeløp).map(BigDecimal::intValue).orElse(null), tekstBuilder);
        }
        if (e.getInntektskategoriEndring().map(InntektskategoriEndring::erEndret).orElse(false)) {
            lagHistorikkinnslagdelForInntektskategori(andelsInfo,
                e.getInntektskategoriEndring().map(InntektskategoriEndring::getTilVerdi).orElse(null), forrigeInntektskategori, tekstBuilder);
        }
    }

    private void lagHistorikkinnslagdelForInntektskategori(String andelsInfo,
                                                           Inntektskategori inntektskategori,
                                                           Inntektskategori forrigeInntektskategori,
                                                           HistorikkInnslagTekstBuilder tekstBuilder) {
        if (inntektskategori != null && !inntektskategori.equals(forrigeInntektskategori)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI_FOR_ANDEL, andelsInfo, forrigeInntektskategori, inntektskategori);
        }
    }

    private void lagHistorikkinnslagdelForFordeling(String andel,
                                                    Inntektskategori inntektskategori,
                                                    Integer fastsattBeløp,
                                                    Integer forrigeBeløp,
                                                    HistorikkInnslagTekstBuilder tekstBuilder) {
        var fastsattÅrsbeløp = fastsattBeløp == null ? null : fastsattBeløp * MND_I_1_ÅR;
        if (fastsattÅrsbeløp != null && !fastsattÅrsbeløp.equals(forrigeBeløp)) {
            tekstBuilder.medTema(HistorikkEndretFeltType.FORDELING_FOR_ANDEL, andel)
                .medEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL, inntektskategori.getNavn(), forrigeBeløp, fastsattBeløp);
        }
    }

    private void leggTilHistorikkinnslagForNyAndel(FastsattBrukersAndel andel, HistorikkInnslagTekstBuilder tekstBuilder) {
        lagHistorikkinnslagdelForNyAndel(BeregningsgrunnlagAndeltype.BRUKERS_ANDEL.getNavn(), andel.getInntektskategori().getNavn(),
            andel.getFastsattBeløp(), tekstBuilder);
    }

    private void lagHistorikkinnslagdelForNyAndel(String andel,
                                                  String inntektskategori,
                                                  Integer fastsattBeløp,
                                                  HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.medTema(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL, andel)
            .medEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL, inntektskategori, null, fastsattBeløp);
    }

}
