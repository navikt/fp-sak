package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.INFOBREV_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.INFOBREV_PÅMINNELSE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

@ApplicationScoped
public class SendBrevForAutopunkt {

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private SøknadRepository søknadRepository;

    public SendBrevForAutopunkt() {
        //CDI
    }

    @Inject
    public SendBrevForAutopunkt(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                BehandlingRepositoryProvider provider) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.søknadRepository = provider.getSøknadRepository();
    }

    public void sendBrevForSøknadIkkeMottatt(Behandling behandling) {
        var dokumentMalType = DokumentMalType.IKKE_SØKT;
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_BEHANDLING)
            || behandling.harBehandlingÅrsak(INFOBREV_OPPHOLD) || behandling.harBehandlingÅrsak(INFOBREV_PÅMINNELSE)) {
            dokumentMalType = DokumentMalType.FORELDREPENGER_INFO_TIL_ANNEN_FORELDER;
        }
        if (!harSendtBrevForMal(behandling.getId(), dokumentMalType)) {
            var bestillBrevDto = opprettBestillBrevDto(behandling, dokumentMalType);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    public void sendBrevForTidligSøknad(Behandling behandling) {
        if (!harSendtBrevForMal(behandling.getId(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG) && erSøktPåPapir(behandling)) {
            var bestillBrevDto = opprettBestillBrevDto(behandling, DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    private BestillBrevDto opprettBestillBrevDto(Behandling behandling,
                                                 DokumentMalType forlengetSaksbehandlingstidTidlig) {
        return new BestillBrevDto(behandling.getId(), behandling.getUuid(), forlengetSaksbehandlingstidTidlig);
    }

    public void sendBrevForEtterkontroll(Behandling behandling) {
        if (!harSendtBrevForMal(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING)) {
            var bestillBrevDto = opprettBestillBrevDto(behandling, DokumentMalType.VARSEL_OM_REVURDERING);
            bestillBrevDto.setArsakskode(RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    private boolean erSøktPåPapir(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .filter(søknad -> !søknad.getElektroniskRegistrert()).isPresent();
    }

    private boolean harSendtBrevForMal(Long behandlingId, DokumentMalType malType) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, malType);
    }

}
