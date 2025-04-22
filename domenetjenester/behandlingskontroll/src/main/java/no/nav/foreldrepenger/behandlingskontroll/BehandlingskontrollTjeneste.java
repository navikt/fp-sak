package no.nav.foreldrepenger.behandlingskontroll;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public interface BehandlingskontrollTjeneste {

    /**
     * Initier ny Behandlingskontroll, oppretter kontekst som brukes til sikre at
     * parallle behandlinger og kjøringer går i tur og orden. Dette skjer gjennom å
     * opprette en {@link BehandlingLås} som legges ved ved lagring.
     *
     * @param behandlingId - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(Long behandlingId);

    /**
     * Initierer ny behandlingskontroll for en ny behandling, som ikke er lagret i
     * behandlingsRepository og derfor ikke har fått tildelt behandlingId
     *
     * @param behandling - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling);

    /**
     * Initier ny Behandlingskontroll, oppretter kontekst som brukes til sikre at
     * parallle behandlinger og kjøringer går i tur og orden. Dette skjer gjennom å
     * opprette en {@link BehandlingLås} som legges ved ved lagring.
     *
     * @param behandlingUuid - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(UUID behandlingUuid);

    /**
     * Prosesser behandling fra dit den sist har kommet. Avhengig av vurderingspunkt
     * (inngang- og utgang-kriterier) vil steget kjøres på nytt.
     *
     * @param kontekst - kontekst for prosessering. Opprettes gjennom
     *                 {@link #initBehandlingskontroll(Long)}
     */
    void prosesserBehandling(BehandlingskontrollKontekst kontekst);

    /**
     * Prosesser forutsatt behandling er i angitt steg og status venter og steget.
     * Vil kalle gjenopptaSteg for angitt steg, senere vanlig framdrift
     *
     * @param kontekst           - kontekst for prosessering. Opprettes gjennom
     *                           {@link #initBehandlingskontroll(Long)}
     * @param behandlingStegType - precondition steg
     */
    void prosesserBehandlingGjenopptaHvisStegVenter(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType);

    /**
     * Prosesser behandling enten fra akitvt steg eller steg angitt av
     * aksjonspunktDefinsjonerKoder dersom noen er eldre
     *
     * @see #prosesserBehandling(BehandlingskontrollKontekst)
     */
    void behandlingTilbakeføringTilTidligsteAksjonspunkt(BehandlingskontrollKontekst kontekst, Collection<AksjonspunktDefinisjon> endredeAksjonspunkt);

    boolean behandlingTilbakeføringHvisTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
            BehandlingStegType tidligereStegType);

    /**
     * FLytt prosesen til et tidlligere steg.
     *
     * @throws IllegalStateException dersom tidligereSteg er etter aktivt steg i
     *                               behandlingen (i følge BehandlingsModell for
     *                               gitt BehandlingType).
     */
    void behandlingTilbakeføringTilTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType tidligereStegType);

    /**
     * Flytt prosessen til senere steg. Hopper over eventuelt mellomliggende steg.
     *
     * Alle mellomliggende steg og aksjonspunkt vil bli satt til AVBRUTT når dette
     * skjer. Prosessen vil ikke kjøres. Det gjelder også dersom neste steg er det
     * definerte neste steget i prosessen (som normalt skulle blitt kalt gjennom
     * {@link #prosesserBehandling(BehandlingskontrollKontekst)}.
     *
     * @throws IllegalStateException dersom senereSteg er før eller lik aktivt steg
     *                               i behandlingen (i følge BehandlingsModell for
     *                               gitt BehandlingType).
     */
    void behandlingFramføringTilSenereBehandlingSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType senereSteg);

    /**
     * Oppretter og håndterer nye aksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Oppretter og håndterer nye overstyringsaksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal
     * lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal
     * lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            Aksjonspunkt aksjonspunkt, String begrunnelse);

    /**
     * Lagrer og håndterer avbrutte aksjonspunkt
     */
    void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer reåpning av aksjonspunkt
     */
    void lagreAksjonspunkterReåpnet(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, boolean beholdToTrinnVurdering,
            boolean setTotrinn);

    /**
     * Lagrer og håndterer aksjonspunktresultater fra utledning utenom steg
     */
    void lagreAksjonspunktResultat(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktResultat> aksjonspunktResultater);

    /**
     * Lagrer og publiserer totrinns-setting for aksjonspunkt
     */
    void setAksjonspunktToTrinn(BehandlingskontrollKontekst kontekst, Aksjonspunkt aksjonspunkt, boolean totrinn);


    /**
     * Lagrer en ny behandling i behandlingRepository og fyrer av event om at en
     * Behandling er opprettet
     */
    void opprettBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling);

    /**
     * Opprett ny behandling for gitt fagsak og BehandlingType.
     * <p>
     * Vil alltid opprette ny behandling, selv om det finnes eksisterende åpen
     * behandling på fagsaken.
     *
     * @param fagsak               - fagsak med eller uten eksisterende behandling
     * @param behandlingType       - type behandling
     * @param behandlingOppdaterer - funksjon for oppdatering av grunnlag
     * @return Behandling - nylig opprettet og lagret.
     */
    Behandling opprettNyBehandling(Fagsak fagsak, BehandlingType behandlingType, Consumer<Behandling> behandlingOppdaterer);

    /**
     * Setter behandlingen på vent. NB IKKE BRUK FRA STEG
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param fristTid               Frist før Behandlingen å adresseres
     * @param venteårsak             Årsak til ventingen.
     *
     */
    Aksjonspunkt settBehandlingPåVentUtenSteg(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime fristTid,
            Venteårsak venteårsak);

    /**
     * Setter behandlingen på vent med angitt hvilket steg det står i. NB IKKE BRUK
     * FRA STEG
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param BehandlingStegType     stegType aksjonspunktet står i.
     * @param fristTid               Frist før Behandlingen å adresseres
     * @param venteårsak             Årsak til ventingen.
     *
     */
    Aksjonspunkt settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingStegType stegType,
            LocalDateTime fristTid,
            Venteårsak venteårsak);

    /**
     * Setter autopunkter av en spesifikk aksjonspunktdefinisjon til utført. Dette
     * klargjør kun behandligen for prosessering, men vil ikke drive prosessen
     * videre.
     *
     * @param aksjonspunktDefinisjon Aksjonspunktdefinisjon til de aksjonspunktene
     *                               som skal lukkes Bruk
     *                               {@link #prosesserBehandling(BehandlingskontrollKontekst)}
     *                               el. tilsvarende for det.
     */
    void settAutopunktTilUtført(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingskontrollKontekst kontekst);

    /**
     * Ny metode som forbereder en behandling for prosessering - setter autopunkt
     * til utført og evt tilbakeføring ved gjenopptak. Behandlingen skal være klar
     * til prosessering uten åpne autopunkt når kallet er ferdig.
     */
    void taBehandlingAvVentSetAlleAutopunktUtført(Behandling behandling, BehandlingskontrollKontekst kontekst);

    void taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(Behandling behandling, BehandlingskontrollKontekst kontekst);

    /** Henlegg en behandling. */
    void henleggBehandling(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsakKode);

    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerFraOgMed(Behandling behandling, BehandlingStegType steg, boolean medInngangOgså);

    void henleggBehandlingFraSteg(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak);

    /**
     * Sjekker i behandlingsmodellen om aksjonspunktet skal løses i eller etter det
     * angitte steget.
     *
     * @param behandling
     * @param behandlingSteg         steget som aksjonspunktet skal sjekkes mot
     * @param aksjonspunktDefinisjon aksjonspunktet som skal sjekkes
     * @return true dersom aksjonspunktet skal løses i eller etter det angitte
     *         steget.
     */
    boolean skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType behandlingSteg,
            AksjonspunktDefinisjon aksjonspunktDefinisjon);

    void fremoverTransisjon(BehandlingStegType målSteg, BehandlingskontrollKontekst kontekst);

    boolean inneholderSteg(Behandling behandling, BehandlingStegType registrerSøknad);

    int sammenlignRekkefølge(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB);

    boolean erStegPassert(Long behandlingId, BehandlingStegType stegType);

    boolean erStegPassert(Behandling behandling, BehandlingStegType stegType);

    boolean erIStegEllerSenereSteg(Long behandlingId, BehandlingStegType stegType);
}
