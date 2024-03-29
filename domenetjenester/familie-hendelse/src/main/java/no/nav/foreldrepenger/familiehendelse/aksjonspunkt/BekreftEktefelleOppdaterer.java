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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftEktefelleAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftEktefelleAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftEktefelleOppdaterer implements AksjonspunktOppdaterer<BekreftEktefelleAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    BekreftEktefelleOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftEktefelleOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                      FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftEktefelleAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var erEktefellesBarn = familieHendelseTjeneste.hentAggregat(behandlingId)
            .getGjeldendeBekreftetVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getErEktefellesBarn);

        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.EKTEFELLES_BARN, konvertBooleanTilFaktaEndretVerdiType(erEktefellesBarn.orElse(null)),
            konvertBooleanTilFaktaEndretVerdiType(dto.getEktefellesBarn()));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_ADOPSJON);


        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medErEktefellesBarn(dto.getEktefellesBarn()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean ektefellesBarn) {
        if (ektefellesBarn == null) {
            return null;
        }
        return ektefellesBarn ? HistorikkEndretFeltVerdiType.EKTEFELLES_BARN : HistorikkEndretFeltVerdiType.IKKE_EKTEFELLES_BARN;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, HistorikkEndretFeltVerdiType original, HistorikkEndretFeltVerdiType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }
}
