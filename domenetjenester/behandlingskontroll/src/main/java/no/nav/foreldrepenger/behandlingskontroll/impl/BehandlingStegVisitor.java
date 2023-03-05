package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegKonfigurasjon;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.StegProsesseringResultat;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * Visitor for å traversere ett behandlingssteg.
 */
class BehandlingStegVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(BehandlingStegVisitor.class);

    private final BehandlingRepository behandlingRepository;
    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingModell behandlingModell;
    private final BehandlingStegKonfigurasjon behandlingStegKonfigurasjon = new BehandlingStegKonfigurasjon(
            EnumSet.allOf(BehandlingStegStatus.class));

    private final BehandlingskontrollEventPubliserer eventPubliserer;

    private final Behandling behandling;

    private final AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    private final BehandlingStegModell stegModell;

    BehandlingStegVisitor(BehandlingskontrollServiceProvider serviceProvider, Behandling behandling,
            BehandlingStegModell stegModell, BehandlingskontrollKontekst kontekst) {

        this.stegModell = Objects.requireNonNull(stegModell, "BehandlingStegModell");
        this.behandling = Objects.requireNonNull(behandling, "behandling");
        this.behandlingModell = Objects.requireNonNull(stegModell.getBehandlingModell(), "BehandlingModell");
        this.kontekst = Objects.requireNonNull(kontekst, "kontekst");
        this.behandlingRepository = serviceProvider.getBehandlingRepository();
        this.aksjonspunktKontrollRepository = serviceProvider.getAksjonspunktKontrollRepository();

        this.eventPubliserer = Objects.requireNonNullElse(serviceProvider.getEventPubliserer(), BehandlingskontrollEventPubliserer.NULL_EVENT_PUB);
    }

    private static boolean erForskjellig(BehandlingStegStatus førsteStegStatus, BehandlingStegStatus nyStegStatus) {
        return !Objects.equals(nyStegStatus, førsteStegStatus);
    }

    protected BehandlingStegModell getStegModell() {
        return stegModell;
    }

    protected StegProsesseringResultat prosesser() {
        var resultat = prosesserSteg(stegModell, false);
        return resultat;
    }

    protected StegProsesseringResultat gjenoppta() {
        var resultat = prosesserSteg(stegModell, true);
        return resultat;
    }

    private StegProsesseringResultat prosesserSteg(BehandlingStegModell stegModell, boolean gjenoppta) {
        var steg = stegModell.getSteg();
        var stegType = stegModell.getBehandlingStegType();
        var apHåndterer = new AksjonspunktResultatOppretter(aksjonspunktKontrollRepository, behandling);

        // Sett riktig status for steget før det utføres
        var førStegStatus = behandling.getBehandlingStegStatus();
        LOG.info("Prosesserer steg={}, stegStatus={}", stegType, førStegStatus);
        // Vanlig prosessering skal ikke gjennomføres hvis steget VENTER.
        if (!gjenoppta && BehandlingStegStatus.VENTER.equals(førStegStatus)) {
            return StegProsesseringResultat.medMuligTransisjon(førStegStatus, BehandleStegResultat.settPåVent().getTransisjon());
        }
        var førStatus = behandling.getStatus();
        var førsteStegStatus = utledStegStatusFørUtføring(stegModell);
        oppdaterBehandlingStegStatus(behandling, stegType, førStegStatus, førsteStegStatus);

        // Utfør steg hvis tillatt av stegets før-status. Utled stegets nye status.
        StegProsesseringResultat stegResultat;
        List<Aksjonspunkt> funnetAksjonspunkter = new ArrayList<>();
        if (erIkkePåVent(behandling) && førsteStegStatus.kanUtføreSteg()) {
            BehandleStegResultat resultat;
            // Her kan man sjekke om tilstand venter og evt la gjenoppta-modus kalle vanlig
            // utfør
            if (gjenoppta) {
                resultat = steg.gjenopptaSteg(kontekst);
            } else {
                resultat = steg.utførSteg(kontekst);
            }

            funnetAksjonspunkter.addAll(apHåndterer.opprettAksjonspunkter(resultat.getAksjonspunktResultater(), stegType));
            var nyStegStatus = håndterResultatAvSteg(stegModell, resultat, behandling);
            stegResultat = StegProsesseringResultat.medMuligTransisjon(nyStegStatus, resultat.getTransisjon());
        } else if (BehandlingStegStatus.erVedUtgang(førsteStegStatus)) {
            var nyStegStatus = utledUtgangStegStatus(stegModell.getBehandlingStegType());
            stegResultat = StegProsesseringResultat.utenOverhopp(nyStegStatus);
        } else {
            stegResultat = StegProsesseringResultat.utenOverhopp(førsteStegStatus);
        }

        avsluttSteg(stegType, førStatus, førsteStegStatus, stegResultat, funnetAksjonspunkter);

        return stegResultat;
    }

    private void avsluttSteg(BehandlingStegType stegType, BehandlingStatus førStatus, BehandlingStegStatus førsteStegStatus,
            StegProsesseringResultat stegResultat,
            List<Aksjonspunkt> funnetAksjonspunkter) {

        LOG.info("Avslutter steg={}, transisjon={} og funnet aksjonspunkter={}", stegType, stegResultat,
                funnetAksjonspunkter.stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).collect(Collectors.toList()));

        var stegTilstandFør = behandling.getSisteBehandlingStegTilstand();

        // Sett riktig status for steget etter at det er utført. Lagre eventuelle
        // endringer fra steg på behandling
        guardAlleÅpneAksjonspunkterHarDefinertVurderingspunkt();
        oppdaterBehandlingStegStatus(behandling, stegType, førsteStegStatus, stegResultat.getNyStegStatus());

        // Publiser statusevent
        var etterStatus = behandling.getStatus();
        eventPubliserer.fireEvent(kontekst, førStatus, etterStatus);

        // Publiser transisjonsevent
        var transisjon = behandlingModell.finnTransisjon(stegResultat.getTransisjon());
        var tilSteg = finnFremoverhoppSteg(stegType, transisjon);
        eventPubliserer.fireEvent(opprettEvent(stegResultat, transisjon, stegTilstandFør.orElse(null), tilSteg));

        // Publiser event om endring i stegets tilstand
        var fraTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(stegTilstandFør);
        var tilTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getBehandlingStegTilstand());
        var behandlingStegTilstandEndringEvent = BehandlingModellImpl.nyBehandlingStegTilstandEndring(kontekst,
                fraTilstand, tilTilstand);
        eventPubliserer.fireEvent(behandlingStegTilstandEndringEvent);

        // Publiser de funnede aksjonspunktene
        if (!funnetAksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, funnetAksjonspunkter, stegType));
        }
    }

    private BehandlingTransisjonEvent opprettEvent(StegProsesseringResultat stegResultat, StegTransisjon transisjon,
            BehandlingStegTilstand fraTilstand, BehandlingStegType tilSteg) {

        return new BehandlingTransisjonEvent(kontekst, stegResultat.getTransisjon(), fraTilstand, tilSteg,
                transisjon.getMålstegHvisHopp().isPresent());
    }

    private BehandlingStegType finnFremoverhoppSteg(BehandlingStegType stegType, StegTransisjon transisjon) {
        BehandlingStegType tilSteg = null;
        if (transisjon.getMålstegHvisHopp().isPresent()) {
            var fraStegModell = behandlingModell.finnSteg(stegType);
            var tilStegModell = transisjon.nesteSteg(fraStegModell);
            tilSteg = tilStegModell != null ? tilStegModell.getBehandlingStegType() : null;
        }
        return tilSteg;
    }

    void markerOvergangTilNyttSteg(BehandlingStegType stegType, BehandlingStegTilstandSnapshot forrigeTilstand) {
        LOG.info("Markerer nytt steg som aktivt: {}", stegType);

        // Flytt aktivt steg til gjeldende steg hvis de ikke er like
        var sluttStatusForAndreSteg = behandlingStegKonfigurasjon.getUtført();
        settBehandlingStegSomGjeldende(stegType, sluttStatusForAndreSteg);

        fyrEventBehandlingStegOvergang(forrigeTilstand, BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getBehandlingStegTilstand()));
    }

    private boolean erIkkePåVent(Behandling behandling) {
        return !behandling.isBehandlingPåVent();
    }

    private void fyrEventBehandlingStegOvergang(BehandlingStegTilstandSnapshot forrigeTilstand, BehandlingStegTilstandSnapshot nyTilstand) {
        var event = BehandlingModellImpl.nyBehandlingStegOvergangEvent(behandlingModell, forrigeTilstand,
                nyTilstand, kontekst);

        eventPubliserer.fireEvent(event);
    }

    private void fyrEventBehandlingStegTilbakeføring(Optional<BehandlingStegTilstand> forrige, Optional<BehandlingStegTilstand> ny) {
        var event = BehandlingModellImpl.nyBehandlingStegOvergangEvent(
                behandlingModell, BehandlingModellImpl.tilBehandlingsStegSnapshot(forrige), BehandlingModellImpl.tilBehandlingsStegSnapshot(ny),
                kontekst);

        eventPubliserer.fireEvent(event);
    }

    private void oppdaterBehandlingStegStatus(Behandling behandling,
            BehandlingStegType stegType,
            BehandlingStegStatus førsteStegStatus,
            BehandlingStegStatus nyStegStatus) {
        var behandlingStegTilstand = behandling.getBehandlingStegTilstand(stegType);
        if (behandlingStegTilstand.isPresent()) {
            if (erForskjellig(førsteStegStatus, nyStegStatus)) {
                InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, stegType, nyStegStatus, BehandlingStegStatus.UTFØRT);
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
                eventPubliserer.fireEvent(kontekst, stegType, førsteStegStatus, nyStegStatus);
            }
        }
    }

    private BehandlingStegStatus utledStegStatusFørUtføring(BehandlingStegModell stegModell) {

        var nåBehandlingStegStatus = behandling.getBehandlingStegStatus();

        var stegType = stegModell.getBehandlingStegType();

        if (erForbiInngang(nåBehandlingStegStatus)) {
            // Hvis vi har kommet forbi INNGANG, så gå direkte videre til det gjeldende
            // statusen
            return nåBehandlingStegStatus;
        }
        var måHåndereAksjonspunktHer = behandling.getÅpneAksjonspunkter().stream()
                .anyMatch(ap -> skalHåndteresHer(stegType, ap, VurderingspunktType.INN));

        var nyStatus = måHåndereAksjonspunktHer ? behandlingStegKonfigurasjon.getInngang()
                : behandlingStegKonfigurasjon.getStartet();

        return nyStatus;
    }

    private boolean skalHåndteresHer(BehandlingStegType stegType, Aksjonspunkt ap, VurderingspunktType vurderingspunktType) {
        return ap.getAksjonspunktDefinisjon().getBehandlingSteg().equals(stegType)
                && ap.getAksjonspunktDefinisjon().getVurderingspunktType().equals(vurderingspunktType);
    }

    private boolean erForbiInngang(BehandlingStegStatus nåBehandlingStegStatus) {
        return (nåBehandlingStegStatus != null) && !Objects.equals(behandlingStegKonfigurasjon.getInngang(), nåBehandlingStegStatus);
    }

    /**
     * Returner ny status på pågående steg.
     */
    private BehandlingStegStatus håndterResultatAvSteg(BehandlingStegModell stegModell, BehandleStegResultat resultat, Behandling behandling) {

        var transisjonIdentifikator = resultat.getTransisjon();
        if (transisjonIdentifikator == null) {
            throw new IllegalArgumentException("Utvikler-feil: mangler transisjon");
        }

        var transisjon = behandlingModell.finnTransisjon(transisjonIdentifikator);

        if (FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT.getId().equals(transisjon.getId())) {
            // tilbakefør til tidligere steg basert på hvilke aksjonspunkter er åpne.
            var forrige = behandling.getSisteBehandlingStegTilstand();
            var behandlingStegStatus = håndterTilbakeføringTilTidligereSteg(behandling, stegModell.getBehandlingStegType());
            fyrEventBehandlingStegTilbakeføring(forrige, behandling.getSisteBehandlingStegTilstand());
            return behandlingStegStatus;
        }

        if (FellesTransisjoner.HENLAGT.getId().equals(transisjon.getId())) {
            return behandlingStegKonfigurasjon.getAvbrutt();
        }
        if (transisjon.getMålstegHvisHopp().isPresent()) {
            return behandlingStegKonfigurasjon.mapTilStatus(transisjon.getRetningForHopp());
        }
        if (FellesTransisjoner.UTFØRT.getId().equals(transisjon.getId())) {
            return utledUtgangStegStatus(stegModell.getBehandlingStegType());
        }
        if (FellesTransisjoner.STARTET.getId().equals(transisjon.getId())) {
            return behandlingStegKonfigurasjon.getStartet();
        }
        if (FellesTransisjoner.SETT_PÅ_VENT.getId().equals(transisjon.getId())) {
            return behandlingStegKonfigurasjon.getVenter();
        }
        throw new IllegalArgumentException("Utvikler-feil: ikke-håndtert transisjon " + transisjon.getId());
    }

    private BehandlingStegStatus utledUtgangStegStatus(BehandlingStegType behandlingStegType) {
        BehandlingStegStatus nyStegStatus;
        if (harÅpneAksjonspunkter(behandling, behandlingStegType)) {
            nyStegStatus = behandlingStegKonfigurasjon.getUtgang();
        } else {
            nyStegStatus = behandlingStegKonfigurasjon.getUtført();
        }
        return nyStegStatus;
    }

    private boolean harÅpneAksjonspunkter(Behandling behandling, BehandlingStegType behandlingStegType) {
        var måHåndereAksjonspunktHer = behandling.getÅpneAksjonspunkter()
                .stream()
                .anyMatch(ap -> skalHåndteresHer(behandlingStegType, ap, VurderingspunktType.UT));

        return måHåndereAksjonspunktHer;
    }

    private BehandlingStegStatus håndterTilbakeføringTilTidligereSteg(Behandling behandling, BehandlingStegType inneværendeBehandlingStegType) {
        var tilbakeførtStegStatus = behandlingStegKonfigurasjon.mapTilStatus(BehandlingStegResultat.TILBAKEFØRT);
        var inneværendeBehandlingStegStatus = behandling.getBehandlingStegStatus();

        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        if (!åpneAksjonspunkter.isEmpty()) {
            var aksjonspunkter = åpneAksjonspunkter.stream().map(a -> a.getAksjonspunktDefinisjon()).collect(Collectors.toList());
            var nesteBehandlingStegModell = behandlingModell.finnTidligsteStegForAksjonspunktDefinisjon(aksjonspunkter);
            var nesteStegStatus = behandlingModell.finnStegStatusFor(nesteBehandlingStegModell.getBehandlingStegType(),
                    aksjonspunkter);

            // oppdater inneværende steg
            oppdaterBehandlingStegStatus(behandling, inneværendeBehandlingStegType, inneværendeBehandlingStegStatus, tilbakeførtStegStatus);

            // oppdater nytt steg
            var nesteStegtype = nesteBehandlingStegModell.getBehandlingStegType();
            oppdaterBehandlingStegType(nesteStegtype, nesteStegStatus.isPresent() ? nesteStegStatus.get() : null, tilbakeførtStegStatus);
        }
        return tilbakeførtStegStatus;
    }

    private void oppdaterBehandlingStegType(BehandlingStegType nesteStegType, BehandlingStegStatus nesteStegStatus,
            BehandlingStegStatus sluttStegStatusVedOvergang) {
        Objects.requireNonNull(behandlingRepository, "behandlingRepository");

        var siste = behandling.getSisteBehandlingStegTilstand().map(BehandlingStegTilstand::getBehandlingSteg).orElse(null);

        if (!erSammeStegSomFør(nesteStegType, siste)) {

            // sett status for neste steg
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, nesteStegType, nesteStegStatus, sluttStegStatusVedOvergang);
        }
    }

    protected void settBehandlingStegSomGjeldende(BehandlingStegType nesteStegType, BehandlingStegStatus sluttStegStatusVedOvergang) {
        var førStatus = behandling.getStatus();
        oppdaterBehandlingStegType(nesteStegType, null, sluttStegStatusVedOvergang);
        if (!Objects.equals(førStatus, behandling.getStatus())) {
            eventPubliserer.fireEvent(kontekst, førStatus, behandling.getStatus());
        }
    }

    private boolean erSammeStegSomFør(BehandlingStegType stegType, BehandlingStegType nåværendeBehandlingSteg) {
        return Objects.equals(nåværendeBehandlingSteg, stegType);
    }

    /**
     * TODO (FC: Trengs denne lenger? Aksjonspunkt har en not-null relasjon til
     * Vurderingspunkt.
     * <p>
     * Verifiser at alle åpne aksjonspunkter har et definert vurderingspunkt i
     * gjenværende steg hvor de må behandles. Sikrer at ikke abstraktpunkt
     * identifiseres ETTER at de skal være håndtert.
     */
    private void guardAlleÅpneAksjonspunkterHarDefinertVurderingspunkt() {
        var aktivtBehandlingSteg = behandling.getAktivtBehandlingSteg();

        List<Aksjonspunkt> gjenværendeÅpneAksjonspunkt = new ArrayList<>(behandling.getÅpneAksjonspunkter());

        // TODO (FC): Denne bør håndteres med event ved overgang
        behandlingModell.hvertStegFraOgMed(aktivtBehandlingSteg)
                .forEach(bsm -> {
                    filterVekkAksjonspunktHåndtertAvFremtidigVurderingspunkt(bsm, gjenværendeÅpneAksjonspunkt);
                });

        if (!gjenværendeÅpneAksjonspunkt.isEmpty()) {
            /*
             * TODO (FC): Lagre og sett behandling på vent i stedet for å kaste exception?
             * Exception mest nyttig i test og utvikling, men i prod bør heller sette
             * behandling til side hvis det skulle være så galt at vurderingspunkt ikke er
             * definert for et identifisert abstraktpunkt.
             */
            throw new IllegalStateException(
                    "Utvikler-feil: Det er definert aksjonspunkt [" + gjenværendeÅpneAksjonspunkt + "] som ikke er håndtert av noe steg"
                            + (aktivtBehandlingSteg == null ? " i sekvensen " : " fra og med: " + aktivtBehandlingSteg));
        }
    }

    private void filterVekkAksjonspunktHåndtertAvFremtidigVurderingspunkt(BehandlingStegModell bsm, List<Aksjonspunkt> åpneAksjonspunkter) {
        var stegType = bsm.getBehandlingStegType();
        var inngangKriterier = stegType.getAksjonspunktDefinisjonerInngang();
        var utgangKriterier = stegType.getAksjonspunktDefinisjonerUtgang();
        åpneAksjonspunkter.removeIf(elem -> {
            var elemAksDef = elem.getAksjonspunktDefinisjon();
            return elem.erÅpentAksjonspunkt() && (inngangKriterier.contains(elemAksDef) || utgangKriterier.contains(elemAksDef));
        });
    }

}
