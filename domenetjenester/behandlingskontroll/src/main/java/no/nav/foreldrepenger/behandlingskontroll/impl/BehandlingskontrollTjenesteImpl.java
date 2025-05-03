package no.nav.foreldrepenger.behandlingskontroll.impl;

import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellVisitor;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegUtfall;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.exception.TekniskException;

/**
 * ALLE ENDRINGER I DENNE KLASSEN SKAL KLARERES OG KODE-REVIEWES MED ANSVARLIG
 * APPLIKASJONSARKITEKT (SE UTVIKLERHÅNDBOK).
 */
@RequestScoped // må være RequestScoped sålenge ikke nøstet prosessering støttes.
public class BehandlingskontrollTjenesteImpl implements BehandlingskontrollTjeneste {

    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private BehandlingskontrollEventPubliserer eventPubliserer;

    /**
     * Sjekker om vi allerede kjører Behandlingskontroll, og aborter forsøk på
     * nøsting av kall (støttes ikke p.t.).
     * <p>
     * Funker sålenge denne tjenesten er en {@link RequestScoped} bean.
     */
    private AtomicBoolean nøstetProsseringGuard = new AtomicBoolean();
    private BehandlingskontrollServiceProvider serviceProvider;

    BehandlingskontrollTjenesteImpl() {
        // for CDI proxy
    }

    /**
     * SE KOMMENTAR ØVERST
     */
    @Inject
    public BehandlingskontrollTjenesteImpl(BehandlingskontrollServiceProvider serviceProvider) {

        this.serviceProvider = serviceProvider;
        this.behandlingRepository = serviceProvider.getBehandlingRepository();
        this.behandlingModellRepository = serviceProvider.getBehandlingModellRepository();
        this.aksjonspunktKontrollRepository = serviceProvider.getAksjonspunktKontrollRepository();
        this.eventPubliserer = serviceProvider.getEventPubliserer();
    }

