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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftMannAdoptererAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftMannAdoptererAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftMannAdoptererOppdaterer implements AksjonspunktOppdaterer<BekreftMannAdoptererAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    @Inject
    public BekreftMannAdoptererOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste,
                                          Historikkinnslag2Repository historikkinnslag2Repository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    BekreftMannAdoptererOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(BekreftMannAdoptererAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();

        var eksisterendeMannAdoptererAlene = finnEksisterendeMannAdoptererAlene(behandlingReferanse);
        var erEndret = !Objects.equals(eksisterendeMannAdoptererAlene.orElse(null), dto.getMannAdoptererAlene()) || param.erBegrunnelseEndret();
        if (erEndret) {
            lagreHistorikkinnslag(param, dto, eksisterendeMannAdoptererAlene.orElse(null));
        }
        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingReferanse.behandlingId());
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medAdoptererAlene(dto.getMannAdoptererAlene()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingReferanse.behandlingId(), oppdatertOverstyrtHendelse);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private void lagreHistorikkinnslag(AksjonspunktOppdaterParameter param, BekreftMannAdoptererAksjonspunktDto dto, Boolean eksisterende) {
        //TODO TFP-5554 5004, 5005, 5006 løses samtidig. Tidligere vært ett historikkinnslag, nå 3. FIX
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel(SkjermlenkeType.FAKTA_OM_ADOPSJON)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getRef().fagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addTekstlinje(HistorikkinnslagTekstlinjeBuilder.fraTilEquals("Mann adopterer", eksisterende, dto.getMannAdoptererAlene()))
            .addTekstlinje(dto.getBegrunnelse())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private Optional<Boolean> finnEksisterendeMannAdoptererAlene(BehandlingReferanse ref) {
        return familieHendelseTjeneste.hentAggregat(ref.behandlingId())
            .getOverstyrtVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getAdoptererAlene);
    }

}
