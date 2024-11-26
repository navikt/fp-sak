package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftEktefelleAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftEktefelleAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftEktefelleOppdaterer implements AksjonspunktOppdaterer<BekreftEktefelleAksjonspunktDto> {

    private Historikkinnslag2Repository historikkinnslag2Repository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    BekreftEktefelleOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftEktefelleOppdaterer(Historikkinnslag2Repository historikkinnslag2Repository, FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftEktefelleAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var eksisterendeErEktefellesBarn = familieHendelseTjeneste.hentAggregat(behandlingId)
            .getGjeldendeBekreftetVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getErEktefellesBarn);

        var erEndret = !Objects.equals(eksisterendeErEktefellesBarn.orElse(null), dto.getEktefellesBarn());
        if (erEndret || param.erBegrunnelseEndret()) {
            lagreHistorikkinnslag(dto, param, behandlingId);
        }

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse.medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder().medErEktefellesBarn(dto.getEktefellesBarn()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private void lagreHistorikkinnslag(BekreftEktefelleAksjonspunktDto dto, AksjonspunktOppdaterParameter param, Long behandlingId) {
        var historikkinnslag = new Historikkinnslag2.Builder().medFagsakId(param.getRef().fagsakId())
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_ADOPSJON)
            .addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().tekst("Barnet er vurdert til å")
                .bold(dto.getEktefellesBarn() ? "være ektefelles/samboers barn" : "ikke være ektefelles/samboers barn"))
            .addTekstlinje(dto.getBegrunnelse())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }
}
