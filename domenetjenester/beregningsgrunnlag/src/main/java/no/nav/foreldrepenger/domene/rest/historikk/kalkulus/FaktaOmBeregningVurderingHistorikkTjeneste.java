package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.aksjonspunkt.ErMottattYtelseEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonskravGyldighetEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.ToggleEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.util.List;

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

    public void lagHistorikkForVurderinger(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        faktaOmBeregningVurderinger.getErNyoppstartetFLEndring()
            .ifPresent(toggleEndring -> lagFaktaVurderingInnslag(tekstBuilder, HistorikkEndretFeltType.FRILANSVIRKSOMHET, toggleEndring, this::konvertBooleanTilNyoppstartetFLVerdiType));

        faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring()
            .ifPresent(toggleEndring -> lagFaktaVurderingInnslag(tekstBuilder, HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, toggleEndring, this::konvertBooleanTilNyIarbeidslivetVerdiType));

        faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(HistorikkEndretFeltType.LØNNSENDRING_I_PERIODEN, toggleEndring.getFraVerdiEllerNull(), toggleEndring.getTilVerdi()));

        faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MILITÆR_ELLER_SIVIL, toggleEndring.getFraVerdiEllerNull(), toggleEndring.getTilVerdi()));

        faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(HistorikkEndretFeltType.VURDER_ETTERLØNN_SLUTTPAKKE, toggleEndring.getFraVerdiEllerNull(), toggleEndring.getTilVerdi()));

        lagHistorikkForErMottattYtelseEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getErMottattYtelseEndringer());

        lagHistorikkForTidsbegrensetArbeidsforholdEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer());

        lagHistorikkForRefusjonGyldighetEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer());
    }

    private void lagHistorikkForTidsbegrensetArbeidsforholdEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erTidsbegrensetArbeidsforholdEndringer.forEach(erTidsbegrensetArbeidsforholdEndring -> {
            ToggleEndring endring = erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring();
            String info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            lagFaktaVurderingInnslag(tekstBuilder, HistorikkEndretFeltType.ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD, info, endring, this::konvertBooleanTilErTidsbegrensetVerdiType);
        });
    }

    private void lagHistorikkForErMottattYtelseEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<ErMottattYtelseEndring> erMottattYtelseEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erMottattYtelseEndringer.forEach(erMottattYtelseEndring -> {
            ToggleEndring endring = erMottattYtelseEndring.getErMottattYtelseEndring();
            if (erMottattYtelseEndring.getArbeidsgiver() != null) {
                String info = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(erMottattYtelseEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_ARBEID,
                    info,
                    endring.getFraVerdiEllerNull(),
                    endring.getTilVerdi());
            } else if (erMottattYtelseEndring.getAktivitetStatus().erFrilanser()) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_FRILANS,
                    endring.getFraVerdiEllerNull(),
                    endring.getTilVerdi());
            }
        });
    }

    private void lagHistorikkForRefusjonGyldighetEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<RefusjonskravGyldighetEndring> refusjonskravGyldighetEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        refusjonskravGyldighetEndringer.forEach(refusjonskravGyldighetEndring -> {
            ToggleEndring erGyldighetUtvidet = refusjonskravGyldighetEndring.getErGyldighetUtvidet();
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NY_REFUSJONSFRIST,
                arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(refusjonskravGyldighetEndring.getArbeidsgiver(), arbeidsforholdOverstyringer),
                erGyldighetUtvidet.getFraVerdiEllerNull(), erGyldighetUtvidet.getTilVerdi());
        });
    }

    private void lagFaktaVurderingInnslag(HistorikkInnslagTekstBuilder tekstBuilder,
                                          HistorikkEndretFeltType endretFeltType,
                                          String info,
                                          ToggleEndring endring,
                                          KonverterBoolenTilVerdiType konverterer) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konverterer.konverter(endring.getFraVerdiEllerNull());
        HistorikkEndretFeltVerdiType nyVerdi = konverterer.konverter(endring.getTilVerdi());
        tekstBuilder.medEndretFelt(endretFeltType, info, opprinneligVerdi, nyVerdi);
    }

    private void lagFaktaVurderingInnslag(HistorikkInnslagTekstBuilder tekstBuilder,
                                          HistorikkEndretFeltType endretFeltType,
                                          ToggleEndring endring,
                                          KonverterBoolenTilVerdiType konverterer) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konverterer.konverter(endring.getFraVerdiEllerNull());
        HistorikkEndretFeltVerdiType nyVerdi = konverterer.konverter(endring.getTilVerdi());
        tekstBuilder.medEndretFelt(endretFeltType, opprinneligVerdi, nyVerdi);
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

    @FunctionalInterface
    interface KonverterBoolenTilVerdiType {
        HistorikkEndretFeltVerdiType konverter(Boolean verdi);
    }

}
