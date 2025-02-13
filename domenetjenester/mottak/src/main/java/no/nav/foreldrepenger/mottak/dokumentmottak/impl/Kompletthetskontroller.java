package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetModell;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
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
        var behandlingId = behandling.getId();
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        var grunnlagSnapshot = behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling);

        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());

        // Vurder kompletthet etter at dokument knyttet til behandling - med mindre man venter på registrering av papirsøknad
        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT)
            .stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .toList();
        var kompletthetResultat = kompletthetModell.vurderKompletthet(ref, stp, åpneAksjonspunkter);

        if (!kompletthetResultat.erOppfylt() && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD)) {
            var åpenKompletthet = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD).orElseThrow();
            if (!kompletthetResultat.erFristUtløpt() && !Objects.equals(åpenKompletthet.getVenteårsak(), kompletthetResultat.venteårsak())) {
                behandlingProsesseringTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, kompletthetResultat.ventefrist(), kompletthetResultat.venteårsak());
            }
        }
        if (kompletthetResultat.erOppfylt() && (kompletthetModell.erKompletthetssjekkEllerPassert(behandlingId)
            || behandling.isBehandlingPåVent() || mottattDokument.getDokumentType().erSøknadType() || mottattDokument.getDokumentType().erEndringsSøknadType())) {
            if (behandling.isBehandlingPåVent()) {
                behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
            }
            if (kompletthetModell.erRegisterinnhentingPassert(behandling.getId())) {
                // Reposisjoner basert på grunnlagsendring i nylig mottatt dokument. Videre reposisjonering gjøres i task etter registeroppdatering
                behandlingProsesseringTjeneste.utledDiffOgReposisjonerBehandlingVedEndringer(behandling, grunnlagSnapshot);
                behandlingProsesseringTjeneste.tvingInnhentingRegisteropplysninger(behandling);
                behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
            } else {
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            }
        }
    }

    void persisterKøetDokumentOgVurderKompletthet(Behandling behandling, MottattDokument mottattDokument, Optional<LocalDate> gjelderFra) {
        // Persister dokument (dvs. knytt dokument til behandlingen)
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, gjelderFra);
        vurderKompletthetForKøetBehandling(behandling);
    }

    private void vurderKompletthetForKøetBehandling(Behandling behandling) {
        var autoPunkter = kompletthetModell.rangerKompletthetsfunksjonerKnyttetTilAutopunkt(behandling.getFagsakYtelseType(), behandling.getType());
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        for (var autopunkt : autoPunkter) {
            var kompletthetResultat = kompletthetModell.vurderKompletthet(ref, stp, autopunkt);
            if (!kompletthetResultat.erOppfylt()) {
                // Et av kompletthetskriteriene er ikke oppfylt, og evt. brev er sendt ut. Logger historikk og avbryter
                if (!kompletthetResultat.erFristUtløpt()) {
                    dokumentmottakerFelles.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, kompletthetResultat.ventefrist(), kompletthetResultat.venteårsak());
                }
                return;
            }
        }
    }

    public void vurderNyForretningshendelse(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        // Forbi kompletthet: Sikre oppdatering dersom behandling står i FatteVedtak eller registerdata er innhentet samme dag.
        // Venter i kompletthet: Prøv på nytt i utvalgte tilfelle
        if (kompletthetModell.erRegisterinnhentingPassert(behandling.getId())) {
            behandlingProsesseringTjeneste.tvingInnhentingRegisteropplysninger(behandling);
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
        } else if (BehandlingÅrsakType.årsakerRelatertTilDød().contains(behandlingÅrsakType) && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        } else if (BehandlingÅrsakType.årsakerRelatertTilDød().contains(behandlingÅrsakType) && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        } else if (BehandlingÅrsakType.RE_HENDELSE_FØDSEL.equals(behandlingÅrsakType) && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
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
