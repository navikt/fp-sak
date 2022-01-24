package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakToTrinnsTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAnnenforelderHarRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAnnenforelderHarRettOppdaterer implements AksjonspunktOppdaterer<AvklarAnnenforelderHarRettDto>  {

    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;
    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste;
    private UføretrygdRepository uføretrygdRepository;

    AvklarAnnenforelderHarRettOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAnnenforelderHarRettOppdaterer(KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste,
                                                FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste,
                                                FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste,
                                                UføretrygdRepository uføretrygdRepository) {
        this.kontrollerOppgittFordelingTjeneste = kontrollerOppgittFordelingTjeneste;
        this.faktaUttakHistorikkTjeneste = faktaUttakHistorikkTjeneste;
        this.faktaUttakToTrinnsTjeneste = faktaUttakToTrinnsTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param) {
        var tidligereVurderingAvMorsUføretrygd = tidligereVurderingAvMorsUføretrygd(param);
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(dto, param, tidligereVurderingAvMorsUføretrygd);
        var totrinn = endretVurderingAvMorsUføretrygd ||
            faktaUttakToTrinnsTjeneste.oppdaterToTrinnskontrollVedEndringerAnnenforelderHarRett(dto, param.getBehandlingId());
        faktaUttakHistorikkTjeneste.byggHistorikkinnslagForAvklarAnnenforelderHarIkkeRett(dto, param, endretVurderingAvMorsUføretrygd, tidligereVurderingAvMorsUføretrygd);
        kontrollerOppgittFordelingTjeneste.avklarAnnenforelderHarIkkeRett(dto, param.getBehandlingId());
        oppdaterUføretrygdVedBehov(dto, param);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();

    }

    private void oppdaterUføretrygdVedBehov(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getAnnenforelderMottarUføretrygd() != null) {
            uføretrygdRepository.hentGrunnlag(param.getBehandlingId())
                .filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd)
                .ifPresent(g -> uføretrygdRepository.lagreUføreGrunnlagOverstyrtVersjon(g.getBehandlingId(), dto.getAnnenforelderMottarUføretrygd()));

        }
    }

    private boolean endretVurderingAvMorsUføretrygd(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param, Boolean tidligereVurdering) {
        return dto.getAnnenforelderMottarUføretrygd() != null && uføretrygdRepository.hentGrunnlag(param.getBehandlingId()).isPresent()
            && !Objects.equals(tidligereVurdering, dto.getAnnenforelderMottarUføretrygd());
    }

    private Boolean tidligereVurderingAvMorsUføretrygd(AksjonspunktOppdaterParameter param) {
        return uføretrygdRepository.hentGrunnlag(param.getBehandlingId())
                .map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
    }

}
