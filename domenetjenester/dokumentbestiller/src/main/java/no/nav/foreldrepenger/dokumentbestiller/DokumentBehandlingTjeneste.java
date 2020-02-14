package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dokumentbestiller.dto.BrevmalDto;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class DokumentBehandlingTjeneste {
    private static final Period MANUELT_VENT_FRIST = Period.ofDays(28);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private FormidlingRestKlient formidlingRestKlient;
    private BehandlingDokumentRepository behandlingDokumentRepository;

    public DokumentBehandlingTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                      OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                      OppgaveTjeneste oppgaveTjeneste,
                                      FormidlingRestKlient formidlingRestKlient,
                                      BehandlingDokumentRepository behandlingDokumentRepository) {

        Objects.requireNonNull(repositoryProvider, "repositoryProvider");
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.formidlingRestKlient = formidlingRestKlient;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
    }

    public List<BrevmalDto> hentBrevmalerFor(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final List<no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto> brevmalDtos = formidlingRestKlient.hentBrevMaler(new BehandlingUuidDto(behandling.getUuid()));
        List<BrevmalDto> brevmalListe = new ArrayList<>();
        for (no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto brevmalDto : brevmalDtos) {
            brevmalListe.add(new BrevmalDto(brevmalDto.getKode(), brevmalDto.getNavn(), mapDokumentMalRestriksjon(brevmalDto.getRestriksjon().getKode()), brevmalDto.getTilgjengelig()));
        }
        return brevmalListe;
    }

    private DokumentMalRestriksjon mapDokumentMalRestriksjon(String restriksjon) {
        if (DokumentMalRestriksjon.ÅPEN_BEHANDLING.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.ÅPEN_BEHANDLING;
        } else if (DokumentMalRestriksjon.ÅPEN_BEHANDLING_IKKE_SENDT.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.ÅPEN_BEHANDLING_IKKE_SENDT;
        } else if (DokumentMalRestriksjon.REVURDERING.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.REVURDERING;
        } else {
            return DokumentMalRestriksjon.INGEN;
        }
    }

    public Optional<BehandlingDokumentEntitet> hentBehandlingDokumentHvisEksisterer(Long behandlingId) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
    }

    public void loggDokumentBestilt(Behandling behandling, DokumentMalType dokumentMalTypeKode) {
        BehandlingDokumentEntitet behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
            .orElse(BehandlingDokumentEntitet.Builder.ny().medBehandling(behandling.getId()).build());
        behandlingDokument.leggTilBestiltDokument(new BehandlingDokumentBestiltEntitet.Builder()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(dokumentMalTypeKode.getKode())
            .build());
        behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
    }

    public boolean erDokumentBestilt(Long behandlingId, DokumentMalType dokumentMalTypeKode) {
        Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (behandlingDokument.isPresent()) {
            boolean dokumentBestilt = behandlingDokument.get().getBestilteDokumenter().stream()
                .map(BehandlingDokumentBestiltEntitet::getDokumentMalType)
                .collect(Collectors.toList())
                .contains(dokumentMalTypeKode.getKode());
            if (dokumentBestilt) {
                return true;
            }
        }
        // TODO(JEJ): Fjerne etter migrering av data til Fpsak er utført (TFP-1404):
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return formidlingRestKlient.erDokumentProdusert(new DokumentProdusertDto(behandling.getUuid(), dokumentMalTypeKode.getKode()));
    }

    public void settBehandlingPåVent(Long behandlingId, Venteårsak venteårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            LocalDateTime.now().plus(MANUELT_VENT_FRIST), venteårsak);
    }

    private void opprettTaskAvsluttOppgave(Behandling behandling) {
        OppgaveÅrsak oppgaveÅrsak = behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK;
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaver).isPresent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak);
        } else if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.REGISTRER_SØKNAD, oppgaver).isPresent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, OppgaveÅrsak.REGISTRER_SØKNAD);
        }
    }

    public void utvidBehandlingsfristManuelt(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, finnNyFristManuelt(behandling));
    }

    void oppdaterBehandlingMedNyFrist(Behandling behandling, LocalDate nyFrist) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    LocalDate finnNyFristManuelt(Behandling behandling) {
        return FPDateUtil.iDag().plusWeeks(behandling.getType().getBehandlingstidFristUker());
    }

    public void utvidBehandlingsfristManueltMedlemskap(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, utledFristMedlemskap(behandling, finnAksjonspunktperiodeForVentPåFødsel()));

    }

    private Period finnAksjonspunktperiodeForVentPåFødsel() {
        AksjonspunktDefinisjon ventPåFødsel = AksjonspunktDefinisjon.VENT_PÅ_FØDSEL;
        return Period.parse(ventPåFødsel.getFristPeriode());
    }

    LocalDate utledFristMedlemskap(Behandling behandling, Period aksjonspunktPeriode) {
        LocalDate vanligFrist = finnNyFristManuelt(behandling);
        Optional<LocalDate> terminFrist = beregnTerminFrist(behandling, aksjonspunktPeriode);
        if (terminFrist.isPresent() && vanligFrist.isAfter(terminFrist.get()) && iFremtiden(terminFrist.get())) {
            return terminFrist.get();
        }
        return vanligFrist;
    }

    private boolean iFremtiden(LocalDate dato) {
        return dato.isAfter(FPDateUtil.iDag());
    }

    private Optional<LocalDate> beregnTerminFrist(Behandling behandling, Period aksjonspunktPeriode) {
        Optional<TerminbekreftelseEntitet> gjeldendeTerminBekreftelse = familieHendelseRepository.hentAggregat(behandling.getId())
            .getGjeldendeTerminbekreftelse();
        if (gjeldendeTerminBekreftelse.isPresent()) {
            LocalDate oppgittTermindato = gjeldendeTerminBekreftelse.get().getTermindato();
            return Optional.of(oppgittTermindato.plus(aksjonspunktPeriode));
        }
        return Optional.empty();
    }
}
