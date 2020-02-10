package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.personopplysning.AvklarForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.domene.personopplysning.AvklartDataBarnAdapter;
import no.nav.foreldrepenger.domene.personopplysning.AvklartDataForeldreAdapter;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.omsorg.OmsorghendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForForeldreansvarAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private OmsorghendelseTjeneste omsorghendelseTjeneste;

    AvklarForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarForeldreansvarOppdaterer(SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                                  OmsorghendelseTjeneste omsorghendelseTjeneste) {
        this.omsorghendelseTjeneste = omsorghendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Behandling behandling, LocalDate forrigeSkjæringstidspunkt) {
        // TODO (lots): Avklare med Jarek om denne blir annerledes for FP
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandling.getId()).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        Behandling behandling = param.getBehandling();
        final LocalDate forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        oppdaterAksjonspunktGrunnlag(dto, behandling);
        boolean skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandling, forrigeSkjæringstidspunkt);

        if (skalReinnhenteRegisteropplysninger) {
            return OppdateringResultat.utenTransisjon().medOppdaterGrunnlag().build();
        } else {
            return OppdateringResultat.utenTransisjon().build();
        }
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForForeldreansvarAksjonspunktDto dto, Behandling behandling) {
        List<AvklartDataForeldreAdapter> foreldreAdapter = new ArrayList<>();
        dto.getForeldre().forEach(foreldre ->
            foreldreAdapter.add(new AvklartDataForeldreAdapter(foreldre.getAktorId(), foreldre.getDødsdato())));

        List<AvklartDataBarnAdapter> barnAdapter = new ArrayList<>();
        dto.getBarn().forEach(barn ->
            barnAdapter.add(new AvklartDataBarnAdapter(barn.getAktørId(), barn.getFodselsdato(), barn.getNummer())));

        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.fraKode(dto.getKode());

        final AvklarForeldreansvarAksjonspunktData data = new AvklarForeldreansvarAksjonspunktData(apDef,
            dto.getOmsorgsovertakelseDato(),dto.getForeldreansvarDato(),dto.getAntallBarn(), foreldreAdapter, barnAdapter);

        omsorghendelseTjeneste.aksjonspunktAvklarForeldreansvar(behandling, data);
    }

}
