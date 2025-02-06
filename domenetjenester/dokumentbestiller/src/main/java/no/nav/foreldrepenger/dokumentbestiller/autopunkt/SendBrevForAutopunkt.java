package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

import java.util.Set;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.INFOBREV_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.INFOBREV_PÅMINNELSE;

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
        var dokumentMal = DokumentMalType.IKKE_SØKT;
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_BEHANDLING)
            || behandling.harBehandlingÅrsak(INFOBREV_OPPHOLD) || behandling.harBehandlingÅrsak(INFOBREV_PÅMINNELSE)) {
            dokumentMal = DokumentMalType.FORELDREPENGER_INFO_TIL_ANNEN_FORELDER;
        } else if (behandling.harNoenBehandlingÅrsaker(Set.of(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, BehandlingÅrsakType.FEIL_IVERKSETTELSE_FRI_UTSETTELSE))) {
            dokumentMal = DokumentMalType.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV;
            // Akkurat denne skal ikke sendes flere ganger for en sak.
            if (dokumentBehandlingTjeneste.erDokumentBestiltForFagsak(behandling.getFagsakId(), dokumentMal)) {
                return;
            }
        }
        if (harIkkeSendtBrevForMal(behandling.getId(), dokumentMal)) {
            var dokumentBestilling = getBuilder(behandling, dokumentMal).build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        }
    }

    public void sendBrevForTidligSøknad(Behandling behandling) {
        var dokumentMal = DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG;
        if (harIkkeSendtBrevForMal(behandling.getId(), dokumentMal) && erSøktPåPapir(behandling)) {
            var dokumentBestilling = getBuilder(behandling, dokumentMal).build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        }
    }

    public void sendBrevForEtterkontroll(Behandling behandling) {
        var dokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;
        if (harIkkeSendtBrevForMal(behandling.getId(), dokumentMal)) {
            var dokumentBestilling = getBuilder(behandling, dokumentMal)
                .medRevurderingÅrsak(RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER)
                .build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        }
    }

    private static DokumentBestilling.Builder getBuilder(Behandling behandling, DokumentMalType dokumentMal) {
        return DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(dokumentMal);
    }

    private boolean erSøktPåPapir(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .filter(søknad -> !søknad.getElektroniskRegistrert()).isPresent();
    }

    private boolean harIkkeSendtBrevForMal(Long behandlingId, DokumentMalType malType) {
        return !dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, malType);
    }

}
