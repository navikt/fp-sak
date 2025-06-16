package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class BehandlingsutredningTjeneste {

    private Period defaultVenteFrist;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    BehandlingsutredningTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingsutredningTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                        BehandlingRepositoryProvider behandlingRepositoryProvider,
                                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                        BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    /**
     * Hent behandlinger for angitt saksnummer (offisielt GSAK saksnummer)
     */
    public List<Behandling> hentBehandlingerForSaksnummer(Saksnummer saksnummer) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);
    }

    public void settBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak) {
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        if (venteårsak == null) {
            venteårsak = Venteårsak.UDEFINERT;
        }

        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak);
    }

    private void doSetBehandlingPåVent(Long behandlingsId, AksjonspunktDefinisjon apDef, LocalDate frist,
                                       Venteårsak venteårsak) {

        var fristTid = bestemFristForBehandlingVent(frist);

        var behandling = behandlingRepository.hentBehandling(behandlingsId);
        var behandlingStegFunnet = behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .map(Aksjonspunkt::getBehandlingStegFunnet)
            .orElse(null); // Dersom autopunkt ikke allerede er opprettet, så er det ikke tilknyttet steg
        behandlingProsesseringTjeneste.settBehandlingPåVent(behandling, behandlingStegFunnet, apDef, fristTid, venteårsak);
    }

    public void endreBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak) {
        var behandling = behandlingRepository.hentBehandling(behandlingsId);
        if (!behandling.isBehandlingPåVent()) {
            var msg = String.format("BehandlingId %s er ikke satt på vent, og ventefrist kan derfor ikke oppdateres",
                behandlingsId);
            throw new FunksjonellException("FP-992332", msg, "Forsett saksbehandlingen");
        }
        if (venteårsak == null) {
            venteårsak = behandling.getVenteårsak();
        }
        var aksjonspunktDefinisjon = behandling.getBehandlingPåVentAksjonspunktDefinisjon();
        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak);
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    public void byttBehandlendeEnhet(Long behandlingId, OrganisasjonsEnhet enhet, String begrunnelse, HistorikkAktør aktør) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, aktør, begrunnelse);
    }

    public void setAnsvarligSaksbehandlerFraKontekst(Behandling behandling) {
        var bruker = getCurrentUserId();
        if (bruker != null) {
            var lås = behandlingRepository.taSkriveLås(behandling);
            behandling.setAnsvarligSaksbehandler(bruker);
            behandlingRepository.lagre(behandling, lås);
        }
    }

    private String getCurrentUserId() {
        var kontekst = KontekstHolder.getKontekst();
        return kontekst.getIdentType().erSystem() ? null : kontekst.getUid();
    }

    public void kanEndreBehandling(Behandling behandling, Long versjon) {
        var kanEndreBehandling = behandlingRepository.erVersjonUendret(behandling.getId(), versjon);
        if (!kanEndreBehandling) {
            throw new BehandlingEndretException();
        }
    }
}
