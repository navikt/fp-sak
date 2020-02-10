package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
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
                                         HistorikkTjenesteAdapter historikkAdapter, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId,
                                              BeregningAktivitetAggregatEntitet registerAktiviteter,
                                              BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                                              String begrunnelse, Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat) {
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        HistorikkInnslagTekstBuilder builder = lagHistorikk(behandlingId, historikkInnslagTekstBuilder, registerAktiviteter, saksbehandledeAktiviteter, begrunnelse, forrigeAggregat);
        historikkAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
        return builder;
    }

    public HistorikkInnslagTekstBuilder lagHistorikk(Long behandlingId,
                                              HistorikkInnslagTekstBuilder tekstBuilder,
                                              BeregningAktivitetAggregatEntitet registerAktiviteter,
                                              BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                                              String begrunnelse, Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat) {
        tekstBuilder
            .medBegrunnelse(begrunnelse)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        registerAktiviteter.getBeregningAktiviteter().forEach(ba -> {
            String aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba, arbeidsforholdOverstyringer);
            HistorikkEndretFeltVerdiType tilVerdi = finnTilVerdi(saksbehandledeAktiviteter, ba);
            HistorikkEndretFeltVerdiType fraVerdi = finnFraVerdi(forrigeAggregat, ba);
            if (!tilVerdi.equals(fraVerdi)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn, fraVerdi, tilVerdi);
            }
        });
        return tekstBuilder;
    }

    private HistorikkEndretFeltVerdiType finnFraVerdi(Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat, BeregningAktivitetEntitet ba) {
        if (forrigeAggregat.isPresent()) {
            boolean finnesIForrige = forrigeAggregat.get().getBeregningAktiviteter().contains(ba);
            return finnesIForrige ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
        }
        return null;
    }

    private HistorikkEndretFeltVerdiType finnTilVerdi(BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter, BeregningAktivitetEntitet ba) {
        boolean finnesISaksbehandletVersjon = finnesMatch(saksbehandledeAktiviteter.getBeregningAktiviteter(), ba);
        return finnesISaksbehandletVersjon ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
    }

    private boolean finnesMatch(List<BeregningAktivitetEntitet> beregningAktiviteter, BeregningAktivitetEntitet beregningAktivitet) {
        return beregningAktiviteter.stream()
            .anyMatch(ba -> Objects.equals(ba, beregningAktivitet));
    }
}
