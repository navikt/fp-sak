package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto.class, adapter = Overstyringshåndterer.class)
public class FaktaUttakSaksbehandlerOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto> {

    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakOverstyringFelles faktaUttakOverstyringFelles;

    FaktaUttakSaksbehandlerOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaUttakSaksbehandlerOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                        FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste,
                                                        FaktaUttakOverstyringFelles faktaUttakOverstyringFelles) {
        super(historikkAdapter, AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);
        this.faktaUttakOverstyringFelles = faktaUttakOverstyringFelles;
        this.faktaUttakHistorikkTjeneste = faktaUttakHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        if (!kanAksjonspunktAktiveres(behandling)) {
            throw new TekniskException("FP-605445", "Kan ikke aktivere aksjonspunkt med kode: " + dto.getAksjonspunktDefinisjon().getKode());
        }
        return faktaUttakOverstyringFelles.håndterOverstyring(dto, behandling);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringFaktaUttakDto.SaksbehandlerOverstyrerFaktaUttakDto dto) {
        //false i utførtAvOverstyrer, etter som det ikke er en "overstyrer"-rolle som ufører
        faktaUttakHistorikkTjeneste.byggHistorikkinnslag(dto.getBekreftedePerioder(), dto.getSlettedePerioder(), behandling, false);
    }

    private boolean kanAksjonspunktAktiveres(Behandling behandling) {
        return (erManuellRevurdering(behandling) || aksjonspunktFinnesFraFør(behandling)) && kanIkkeFinnesAksjonspunktFaktaUttak(behandling);
    }

    private boolean aksjonspunktFinnesFraFør(Behandling behandling) {
        return behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);
    }

    private boolean kanIkkeFinnesAksjonspunktFaktaUttak(Behandling behandling) {
       return behandling.getAksjonspunkter().stream()
            .noneMatch(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER) && !AksjonspunktStatus.AVBRUTT.equals(ap.getStatus()));
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        var erÅrsakHendelse = behandling.getBehandlingÅrsaker().stream().anyMatch(årsak -> Objects.equals(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, årsak.getBehandlingÅrsakType()));
        return Objects.equals(BehandlingType.REVURDERING, behandling.getType()) && (behandling.erManueltOpprettet() || erÅrsakHendelse);
    }

}
