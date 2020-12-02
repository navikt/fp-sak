package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import java.time.LocalDate;
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
        HistorikkInnslagTekstBuilder builder = lagHistorikk(behandlingId, historikkInnslagTekstBuilder, registerAktiviteter,
                saksbehandledeAktiviteter, begrunnelse, forrigeAggregat);
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
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
                .getArbeidsforholdOverstyringer();
        registerAktiviteter.getBeregningAktiviteter().forEach(ba -> {
            String aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba,
                    arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(tekstBuilder, saksbehandledeAktiviteter, forrigeAggregat, ba, aktivitetnavn);
            lagPeriodeHistorikk(tekstBuilder, saksbehandledeAktiviteter, ba, aktivitetnavn);
        });
        return tekstBuilder;
    }

    private void lagSkalBrukesHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
            Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat, BeregningAktivitetEntitet ba, String aktivitetnavn) {
        HistorikkEndretFeltVerdiType skalBrukesTilVerdi = finnSkalBrukesTilVerdi(saksbehandledeAktiviteter, ba);
        HistorikkEndretFeltVerdiType skalBrukesFraVerdi = finnSkalBrukesFraVerdi(forrigeAggregat, ba);
        if (!skalBrukesTilVerdi.equals(skalBrukesFraVerdi)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn, skalBrukesFraVerdi, skalBrukesTilVerdi);
        }
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesFraVerdi(Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat,
            BeregningAktivitetEntitet ba) {
        if (forrigeAggregat.isPresent()) {
            boolean finnesIForrige = forrigeAggregat.get().getBeregningAktiviteter().stream().anyMatch(a -> a.getNøkkel().equals(ba.getNøkkel()));
            return finnesIForrige ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
        }
        return null;
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesTilVerdi(BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
            BeregningAktivitetEntitet ba) {
        boolean finnesISaksbehandletVersjon = finnesMatch(saksbehandledeAktiviteter.getBeregningAktiviteter(), ba);
        return finnesISaksbehandletVersjon ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
    }

    private void lagPeriodeHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
            BeregningAktivitetEntitet ba, String aktivitetnavn) {
        Optional<BeregningAktivitetEntitet> saksbehandletAktivitet = saksbehandledeAktiviteter.getBeregningAktiviteter().stream()
                .filter(a -> Objects.equals(a.getNøkkel(), ba.getNøkkel())).findFirst();
        if (saksbehandletAktivitet.isPresent()) {
            LocalDate nyPeriodeTom = saksbehandletAktivitet.get().getPeriode().getTomDato();
            LocalDate gammelPeriodeTom = ba.getPeriode().getTomDato();
            if (!nyPeriodeTom.equals(gammelPeriodeTom)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_TOM, gammelPeriodeTom, nyPeriodeTom);
                tekstBuilder.medTema(HistorikkEndretFeltType.AKTIVITET, aktivitetnavn);
            }
        }

    }

    private boolean finnesMatch(List<BeregningAktivitetEntitet> beregningAktiviteter, BeregningAktivitetEntitet beregningAktivitet) {
        return beregningAktiviteter.stream()
                .anyMatch(ba -> Objects.equals(ba.getNøkkel(), beregningAktivitet.getNøkkel()));
    }
}
