package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class BeregningsaktivitetHistorikkKalkulusTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsaktivitetHistorikkKalkulusTjeneste() {
        // for CDI proxy
    }

    @Inject
    BeregningsaktivitetHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                 HistorikkTjenesteAdapter historikkAdapter,
                                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId,
                                                     String begrunnelse,
                                                     OppdaterBeregningsgrunnlagResultat endringsaggregat) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        var builder = lagHistorikk(behandlingId, historikkInnslagTekstBuilder, endringsaggregat.getBeregningAktivitetEndringer(),
            begrunnelse);
        historikkAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
        return builder;
    }

    private HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId,
                                                      HistorikkInnslagTekstBuilder tekstBuilder,
                                                      List<BeregningAktivitetEndring> beregningAktivitetEndringer,
                                                      String begrunnelse) {
        tekstBuilder.medBegrunnelse(begrunnelse).medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        beregningAktivitetEndringer.forEach(ba -> {
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba.getAktivitetNøkkel(),
                arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(tekstBuilder, ba, aktivitetnavn);
            lagPeriodeHistorikk(tekstBuilder, ba, aktivitetnavn);
        });
        return tekstBuilder;
    }

    private void lagSkalBrukesHistorikk(HistorikkInnslagTekstBuilder tekstBuilder,
                                        BeregningAktivitetEndring ba,
                                        String aktivitetnavn) {
        var skalBrukesTilVerdi = finnSkalBrukesTilVerdi(ba);
        var skalBrukesFraVerdi = finnSkalBrukesFraVerdi(ba);
        if (!Objects.equals(skalBrukesTilVerdi, skalBrukesFraVerdi)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn, skalBrukesFraVerdi,
                skalBrukesTilVerdi);
        }
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesFraVerdi(BeregningAktivitetEndring ba) {
        if (ba.getSkalBrukesEndring() != null && ba.getSkalBrukesEndring().getFraVerdi().isPresent()) {
            return ba.getSkalBrukesEndring().getFraVerdi().get() ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
        }
        return null;
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesTilVerdi(BeregningAktivitetEndring ba) {
        if (ba.getSkalBrukesEndring() != null) {
            return ba.getSkalBrukesEndring().getTilVerdi() ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
        }
        return null;
    }

    private void lagPeriodeHistorikk(HistorikkInnslagTekstBuilder tekstBuilder,
                                     BeregningAktivitetEndring ba,
                                     String aktivitetnavn) {
        if (ba.getTomDatoEndring() != null) {
            var nyPeriodeTom = ba.getTomDatoEndring().getTilVerdi();
            var gammelPeriodeTom = ba.getTomDatoEndring().getFraVerdi();
            if (!Objects.equals(nyPeriodeTom, gammelPeriodeTom)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_TOM, gammelPeriodeTom, nyPeriodeTom);
                tekstBuilder.medTema(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn);
            }
        }

    }
}
