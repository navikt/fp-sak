package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

@BehandlingStegRef(BehandlingStegType.IVERKSETT_VEDTAK)
@BehandlingTypeRef(BehandlingType.INNSYN) // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteInnsynVedtakStegFelles implements IverksetteVedtakSteg {
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private BehandlingRepository behandlingRepository;

    public IverksetteInnsynVedtakStegFelles() {
        // for CDI proxy
    }

    @Inject
    protected IverksetteInnsynVedtakStegFelles(DokumentBestillerTjeneste dokumentBestillerTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        bestillVedtaksbrev(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void bestillVedtaksbrev(BehandlingskontrollKontekst kontekst) {
        var bestillBrevDto = brevDto(kontekst);
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private BestillBrevDto brevDto(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        var begrunnelse = ap.getBegrunnelse();
        var fritekst = nullOrEmpty(begrunnelse) ? " " : begrunnelse;

        return new BestillBrevDto(kontekst.getBehandlingId(), behandling.getUuid(), DokumentMalType.INNSYN_SVAR, fritekst);
    }

    private boolean nullOrEmpty(String begrunnelse) {
        return Objects.isNull(begrunnelse) || Objects.equals(begrunnelse, "");
    }
}
