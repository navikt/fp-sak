package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaInngangsVilkårUtleder;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Denne klassen er en utvidelse av {@link BehandlingskontrollTjeneste} som håndterer oppdatering på åpen behandling.
 * <p>
 * Ikke endr denne klassen dersom du ikke har en komplett forståelse av hvordan denne protokollen fungerer.
 */
@Dependent
public class Endringskontroller {
    private static final Logger LOG = LoggerFactory.getLogger(Endringskontroller.class);
    private static final Set<BehandlingStegType> STARTPUNKT_STEG_INNGANG_VILKÅR = Set.of(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT.getBehandlingSteg(),
        StartpunktType.SØKERS_RELASJON_TIL_BARNET.getBehandlingSteg());
    private static final AksjonspunktDefinisjon SPESIALHÅNDTERT_AKSJONSPUNKT = AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU;
    private static final StartpunktType SPESIALHÅNDTERT_AKSJONSPUNKT_STARTPUNKT = StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private BehandlingModellTjeneste behandlingModellTjeneste;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private Instance<KontrollerFaktaInngangsVilkårUtleder> kontrollerFaktaTjenester;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Instance<StartpunktTjeneste> startpunktTjenester;
    private TotrinnRepository totrinnRepository;

    Endringskontroller() {
        // For CDI proxy
    }

    @Inject
    public Endringskontroller(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                              AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                              BehandlingModellTjeneste behandlingModellTjeneste,
                              @Any Instance<StartpunktTjeneste> startpunktTjenester,
                              RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                              @Any Instance<KontrollerFaktaInngangsVilkårUtleder> kontrollerFaktaTjenester,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                              TotrinnRepository totrinnRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.behandlingModellTjeneste = behandlingModellTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.startpunktTjenester = startpunktTjenester;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.kontrollerFaktaTjenester = kontrollerFaktaTjenester;
        this.totrinnRepository = totrinnRepository;
    }

    public boolean erRegisterinnhentingPassert(Behandling behandling) {
        return behandling.getType().erYtelseBehandlingType()
            && behandlingModellTjeneste.erStegAEtterStegB(behandling.getFagsakYtelseType(), behandling.getType(), behandling.getAktivtBehandlingSteg(), BehandlingStegType.INNHENT_REGISTEROPP);
    }

