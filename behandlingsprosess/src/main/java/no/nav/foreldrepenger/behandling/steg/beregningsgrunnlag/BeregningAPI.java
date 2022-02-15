package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

public interface BeregningAPI {

    BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(Long behandlingId, BehandlingStegType behandlingStegType);

    OppdaterBeregningsgrunnlagResultat oppdater(AksjonspunktOppdaterParameter parameter, BekreftetAksjonspunktDto bekreftAksjonspunktDto);

    Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId);

    Optional<BeregningsgrunnlagDto> hentForGUI(Long behandlingId);

    void kopierFastsatt(Long behandlingId);

    void rydd(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg);
}
