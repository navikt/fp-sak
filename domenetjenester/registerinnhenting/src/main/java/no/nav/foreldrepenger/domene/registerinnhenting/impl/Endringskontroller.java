package no.nav.foreldrepenger.domene.registerinnhenting.impl;

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
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
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
                              @Any Instance<StartpunktTjeneste> startpunktTjenester,
                              RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                              @Any Instance<KontrollerFaktaInngangsVilkårUtleder> kontrollerFaktaTjenester,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                              TotrinnRepository totrinnRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.startpunktTjenester = startpunktTjenester;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.kontrollerFaktaTjenester = kontrollerFaktaTjenester;
        this.totrinnRepository = totrinnRepository;
    }

    public boolean erRegisterinnhentingPassert(Behandling behandling) {
        return behandling.getType().erYtelseBehandlingType() && behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP);
    }

    // Kalles når behandlingen har ligget over natten (en dag) - selv om EndringsresultatDiff er tom. For å få med endringer i andre ytelser
    public void vurderNySimulering(Behandling behandling) {
        // Engangsstønad påvirkes ikke av andre ytelser. Hvis det ikke ble feilutbetaling i forrige simulering så oppstår den ikke plutselig
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) ||
            !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FEILUTBETALING)) {
            return;
        }
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FEILUTBETALING) ||
            !behandling.getÅpneAksjonspunkter(AksjonspunktDefinisjon.getForeslåVedtakAksjonspunkter()).isEmpty()) {
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            doSpolTilSteg(kontekst, behandling, BehandlingStegType.SIMULER_OPPDRAG, null, null);
        }
    }

    public void spolTilStartpunkt(Behandling behandling, EndringsresultatDiff endringsresultat, StartpunktType senesteStartpunkt) {
        var behandlingId = behandling.getId();
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var startpunkt = FagsakYtelseTypeRef.Lookup.find(startpunktTjenester, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()))
            .utledStartpunktForDiffBehandlingsgrunnlag(ref, skjæringstidspunkter, endringsresultat);

        if (startpunkt.getRangering() > senesteStartpunkt.getRangering()) {
            startpunkt = senesteStartpunkt;
        }

        if (startpunkt.equals(StartpunktType.UDEFINERT)) {
            if (harUtførtKontrollerFakta(behandling) && STARTPUNKT_STEG_INNGANG_VILKÅR.contains(behandling.getAktivtBehandlingSteg())) {
                var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
                utledAksjonspunkterTilHøyreForStartpunkt(kontekst, behandling.getAktivtBehandlingSteg(), ref, skjæringstidspunkter, behandling);
            }
            return; // Ingen detekterte endringer - ingen tilbakespoling
        }

        doSpolTilStartpunkt(ref, skjæringstidspunkter, behandling, startpunkt);
    }

    private void doSpolTilStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling behandling, StartpunktType startpunktType) {
        var startPunktSteg = startpunktType.getBehandlingSteg();
        var skalSpesialHåndteres = behandling.getÅpentAksjonspunktMedDefinisjonOptional(SPESIALHÅNDTERT_AKSJONSPUNKT).isPresent() &&
            behandlingskontrollTjeneste.sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
                SPESIALHÅNDTERT_AKSJONSPUNKT.getBehandlingSteg(), startPunktSteg) < 0;
        var tilSteg = skalSpesialHåndteres ? SPESIALHÅNDTERT_AKSJONSPUNKT.getBehandlingSteg() : startPunktSteg;

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
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
            utledAksjonspunkterTilHøyreForStartpunkt(kontekst, sjekkSteg, ref, stp, behandling);
        }

        if (tilbakeføres) {
            // Eventuelt ta behandling av vent
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            // Spol tilbake
            behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, tilSteg);
        }
        loggSpoleutfall(behandling, fraSteg, tilSteg, tilbakeføres);
    }

    private boolean skalTilbakeføres(Behandling behandling, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        // Dersom vi står i UTGANG, og skal til samme steg som vi står i, vil det også være en tilbakeføring siden vi går UTGANG -> INNGANG
        var sammenlign = behandlingskontrollTjeneste.sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(), fraSteg, tilSteg);
        return sammenlign == 0 && BehandlingStegStatus.UTGANG.equals(behandling.getBehandlingStegStatus()) || sammenlign > 0;
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
    private void utledAksjonspunkterTilHøyreForStartpunkt(BehandlingskontrollKontekst kontekst, BehandlingStegType fomSteg, BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling behandling) {
        var resultater = FagsakYtelseTypeRef.Lookup.find(KontrollerFaktaInngangsVilkårUtleder.class, kontrollerFaktaTjenester, ref.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.fagsakYtelseType().getKode()))
            .utledAksjonspunkterFomSteg(ref, stp, fomSteg);
        var resultatDef = resultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toSet());
        var avbrytes = behandling.getÅpneAksjonspunkter().stream()
            .filter(ap -> !ap.erManueltOpprettet() && !ap.erAutopunkt() && !SPESIALHÅNDTERT_AKSJONSPUNKT.equals(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !erReturBeslutter(ref, ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !resultatDef.contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(ref.fagsakYtelseType(), ref.behandlingType(), fomSteg, ap.getAksjonspunktDefinisjon()))
            .toList();
        var opprettes = resultater.stream()
            .filter(ar -> !AksjonspunktStatus.UTFØRT.equals(behandling.getAksjonspunktMedDefinisjonOptional(ar.getAksjonspunktDefinisjon())
                .map(Aksjonspunkt::getStatus).orElse(AksjonspunktStatus.OPPRETTET)))
            .toList();
        if (!avbrytes.isEmpty()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), avbrytes);
        }
        if (!opprettes.isEmpty()) {
            behandlingskontrollTjeneste.lagreAksjonspunktResultat(kontekst, BehandlingStegType.KONTROLLER_FAKTA, opprettes);
        }
    }

    private boolean erReturBeslutter(BehandlingReferanse ref, AksjonspunktDefinisjon apDef) {
        return totrinnRepository.hentTotrinnaksjonspunktvurderinger(ref.behandlingId()).stream()
            .anyMatch(v -> v.isAktiv() && !v.isGodkjent() && v.getAksjonspunktDefinisjon().equals(apDef));

    }
}
