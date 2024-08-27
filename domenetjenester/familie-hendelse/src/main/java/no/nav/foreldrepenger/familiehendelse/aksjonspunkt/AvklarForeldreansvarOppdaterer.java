package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForForeldreansvarAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForForeldreansvarAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AvklarForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarForeldreansvarOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        oppdaterAksjonspunktGrunnlag(dto, behandlingId);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForForeldreansvarAksjonspunktDto dto, Long behandlingId) {

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato())
                .medForeldreansvarDato(dto.getForeldreansvarDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
    }

}
