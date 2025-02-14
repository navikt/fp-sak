package no.nav.foreldrepenger.kompletthet.implV2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
public class KompletthetsjekkerSøknad {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetsjekkerSøknad.class);
    private SøknadRepository søknadRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private ManglendeVedleggTjeneste manglendeVedleggTjeneste;
    private final Period ventefristForTidligSøknad = Period.ofWeeks(4);

    public KompletthetsjekkerSøknad() {
        //CDI
    }

    @Inject
    public KompletthetsjekkerSøknad(SøknadRepository søknadRepository,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    BehandlingVedtakRepository behandlingVedtakRepository,
                                    ManglendeVedleggTjeneste manglendeVedleggTjeneste) {
        this.søknadRepository = søknadRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.manglendeVedleggTjeneste = manglendeVedleggTjeneste;
    }

    public Optional<LocalDateTime> erSøknadMottattForTidlig(Skjæringstidspunkt stp) {
        var permisjonsstart = stp.getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            var ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            var erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                var ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    public boolean erSøknadMottattElektronisk(BehandlingReferanse ref) {
        return søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId())
                .map(SøknadEntitet::getElektroniskRegistrert)
                .orElse(false);
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


}
