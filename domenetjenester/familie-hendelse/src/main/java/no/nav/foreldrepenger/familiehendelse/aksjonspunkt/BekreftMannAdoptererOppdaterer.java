package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftMannAdoptererAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftMannAdoptererAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftMannAdoptererOppdaterer implements AksjonspunktOppdaterer<BekreftMannAdoptererAksjonspunktDto> {

    private BehandlingRepositoryProvider repositoryProvider;

    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    BekreftMannAdoptererOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftMannAdoptererOppdaterer(BehandlingRepositoryProvider repositoryProvider, HistorikkTjenesteAdapter historikkAdapter,
                                          FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.repositoryProvider = repositoryProvider;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftMannAdoptererAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        boolean totrinn = håndterEndringHistorikk(dto, behandling, param);

        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medAdoptererAlene(dto.getMannAdoptererAlene()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(BekreftMannAdoptererAksjonspunktDto dto, Behandling behandling, AksjonspunktOppdaterParameter param) {
        Optional<Boolean> mannAdoptererAlene = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId())
            .getOverstyrtVersjon()
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getAdoptererAlene);

        boolean erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.MANN_ADOPTERER, konvertBooleanTilFaktaEndretVerdiType(mannAdoptererAlene.orElse(null)),
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

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, HistorikkEndretFeltVerdiType original, HistorikkEndretFeltVerdiType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

}