    // Kalles når behandlingen har ligget over natten (en dag) - selv om EndringsresultatDiff er tom. For å få med endringer i andre ytelser
    public void vurderNySimulering(Behandling behandling, BehandlingLås lås) {
        // Engangsstønad påvirkes ikke av andre ytelser. Hvis det ikke ble feilutbetaling i forrige simulering så oppstår den ikke plutselig
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) ||
            !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FEILUTBETALING)) {
            return;
        }
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FEILUTBETALING) ||
            !behandling.getÅpneAksjonspunkter(AksjonspunktDefinisjon.getForeslåVedtakAksjonspunkter()).isEmpty()) {
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
            doSpolTilSteg(kontekst, behandling, BehandlingStegType.SIMULER_OPPDRAG, null, null);
        }
    }

    public void spolTilStartpunkt(Behandling behandling, BehandlingLås lås, EndringsresultatDiff endringsresultat, StartpunktType senesteStartpunkt) {
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);

        var startpunkt = FagsakYtelseTypeRef.Lookup.find(startpunktTjenester, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()))
            .utledStartpunktForDiffBehandlingsgrunnlag(ref, skjæringstidspunkter, endringsresultat);

        if (startpunkt.getRangering() > senesteStartpunkt.getRangering()) {
            startpunkt = senesteStartpunkt;
        }

        if (startpunkt.equals(StartpunktType.UDEFINERT)) {
            if (harUtførtKontrollerFakta(behandling) && STARTPUNKT_STEG_INNGANG_VILKÅR.contains(behandling.getAktivtBehandlingSteg())) {
                utledAksjonspunkterTilHøyreForStartpunkt(kontekst, behandling, behandling.getAktivtBehandlingSteg(), ref, skjæringstidspunkter);
            }
            return; // Ingen detekterte endringer - ingen tilbakespoling
        }

        doSpolTilStartpunkt(ref, skjæringstidspunkter, kontekst, behandling, startpunkt);
    }

    private void doSpolTilStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, BehandlingskontrollKontekst kontekst,
                                     Behandling behandling, StartpunktType startpunktType) {
        var startPunktSteg = startpunktType.getBehandlingSteg();
        var skalSpesialHåndteres = behandling.getÅpentAksjonspunktMedDefinisjonOptional(SPESIALHÅNDTERT_AKSJONSPUNKT).isPresent() &&
            SPESIALHÅNDTERT_AKSJONSPUNKT_STARTPUNKT.getRangering() < startpunktType.getRangering();
        var tilSteg = skalSpesialHåndteres ? SPESIALHÅNDTERT_AKSJONSPUNKT.getBehandlingSteg() : startPunktSteg;

        oppdaterStartpunktVedBehov(behandling, startpunktType);
        doSpolTilSteg(kontekst, behandling, tilSteg, ref, stp);
    }

    private void doSpolTilSteg(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType tilSteg, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        // Inkluderer tilbakeføring samme steg UTGANG->INNGANG
        var fraSteg = behandling.getAktivtBehandlingSteg();
        var tilbakeføres = skalTilbakeføres(behandling, fraSteg, tilSteg);
        // Gjør aksjonspunktutledning utenom steg kun dersom man står i eller skal gå tilbake til inngangsvilkår
        var sjekkSteg = tilbakeføres ? tilSteg : fraSteg;
        if (ref != null && harUtførtKontrollerFakta(behandling) && STARTPUNKT_STEG_INNGANG_VILKÅR.contains(sjekkSteg)) {
            utledAksjonspunkterTilHøyreForStartpunkt(kontekst, behandling, sjekkSteg, ref, stp);
        }

        if (tilbakeføres) {
            // Eventuelt ta behandling av vent. Kan flytte på behandling dersom autopunkt med tilbakehopp
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(kontekst, behandling);
            var tilbakeføresNySjekk = skalTilbakeføres(behandling, behandling.getAktivtBehandlingSteg(), tilSteg);
            if (tilbakeføresNySjekk) {
                // Spol tilbake
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
            }
        }
        loggSpoleutfall(behandling, fraSteg, tilSteg, tilbakeføres);
    }

    private boolean skalTilbakeføres(Behandling behandling, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        // Dersom vi står i UTGANG, og skal til samme steg som vi står i, vil det også være en tilbakeføring siden vi går UTGANG -> INNGANG
        return Objects.equals(fraSteg, tilSteg) && BehandlingStegStatus.UTGANG.equals(behandling.getBehandlingStegStatus())
            || behandlingModellTjeneste.erStegAEtterStegB(behandling.getFagsakYtelseType(), behandling.getType(), fraSteg, tilSteg);
    }

    private boolean harUtførtKontrollerFakta(Behandling behandling) {
        return behandling.harSattStartpunkt();
    }

    private void oppdaterStartpunktVedBehov(Behandling behandling, StartpunktType nyttStartpunkt) {
        if (!behandling.harSattStartpunkt() || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            return;
        }
        //Skal bare settes på nytt dersom startpunkt er tidligere i prosessen.
        var gammelt = behandling.getStartpunkt();
        if (nyttStartpunkt.getRangering() < gammelt.getRangering()) {
            behandling.setStartpunkt(nyttStartpunkt);
        }
    }

    private void loggSpoleutfall(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg, boolean tilbakeført) {
        if (tilbakeført && !førSteg.equals(etterSteg)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForTilbakespoling(behandling, førSteg, etterSteg);
            LOG.info("Behandling {} har mottatt en endring som medførte spoling tilbake. Før-steg {}, etter-steg {}", behandling.getId(),
                førSteg.getNavn(), etterSteg.getNavn());
        } else {
            LOG.info("Behandling {} har mottatt en endring som ikke medførte spoling tilbake. Før-steg {}, etter-steg {}", behandling.getId(),
                førSteg.getNavn(), etterSteg.getNavn());
        }
    }

    // Orkestrerer aksjonspunktene for kontroll av fakta som utføres ifm tilbakehopp til et sted innen inngangsvilkår
    private void utledAksjonspunkterTilHøyreForStartpunkt(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType fomSteg, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var resultater = FagsakYtelseTypeRef.Lookup.find(KontrollerFaktaInngangsVilkårUtleder.class, kontrollerFaktaTjenester, ref.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.fagsakYtelseType().getKode()))
            .utledAksjonspunkterFomSteg(ref, stp, fomSteg);
        var resultatDef = resultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toSet());
        var avbrytes = behandling.getÅpneAksjonspunkter().stream()
            .filter(ap -> !ap.erManueltOpprettet() && !ap.erAutopunkt() && !SPESIALHÅNDTERT_AKSJONSPUNKT.equals(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !erReturBeslutter(ref, ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !resultatDef.contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> behandlingModellTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(ref.fagsakYtelseType(), ref.behandlingType(), fomSteg, ap.getAksjonspunktDefinisjon()))
            .toList();
        var opprettes = resultater.stream()
            .filter(ar -> !AksjonspunktStatus.UTFØRT.equals(behandling.getAksjonspunktMedDefinisjonOptional(ar.getAksjonspunktDefinisjon())
                .map(Aksjonspunkt::getStatus).orElse(AksjonspunktStatus.OPPRETTET)))
            .toList();
        if (!avbrytes.isEmpty()) {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterAvbrutt(behandling, kontekst.getSkriveLås(), avbrytes);
        }
        if (!opprettes.isEmpty()) {
            var opprettAksjonspunkt = opprettes.stream().filter(ar -> !ar.getAksjonspunktDefinisjon().erAutopunkt()).toList();
            if (!opprettAksjonspunkt.isEmpty()) {
                aksjonspunktkontrollTjeneste.lagreAksjonspunktResultat(behandling, kontekst.getSkriveLås(), BehandlingStegType.KONTROLLER_FAKTA, opprettAksjonspunkt);
            }
            opprettes.stream()
                .filter(ar -> ar.getAksjonspunktDefinisjon().erAutopunkt())
                .findFirst()
                .ifPresent(ar -> behandlingskontrollTjeneste.settBehandlingPåVent(kontekst, behandling, BehandlingStegType.KONTROLLER_FAKTA,
                    ar.getAksjonspunktDefinisjon(), ar.getFrist(), ar.getVenteårsak()));
        }
    }

    private boolean erReturBeslutter(BehandlingReferanse ref, AksjonspunktDefinisjon apDef) {
        return totrinnRepository.hentTotrinnaksjonspunktvurderinger(ref.behandlingId()).stream()
            .anyMatch(v -> v.isAktiv() && !v.isGodkjent() && v.getAksjonspunktDefinisjon().equals(apDef));

    }
}
