package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktivitetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningAktiviteterEndring;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class BeregningsaktivitetHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsaktivitetHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    BeregningsaktivitetHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                         HistorikkTjenesteAdapter historikkAdapter,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId, BeregningAktiviteterEndring beregningAktiviteterEndring, String begrunnelse) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        var builder = lagHistorikk(behandlingId, historikkInnslagTekstBuilder, beregningAktiviteterEndring, begrunnelse);
        historikkAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
        return builder;
    }

    public HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId,
                                                     HistorikkInnslagTekstBuilder tekstBuilder,
                                                     BeregningAktiviteterEndring beregningAktiviteterEndring,
                                                     String begrunnelse) {
        tekstBuilder.medBegrunnelse(begrunnelse).medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        beregningAktiviteterEndring.getAktivitetEndringer().forEach(aktivitetEndring -> {
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(
                aktivitetEndring.getAktivitetNøkkel(), arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(tekstBuilder, aktivitetEndring, aktivitetnavn);
            lagPeriodeHistorikk(tekstBuilder, aktivitetEndring, aktivitetnavn);
        });
        return tekstBuilder;
    }

    private void lagSkalBrukesHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, BeregningAktivitetEndring aktivitetEndring, String aktivitetnavn) {
        var skalBrukesEndring = aktivitetEndring.getSkalBrukesEndring();
        if (skalBrukesEndring != null) {
            var skalBrukesTilVerdi = skalBrukesEndring.getTilVerdi() ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
            var skalBrukesFraVerdi = skalBrukesEndring.getTilVerdi() ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
            if (!skalBrukesTilVerdi.equals(skalBrukesFraVerdi)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn, skalBrukesFraVerdi, skalBrukesTilVerdi);
            }
        }
    }

    private void lagPeriodeHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, BeregningAktivitetEndring aktivitetEndring, String aktivitetnavn) {
        if (aktivitetEndring.getTomDatoEndring() != null) {
            var tomDatoEndring = aktivitetEndring.getTomDatoEndring();
            var nyPeriodeTom = tomDatoEndring.getTilVerdi();
            if (tomDatoEndring.erEndret()) {
                var gammelPeriodeTom = tomDatoEndring.getFraVerdi();
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_TOM,
                    gammelPeriodeTom.orElse(null), nyPeriodeTom);
                tekstBuilder.medTema(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn);
            }
        }

    }

    private boolean finnesMatch(List<BeregningAktivitetEntitet> beregningAktiviteter, BeregningAktivitetEntitet beregningAktivitet) {
        return beregningAktiviteter.stream().anyMatch(ba -> Objects.equals(ba.getNøkkel(), beregningAktivitet.getNøkkel()));
    }
}
