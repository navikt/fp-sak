package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto> {
    private static final AksjonspunktDefinisjon FASTSETTBRUTTOSNKODE = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                           VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste,
                                                           BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                           BeregningHåndterer beregningHåndterer) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        // Aksjonspunkt "opprettet" i GUI må legge til, bør endre på hvordan dette er løst
        if (dto.getErVarigEndretNaering()) {
            if (dto.getBruttoBeregningsgrunnlag() != null) {
                var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());
                var input = tjeneste.lagInput(param.getRef().getBehandlingId());
                beregningHåndterer.håndterVurderVarigEndretNyoppstartetSN(input, OppdatererDtoMapper.mapdVurderVarigEndringEllerNyoppstartetSNDto(dto));
            } else {
                behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(FASTSETTBRUTTOSNKODE));
            }
        } else {
            behandling.getÅpentAksjonspunktMedDefinisjonOptional(FASTSETTBRUTTOSNKODE)
                .ifPresent(a -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(a)));
        }

        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(param, dto);

        return OppdateringResultat.utenOveropp();
    }
}
