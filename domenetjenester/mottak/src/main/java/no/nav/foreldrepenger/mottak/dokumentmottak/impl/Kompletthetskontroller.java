package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
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
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Kompletthetsjekker kompletthetsjekker;

    public Kompletthetskontroller() {
        // For CDI proxy
    }

    @Inject
    public Kompletthetskontroller(DokumentmottakerFelles dokumentmottakerFelles,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  Kompletthetsjekker kompletthetsjekker) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
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
        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .toList();
        var kompletthetResultat = vurderKompletthet(ref, stp, åpneAksjonspunkter);

        if (!kompletthetResultat.erOppfylt() && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD)) {
            var åpenKompletthet = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD).orElseThrow();
            if (!kompletthetResultat.erFristUtløpt() && !Objects.equals(åpenKompletthet.getVenteårsak(), kompletthetResultat.venteårsak())) {
                behandlingProsesseringTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, kompletthetResultat.ventefrist(), kompletthetResultat.venteårsak());
            }
        }
        if (kompletthetResultat.erOppfylt() && (erKompletthetssjekkEllerPassert(behandlingId)
            || behandling.isBehandlingPåVent() || mottattDokument.getDokumentType().erSøknadType() || mottattDokument.getDokumentType().erEndringsSøknadType())) {
            if (behandling.isBehandlingPåVent()) {
                behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
            }
            if (erRegisterinnhentingPassert(behandling.getId())) {
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
    }

    public void vurderNyForretningshendelse(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        // Forbi kompletthet: Sikre oppdatering dersom behandling står i FatteVedtak eller registerdata er innhentet samme dag.
        // Venter i kompletthet: Prøv på nytt i utvalgte tilfelle
        if (erRegisterinnhentingPassert(behandling.getId())) {
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

    /*
     * Vurderer kompletthet for en behandling basert på hvilke aksjonspunkter som er åpne.
     * Skal i praksis aldri kunne være flere av disse, men er teoretisk mulig ved endringer i steg eller aksjonspunktdefinisjoner.
     */
    private KompletthetResultat vurderKompletthet(BehandlingReferanse ref, Skjæringstidspunkt stp, List<AksjonspunktDefinisjon> aksjonspunkter) {
        if (aksjonspunkter.contains(VENT_PÅ_SØKNAD)) {
            return kompletthetsjekker.vurderSøknadMottatt(ref);
        }
        if (aksjonspunkter.contains(VENT_PGA_FOR_TIDLIG_SØKNAD)) {
            return kompletthetsjekker.vurderSøknadMottattForTidlig(ref, stp);
        }
        if (aksjonspunkter.contains(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD)) {
            return kompletthetsjekker.vurderForsendelseKomplett(ref, stp);
        }
        if (aksjonspunkter.contains(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)) {
            return kompletthetsjekker.vurderEtterlysningInntektsmelding(ref, stp);
        }
        return KompletthetResultat.oppfylt(); // Enten AUTO_KØET_BEHANDLING eller ikke aktuelt å sjekke kompletthet => oppfylt
    }

    private boolean erRegisterinnhentingPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erStegPassert(behandlingId, BehandlingStegType.INNHENT_REGISTEROPP);
    }

    private boolean erKompletthetssjekkEllerPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
    }
}
