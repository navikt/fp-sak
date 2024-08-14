package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

public interface BeregningTjeneste {

    Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse);

    Optional<BeregningsgrunnlagDto> hentGuiDto(BehandlingReferanse referanse);

    BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse referanse, BehandlingStegType stegType);

    void lagre(BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag, BehandlingReferanse referanse);

    void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand);

}
