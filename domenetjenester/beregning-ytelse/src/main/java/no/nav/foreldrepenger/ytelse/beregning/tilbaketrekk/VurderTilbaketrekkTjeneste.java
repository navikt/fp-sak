package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.Collection;
import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

@ApplicationScoped
public class VurderTilbaketrekkTjeneste {

    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    VurderTilbaketrekkTjeneste() {
    }

    @Inject
    public VurderTilbaketrekkTjeneste(BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
                                      InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public boolean skalVurdereTilbaketrekk(BehandlingReferanse ref) {
        return skalVurdereTilbaketrekk(ref, finnYrkesaktiviteter(ref));
    }

    private boolean skalVurdereTilbaketrekk(BehandlingReferanse ref, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var brAndelTidslinje = beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(ref);
        return VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, yrkesaktiviteter, ref.getUtledetSkjæringstidspunkt());
    }

    private Collection<Yrkesaktivitet> finnYrkesaktiviteter(BehandlingReferanse ref) {
        var aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId())
                .getAktørArbeidFraRegister(ref.aktørId());
        return aktørArbeidFraRegister
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList());
    }
}
