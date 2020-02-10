package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_KOMPLETT_OPPDATERING;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetModell;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Denne klassen evaluerer hvilken effekt en ekstern hendelse (dokument, forretningshendelse) har på en åpen behandlings
 * kompletthet, og etterfølgende effekt på behandlingskontroll (gjennom {@link Endringskontroller})
 */
@Dependent
public class Kompletthetskontroller {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private KompletthetModell kompletthetModell;
    BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public Kompletthetskontroller() {
        // For CDI proxy
    }

    @Inject
    public Kompletthetskontroller(DokumentmottakerFelles dokumentmottakerFelles,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  KompletthetModell kompletthetModell,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.kompletthetModell = kompletthetModell;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    void persisterDokumentOgVurderKompletthet(Behandling behandling, MottattDokument mottattDokument) {
        // Ta snapshot av gjeldende grunnlag-id-er før oppdateringer
        Long behandlingId = behandling.getId();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));

        EndringsresultatSnapshot grunnlagSnapshot = behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling);

        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());

        // Vurder kompletthet etter at dokument knyttet til behandling - med mindre man venter på registrering av papirsøknad
        List<AksjonspunktDefinisjon> åpneAksjonspunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).collect(Collectors.toList());
        KompletthetResultat kompletthetResultat = kompletthetModell.vurderKompletthet(ref, åpneAksjonspunkter);
        if (!kompletthetResultat.erOppfylt()) {
            settPåVent(behandling, kompletthetResultat);
        } else if (kompletthetModell.erKompletthetssjekkEllerPassert(behandlingId) || behandling.isBehandlingPåVent()
            || mottattDokument.getDokumentType().erSøknadType() || mottattDokument.getDokumentType().erEndringsSøknadType()) {
            spolKomplettBehandlingTilStartpunkt(behandling, grunnlagSnapshot);
            if (kompletthetModell.erKompletthetssjekkPassert(behandlingId)) {
                behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling);
            } else {
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            }
        }
    }

    private void settPåVent(Behandling behandling, KompletthetResultat kompletthetResultat) {
        if (kompletthetResultat.erFristUtløpt() || behandling.isBehandlingPåVent()) {
            // Tidsfrist for kompletthetssjekk er utløpt, skal derfor ikke settes på vent på nytt
            return;
        }
        // TODO (JOL): Logikken nå reflekterer det som lå i EndrKontroll. Avklar om andre autopunkt skal erstattes med det under.
        // Settes på vent til behandlig er komplett
        behandlingProsesseringTjeneste.settBehandlingPåVent(behandling, AUTO_VENT_KOMPLETT_OPPDATERING,
            kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak());
        dokumentmottakerFelles.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling,
            HistorikkinnslagType.BEH_VENT, kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak());
    }

    void persisterKøetDokumentOgVurderKompletthet(Behandling behandling, MottattDokument mottattDokument, Optional<LocalDate> gjelderFra) {
        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, gjelderFra);
        vurderKompletthetForKøetBehandling(behandling);
    }

    public void oppdaterKompletthetForKøetBehandling(Behandling behandling) {
        vurderKompletthetForKøetBehandling(behandling);
    }

    private void vurderKompletthetForKøetBehandling(Behandling behandling) {
        List<AksjonspunktDefinisjon> autoPunkter = kompletthetModell.rangerKompletthetsfunksjonerKnyttetTilAutopunkt(behandling.getFagsakYtelseType(), behandling.getType());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        for (AksjonspunktDefinisjon autopunkt : autoPunkter) {
            KompletthetResultat kompletthetResultat = kompletthetModell.vurderKompletthet(ref, autopunkt);
            if (!kompletthetResultat.erOppfylt()) {
                // Et av kompletthetskriteriene er ikke oppfylt, og evt. brev er sendt ut. Logger historikk og avbryter
                if (!kompletthetResultat.erFristUtløpt()) {
                    dokumentmottakerFelles.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling,
                        HistorikkinnslagType.BEH_VENT, kompletthetResultat.getVentefrist(), kompletthetResultat.getVenteårsak());
                }
                return;
            }
        }
    }

    public void vurderNyForretningshendelse(Behandling behandling) {
        if (kompletthetModell.erKompletthetssjekkPassert(behandling.getId())) {
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling);
        }
    }

    void spolKomplettBehandlingTilStartpunkt(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
        // Behandling er komplett - nullstill venting
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        if (kompletthetModell.erKompletthetssjekkPassert(behandling.getId())) {
            // Reposisjoner basert på grunnlagsendring i nylig mottatt dokument. Videre reposisjonering gjøres i task etter registeroppdatering
            EndringsresultatDiff diff = behandlingProsesseringTjeneste.finnGrunnlagsEndring(behandling, grunnlagSnapshot);
            behandlingProsesseringTjeneste.reposisjonerBehandlingVedEndringer(behandling, diff);
        }
    }

    void flyttTilbakeTilRegistreringPapirsøknad(Behandling behandling) {
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, BehandlingStegType.REGISTRER_SØKNAD);
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
    }

    boolean støtterBehandlingstypePapirsøknad(Behandling behandling) {
        return behandlingProsesseringTjeneste.erStegAktueltForBehandling(behandling, BehandlingStegType.REGISTRER_SØKNAD);
    }

}
