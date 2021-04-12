package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIBeregning;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.BehandlingsresultatBeregningTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class BehandlingsresultatBeregningTjenesteFP implements BehandlingsresultatBeregningTjeneste {
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    public BehandlingsresultatBeregningTjenesteFP() {
        // CDI
    }

    @Inject
    public BehandlingsresultatBeregningTjenesteFP(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                  ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public boolean erUgunst(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> revurderingBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.getBehandlingId());
        Optional<BeregningsgrunnlagEntitet> forrigeBG = ref.getOriginalBehandlingId().flatMap(beregningsgrunnlagTjeneste::hentBeregningsgrunnlagEntitetForBehandling);
        Optional<LocalDate> sisteDagMedFP = finnSisteUttaksdato(ref);
        return ErEndringIBeregning.vurderUgunst(revurderingBG, forrigeBG, sisteDagMedFP);
    }

    @Override
    public boolean erEndring(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> revurderingBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.getBehandlingId());
        Optional<BeregningsgrunnlagEntitet> forrigeBG = ref.getOriginalBehandlingId().flatMap(beregningsgrunnlagTjeneste::hentBeregningsgrunnlagEntitetForBehandling);
        Optional<LocalDate> sisteDagMedFP = finnSisteUttaksdato(ref);
        return ErEndringIBeregning.vurder(revurderingBG, forrigeBG, sisteDagMedFP);
    }

    private Optional<LocalDate> finnSisteUttaksdato(BehandlingReferanse ref) {
        List<ForeldrepengerUttakPeriode> perioder = uttakTjeneste.hentUttakHvisEksisterer(ref.getBehandlingId())
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(Collections.emptyList());
        return perioder.stream()
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getTom)
            .max(Comparator.naturalOrder());
    }

}
