package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BeregningsresultatTidslinjetjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.HindreTilbaketrekkNårAlleredeUtbetalt;

import java.util.Collection;
import java.util.Collections;

@BehandlingStegRef(BehandlingStegType.HINDRE_TILBAKETREKK)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;

    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                  @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        if (aggregatTY.getUtbetBeregningsresultatFP() != null) {
            // I enkelte tilfeller kopieres utbet resultat i vurder tilbaketrekk steget,
            // isåfall skal vi bare bruke dette og ikke reberegne noe mer her
            // TFP-4279
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            var revurderingTY = aggregatTY.getBgBeregningsresultatFP();
            var behandlingReferanse = BehandlingReferanse.fra(behandling);
            var brAndelTidslinje = beregningsresultatTidslinjetjeneste
                    .lagTidslinjeForRevurdering(behandlingReferanse);
            var utledetSkjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
            var utbetBR = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(revurderingTY, brAndelTidslinje,
                    finnYrkesaktiviteter(behandlingReferanse), utledetSkjæringstidspunkt);

            // Reberegn feriepenger
            var ref = BehandlingReferanse.fra(behandling);
            var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, behandlingReferanse.fagsakYtelseType()).orElseThrow();
            feriepengerTjeneste.beregnFeriepenger(ref, utbetBR);
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBR);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Collection<Yrkesaktivitet> finnYrkesaktiviteter(BehandlingReferanse behandlingReferanse) {
        var aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId())
                .getAktørArbeidFraRegister(behandlingReferanse.aktørId());
        return aktørArbeidFraRegister
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList());
    }

}
