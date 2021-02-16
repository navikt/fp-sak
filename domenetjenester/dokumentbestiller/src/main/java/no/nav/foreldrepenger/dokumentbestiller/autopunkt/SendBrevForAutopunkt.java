package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.INFOBREV_OPPHOLD;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class SendBrevForAutopunkt {

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
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
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    public void sendBrevForSøknadIkkeMottatt(Behandling behandling, Aksjonspunkt ap) {
        var dokumentMalType = DokumentMalType.IKKE_SØKT;
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_BEHANDLING) || behandling.harBehandlingÅrsak(INFOBREV_OPPHOLD)) {
            if (Environment.current().isProd()) {
                dokumentMalType = DokumentMalType.INFO_TIL_ANNEN_FORELDER_DOK;
            } else {
                dokumentMalType = DokumentMalType.INFO_TIL_ANNEN_FORELDER;
            }
        }
        if ((DokumentMalType.IKKE_SØKT.equals(dokumentMalType) && !harSendtBrevForMal(behandling.getId(), dokumentMalType) && !harSendtBrevForMal(behandling.getId(), DokumentMalType.INNTEKTSMELDING_FOR_TIDLIG_DOK))
            || (DokumentMalType.INFO_TIL_ANNEN_FORELDER_DOK.equals(dokumentMalType) && !harSendtBrevForMal(behandling.getId(), dokumentMalType))
            || (DokumentMalType.INFO_TIL_ANNEN_FORELDER.equals(dokumentMalType) && !harSendtBrevForMal(behandling.getId(), dokumentMalType))) {
            // TODO(JEJ): Gjøre enkel !harSendtBrevForMal(behandling.getId(), dokumentMalType) igjen når det har gått litt tid siden begge ble lansert, inntil det bør begge sjekkes for å unngå dobbeltbrev til bruker...
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalType);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
    }

    public void sendBrevForTidligSøknad(Behandling behandling, Aksjonspunkt ap) {
        var dokumentMalType = DokumentMalType.FORLENGET_TIDLIG_SOK;
        if (!harSendtBrevForMal(behandling.getId(), dokumentMalType) &&
            erSøktPåPapir(behandling)) {
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalType);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
        oppdaterBehandlingMedNyFrist(behandling.getId(), beregnBehandlingstidsfrist(ap, behandling));
    }

    public void sendBrevForVenterPåFødsel(Behandling behandling, Aksjonspunkt ap) {
        LocalDate frist = ap.getFristTid().toLocalDate();
        var dokumentMalType = DokumentMalType.FORLENGET_MEDL_DOK;

        if (!harSendtBrevForMal(behandling.getId(), dokumentMalType) &&
            frist.isAfter(LocalDate.now().plusDays(1))) {
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalType);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
        oppdaterBehandlingMedNyFrist(behandling.getId(), beregnBehandlingstidsfrist(ap, behandling));
    }

    public void sendBrevForVenterPåOpptjening(Behandling behandling, Aksjonspunkt ap) {
        var dokumentMalType = DokumentMalType.FORLENGET_OPPTJENING;
        if (!harSendtBrevForMal(behandling.getId(), dokumentMalType) &&
            skalSendeForlengelsesbrevAutomatisk()) { //NOSONAR
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalType);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
        oppdaterBehandlingMedNyFrist(behandling.getId(), beregnBehandlingstidsfrist(ap, behandling));
    }

    public void sendBrevForEtterkontroll(Behandling behandling) {
        if (!harSendtBrevForMal(behandling.getId(), DokumentMalType.REVURDERING_DOK)
            && !harSendtBrevForMal(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING)) {

            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
            bestillBrevDto.setÅrsakskode(RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER.getKode());
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
    }

    private boolean erSøktPåPapir(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .filter(søknad -> !søknad.getElektroniskRegistrert()).isPresent();
    }

    private boolean harSendtBrevForMal(Long behandlingId, DokumentMalType malType) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, malType);
    }

    private LocalDate beregnBehandlingstidsfrist(Aksjonspunkt ap, Behandling behandling) {
        return LocalDate.from(ap.getFristTid().plusWeeks(behandling.getType().getBehandlingstidFristUker()));
    }

    void oppdaterBehandlingMedNyFrist(long behandlingId, LocalDate nyFrist) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    private boolean skalSendeForlengelsesbrevAutomatisk() {
        return false;
    }

}
