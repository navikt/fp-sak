package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForForeldreansvarAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    AvklarForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarForeldreansvarOppdaterer(OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        oppdaterAksjonspunktGrunnlag(dto, behandlingId);
        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().build();
        } else {
            return OppdateringResultat.utenTransisjon().medOppdaterGrunnlag().build();
        }
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForForeldreansvarAksjonspunktDto dto, Long behandlingId) {

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato())
                .medForeldreansvarDato(dto.getForeldreansvarDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
    }

}
