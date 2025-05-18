package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftMannAdoptererAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftMannAdoptererAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftMannAdoptererOppdaterer implements AksjonspunktOppdaterer<BekreftMannAdoptererAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public BekreftMannAdoptererOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste,
                                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    BekreftMannAdoptererOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(BekreftMannAdoptererAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();

        var eksisterendeMannAdoptererAlene = finnEksisterendeMannAdoptererAlene(behandlingReferanse);
        var erEndret = !Objects.equals(eksisterendeMannAdoptererAlene.orElse(null), dto.getMannAdoptererAlene());
        if (erEndret || param.erBegrunnelseEndret()) {
            lagreHistorikkinnslag(param, dto, eksisterendeMannAdoptererAlene.orElse(null));
        }
        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingReferanse.behandlingId());
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medAdoptererAlene(dto.getMannAdoptererAlene()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingReferanse.behandlingId(), oppdatertOverstyrtHendelse);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private void lagreHistorikkinnslag(AksjonspunktOppdaterParameter param, BekreftMannAdoptererAksjonspunktDto dto, Boolean eksisterende) {
        //TODO TFP-5554 5004, 5005, 5006 løses samtidig. Tidligere vært ett historikkinnslag, nå 3. FIX
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel(SkjermlenkeType.FAKTA_OM_ADOPSJON)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Mann adopterer", eksisterende, dto.getMannAdoptererAlene()))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private Optional<Boolean> finnEksisterendeMannAdoptererAlene(BehandlingReferanse ref) {
        return familieHendelseTjeneste.hentAggregat(ref.behandlingId())
            .getOverstyrtVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getAdoptererAlene);
    }

}
