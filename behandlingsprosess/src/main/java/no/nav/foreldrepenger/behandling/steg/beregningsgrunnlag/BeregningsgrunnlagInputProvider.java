package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class BeregningsgrunnlagInputProvider {

    private Instance<BeregningsgrunnlagInputFelles> beregningsgrunnlagInputTjeneste;
    private Instance<BeregningsgrunnlagGUIInputFelles> beregningsgrunnlagRestInputTjeneste;

    public BeregningsgrunnlagInputProvider() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputProvider(@Any Instance<BeregningsgrunnlagInputFelles> beregningsgrunnlagInputTjeneste,
            @Any Instance<BeregningsgrunnlagGUIInputFelles> beregningsgrunnlagRestInputTjeneste) {
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningsgrunnlagRestInputTjeneste = beregningsgrunnlagRestInputTjeneste;
    }

    public BeregningsgrunnlagInputFelles getTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregningsgrunnlagInputTjeneste, fagsakYtelseType).orElseThrow();
    }

    public BeregningsgrunnlagGUIInputFelles getRestInputTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregningsgrunnlagRestInputTjeneste, fagsakYtelseType).orElseThrow();
    }

}
