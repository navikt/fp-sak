package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForForeldreansvarAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AvklarForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarForeldreansvarOppdaterer(SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        oppdaterAksjonspunktGrunnlag(dto, behandlingId);
        var skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        if (skalReinnhenteRegisteropplysninger) {
            return OppdateringResultat.utenTransisjon().medOppdaterGrunnlag().build();
        }
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