    @Override
    public void prosesserBehandling(BehandlingskontrollKontekst kontekst) {
        var behandling = hentBehandling(kontekst);
        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            return;
        }
        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());
        BehandlingModellVisitor stegVisitor = new TekniskBehandlingStegVisitor(serviceProvider, kontekst);

        prosesserBehandling(kontekst, modell, stegVisitor);
    }

    @Override
    public void prosesserBehandlingGjenopptaHvisStegVenter(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType) {
        var behandling = hentBehandling(kontekst);
        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            return;
        }
        var tilstand = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshotHvisSteg(behandling, behandlingStegType);
        if (tilstand != null && BehandlingStegStatus.VENTER.equals(tilstand.status())) {
            var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());
            BehandlingModellVisitor stegVisitor = new TekniskBehandlingStegVenterVisitor(serviceProvider, kontekst);

            prosesserBehandling(kontekst, modell, stegVisitor);
        }
    }

    private BehandlingStegUtfall prosesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell, BehandlingModellVisitor visitor) {

        validerOgFlaggStartetProsessering();
        BehandlingStegUtfall behandlingStegUtfall;
        try {
            fyrEventBehandlingskontrollStartet(kontekst);
            behandlingStegUtfall = doProsesserBehandling(kontekst, modell, visitor);
            fyrEventBehandlingskontrollStoppet(kontekst, behandlingStegUtfall);
        } catch (RuntimeException e) {
            fyrEventBehandlingskontrollException(kontekst, e);
            throw e;
        } finally {
            ferdigProsessering();
        }
        return behandlingStegUtfall;
    }

    @Override
    public void behandlingTilbakeføringTilTidligsteAksjonspunkt(BehandlingskontrollKontekst kontekst,
            Collection<AksjonspunktDefinisjon> oppdaterteAksjonspunkter) {

        if (oppdaterteAksjonspunkter == null || oppdaterteAksjonspunkter.isEmpty()) {
            return;
        }

        var behandlingId = kontekst.getBehandlingId();
        var behandling = serviceProvider.hentBehandling(behandlingId);

        var stegType = behandling.getAktivtBehandlingSteg();

        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doTilbakeføringTilTidligsteAksjonspunkt(kontekst, behandling, stegType, modell, oppdaterteAksjonspunkter);
        } finally {
            ferdigProsessering();
        }

    }

    @Override
    public boolean behandlingTilbakeføringHvisTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
            BehandlingStegType tidligereStegType) {
        if (!erSenereSteg(kontekst, tidligereStegType)) {
            behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tidligereStegType);
            return true;
        }
        return false;
    }

    private boolean erSenereSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType tidligereStegType) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        return sammenlignRekkefølge(kontekst.getYtelseType(), kontekst.getBehandlingType(),
                behandling.getAktivtBehandlingSteg(), tidligereStegType) < 0;
    }

    @Override
    public void behandlingTilbakeføringTilTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
            BehandlingStegType tidligereStegType) {

        var startStatusForNyttSteg = BehandlingStegStatus.INNGANG;
        var behandlingId = kontekst.getBehandlingId();
        var behandling = serviceProvider.hentBehandling(behandlingId);

        var stegType = behandling.getAktivtBehandlingSteg();

        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doTilbakeføringTilTidligereBehandlngSteg(kontekst, behandling, modell, tidligereStegType, stegType, startStatusForNyttSteg);
        } finally {
            ferdigProsessering();
        }
    }

    @Override
    public int sammenlignRekkefølge(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB) {
        var modell = getModell(behandlingType, ytelseType);
        return modell.erStegAFørStegB(stegA, stegB) ? -1
                : modell.erStegAFørStegB(stegB, stegA) ? 1
                        : 0;
    }

    @Override
    public boolean erStegPassert(Behandling behandling, BehandlingStegType behandlingSteg) {
        return sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
                behandling.getAktivtBehandlingSteg(), behandlingSteg) > 0;
    }

    @Override
    public boolean erIStegEllerSenereSteg(Behandling behandling, BehandlingStegType behandlingSteg) {
        return sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
            behandling.getAktivtBehandlingSteg(), behandlingSteg) >= 0;
    }

    @Override
    public void behandlingFramføringTilSenereBehandlingSteg(BehandlingskontrollKontekst kontekst,
            BehandlingStegType senereSteg) {

        var statusInngang = BehandlingStegStatus.INNGANG;
        var behandlingId = kontekst.getBehandlingId();
        var behandling = serviceProvider.hentBehandling(behandlingId);

        var inneværendeSteg = behandling.getAktivtBehandlingSteg();

        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doFramføringTilSenereBehandlingSteg(kontekst, senereSteg, statusInngang, behandling, inneværendeSteg, modell);
        } finally {
            ferdigProsessering();
        }
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        // først lås
        var lås = serviceProvider.taLås(behandling.getId());

        // så les
        return new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(), lås,
            behandling.getFagsakYtelseType(), behandling.getType());
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling, BehandlingLås skriveLås) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(skriveLås, "lås");

        // så les
        return new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(), skriveLås,
            behandling.getFagsakYtelseType(), behandling.getType());
    }

    void aksjonspunkterEndretStatus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter) {
        // handlinger som skal skje når funnet
        if (!aksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, aksjonspunkter, behandlingStegType));
        }
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunkter) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> nyeAksjonspunkt = new ArrayList<>();
        forberedeOpprettAutopunkt(behandling, aksjonspunkter);
        aksjonspunkter.forEach(apdef -> nyeAksjonspunkt.add(aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, apdef)));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, null, nyeAksjonspunkt);
        return nyeAksjonspunkt;
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktDefinisjon> aksjonspunkter) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        forberedeOpprettAutopunkt(behandling, aksjonspunkter);
        List<Aksjonspunkt> nyeAksjonspunkt = new ArrayList<>();
        aksjonspunkter.forEach(apdef -> nyeAksjonspunkt.add(aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, apdef, behandlingStegType)));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, nyeAksjonspunkt);
        return nyeAksjonspunkt;
    }

    private void forberedeOpprettAutopunkt(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        aksjonspunktDefinisjoner.stream()
            .filter(apd -> AksjonspunktType.AUTOPUNKT.equals(apd.getAksjonspunktType()))
            .findFirst()
            .ifPresent(apd -> aksjonspunktKontrollRepository.forberedSettPåVentMedAutopunkt(behandling, apd));
    }

    @Override
    public void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> utførte = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erUtført()).forEach(ap -> {
            aksjonspunktKontrollRepository.setTilUtført(ap, null);
            utførte.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, utførte);
    }

    @Override
    public void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            Aksjonspunkt aksjonspunkt, String begrunnelse) {
        Objects.requireNonNull(aksjonspunkt);
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> utførte = new ArrayList<>();

        if (!aksjonspunkt.erUtført() || !Objects.equals(aksjonspunkt.getBegrunnelse(), begrunnelse)) {
            aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, begrunnelse);
            utførte.add(aksjonspunkt);
        }

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, utførte);
    }

    @Override
    public void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        lagreAksjonspunkterAvbrutt(kontekst, behandling, behandlingStegType, aksjonspunkter);
    }

    private void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                            BehandlingStegType behandlingStegType, List<Aksjonspunkt> aksjonspunkter) {

        List<Aksjonspunkt> avbrutte = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erAvbrutt()).forEach(ap -> {
            aksjonspunktKontrollRepository.setTilAvbrutt(ap);
            avbrutte.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, avbrutte);
    }

    @Override
    public void lagreAksjonspunkterReåpnet(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, boolean beholdToTrinnVurdering,
            boolean setTotrinn) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> reåpnet = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erOpprettet()).forEach(ap -> {
            if (beholdToTrinnVurdering) {
                aksjonspunktKontrollRepository.setReåpnet(ap);
            } else {
                aksjonspunktKontrollRepository.setReåpnetMedTotrinn(ap, setTotrinn);
            }
            reåpnet.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, null, reåpnet);
    }

    @Override
    public void lagreAksjonspunktResultat(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktResultat> aksjonspunktResultater) {
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        var apHåndterer = new AksjonspunktResultatOppretter(aksjonspunktKontrollRepository, behandling);
        var endret = apHåndterer.opprettAksjonspunkter(aksjonspunktResultater, behandlingStegType);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, endret);
    }

    @Override
    public void setAksjonspunktToTrinn(BehandlingskontrollKontekst kontekst, Aksjonspunkt aksjonspunkt, boolean totrinn) {
        if (aksjonspunkt.isToTrinnsBehandling() == totrinn) {
            return;
        }
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        if (!aksjonspunkt.erÅpentAksjonspunkt()) {
            aksjonspunktKontrollRepository.setReåpnet(aksjonspunkt);
        }
        aksjonspunktKontrollRepository.setToTrinnsBehandlingKreves(aksjonspunkt, totrinn);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, null, List.of(aksjonspunkt));
    }

    @Override
    public void opprettBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        var fagsakLås = serviceProvider.taFagsakLås(behandling.getFagsakId());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        serviceProvider.oppdaterLåsVersjon(fagsakLås);
        eventPubliserer.fireEvent(kontekst, null, behandling.getStatus());
    }

    @Override
    public Behandling opprettNyBehandling(Fagsak fagsak, BehandlingType behandlingType, Consumer<Behandling> behandlingOppdaterer) {
        var behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        var nyBehandling = behandlingBuilder.build();
        behandlingOppdaterer.accept(nyBehandling);

        var kontekst = this.initBehandlingskontroll(nyBehandling);
        this.opprettBehandling(kontekst, nyBehandling);
        return nyBehandling;
    }

    void avsluttBehandling(BehandlingskontrollKontekst kontekst) {
        var behandling = hentBehandling(kontekst);
        var gammelStatus = behandling.getStatus();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        eventPubliserer.fireEvent(kontekst, gammelStatus, behandling.getStatus());

    }

    @Override
    public Aksjonspunkt settBehandlingPåVentUtenSteg(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjonIn,
            LocalDateTime fristTid, Venteårsak venteårsak) {
        return settBehandlingPåVent(behandling, aksjonspunktDefinisjonIn, null, fristTid, venteårsak);
    }

    @Override
    public Aksjonspunkt settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjonIn,
            BehandlingStegType stegType, LocalDateTime fristTid, Venteårsak venteårsak) {
        var kontekst = initBehandlingskontroll(behandling);
        aksjonspunktKontrollRepository.forberedSettPåVentMedAutopunkt(behandling, aksjonspunktDefinisjonIn);
        var aksjonspunkt = aksjonspunktKontrollRepository.settBehandlingPåVent(behandling, aksjonspunktDefinisjonIn, stegType, fristTid,
                venteårsak);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        if (aksjonspunkt != null) {
            aksjonspunkterEndretStatus(kontekst, aksjonspunkt.getBehandlingStegFunnet(), singletonList(aksjonspunkt));
        }
        return aksjonspunkt;
    }

    @Override
    public void settAutopunktTilUtført(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingskontrollKontekst kontekst) {
        var åpneAksjonspunktAvDef = behandling.getÅpneAksjonspunkter(List.of(aksjonspunktDefinisjon));
        lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), åpneAksjonspunktAvDef);
    }

    private void settAutopunkterTilUtført(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        var åpneAutopunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT);
        lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), åpneAutopunkter);
    }

    private void settAutopunkterTilAvbrutt(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        var åpneAutopunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT);
        lagreAksjonspunkterAvbrutt(kontekst, behandling, behandling.getAktivtBehandlingSteg(), åpneAutopunkter);
    }

    @Override
    public void taBehandlingAvVentSetAlleAutopunktUtført(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        doForberedGjenopptak(behandling, kontekst, false);
    }

    @Override
    public void taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        doForberedGjenopptak(behandling, kontekst, true);
    }

    private void doForberedGjenopptak(Behandling behandling, BehandlingskontrollKontekst kontekst, boolean erHenleggelse) {
        var aksjonspunkterSomMedførerTilbakehopp = behandling.getÅpneAksjonspunkter().stream()
                .filter(a -> a.getAksjonspunktDefinisjon().tilbakehoppVedGjenopptakelse())
                .toList();

        if (aksjonspunkterSomMedførerTilbakehopp.size() > 1) {
            throw new TekniskException("FP-105126",
                String.format("BehandlingId %s har flere enn et aksjonspunkt, hvor aksjonspunktet fører til tilbakehopp ved gjenopptakelse. Kan ikke gjenopptas.", behandling.getId()));
        }
        if (erHenleggelse) {
            settAutopunkterTilAvbrutt(kontekst, behandling);
        } else {
            settAutopunkterTilUtført(kontekst, behandling);
        }
        if (aksjonspunkterSomMedførerTilbakehopp.size() == 1) {
            var ap = aksjonspunkterSomMedførerTilbakehopp.getFirst();
            var behandlingStegFunnet = ap.getBehandlingStegFunnet();
            behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, behandlingStegFunnet);
            // I tilfelle tilbakehopp reåpner autopunkt - de skal reutledes av steget.
            settAutopunkterTilUtført(kontekst, behandling);
        }
    }

    @Override
    public void henleggBehandling(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak) {
        // valider invarianter
        Objects.requireNonNull(årsak, "årsak");
        var behandling = hentBehandling(kontekst);

        var stegTilstandFør = doHenleggBehandling(kontekst, behandling, årsak);
        var sluttSteg = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType()).getSluttSteg().getBehandlingStegType();

        // FIXME (MAUR): Bør løses via FellesTransisjoner og unngå hardkoding av
        // BehandlingStegType her.
        // må fremoverføres for å trigge riktig events for opprydding
        behandlingFramføringTilSenereBehandlingSteg(kontekst, sluttSteg);

        publiserFremhoppTransisjonHenleggelse(kontekst, stegTilstandFør, sluttSteg);

        // sett Avsluttet og fyr status
        avsluttBehandling(kontekst);
    }

    @Override
    public void henleggBehandlingFraSteg(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak) {
        // valider invarianter
        Objects.requireNonNull(årsak, "årsak");
        var behandling = hentBehandling(kontekst);

        var stegTilstandFør = doHenleggBehandling(kontekst, behandling, årsak);
        var sluttSteg = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType()).getSluttSteg().getBehandlingStegType();

        // TODO håndter henleggelse fra tidlig steg. Nå avbrytes steget og behandlingen
        // framoverføres ikke (ok?).
        // OBS mulig rekursiv prosessering kan oppstå (evt må BehKtrl framføre til ived
        // og fortsette)
        publiserFremhoppTransisjonHenleggelse(kontekst, stegTilstandFør, sluttSteg);

        // sett Avsluttet og fyr status
        avsluttBehandling(kontekst);
    }

    private BehandlingStegTilstandSnapshot doHenleggBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingResultatType årsak) {

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new TekniskException( "FP-143308", String.format("BehandlingId %s er allerede avsluttet, kan ikke henlegges",
                behandling.getId()));
        }

        // sett årsak
        var eksisterende = behandling.getBehandlingsresultat();
        if (eksisterende == null) {
            Behandlingsresultat.builder().medBehandlingResultatType(årsak).buildFor(behandling);
        } else {
            Behandlingsresultat.builderEndreEksisterende(eksisterende).medBehandlingResultatType(årsak);
        }

        BehandlingStegType behandlingStegType = null;
        var stegTilstandFør = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
        if (stegTilstandFør != null) {
            behandlingStegType = stegTilstandFør.steg();
        }

        // avbryt aksjonspunkt
        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        åpneAksjonspunkter.forEach(aksjonspunktKontrollRepository::setTilAvbrutt);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, åpneAksjonspunkter, behandlingStegType));
        return stegTilstandFør;
    }

    private void publiserFremhoppTransisjonHenleggelse(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot stegTilstandFør,
                                                       BehandlingStegType stegEtter) {
        // Publiser transisjonsevent (eventobserver(e) håndterer tilhørende transisjonsregler)
        var event = new BehandlingTransisjonEvent(kontekst, new Transisjon(StegTransisjon.HOPPOVER, stegEtter), stegTilstandFør);
        eventPubliserer.fireEvent(event);
    }

    @Override
    public boolean skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType,
            BehandlingStegType behandlingSteg, AksjonspunktDefinisjon apDef) {

        var modell = getModell(behandlingType, ytelseType);
        var apLøsesteg = Optional.ofNullable(modell
                .finnTidligsteStegForAksjonspunktDefinisjon(singletonList(apDef)))
                .map(BehandlingStegModell::getBehandlingStegType)
                .orElse(null);
        if (apLøsesteg == null) {
            // AksjonspunktDefinisjon finnes ikke på stegene til denne behandlingstypen. Ap
            // kan derfor ikke løses.
            return false;
        }

        return behandlingSteg.equals(apLøsesteg) || modell.erStegAFørStegB(behandlingSteg, apLøsesteg);
    }

    // TODO: (PK-49128) Midlertidig løsning for å filtrere aksjonspunkter til høyre
    // for steg i hendelsemodul
    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerFraOgMed(BehandlingskontrollKontekst kontekst, BehandlingStegType steg, boolean medInngangOgså) {
        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());
        return modell.finnAksjonspunktDefinisjonerFraOgMed(steg, medInngangOgså);
    }

    protected BehandlingStegUtfall doProsesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell,
            BehandlingModellVisitor visitor) {

        var behandling = hentBehandling(kontekst);

        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke prosessere avsluttet behandling");
        }

        var startSteg = behandling.getAktivtBehandlingSteg();
        var behandlingStegUtfall = modell.prosesserFra(startSteg, visitor);

        if (behandlingStegUtfall == null) {
            avsluttBehandling(kontekst);
        }
        return behandlingStegUtfall;
    }

    protected void doFramføringTilSenereBehandlingSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType senereSteg, final BehandlingStegStatus startStatusForNyttSteg,
            Behandling behandling, BehandlingStegType inneværendeSteg, BehandlingModell modell) {
        if (!erSenereSteg(modell, inneværendeSteg, senereSteg)) {
            throw new IllegalStateException(
                    "Kan ikke angi steg [" + senereSteg + "] som er før eller lik inneværende steg [" + inneværendeSteg + "]" + "for behandlingId "
                            + behandling.getId());
        }
        oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(kontekst, behandling, senereSteg, startStatusForNyttSteg,
                BehandlingStegStatus.AVBRUTT);
    }

    protected void doTilbakeføringTilTidligereBehandlngSteg(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingModell modell,
            BehandlingStegType tidligereStegType, BehandlingStegType stegType,
            final BehandlingStegStatus startStatusForNyttSteg) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException(
                    "Kan ikke tilbakeføre fra [" + stegType + "]");
        }
        if (!erLikEllerTidligereSteg(modell, stegType, tidligereStegType)) {
            throw new IllegalStateException(
                "Kan ikke angi steg [" + tidligereStegType + "] som er etter [" + stegType + "]" + "for behandlingId " + behandling.getId());
        }
        if (tidligereStegType.equals(stegType) && behandling.getBehandlingStegStatus() != null && behandling.getBehandlingStegStatus()
            .erVedInngang()) {
            // Her står man allerede på steget man skal tilbakeføres, på inngang -> ingen
            // tilbakeføring gjennomføres.
            return;
        }
        oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(kontekst, behandling, tidligereStegType, startStatusForNyttSteg,
                BehandlingStegStatus.TILBAKEFØRT);
    }

    protected void doTilbakeføringTilTidligsteAksjonspunkt(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType stegType, BehandlingModell modell,
            Collection<AksjonspunktDefinisjon> oppdaterteAksjonspunkter) {
        Consumer<BehandlingStegType> oppdaterBehandlingStegStatus = bst -> {
            var stegStatus = modell.finnStegStatusFor(bst, oppdaterteAksjonspunkter);
            if (stegStatus.isPresent() && !(Objects.equals(stegStatus.get(), behandling.getBehandlingStegStatus()) && Objects.equals(bst,
                behandling.getAktivtBehandlingSteg()))) {
                // er på starten av steg med endret aksjonspunkt. Ikke kjør steget her, kun
                // oppdater
                oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(kontekst, behandling, bst, stegStatus.get(),
                    BehandlingStegStatus.TILBAKEFØRT);
            }
        };

        var førsteAksjonspunktSteg = modell
                .finnTidligsteStegForAksjonspunktDefinisjon(oppdaterteAksjonspunkter);

        var aksjonspunktStegType = førsteAksjonspunktSteg == null ? null
                : førsteAksjonspunktSteg.getBehandlingStegType();

        if (Objects.equals(stegType, aksjonspunktStegType)) {
            // samme steg, kan ha ny BehandlingStegStatus
            oppdaterBehandlingStegStatus.accept(stegType);
        } else {
            // tilbakeføring til tidligere steg
            var revidertStegType = modell.finnFørsteSteg(stegType, aksjonspunktStegType);
            oppdaterBehandlingStegStatus.accept(revidertStegType.getBehandlingStegType());
        }
    }

    protected void fireEventBehandlingStegOvergang(BehandlingskontrollKontekst kontekst,
                                                   BehandlingStegTilstandSnapshot forrigeTilstand, BehandlingStegTilstandSnapshot nyTilstand) {
        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());
        var event = BehandlingStegOvergangEvent.nyBehandlingStegOvergangEvent(kontekst, modell, forrigeTilstand, nyTilstand);
        getEventPubliserer().fireEvent(event);
    }

    protected void oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(BehandlingskontrollKontekst kontekst,
                                                                                           Behandling behandling,
                                                                                           BehandlingStegType revidertStegType,
                                                                                           BehandlingStegStatus behandlingStegStatus,
                                                                                           BehandlingStegStatus sluttStatusForAndreÅpneSteg) {
        // Eksisterende tilstand
        var statusFør = behandling.getStatus();
        var fraTilstand = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);

        // Oppdater behandling og lagre
        InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, revidertStegType, behandlingStegStatus, sluttStatusForAndreÅpneSteg);
        var skriveLås = kontekst.getSkriveLås();
        behandlingRepository.lagre(behandling, skriveLås);

        // Publiser oppdatering
        var statusEtter = behandling.getStatus();
        var tilTilstand = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
        fireEventBehandlingStegOvergang(kontekst, fraTilstand, tilTilstand);
        eventPubliserer.fireEvent(kontekst, statusFør, statusEtter);
    }

    protected Behandling hentBehandling(BehandlingskontrollKontekst kontekst) {
        Objects.requireNonNull(kontekst, "kontekst");
        var behandlingId = kontekst.getBehandlingId();
        return serviceProvider.hentBehandling(behandlingId);
    }

    protected BehandlingskontrollEventPubliserer getEventPubliserer() {
        return eventPubliserer;
    }

    protected BehandlingModell getModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return behandlingModellRepository.getModell(behandlingType, ytelseType);
    }

    private void fyrEventBehandlingskontrollException(BehandlingskontrollKontekst kontekst, RuntimeException e) {
        var behandling = hentBehandling(kontekst);
        var snapshot = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
        var stoppetEvent = new BehandlingskontrollEvent.ExceptionEvent(kontekst, snapshot, e);
        eventPubliserer.fireEvent(stoppetEvent);
    }

    private void fyrEventBehandlingskontrollStoppet(BehandlingskontrollKontekst kontekst, BehandlingStegUtfall stegUtfall) {
        var behandling = hentBehandling(kontekst);
        BehandlingskontrollEvent event;
        if (behandling.erAvsluttet()) {
            var snapshot = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
            event = new BehandlingskontrollEvent.AvsluttetEvent(kontekst, snapshot);
        } else if (stegUtfall == null) {
            var snapshot = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshotSiste(behandling);
            event = new BehandlingskontrollEvent.StoppetEvent(kontekst, snapshot);
        } else {
            var snapshot = new BehandlingStegTilstandSnapshot(stegUtfall.behandlingStegType(), stegUtfall.resultat());
            event = new BehandlingskontrollEvent.StoppetEvent(kontekst, snapshot);
        }
        eventPubliserer.fireEvent(event);
    }

    private void fyrEventBehandlingskontrollStartet(BehandlingskontrollKontekst kontekst) {
        var behandling = hentBehandling(kontekst);
        var snapshot = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
        var startetEvent = new BehandlingskontrollEvent.StartetEvent(kontekst, snapshot);
        eventPubliserer.fireEvent(startetEvent);
    }

    @Override
    public void fremoverTransisjon(BehandlingStegType målSteg, BehandlingskontrollKontekst kontekst) {
        if (målSteg == null) {
            throw new IllegalArgumentException("Må oppgit målsteg");
        }
        var transisjon = new Transisjon(StegTransisjon.HOPPOVER, målSteg);
        var behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        var stegTilstandFør = BehandlingStegTilstandSnapshot.tilBehandlingsStegSnapshot(behandling);
        var fraSteg = behandling.getAktivtBehandlingSteg();

        // Flytt behandlingssteg-peker fremover
        var modell = getModell(kontekst.getBehandlingType(), kontekst.getYtelseType());
        var fraStegModell = modell.finnSteg(fraSteg);
        var tilStegModell = transisjon.nesteSteg(fraStegModell);
        var tilSteg = Optional.ofNullable(tilStegModell).map(BehandlingStegModell::getBehandlingStegType).orElse(null);
        if (!Objects.equals(transisjon.målSteg(), tilSteg)) {
            throw new IllegalArgumentException("Utviklerfeil mismatch mellom målsteg " + transisjon.målSteg() + " og utledet tilSteg " + tilSteg);
        }

        behandlingFramføringTilSenereBehandlingSteg(kontekst, tilSteg);

        // Publiser tranisjonsevent (eventobserver(e) håndterer tilhørende
        // tranisjonsregler)
        var event = new BehandlingTransisjonEvent(kontekst, transisjon, stegTilstandFør);
        eventPubliserer.fireEvent(event);
    }

    @Override
    public boolean inneholderSteg(Behandling behandling, BehandlingStegType behandlingStegType) {
        var modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return modell.hvertSteg()
                .anyMatch(steg -> steg.getBehandlingStegType().equals(behandlingStegType));
    }

    private boolean erSenereSteg(BehandlingModell modell, BehandlingStegType inneværendeSteg,
            BehandlingStegType forventetSenereSteg) {
        return modell.erStegAFørStegB(inneværendeSteg, forventetSenereSteg);
    }

    private boolean erLikEllerTidligereSteg(BehandlingModell modell, BehandlingStegType inneværendeSteg,
            BehandlingStegType forventetTidligereSteg) {
        // TODO (BIXBITE) skal fjernes når innlegging av papirsøknad er inn i et steg
        if (inneværendeSteg == null) {
            return false;
        }
        if (Objects.equals(inneværendeSteg, forventetTidligereSteg)) {
            return true;
        }
        var førsteSteg = modell.finnFørsteSteg(inneværendeSteg, forventetTidligereSteg).getBehandlingStegType();
        return Objects.equals(forventetTidligereSteg, førsteSteg);
    }

    private void validerOgFlaggStartetProsessering() {
        if (nøstetProsseringGuard.get()) {
            throw new IllegalStateException("Støtter ikke nøstet prosessering i " + getClass().getSimpleName());
        }
        nøstetProsseringGuard.set(true);
    }

    private void ferdigProsessering() {
        nøstetProsseringGuard.set(false);
    }
}
