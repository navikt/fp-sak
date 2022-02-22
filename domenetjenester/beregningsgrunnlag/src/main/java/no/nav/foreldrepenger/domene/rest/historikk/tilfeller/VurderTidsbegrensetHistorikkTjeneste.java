package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD")
public class VurderTidsbegrensetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    VurderTidsbegrensetHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderTidsbegrensetHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .stream()
            .flatMap(fv -> fv.getErTidsbegrensetArbeidsforholdEndringer().stream())
            .forEach(e -> lagHistorikkInnslag(e, tekstBuilder, iayGrunnlag.getArbeidsforholdOverstyringer()));
    }

    private void lagHistorikkInnslag(ErTidsbegrensetArbeidsforholdEndring erTidsbegrensetArbeidsforholdEndring,
                                     HistorikkInnslagTekstBuilder tekstBuilder,
                                     List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        oppdaterVedEndretVerdi(erTidsbegrensetArbeidsforholdEndring, tekstBuilder, arbeidsforholdOverstyringer);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? HistorikkEndretFeltVerdiType.TIDSBEGRENSET_ARBEIDSFORHOLD : HistorikkEndretFeltVerdiType.IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD;
    }

    private void oppdaterVedEndretVerdi(ErTidsbegrensetArbeidsforholdEndring erTidsbegrensetArbeidsforholdEndring,
                                        HistorikkInnslagTekstBuilder tekstBuilder,
                                        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var verdiEndring = erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring();
        if (verdiEndring.erEndring()) {
            var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER,
                Optional.ofNullable(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver()),
                Optional.ofNullable(erTidsbegrensetArbeidsforholdEndring.getArbeidsforholdRef()), arbeidsforholdOverstyringer);
            var opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getFraVerdi());
            var nyVerdi = konvertBooleanTilFaktaEndretVerdiType(verdiEndring.getTilVerdi());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD, arbeidsforholdInfo, opprinneligVerdi, nyVerdi);
        }
    }


}
