package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
class DtoTjenesteFelles {

    private static final Logger LOG = LoggerFactory.getLogger(DtoTjenesteFelles.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MottatteDokumentRepository dokumentRepository;

    @Inject
    DtoTjenesteFelles(BehandlingRepository behandlingRepository,
                      BehandlingVedtakRepository vedtakRepository,
                      FamilieHendelseTjeneste familieHendelseTjeneste,
                      MottatteDokumentRepository dokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vedtakRepository = vedtakRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.dokumentRepository = dokumentRepository;
    }

    DtoTjenesteFelles() {
        //CDI
    }

    Optional<BehandlingVedtak> finnGjeldendeVedtak(Fagsak fagsak) {
        var gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(fagsak);
        gjeldendeVedtak.ifPresentOrElse(v -> LOG.info("Fant gjeldende vedtak for sak {} {}", fagsak.getSaksnummer(), v.getId()),
            () -> LOG.info("Fant ikke et vedtak for sak {}", fagsak.getSaksnummer()));
        return gjeldendeVedtak;
    }

    List<MottattDokument> finnRelevanteSøknadsdokumenter(Fagsak fagsak) {
        return dokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId())
            .stream()
            .filter(MottattDokument::erSøknadsDokument)
            .filter(md -> md.getJournalpostId() != null)
            .filter(md -> md.getMottattTidspunkt() != null)
            .filter(md -> md.getBehandlingId() != null)
            .toList();
    }

    static SøknadStatus statusForSøknad(Optional<Behandling> åpenYtelseBehandling, Long behandlingId) {
        return åpenYtelseBehandling.filter(b -> b.getId().equals(behandlingId)).map(b -> SøknadStatus.MOTTATT).orElse(SøknadStatus.BEHANDLET);
    }

