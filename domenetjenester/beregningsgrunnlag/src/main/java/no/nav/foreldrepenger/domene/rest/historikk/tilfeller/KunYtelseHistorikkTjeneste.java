package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
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
    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                                BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        var kunYtelseDto = dto.getKunYtelseFordeling();
        var periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().getFirst();
        var forrigePeriode = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().getFirst());
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        var andeler = kunYtelseDto.getAndeler();
        for (var andel : andeler) {
            if (andel.getNyAndel()) {
                tekstlinjerBuilder.addAll(leggTilHistorikkinnslagForNyAndel(andel));
            } else {
                var korrektAndel = getKorrektAndel(andel, periode, forrigePeriode);
                tekstlinjerBuilder.addAll(leggTilHistorikkinnslag(andel, korrektAndel, forrigePeriode, arbeidsforholdOverstyringer));
            }
        }
        return tekstlinjerBuilder;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> leggTilHistorikkinnslag(FastsattBrukersAndel andel,
                                                                            BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                                                            Optional<BeregningsgrunnlagPeriode> forrigePeriode,
                                                                            List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        Integer fastsattÅrsbeløp = andel.getFastsattBeløp() * MND_I_1_ÅR;
        var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(korrektAndel.getAktivitetStatus(),
            korrektAndel.getArbeidsgiver(), korrektAndel.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
        if (forrigePeriode.isPresent()) {
            var andelIForrige = finnAndelFraPeriode(forrigePeriode.get(), andel);
            var forrigeInntektskategori = andelIForrige.getInntektskategori();
            var forrigeBeløp = andelIForrige.getBeregnetPrÅr() == null ? null : andelIForrige.getBeregnetPrÅr().intValue();
            if (forrigeBeløp != null && !forrigeBeløp.equals(fastsattÅrsbeløp)) {
                tekstlinjerBuilder.addAll(lagHistorikkinnslagdelForFordeling(andelsInfo, forrigeInntektskategori, andel.getFastsattBeløp(),
                    andelIForrige.getBeregnetPrÅr().divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP).intValue()));
            }
            if (forrigeInntektskategori != null && !forrigeInntektskategori.equals(andel.getInntektskategori())) {
                tekstlinjerBuilder.add(lagHistorikkinnslagdelForInntektskategori(andelsInfo, forrigeInntektskategori, andel.getInntektskategori()));
            }
        } else {
            var forrigeBeløp = korrektAndel.getBeregnetPrÅr() == null ? null : korrektAndel.getBeregnetPrÅr()
                .divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_UP)
                .intValue();
            tekstlinjerBuilder.addAll(
                lagHistorikkinnslagdelForFordeling(andelsInfo, andel.getInntektskategori(), andel.getFastsattBeløp(), forrigeBeløp));
            tekstlinjerBuilder.add(lagHistorikkinnslagdelForInntektskategori(andelsInfo, null, andel.getInntektskategori()));
        }

        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());
        return tekstlinjerBuilder;
    }

    private HistorikkinnslagTekstlinjeBuilder lagHistorikkinnslagdelForInntektskategori(String andelsInfo,
                                                                                        Inntektskategori forrigeInntektskategori,
                                                                                        Inntektskategori inntektskategori) {
        if (inntektskategori != null && !inntektskategori.equals(forrigeInntektskategori)) {
            var forrigeInntektskategoriNavn = forrigeInntektskategori != null ? forrigeInntektskategori.getNavn() : null;
            return new HistorikkinnslagTekstlinjeBuilder().fraTil("Inntektskategori for " + andelsInfo, forrigeInntektskategoriNavn,
                inntektskategori.getNavn());
        }
        return null;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkinnslagdelForFordeling(String andel,
                                                                                       Inntektskategori inntektskategori,
                                                                                       Integer fastsattBeløp,
                                                                                       Integer forrigeBeløp) {
        var fastsattÅrsbeløp = fastsattBeløp == null ? null : fastsattBeløp * MND_I_1_ÅR;
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        if (fastsattÅrsbeløp != null && !fastsattÅrsbeløp.equals(forrigeBeløp)) {
            tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().tekst("Fordeling for").bold(andel + ":"));
            tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().fraTil(inntektskategori.getNavn(), forrigeBeløp, fastsattBeløp));
        }
        return tekstlinjerBuilder;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> leggTilHistorikkinnslagForNyAndel(FastsattBrukersAndel andel) {
        var inntektskategori = andel.getInntektskategori().getNavn();
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().bold("Det er lagt til ny aktivitet:"));
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().bold(BeregningsgrunnlagAndeltype.BRUKERS_ANDEL.getNavn()));
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().fraTil(inntektskategori, null, andel.getFastsattBeløp()));
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());

        return tekstlinjerBuilder;
    }

    private BeregningsgrunnlagPrStatusOgAndel getKorrektAndel(FastsattBrukersAndel andel,
                                                              BeregningsgrunnlagPeriode periode,
                                                              Optional<BeregningsgrunnlagPeriode> forrigePeriodeOpt) {
        if (andel.getLagtTilAvSaksbehandler() && !andel.getNyAndel()) {
            var forrigePeriode = forrigePeriodeOpt.orElseThrow(
                () -> new IllegalStateException("Skal ha bereninsgrunnlag fra KOFAKBER_UT om man har lagt til en andel tidligere"));
            return finnAndelFraPeriode(forrigePeriode, andel);
        }
        return finnAndelFraPeriode(periode, andel);
    }

    private BeregningsgrunnlagPrStatusOgAndel finnAndelFraPeriode(BeregningsgrunnlagPeriode periode, FastsattBrukersAndel andel) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke andel med andelsnr " + andel.getAndelsnr()));
    }

}
