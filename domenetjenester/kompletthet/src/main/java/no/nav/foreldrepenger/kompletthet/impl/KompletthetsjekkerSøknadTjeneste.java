package no.nav.foreldrepenger.kompletthet.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetsjekkerSøknadTjeneste {
    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    private static final Period VENTEFRIST_ETTERSENDELSE_FRA_MOTATT_DATO_UKER = Period.ofWeeks(1);
    private static final Period VENTEFRIST_FOR_MANGLENDE_SØKNAD = Period.ofWeeks(4);
    private static final Period VENTEFRIST_FOR_TIDLIG_SØKNAD = Period.ofWeeks(4);

    private SøknadRepository søknadRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private ManglendeVedleggTjeneste manglendeVedleggTjeneste;

    public KompletthetsjekkerSøknadTjeneste() {
        //CDI
    }

    @Inject
    public KompletthetsjekkerSøknadTjeneste(BehandlingRepositoryProvider provider, ManglendeVedleggTjeneste manglendeVedleggTjeneste) {
        this.søknadRepository = provider.getSøknadRepository();
        this.mottatteDokumentRepository = provider.getMottatteDokumentRepository();
        this.behandlingVedtakRepository = provider.getBehandlingVedtakRepository();
        this.manglendeVedleggTjeneste = manglendeVedleggTjeneste;
    }

    public Optional<LocalDateTime> erSøknadMottattForTidlig(Skjæringstidspunkt stp) {
        var permisjonsstart = stp.getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            var ventefrist = permisjonsstart.get().minus(VENTEFRIST_FOR_TIDLIG_SØKNAD);
            var erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                var ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    public boolean erSøknadMottatt(BehandlingReferanse ref) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
        var mottattSøknad = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.fagsakId())
                .stream()
                .filter(mottattDokument ->
                        DokumentTypeId.getSøknadTyper().contains(mottattDokument.getDokumentType()) ||
                        DokumentKategori.SØKNAD.equals(mottattDokument.getDokumentKategori()))
                .findFirst();
        // sjekker på både søknad og mottatte dokumenter siden søknad ikke lagres med en gang
        return søknad.isPresent() || mottattSøknad.isPresent();
    }

    public boolean endringssøknadErMottatt(Behandling behandling) {
        var vedtaksdato = behandlingVedtakRepository.hentBehandlingVedtakFraRevurderingensOriginaleBehandling(behandling).getVedtaksdato();
        var søknadOptional = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        return søknadOptional.isPresent() && søknadOptional.get().erEndringssøknad() && !søknadOptional.get().getMottattDato().isBefore(vedtaksdato);
    }

    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
        return manglendeVedleggTjeneste.utledManglendeVedleggForSøknad(ref);
    }

    public LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plus(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }

    public Optional<LocalDateTime> finnVentefristForManglendeVedlegg(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        Objects.requireNonNull(behandlingId, "behandlingId må være satt");
        var søknad = søknadRepository.hentSøknad(behandlingId);
        Objects.requireNonNull(søknad, "søknad kan ikke være null");

        var ønsketFrist = søknad.getMottattDato().plus(VENTEFRIST_ETTERSENDELSE_FRA_MOTATT_DATO_UKER);
        return finnVentefrist(ønsketFrist);
    }

    private Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            var ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

}
