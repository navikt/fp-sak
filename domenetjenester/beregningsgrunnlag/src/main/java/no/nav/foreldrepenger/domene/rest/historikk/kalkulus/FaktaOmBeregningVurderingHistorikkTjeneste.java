package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;


import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.aksjonspunkt.ErMottattYtelseEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonskravGyldighetEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

/**
 * Lager historikk for radioknapp-vurderinger i fakta om beregning.
 */
@Dependent
public class FaktaOmBeregningVurderingHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public FaktaOmBeregningVurderingHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FaktaOmBeregningVurderingHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag,
                                                      InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public List<HistorikkinnslagLinjeBuilder> lagHistorikkForVurderinger(Long behandlingId, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        faktaOmBeregningVurderinger.getErNyoppstartetFLEndring().ifPresent(e ->
            linjer.add(fraTilEquals("Frilansvirksomhet", konvertBooleanTilNyoppstartetFLVerdiType(e.getFraVerdiEllerNull()), konvertBooleanTilNyoppstartetFLVerdiType(e.getTilVerdi()))));
        faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring().ifPresent(e ->
            linjer.add(fraTilEquals("Selvstendig næringsdrivende", konvertBooleanTilNyIarbeidslivetVerdiType(e.getFraVerdiEllerNull()), konvertBooleanTilNyIarbeidslivetVerdiType(e.getTilVerdi()))));
        faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring().ifPresent(e ->
            linjer.add(fraTilEquals("Lønnsendring i beregningsperioden", e.getFraVerdiEllerNull(), e.getTilVerdi())));
        faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring().ifPresent(e ->
            linjer.add(fraTilEquals("Militær- eller siviltjeneste", e.getFraVerdiEllerNull(), e.getTilVerdi())));
        faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring().ifPresent(e ->
            linjer.add(fraTilEquals("Har søker inntekt fra etterlønn eller sluttpakke", e.getFraVerdiEllerNull(), e.getTilVerdi())));


        linjer.addAll(lagHistorikkForErMottattYtelseEndringer(behandlingId, faktaOmBeregningVurderinger.getErMottattYtelseEndringer()));
        linjer.addAll(lagHistorikkForTidsbegrensetArbeidsforholdEndringer(behandlingId, faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer()));
        linjer.addAll(lagHistorikkForRefusjonGyldighetEndringer(behandlingId, faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer()));

        return linjer;
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForTidsbegrensetArbeidsforholdEndringer(Long behandlingId, List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erTidsbegrensetArbeidsforholdEndringer.forEach(erTidsbegrensetArbeidsforholdEndring -> {
            var endring = erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring();
            var info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            linjer.add(fraTilEquals(String.format("Endring tidsbegrenset arbeidsforhold %s", info), konvertBooleanTilErTidsbegrensetVerdiType(endring.getFraVerdiEllerNull()), konvertBooleanTilErTidsbegrensetVerdiType(endring.getTilVerdi())));
        });
        return linjer;
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForErMottattYtelseEndringer(Long behandlingId, List<ErMottattYtelseEndring> erMottattYtelseEndringer) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erMottattYtelseEndringer.forEach(erMottattYtelseEndring -> {
            var endring = erMottattYtelseEndring.getErMottattYtelseEndring();
            if (erMottattYtelseEndring.getArbeidsgiver() != null) {
                var info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erMottattYtelseEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
                linjer.add(fraTilEquals(String.format("Mottar søker ytelse for arbeid i %s", info), endring.getFraVerdiEllerNull(), endring.getTilVerdi()));
            } else if (erMottattYtelseEndring.getAktivitetStatus().erFrilanser()) {
                linjer.add(fraTilEquals("Mottar søker ytelse for frilansaktiviteten", endring.getFraVerdiEllerNull(), endring.getTilVerdi()));
            }
        });
        return linjer;
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForRefusjonGyldighetEndringer(Long behandlingId, List<RefusjonskravGyldighetEndring> refusjonskravGyldighetEndringer) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        refusjonskravGyldighetEndringer.forEach(refusjonskravGyldighetEndring -> {
            var erGyldighetUtvidet = refusjonskravGyldighetEndring.getErGyldighetUtvidet();
            var navnVerdi = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(refusjonskravGyldighetEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            linjer.add(fraTilEquals(String.format("Ny refusjonsfrist %s", navnVerdi), erGyldighetUtvidet.getFraVerdiEllerNull(), erGyldighetUtvidet.getTilVerdi()));
        });
        return linjer;
    }

    private String konvertBooleanTilErTidsbegrensetVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? "tidsbegrenset" : "ikke tidsbegrenset";
    }

    private String konvertBooleanTilNyIarbeidslivetVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? "ny i arbeidslivet" : "ikke ny i arbeidslivet";
    }

    private String konvertBooleanTilNyoppstartetFLVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? "nyoppstartet" : "ikke nyoppstartet";
    }

}
