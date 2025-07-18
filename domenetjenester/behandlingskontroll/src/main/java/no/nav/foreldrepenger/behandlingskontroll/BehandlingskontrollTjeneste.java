package no.nav.foreldrepenger.behandlingskontroll;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface BehandlingskontrollTjeneste {

    /**
     * Initierer ny behandlingskontroll for en behandling, som ikke nødvendigvis er lagret i
     * behandlingsRepository (og fått tildelt behandlingId)
     *
     * @param behandling - må være med
     * @param skriveLås - behandlingen må være låst - helst med behandlingId før den er lest opp fra behandlingRepository
     */
    BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling, BehandlingLås skriveLås);

    /**
     * Prosesser behandling fra dit den sist har kommet. Avhengig av vurderingspunkt
     * (inngang- og utgang-kriterier) vil steget kjøres på nytt.
     *
     * @param kontekst - kontekst for prosessering. Opprettes gjennom
     *                 {@link #initBehandlingskontroll(Behandling, BehandlingLås)}
     */
    void prosesserBehandling(BehandlingskontrollKontekst kontekst);

    /**
     * Prosesser forutsatt behandling er i angitt steg og status venter og steget.
     * Vil kalle gjenopptaSteg for angitt steg, senere vanlig framdrift
     *
     * @param kontekst           - kontekst for prosessering. Opprettes gjennom
     *                           {@link #initBehandlingskontroll(Behandling, BehandlingLås skriveLås)}
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
    Aksjonspunkt settBehandlingPåVentUtenSteg(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                              AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime fristTid, Venteårsak venteårsak);

    /**
     * Setter behandlingen på vent med angitt hvilket steg det står i. NB IKKE BRUK FRA STEG
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param stegType               stegType aksjonspunktet står i.
     * @param fristTid               Frist før Behandlingen å adresseres
     * @param venteårsak             Årsak til ventingen.
     *
     */
    Aksjonspunkt settBehandlingPåVent(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType stegType,
                                      AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime fristTid, Venteårsak venteårsak);

    /**
     * Setter autopunkter av en spesifikk aksjonspunktdefinisjon til utført.
     * Dette klargjør kun behandligen for prosessering, men vil ikke drive prosessen videre.
     * Disse metodene bør bare brukes ved eksplisitt behov - bruk heller {@link #taBehandlingAvVentSetAlleAutopunktUtført}
     *
     * @param behandling   Kontekst for prosessering med skrivelås for behandlingen
     * @param aksjonspunkt Åpent aksjonspunkt som skal lukkes (utføres eller avbrytes)
     */
    void settAutopunktTilUtført(BehandlingskontrollKontekst kontekst, Behandling behandling, Aksjonspunkt aksjonspunkt);

    void settAutopunktTilAvbrutt(BehandlingskontrollKontekst kontekst, Behandling behandling, Aksjonspunkt aksjonspunkt);

    /**
     * Ny metode som forbereder en behandling for prosessering
     * Setter autopunkt til utført og foretar tilbakeføring ved gjenopptak dersom autopunkt har tilbakehoppVedGjenopptakelse = true.
     * Behandlingen skal være klar til prosessering uten åpne autopunkt når kallet er ferdig.
     */
    void taBehandlingAvVentSetAlleAutopunktUtført(BehandlingskontrollKontekst kontekst, Behandling behandling);

    void taBehandlingAvVentSetAlleAutopunktAvbruttForHenleggelse(BehandlingskontrollKontekst kontekst, Behandling behandling);

    /** Henlegg en behandling. */
    void henleggBehandling(BehandlingskontrollKontekst kontekst);

    void henleggBehandlingFraSteg(BehandlingskontrollKontekst kontekst);

    void fremoverTransisjon(BehandlingskontrollKontekst kontekst, BehandlingStegType målSteg);
}
