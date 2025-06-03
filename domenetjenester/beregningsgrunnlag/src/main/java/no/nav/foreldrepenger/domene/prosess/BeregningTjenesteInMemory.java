package no.nav.foreldrepenger.domene.prosess;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

@RequestScoped
@Alternative
public class BeregningTjenesteInMemory implements BeregningTjeneste {

    private final Map<UUID, BeregningsgrunnlagGrunnlag> inMemoryBeregningsgrunlagLagring = new LinkedHashMap<>();

    public BeregningTjenesteInMemory() {
    }


    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        return Optional.ofNullable(inMemoryBeregningsgrunlagLagring.get(referanse.behandlingUuid()));
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGuiDto(BehandlingReferanse referanse) {
        return Optional.empty();
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse referanse, BehandlingStegType stegType) {
        throw new IllegalStateException("Skal ikke kalles for InMemory implementasjon, bruk heller #lagre");
    }

    @Override
    public void lagre(BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag, BehandlingReferanse referanse) {
        inMemoryBeregningsgrunlagLagring.put(referanse.behandlingUuid(), beregningsgrunnlagGrunnlag);
    }

    @Override
    public void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand) {
        if (!BeregningsgrunnlagTilstand.FASTSATT.equals(tilstand)) {
            throw new IllegalStateException("Støtter kun kopiering av fastsatte grunnlag!");
        }
        var originaltGr = hent(originalbehandling);
        originaltGr.ifPresent(gr -> lagre(gr, revurdering));
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdatering, BehandlingReferanse referanse) {
        return Optional.empty();
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse) {
        return Optional.empty();
    }

    @Override
    public void avslutt(BehandlingReferanse referanse) {
        // Ikke relevant for in-memory tjenesten
    }

    @Override
    public boolean kanStartesISteg(BehandlingReferanse referanse, BehandlingStegType stegType) {
        // Ikke relevant for in-memory tjenesten
        return false;
    }

}
