package no.nav.foreldrepenger.behandling.steg.vedtak;

import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlFeil;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingSendtTilbakeTask;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class FatteVedtakTjeneste {

    public static final String UTVIKLER_FEIL_VEDTAK = "Utvikler-feil: Vedtak kan ikke fattes, behandlingsresultat er ";
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_REVURDERING = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET,
            BehandlingResultatType.OPPHØR, BehandlingResultatType.FORELDREPENGER_ENDRET,
            BehandlingResultatType.INGEN_ENDRING));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_KLAGE = new HashSet<>(
        Arrays.asList(BehandlingResultatType.KLAGE_AVVIST, BehandlingResultatType.KLAGE_MEDHOLD
            , BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET, BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET,
            BehandlingResultatType.DELVIS_MEDHOLD_I_KLAGE, BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE, BehandlingResultatType.UGUNST_MEDHOLD_I_KLAGE));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_ANKE = new HashSet<>(
        Arrays.asList(BehandlingResultatType.ANKE_AVVIST, BehandlingResultatType.ANKE_OMGJOER
            , BehandlingResultatType.ANKE_DELVIS_OMGJOERING_TIL_GUNST, BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET,
            BehandlingResultatType.ANKE_TIL_UGUNST, BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_INNSYN = new HashSet<>(
        Arrays.asList(BehandlingResultatType.INNSYN_AVVIST, BehandlingResultatType.INNSYN_DELVIS_INNVILGET, BehandlingResultatType.INNSYN_INNVILGET));

    private LagretVedtakRepository lagretVedtakRepository;
    private FatteVedtakXmlTjeneste vedtakXmlTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;

    FatteVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakTjeneste(LagretVedtakRepository vedtakRepository,
                        FatteVedtakXmlTjeneste vedtakXmlTjeneste,
                        VedtakTjeneste vedtakTjeneste,
                        OppgaveTjeneste oppgaveTjeneste,
                        TotrinnTjeneste totrinnTjeneste,
                        BehandlingVedtakTjeneste behandlingVedtakTjeneste) {
        this.lagretVedtakRepository = vedtakRepository;
        this.vedtakXmlTjeneste = vedtakXmlTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingVedtakTjeneste = behandlingVedtakTjeneste;
    }

    public BehandleStegResultat fattVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        verifiserBehandlingsresultat(behandling);
        if (behandling.isToTrinnsBehandling()) {
            Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, OppgaveÅrsak.GODKJENNE_VEDTAK, OpprettOppgaveForBehandlingSendtTilbakeTask.TASKTYPE);
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = totrinnaksjonspunktvurderinger.stream()
                    .filter(a -> !TRUE.equals(a.isGodkjent()))
                    .map(Totrinnsvurdering::getAksjonspunktDefinisjon).collect(Collectors.toList());

                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            } else {
                oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            }
        } else {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        }

        behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);

        opprettLagretVedtak(behandling);

        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

    private void verifiserBehandlingsresultat(Behandling behandling) { // NOSONAR dette er bare enkel verifisering og har ikke høy complexity
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            if (!VEDTAKSTILSTANDER_KLAGE.contains(behandlingsresultat.getBehandlingResultatType())) {
                throw new IllegalStateException(
                    UTVIKLER_FEIL_VEDTAK //$NON-NLS-1$
                        + (behandlingsresultat.getBehandlingResultatType().getNavn()));
            }
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            if (!VEDTAKSTILSTANDER_ANKE.contains(behandlingsresultat.getBehandlingResultatType())) {
                throw new IllegalStateException(
                    UTVIKLER_FEIL_VEDTAK //$NON-NLS-1$
                        + (behandlingsresultat.getBehandlingResultatType().getNavn()));
            }
        } else if (BehandlingType.INNSYN.equals(behandling.getType())) {
            if (!VEDTAKSTILSTANDER_INNSYN.contains(behandlingsresultat.getBehandlingResultatType())) {
                throw new IllegalStateException(
                    UTVIKLER_FEIL_VEDTAK //$NON-NLS-1$
                        + (behandlingsresultat.getBehandlingResultatType().getNavn()));
            }
        } else if (behandling.erRevurdering()) {
            if (!VEDTAKSTILSTANDER_REVURDERING.contains(behandlingsresultat.getBehandlingResultatType())) {
                throw new IllegalStateException(
                    UTVIKLER_FEIL_VEDTAK //$NON-NLS-1$
                        + (behandlingsresultat.getBehandlingResultatType().getNavn()));
            }
        } else if (behandlingsresultat == null || !VEDTAKSTILSTANDER.contains(behandlingsresultat.getBehandlingResultatType())) {
            throw new IllegalStateException(
                UTVIKLER_FEIL_VEDTAK //$NON-NLS-1$
                    + (behandlingsresultat == null ? "null" : behandlingsresultat.getBehandlingResultatType().getNavn()));
        }
    }

    private void opprettLagretVedtak(Behandling behandling) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return;
        }
        if (!erKlarForVedtak(behandling)) {
            throw VedtakXmlFeil.FACTORY.behandlingErIFeilTilstand(behandling.getId(), behandling.getStatus().getKode())
                .toException();
        }
        LagretVedtak lagretVedtak = LagretVedtak.builder()
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medXmlClob(vedtakXmlTjeneste.opprettVedtakXml(behandling.getId()))
            .build();
        lagretVedtakRepository.lagre(lagretVedtak);
    }

    private boolean erKlarForVedtak(Behandling behandling) {
        return BehandlingType.KLAGE.equals(behandling.getType()) || BehandlingType.ANKE.equals(behandling.getType()) || BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus());
    }

}
