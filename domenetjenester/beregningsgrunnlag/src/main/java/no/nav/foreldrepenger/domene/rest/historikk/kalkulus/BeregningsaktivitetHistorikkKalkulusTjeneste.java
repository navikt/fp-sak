package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.LINJESKIFT;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
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
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        for (var ba : endringsaggregat.getBeregningAktivitetEndringer()) {
            var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId()).getArbeidsforholdOverstyringer();
            var aktivitetnavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningaktivitet(ba.getAktivitetNøkkel(), arbeidsforholdOverstyringer);
            var skalBrukes = lagSkalBrukesHistorikk(ba);
            var tomDatoEndret = lagPeriodeHistorikk(ba);
            if (skalBrukes.isPresent() || tomDatoEndret.isPresent()) {
                linjer.add(new HistorikkinnslagLinjeBuilder().bold(aktivitetnavn));
                skalBrukes.ifPresent(linjer::add);
                tomDatoEndret.ifPresent(linjer::add);
                linjer.add(LINJESKIFT);
            }
        }

        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_BEREGNING)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private Optional<HistorikkinnslagLinjeBuilder> lagSkalBrukesHistorikk(BeregningAktivitetEndring ba) {
        var skalBrukesTilVerdi = finnSkalBrukesTilVerdi(ba);
        var skalBrukesFraVerdi = finnSkalBrukesFraVerdi(ba);
        if (Objects.equals(skalBrukesTilVerdi, skalBrukesFraVerdi)) {
            return Optional.empty();
        }
        return Optional.ofNullable(fraTilEquals("Aktivitet", skalBrukesFraVerdi, skalBrukesTilVerdi));
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

    private Optional<HistorikkinnslagLinjeBuilder> lagPeriodeHistorikk(BeregningAktivitetEndring ba) {
        if (ba.getTomDatoEndring() == null) {
            return Optional.empty();
        }
        var nyPeriodeTom = ba.getTomDatoEndring().getTilVerdi();
        var gammelPeriodeTom = ba.getTomDatoEndring().getFraVerdi();
        if (Objects.equals(nyPeriodeTom, gammelPeriodeTom)) {
            return Optional.empty();
        }

        return Optional.of(fraTilEquals("Periode t.o.m.", gammelPeriodeTom, nyPeriodeTom));
    }
}