    List<Behandling> finnIkkeHenlagteBehandlinger(Fagsak fagsak) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());
    }

    Behandling finnBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    Set<Sak.Aksjonspunkt> finnAksjonspunkt(List<Behandling> ikkeHenlagteBehandlinger) {
        return ikkeHenlagteBehandlinger.stream().flatMap(b -> b.getÅpneAksjonspunkter().stream()).map(a -> {
            var type = switch (a.getAksjonspunktDefinisjon()) {
                case AUTO_MANUELT_SATT_PÅ_VENT -> Sak.Aksjonspunkt.Type.VENT_MANUELT_SATT;
                case AUTO_VENT_PÅ_FØDSELREGISTRERING -> Sak.Aksjonspunkt.Type.VENT_FØDSEL;
                case AUTO_VENTER_PÅ_KOMPLETT_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD;
                case AUTO_SATT_PÅ_VENT_REVURDERING -> Sak.Aksjonspunkt.Type.VENT_REVURDERING;
                case VENT_PGA_FOR_TIDLIG_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD;
                case AUTO_KØET_BEHANDLING -> Sak.Aksjonspunkt.Type.VENT_KØET_BEHANDLING;
                case VENT_PÅ_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_SØKNAD;
                case AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST -> Sak.Aksjonspunkt.Type.VENT_INNTEKT_RAPPORTERINGSFRIST;
                case AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> Sak.Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT;
                case AUTO_VENT_ETTERLYST_INNTEKTSMELDING -> Sak.Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING;
                case AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN -> Sak.Aksjonspunkt.Type.VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;
                case AUTO_VENT_PÅ_SYKEMELDING -> Sak.Aksjonspunkt.Type.VENT_SYKEMELDING;
                case AUTO_VENT_PÅ_KABAL_KLAGE -> Sak.Aksjonspunkt.Type.VENT_KABAL_KLAGE;
                case AUTO_VENT_PÅ_KABAL_ANKE -> Sak.Aksjonspunkt.Type.VENT_PÅ_KABAL_ANKE;
                default -> null;
            };

            var venteÅrsak = switch (a.getVenteårsak()) {
                case ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER -> Sak.Aksjonspunkt.Venteårsak.ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER;
                case AVV_DOK -> Sak.Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON;
                case AVV_FODSEL -> Sak.Aksjonspunkt.Venteårsak.AVVENT_FØDSEL;
                case AVV_RESPONS_REVURDERING -> Sak.Aksjonspunkt.Venteårsak.AVVENT_RESPONS_REVURDERING;
                case FOR_TIDLIG_SOKNAD -> Sak.Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD;
                case UTV_FRIST -> Sak.Aksjonspunkt.Venteårsak.UTVIDET_FRIST;
                case VENT_PÅ_BRUKERTILBAKEMELDING -> Sak.Aksjonspunkt.Venteårsak.BRUKERTILBAKEMELDING;
                case VENT_UTLAND_TRYGD -> Sak.Aksjonspunkt.Venteårsak.UTLAND_TRYGD;
                case VENT_INNTEKT_RAPPORTERINGSFRIST -> Sak.Aksjonspunkt.Venteårsak.INNTEKT_RAPPORTERINGSFRIST;
                case VENT_MANGLENDE_SYKEMELDING -> Sak.Aksjonspunkt.Venteårsak.MANGLENDE_SYKEMELDING;
                case VENT_OPDT_INNTEKTSMELDING -> Sak.Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING;
                case VENT_OPPTJENING_OPPLYSNINGER -> Sak.Aksjonspunkt.Venteårsak.OPPTJENING_OPPLYSNINGER;
                case VENT_SØKNAD_SENDT_INFORMASJONSBREV -> Sak.Aksjonspunkt.Venteårsak.SENDT_INFORMASJONSBREV;
                case VENT_ÅPEN_BEHANDLING -> Sak.Aksjonspunkt.Venteårsak.ÅPEN_BEHANDLING;
                case VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> Sak.Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT;
                default -> null;
            };
            return new Sak.Aksjonspunkt(type, venteÅrsak, a.getFristTid());
        }).filter(a -> a.type() != null).collect(Collectors.toSet());
    }

    boolean erAvsluttet(Fagsak fagsak) {
        return !fagsak.erÅpen();
    }

    Sak.FamilieHendelse finnFamilieHendelse(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeFamilieHendelse(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnBekreftetSøknadFamilieHendelse(åpenYtelseBehandling.get().getId()).orElse(null);
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnBekreftetSøknadFamilieHendelse(b.getId())).orElse(null);
    }

    Optional<Behandling> hentÅpenBehandling(Fagsak fagsak) {
        var åpenYtelseBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId())
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .max(Comparator.comparing(Behandling::getOpprettetTidspunkt));
        åpenYtelseBehandling.ifPresentOrElse(b -> LOG.info("Fant åpen ytelsebehandling for sak {} {}", fagsak.getSaksnummer(), b.getId()), () -> LOG.info("Ingen åpen ytelsebehandling for sak {}", fagsak.getSaksnummer()));
        return åpenYtelseBehandling;
    }

    private Optional<Sak.FamilieHendelse> finnBekreftetSøknadFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling).flatMap(agg -> {
            var versjon = agg.getBekreftetVersjon().orElseGet(agg::getSøknadVersjon);
            return tilDto(versjon);
        });
    }

    private Sak.FamilieHendelse finnGjeldendeFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling).flatMap(fhg -> tilDto(fhg.getGjeldendeVersjon())).orElse(null);
    }

    private static Optional<Sak.FamilieHendelse> tilDto(FamilieHendelseEntitet familieHendelse) {
        var fødselsdato = familieHendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder()).orElse(null);
        var termindato = familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var omsorgsovertakelse = familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null);
        if (fødselsdato == null && termindato == null && omsorgsovertakelse == null) {
            LOG.info("Familiehendelse uten fødselsdato, termindato eller omsorgsovertakelse: {}", familieHendelse);
            return Optional.empty();
        }
        return Optional.of(
            new Sak.FamilieHendelse(fødselsdato, termindato, familieHendelse.getAntallBarn() == null ? 0 : familieHendelse.getAntallBarn(),
                omsorgsovertakelse));
    }

    Stream<BehandlingVedtak> finnVedtakForFagsak(Fagsak fagsak) {
        var behandlingerMedVedtak = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        return behandlingerMedVedtak.stream()
            .map(b -> vedtakRepository.hentForBehandlingHvisEksisterer(b.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }
}
