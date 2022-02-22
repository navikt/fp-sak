package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErMottattYtelseEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_MOTTAR_YTELSE")
public class MottarYtelseHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public MottarYtelseHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public MottarYtelseHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var frilanserErMottattYtelseEndring = oppdaterResultat.getFaktaOmBeregningVurderinger()
            .stream()
            .flatMap(e -> e.getErMottattYtelseEndringer().stream())
            .filter(e -> e.getAktivitetStatus().erFrilanser())
            .findFirst();
        frilanserErMottattYtelseEndring.ifPresent(erMottattYtelseEndring -> lagHistorikkinnslagForFrilans(erMottattYtelseEndring, tekstBuilder));
        var arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .stream()
            .flatMap(e -> e.getErMottattYtelseEndringer().stream())
            .filter(e -> e.getAktivitetStatus().erArbeidstaker())
            .forEach(e -> lagHistorikkinnslagForArbeidstakerUtenIM(e, tekstBuilder, arbeidsforholdOverstyringer));
    }

    private void lagHistorikkinnslagForArbeidstakerUtenIM(ErMottattYtelseEndring erMottattYtelseEndring,
                                                          HistorikkInnslagTekstBuilder tekstBuilder,
                                                          List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var verdier = erMottattYtelseEndring.getErMottattYtelseEndring();
        if (verdier.erEndring()) {
            var andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                erMottattYtelseEndring.getAktivitetStatus(), Optional.ofNullable(erMottattYtelseEndring.getArbeidsgiver()),
                Optional.ofNullable(erMottattYtelseEndring.getArbeidsforholdRef()), arbeidsforholdOverstyringer);
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_ARBEID, andelsInfo, verdier.getFraVerdi(), verdier.getTilVerdi());
        }
    }

    private void lagHistorikkinnslagForFrilans(ErMottattYtelseEndring erMottattYtelseEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        var endring = erMottattYtelseEndring.getErMottattYtelseEndring();
        if (endring.erEndring()) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_FRILANS, endring.getFraVerdi(), endring.getTilVerdi());
        }
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(BeregningsgrunnlagPrStatusOgAndel andelINyttBg,
                                                                               Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.gjelderSammeArbeidsforhold(andelINyttBg))
            .findFirst()
            .stream()
            .flatMap(andel -> andel.mottarYtelse().stream())
            .findFirst();
    }

}
