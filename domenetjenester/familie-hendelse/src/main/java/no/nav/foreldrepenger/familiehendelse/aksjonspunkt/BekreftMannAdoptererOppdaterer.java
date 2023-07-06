package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftMannAdoptererAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftMannAdoptererAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftMannAdoptererOppdaterer implements AksjonspunktOppdaterer<BekreftMannAdoptererAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public BekreftMannAdoptererOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    BekreftMannAdoptererOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(BekreftMannAdoptererAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var totrinn = håndterEndringHistorikk(dto, behandlingReferanse, param);

        final var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medAdoptererAlene(dto.getMannAdoptererAlene()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(BekreftMannAdoptererAksjonspunktDto dto, BehandlingReferanse ref, AksjonspunktOppdaterParameter param) {
        var mannAdoptererAlene = familieHendelseTjeneste.hentAggregat(ref.behandlingId())
            .getOverstyrtVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getAdoptererAlene);

        var erEndret = oppdaterVedEndretVerdi(konvertBooleanTilFaktaEndretVerdiType(mannAdoptererAlene.orElse(null)),
            konvertBooleanTilFaktaEndretVerdiType(dto.getMannAdoptererAlene()));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_ADOPSJON);

        return erEndret;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean mannAdoptererAlene) {
        if (mannAdoptererAlene == null) {
            return null;
        }
        return mannAdoptererAlene ? HistorikkEndretFeltVerdiType.ADOPTERER_ALENE : HistorikkEndretFeltVerdiType.ADOPTERER_IKKE_ALENE;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltVerdiType original, HistorikkEndretFeltVerdiType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.MANN_ADOPTERER, original, bekreftet);
            return true;
        }
        return false;
    }

}
