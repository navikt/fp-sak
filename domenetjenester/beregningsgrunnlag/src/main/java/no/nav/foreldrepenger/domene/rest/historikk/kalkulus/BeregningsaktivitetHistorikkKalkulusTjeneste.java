package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.ArrayList;
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
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

@ApplicationScoped
public class BeregningsaktivitetHistorikkKalkulusTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsaktivitetHistorikkKalkulusTjeneste() {
        // for CDI proxy
    }

    @Inject
    BeregningsaktivitetHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                 Historikkinnslag2Repository historikkinnslagRepository,
                                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(BehandlingReferanse behandlingReferanse,
                             String begrunnelse,
                             OppdaterBeregningsgrunnlagResultat endringsaggregat) {
        var tekstlinjer = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        for (var ba : endringsaggregat.getBeregningAktivitetEndringer()) {
            var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId()).getArbeidsforholdOverstyringer();
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba.getAktivitetNøkkel(), arbeidsforholdOverstyringer);
            lagSkalBrukesHistorikk(ba, aktivitetnavn).ifPresent(tekstlinjer::add);
            lagPeriodeHistorikk(ba, aktivitetnavn).ifPresent(tekstlinjer::add);
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

    private Optional<HistorikkinnslagTekstlinjeBuilder> lagSkalBrukesHistorikk(BeregningAktivitetEndring ba, String aktivitetnavn) {
        var skalBrukesTilVerdi = finnSkalBrukesTilVerdi(ba);
        var skalBrukesFraVerdi = finnSkalBrukesFraVerdi(ba);
        if (Objects.equals(skalBrukesTilVerdi, skalBrukesFraVerdi)) {
            return Optional.empty();
        }
        return Optional.ofNullable(fraTilEquals(String.format("Aktivitet %s", aktivitetnavn), skalBrukesFraVerdi, skalBrukesTilVerdi));
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

    private Optional<HistorikkinnslagTekstlinjeBuilder> lagPeriodeHistorikk(BeregningAktivitetEndring ba, String aktivitetnavn) {
        if (ba.getTomDatoEndring() == null) {
            return Optional.empty();
        }
        var nyPeriodeTom = ba.getTomDatoEndring().getTilVerdi();
        var gammelPeriodeTom = ba.getTomDatoEndring().getFraVerdi();
        if (Objects.equals(nyPeriodeTom, gammelPeriodeTom)) {
            return Optional.empty();
        }

        return Optional.of(new HistorikkinnslagTekstlinjeBuilder()
            .fraTil("Periode t.o.m.", gammelPeriodeTom, nyPeriodeTom)
            .tekst(String.format("__Det er lagt til ny aktivitet: %s__", aktivitetnavn)));
    }
}
