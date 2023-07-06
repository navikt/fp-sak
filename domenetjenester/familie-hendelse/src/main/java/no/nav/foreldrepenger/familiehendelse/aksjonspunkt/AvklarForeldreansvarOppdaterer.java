package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForForeldreansvarAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingRepository behandlingRepository;

    AvklarForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarForeldreansvarOppdaterer(SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste, BehandlingRepository behandlingRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        oppdaterAksjonspunktGrunnlag(dto, behandling);
        var skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        if (skalReinnhenteRegisteropplysninger) {
            return OppdateringResultat.utenTransisjon().medOppdaterGrunnlag().build();
        }
        return OppdateringResultat.utenTransisjon().build();
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForForeldreansvarAksjonspunktDto dto, Behandling behandling) {

        final var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato())
                .medForeldreansvarDato(dto.getForeldreansvarDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
    }

}
