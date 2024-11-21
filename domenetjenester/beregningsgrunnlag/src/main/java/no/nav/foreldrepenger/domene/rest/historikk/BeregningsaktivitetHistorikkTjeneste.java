package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;

@ApplicationScoped
public class BeregningsaktivitetHistorikkTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsaktivitetHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    BeregningsaktivitetHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                         Historikkinnslag2Repository historikkinnslagRepository,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(BehandlingReferanse behandlingReferanse,
                             BeregningAktivitetAggregatEntitet registerAktiviteter,
                             BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                             String begrunnelse,
                             Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat) {

        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        for (var ba : registerAktiviteter.getBeregningAktiviteter()) {
            var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId()).getArbeidsforholdOverstyringer();
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba, arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(saksbehandledeAktiviteter, forrigeAggregat, ba, aktivitetnavn).ifPresent(tekstlinjer::add);
            lagPeriodeHistorikk(saksbehandledeAktiviteter, ba, aktivitetnavn).ifPresent(tekstlinjer::add);
        }

        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_BEREGNING)
            .medTekstlinjer(tekstlinjer)
            .addTekstlinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private Optional<HistorikkinnslagTekstlinjeBuilder> lagSkalBrukesHistorikk(BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                                                                               Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat,
                                                                               BeregningAktivitetEntitet ba,
                                                                               String aktivitetnavn) {
        var skalBrukesTilVerdi = finnSkalBrukesTilVerdi(saksbehandledeAktiviteter, ba);
        var skalBrukesFraVerdi = finnSkalBrukesFraVerdi(forrigeAggregat, ba);
        if (skalBrukesTilVerdi.equals(skalBrukesFraVerdi)) {
            return Optional.empty();
        }
        return Optional.ofNullable(fraTilEquals(String.format("Aktivitet %s", aktivitetnavn), skalBrukesFraVerdi, skalBrukesTilVerdi));
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesFraVerdi(Optional<BeregningAktivitetAggregatEntitet> forrigeAggregat,
                                                                BeregningAktivitetEntitet ba) {
        if (forrigeAggregat.isPresent()) {
            var finnesIForrige = forrigeAggregat.get()
                .getBeregningAktiviteter()
                .stream()
                .anyMatch(a -> a.getNøkkel().equals(ba.getNøkkel()));
            return finnesIForrige ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
        }
        return null;
    }

    private HistorikkEndretFeltVerdiType finnSkalBrukesTilVerdi(BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                                                                BeregningAktivitetEntitet ba) {
        var finnesISaksbehandletVersjon = finnesMatch(saksbehandledeAktiviteter.getBeregningAktiviteter(), ba);
        return finnesISaksbehandletVersjon ? HistorikkEndretFeltVerdiType.BENYTT : HistorikkEndretFeltVerdiType.IKKE_BENYTT;
    }

    private Optional<HistorikkinnslagTekstlinjeBuilder> lagPeriodeHistorikk(BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter,
                                                                            BeregningAktivitetEntitet ba,
                                                                            String aktivitetnavn) {
        var saksbehandletAktivitet = saksbehandledeAktiviteter.getBeregningAktiviteter()
            .stream()
            .filter(a -> Objects.equals(a.getNøkkel(), ba.getNøkkel()))
            .findFirst();
        if (saksbehandletAktivitet.isEmpty()) {
            return Optional.empty();
        }
        var nyPeriodeTom = saksbehandletAktivitet.get().getPeriode().getTomDato();
        var gammelPeriodeTom = ba.getPeriode().getTomDato();
        if (nyPeriodeTom.equals(gammelPeriodeTom)) {
            return Optional.empty();
        }

        return Optional.of(new HistorikkinnslagTekstlinjeBuilder()
            .fraTil("Periode t.o.m.", gammelPeriodeTom, nyPeriodeTom)
            .tekst(String.format("__Det er lagt til ny aktivitet: %s__", aktivitetnavn)));
    }

    private boolean finnesMatch(List<BeregningAktivitetEntitet> beregningAktiviteter,
                                BeregningAktivitetEntitet beregningAktivitet) {
        return beregningAktiviteter.stream()
            .anyMatch(ba -> Objects.equals(ba.getNøkkel(), beregningAktivitet.getNøkkel()));
    }
}
