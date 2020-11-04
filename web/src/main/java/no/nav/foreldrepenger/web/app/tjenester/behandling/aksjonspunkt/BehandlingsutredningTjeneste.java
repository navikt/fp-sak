package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class BehandlingsutredningTjeneste {

    private Period defaultVenteFrist;
    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    BehandlingsutredningTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingsutredningTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                        BehandlingRepositoryProvider behandlingRepositoryProvider,
                                        OppgaveTjeneste oppgaveTjeneste,
                                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                        BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    /**
     * Hent behandlinger for angitt saksnummer (offisielt GSAK saksnummer)
     */
    public List<Behandling> hentBehandlingerForSaksnummer(Saksnummer saksnummer) {
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);
        return behandlinger;
    }

    public void settBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        if (venteårsak == null) {
            venteårsak = Venteårsak.UDEFINERT;
        }

        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak);
    }

    private void doSetBehandlingPåVent(Long behandlingsId, AksjonspunktDefinisjon apDef, LocalDate frist,
                                       Venteårsak venteårsak) {

        LocalDateTime fristTid = bestemFristForBehandlingVent(frist);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);
        oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
        BehandlingStegType behandlingStegFunnet = behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .map(Aksjonspunkt::getBehandlingStegFunnet)
            .orElse(null); // Dersom autopunkt ikke allerede er opprettet, så er det ikke tilknyttet steg
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, apDef, behandlingStegFunnet, fristTid, venteårsak);
    }

    public void endreBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);
        if (!behandling.isBehandlingPåVent()) {
            throw BehandlingsutredningTjenesteFeil.FACTORY.kanIkkeEndreVentefristForBehandlingIkkePaVent(behandlingsId)
                .toException();
        }
        if (venteårsak == null) {
            venteårsak = behandling.getVenteårsak();
        }
        AksjonspunktDefinisjon aksjonspunktDefinisjon = behandling.getBehandlingPåVentAksjonspunktDefinisjon();
        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak);
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    public void byttBehandlendeEnhet(Long behandlingId, OrganisasjonsEnhet enhet, String begrunnelse, HistorikkAktør aktør) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, aktør, begrunnelse);
    }

    public void kanEndreBehandling(Long behandlingId, Long versjon) {
        Boolean kanEndreBehandling = behandlingRepository.erVersjonUendret(behandlingId, versjon);
        if (!kanEndreBehandling) {
            throw BehandlingsutredningTjenesteFeil.FACTORY.endringerHarForekommetPåBehandlingen().toException();
        }
    }

    interface BehandlingsutredningTjenesteFeil extends DeklarerteFeil {
        BehandlingsutredningTjenesteFeil FACTORY = FeilFactory.create(BehandlingsutredningTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-992332", feilmelding = "BehandlingId %s er ikke satt på vent, og ventefrist kan derfor ikke oppdateres", løsningsforslag = "Forsett saksbehandlingen", logLevel = ERROR)
        Feil kanIkkeEndreVentefristForBehandlingIkkePaVent(Long behandlingId);

        @FunksjonellFeil(feilkode = "FP-837578", feilmelding = "Behandlingen er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet.", løsningsforslag = "Last inn behandlingen på nytt.", logLevel = WARN)
        Feil endringerHarForekommetPåBehandlingen();
    }
}
