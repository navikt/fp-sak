package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;


import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
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

    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkForVurderinger(Long behandlingId, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        faktaOmBeregningVurderinger.getErNyoppstartetFLEndring().ifPresent(e ->
            tekstlinjer.add(fraTilEquals("Frilansvirksomhet", konvertBooleanTilNyoppstartetFLVerdiType(e.getFraVerdiEllerNull()), konvertBooleanTilNyoppstartetFLVerdiType(e.getTilVerdi()))));
        faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring().ifPresent(e ->
            tekstlinjer.add(fraTilEquals("Selvstendig næringsdrivende", konvertBooleanTilNyIarbeidslivetVerdiType(e.getFraVerdiEllerNull()), konvertBooleanTilNyIarbeidslivetVerdiType(e.getTilVerdi()))));
        faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring().ifPresent(e ->
            tekstlinjer.add(fraTilEquals("Lønnsendring i beregningsperioden", e.getFraVerdiEllerNull(), e.getTilVerdi())));
        faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring().ifPresent(e ->
            tekstlinjer.add(fraTilEquals("Militær- eller siviltjeneste", e.getFraVerdiEllerNull(), e.getTilVerdi())));
        faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring().ifPresent(e ->
            tekstlinjer.add(fraTilEquals("Har søker inntekt fra etterlønn eller sluttpakke", e.getFraVerdiEllerNull(), e.getTilVerdi())));


        tekstlinjer.addAll(lagHistorikkForErMottattYtelseEndringer(behandlingId, faktaOmBeregningVurderinger.getErMottattYtelseEndringer()));
        tekstlinjer.addAll(lagHistorikkForTidsbegrensetArbeidsforholdEndringer(behandlingId, faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer()));
        tekstlinjer.addAll(lagHistorikkForRefusjonGyldighetEndringer(behandlingId, faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer()));

        return tekstlinjer;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkForTidsbegrensetArbeidsforholdEndringer(Long behandlingId, List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erTidsbegrensetArbeidsforholdEndringer.forEach(erTidsbegrensetArbeidsforholdEndring -> {
            var endring = erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring();
            var info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            tekstlinjer.add(fraTilEquals(String.format("Endring tidsbegrenset arbeidsforhold %s", info), konvertBooleanTilErTidsbegrensetVerdiType(endring.getFraVerdiEllerNull()), konvertBooleanTilErTidsbegrensetVerdiType(endring.getTilVerdi())));
        });
        return tekstlinjer;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkForErMottattYtelseEndringer(Long behandlingId, List<ErMottattYtelseEndring> erMottattYtelseEndringer) {
        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erMottattYtelseEndringer.forEach(erMottattYtelseEndring -> {
            var endring = erMottattYtelseEndring.getErMottattYtelseEndring();
            if (erMottattYtelseEndring.getArbeidsgiver() != null) {
                var info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erMottattYtelseEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
                tekstlinjer.add(fraTilEquals(String.format("Mottar søker ytelse for arbeid i %s", info), endring.getFraVerdiEllerNull(), endring.getTilVerdi()));
            } else if (erMottattYtelseEndring.getAktivitetStatus().erFrilanser()) {
                tekstlinjer.add(fraTilEquals("Mottar søker ytelse for frilansaktiviteten", endring.getFraVerdiEllerNull(), endring.getTilVerdi()));
            }
        });
        return tekstlinjer;
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkForRefusjonGyldighetEndringer(Long behandlingId, List<RefusjonskravGyldighetEndring> refusjonskravGyldighetEndringer) {
        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        refusjonskravGyldighetEndringer.forEach(refusjonskravGyldighetEndring -> {
            var erGyldighetUtvidet = refusjonskravGyldighetEndring.getErGyldighetUtvidet();
            var navnVerdi = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(refusjonskravGyldighetEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            tekstlinjer.add(fraTilEquals(String.format("Ny refusjonsfrist %s", navnVerdi), erGyldighetUtvidet.getFraVerdiEllerNull(), erGyldighetUtvidet.getTilVerdi()));
        });
        return tekstlinjer;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilErTidsbegrensetVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? HistorikkEndretFeltVerdiType.TIDSBEGRENSET_ARBEIDSFORHOLD : HistorikkEndretFeltVerdiType.IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilNyIarbeidslivetVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? HistorikkEndretFeltVerdiType.NY_I_ARBEIDSLIVET : HistorikkEndretFeltVerdiType.IKKE_NY_I_ARBEIDSLIVET;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilNyoppstartetFLVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

}
